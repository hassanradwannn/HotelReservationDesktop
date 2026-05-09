package Services;


import Controllers.*;
import Models.*;
import Repositories.*;
import Utils.*;
import Utils.exceptions.*;
import Repositories.database.DatabaseConnection;
import Repositories.DatabaseInitializer.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Repositories.database.DatabaseConnection;
import Utils.exceptions.InvalidPaymentException;

public class ReservationPaymentService {
    private final InvoiceRepository invoiceRepository;

    public ReservationPaymentService() {
        this(new InvoiceRepository());
    }

    public ReservationPaymentService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    public double getDepositAmount(Reservation reservation) {
        return reservation.getDepositAmount();
    }

    public double getRemainingAmount(Reservation reservation) {
        return reservation.getRemainingAmount();
    }

    public double getPaidAmount(Reservation reservation) {
        if (reservation == null) {
            return 0.0;
        }

        invoiceRepository.ensureSchema();
        try {
            InvoiceRepository.PaidAmountRecord paidAmount = invoiceRepository.findPaidAmount(reservation);
            return paidAmountWithDepositFallback(
                    reservation,
                    paidAmount.getPaidTotal(),
                    paidAmount.hasDepositMarker());
        } catch (Exception ex) {
            return reservation.isDepositPaid() ? reservation.getDepositAmount() : 0.0;
        }
    }

    public double getEarlyCheckOutRefundAmount(Reservation reservation) {
        if (reservation == null) {
            return 0.0;
        }

        invoiceRepository.ensureSchema();
        try {
            return invoiceRepository.findEarlyCheckOutRefundAmount(reservation);
        } catch (Exception ex) {
            System.out.println("Could not load early checkout refund: " + ex.getMessage());
        }
        return 0.0;
    }

    public double getGrossPaidAmountBeforeRefunds(Reservation reservation) {
        if (reservation == null) {
            return 0.0;
        }

        invoiceRepository.ensureSchema();
        try {
            return paidAmountWithDepositFallback(
                    reservation,
                    invoiceRepository.findGrossPaidAmountBeforeRefunds(reservation),
                    false);
        } catch (Exception ex) {
            return getPaidAmount(reservation) + getEarlyCheckOutRefundAmount(reservation);
        }
    }

    public Map<String, ReservationPaymentSummary> getPaymentSummaries(List<Reservation> reservations) {
        Map<String, ReservationPaymentSummary> summaries = new HashMap<>();
        if (reservations == null || reservations.isEmpty()) {
            return summaries;
        }

        invoiceRepository.ensureSchema();
        try {
            Map<String, ReservationPaymentSummary> rawSummaries = invoiceRepository.findPaymentSummaries(reservations);
            for (Reservation reservation : reservations) {
                ReservationPaymentSummary rawSummary = rawSummaries.getOrDefault(
                        reservation.getReservationId(),
                        new ReservationPaymentSummary(0.0, 0.0));
                summaries.put(
                        reservation.getReservationId(),
                        new ReservationPaymentSummary(
                                paidAmountWithDepositFallback(
                                        reservation,
                                        rawSummary.getGrossPaidAmountBeforeRefunds(),
                                        false),
                                rawSummary.getEarlyCheckOutRefundAmount()));
            }
        } catch (Exception ex) {
            System.out.println("Could not load payment summaries: " + ex.getMessage());
            for (Reservation reservation : reservations) {
                summaries.put(
                        reservation.getReservationId(),
                        new ReservationPaymentSummary(
                                paidAmountWithDepositFallback(reservation, 0.0, false),
                                0.0));
            }
        }
        return summaries;
    }

    public boolean payDeposit(Reservation reservation, Guest guest) {
        return payDeposit(reservation, guest, PaymentMethod.ONLINE);
    }

    public boolean payDeposit(Reservation reservation, Guest guest, PaymentMethod paymentMethod) {
        if (reservation == null || guest == null) {
            throw new IllegalArgumentException("Reservation and guest are required.");
        }
        if (paymentMethod == null) {
            throw new IllegalArgumentException("Payment method must be provided.");
        }
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalArgumentException("Only pending reservations can receive a deposit.");
        }
        if (reservation.isDepositPaid()) {
            throw new IllegalArgumentException("Deposit already paid.");
        }

        User freshData = UserDatabase.findUser(guest.getUsername());
        if (freshData instanceof Guest dbGuest) {
            guest.setBalance(dbGuest.getBalance());
        }

        double deposit = getDepositAmount(reservation);
        if (guest.getBalance() < deposit) {
            throw new IllegalArgumentException(
                    "Insufficient balance. Need $" + deposit + ", have $" + guest.getBalance());
        }

        double newBalance = guest.getBalance() - deposit;
        invoiceRepository.ensureSchema();
        try (Connection conn = DatabaseConnection.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            try {
                try (PreparedStatement stmt = conn.prepareStatement("UPDATE users SET balance = ? WHERE username = ?")) {
                    stmt.setDouble(1, newBalance);
                    stmt.setString(2, guest.getUsername());
                    if (stmt.executeUpdate() == 0) {
                        throw new IllegalArgumentException("Guest account could not be found.");
                    }
                }

                ReservationStatus nextStatus = depositPaidStatus(reservation);
                try (PreparedStatement stmt = conn.prepareStatement("UPDATE reservations SET status = ? WHERE reservation_id = ?")) {
                    stmt.setString(1, nextStatus.toString());
                    stmt.setString(2, reservation.getReservationId());
                    if (stmt.executeUpdate() == 0) {
                        throw new IllegalArgumentException("Reservation could not be found.");
                    }
                }

                String invoiceSql = """
                    INSERT INTO invoices
                    (reservation_id, guest_username, room_number, total_amount, payment_method, paid, payment_date)
                    VALUES (?, ?, ?, ?, ?, ?, CURRENT_DATE)
                """;
                try (PreparedStatement stmt = conn.prepareStatement(invoiceSql)) {
                    stmt.setString(1, reservation.getReservationId());
                    stmt.setString(2, guest.getUsername());
                    stmt.setString(3, reservation.getRoom().getRoomNumber());
                    stmt.setDouble(4, deposit);
                    stmt.setString(5, "DEPOSIT_" + paymentMethod);
                    stmt.setBoolean(6, true);
                    stmt.executeUpdate();
                }

                conn.commit();
                conn.setAutoCommit(originalAutoCommit);
            } catch (Exception ex) {
                conn.rollback();
                conn.setAutoCommit(originalAutoCommit);
                throw ex;
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException("Deposit payment failed: " + ex.getMessage(), ex);
        }

        guest.setBalance(newBalance);
        reservation.setDepositPaid(true);
        reservation.setStatus(depositPaidStatus(reservation));
        try {
            new Invoice(deposit, paymentMethod);
        } catch (Exception ignored) {
        }
        Database.notifyDataChanged();

        return true;
    }

    public boolean checkOutGuest(Reservation reservation, PaymentMethod paymentMethod) {
        if (reservation.getStatus() != ReservationStatus.ONGOING
                && reservation.getStatus() != ReservationStatus.CHECKING_OUT) {
            throw new IllegalArgumentException("Reservation must be ONGOING or CHECKING_OUT to check out.");
        }

        if (paymentMethod == null) {
            throw new IllegalArgumentException("Payment method must be provided.");
        }

        Guest guest = reservation.getGuest();
        if (reservation.getStatus() == ReservationStatus.ONGOING) {
            applyActualCheckOutDateIfEarly(reservation);
        }
        refundOverpaymentIfNeeded(reservation, guest);

        User freshData = UserDatabase.findUser(guest.getUsername());
        if (freshData instanceof Guest dbGuest) {
            guest.setBalance(dbGuest.getBalance());
        }

        double remaining = Math.max(0, reservation.getTotalPrice() - getPaidAmount(reservation));
        if (guest.getBalance() < remaining) {
            throw new IllegalArgumentException("Insufficient balance for checkout payment. Need $" + remaining + ", have $" + guest.getBalance());
        }

        if (remaining > 0.009) {
            guest.setBalance(guest.getBalance() - remaining);
            DatabaseSaver.updateUserBalance(guest.getUsername(), guest.getBalance());
            try {
                Invoice invoice = new Invoice(remaining, paymentMethod);
                invoice.processPayment();
                invoiceRepository.saveInvoice(reservation.getReservationId(), guest.getUsername(), reservation.getRoom().getRoomNumber(), remaining, paymentMethod.toString(), true);
            } catch (InvalidPaymentException e) {
                guest.setBalance(guest.getBalance() + remaining);
                DatabaseSaver.updateUserBalance(guest.getUsername(), guest.getBalance());
                throw new IllegalArgumentException("Checkout payment failed: " + e.getMessage());
            }
        }

        reservation.setFullPaid(true);
        reservation.setStatus(ReservationStatus.COMPLETED);
        DatabaseSaver.updateReservationStatus(reservation.getReservationId(), ReservationStatus.COMPLETED.toString());
        return true;
    }

    public void lateCheckOutFee(Reservation reservation, Guest guest){
        LocalDate today = SystemTime.getToday();
        if (reservation.getStatus() != ReservationStatus.ONGOING && reservation.getRemainingAmount() > 0 && reservation.getCheckOutDate().isBefore(today)){
            double price = reservation.getTotalPrice();
            price += (10/100 * price);
        }
    }

    private double paidAmountWithDepositFallback(Reservation reservation, double paidTotal, boolean hasDepositMarker) {
        double deposit = getDepositAmount(reservation);
        boolean depositWasPaid = reservation.isDepositPaid()
                || hasDepositMarker
                || statusImpliesDepositPaid(reservation.getStatus());

        if (depositWasPaid && paidTotal < deposit) {
            return deposit;
        }
        return paidTotal;
    }

    private boolean statusImpliesDepositPaid(ReservationStatus status) {
        return status == ReservationStatus.CONFIRMED
                || status == ReservationStatus.CHECKING_IN
                || status == ReservationStatus.ONGOING
                || status == ReservationStatus.CHECKING_OUT;
    }

    private ReservationStatus depositPaidStatus(Reservation reservation) {
        return reservation.getCheckInDate() != null && reservation.getCheckInDate().isEqual(SystemTime.getToday())
                ? ReservationStatus.CHECKING_IN
                : ReservationStatus.CONFIRMED;
    }

    private void applyActualCheckOutDateIfEarly(Reservation reservation) {
        LocalDate today = SystemTime.getToday();
        if (reservation.getCheckInDate() == null || reservation.getCheckOutDate() == null) {
            return;
        }
        if (today.isBefore(reservation.getCheckInDate()) || !today.isBefore(reservation.getCheckOutDate())) {
            return;
        }

        LocalDate actualCheckOut = today.isEqual(reservation.getCheckInDate())
                ? reservation.getCheckInDate().plusDays(1)
                : today;
        reservation.setCheckOutDate(actualCheckOut);
        reservation.setTotalPrice();
        DatabaseSaver.updateReservationDates(
                reservation.getReservationId(),
                reservation.getCheckInDate(),
                actualCheckOut);
    }

    private void refundOverpaymentIfNeeded(Reservation reservation, Guest guest) {
        if (getEarlyCheckOutRefundAmount(reservation) > 0.009) {
            return;
        }

        double paid = getPaidAmount(reservation);
        double refund = paid - reservation.getTotalPrice();
        if (refund <= 0.009) {
            return;
        }

        User freshData = UserDatabase.findUser(guest.getUsername());
        if (freshData instanceof Guest dbGuest) {
            guest.setBalance(dbGuest.getBalance());
        }

        guest.setBalance(guest.getBalance() + refund);
        DatabaseSaver.updateUserBalance(guest.getUsername(), guest.getBalance());
        invoiceRepository.saveInvoice(
                reservation.getReservationId(),
                guest.getUsername(),
                reservation.getRoom().getRoomNumber(),
                refund,
                "EARLY_CHECKOUT_REFUND",
                false);
    }
}
