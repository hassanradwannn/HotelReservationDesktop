import java.time.LocalDate;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import database.DatabaseConnection;
import exceptions.InvalidPaymentException;

public abstract class ReservationService {

    private static List<Room> rooms = Database.getRooms();
    private static List<Reservation> reservations = Database.getReservations();

    public static boolean isDateRangeValid(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            return false;
        }

        LocalDate today = SystemTime.getToday();

        if (checkIn.isBefore(today)) {
            return false;
        }
        if (!checkOut.isAfter(checkIn)) {
            return false;
        }

        return true;
    }
    
    public static boolean isRoomAvailable(Room room, LocalDate checkIn, LocalDate checkOut) {
        for (Reservation reservation : Database.getReservations()) {
            if (reservation.getRoom().getRoomNumber().equalsIgnoreCase(room.getRoomNumber())
                    && reservation.getStatus() != ReservationStatus.CANCELLED
                    && reservation.overlaps(checkIn, checkOut)) {
                return false;
            }
        }
        return true;
    }

    public static Reservation createReservation(Guest guest, Room room,
                                         LocalDate checkIn, LocalDate checkOut, boolean addGym) {
        if (hasOverlappingReservation(room, checkIn, checkOut)) {
    throw new IllegalArgumentException("Room is already reserved during this date range.");
}

        if (!isDateRangeValid(checkIn, checkOut)) {
            throw new IllegalArgumentException("Invalid reservation dates.");
        }

        LocalDate today = SystemTime.getToday();
        if (checkIn.isBefore(today)) {
            throw new IllegalArgumentException("Check-in date cannot be in the past.");
        }

        if (!isRoomAvailable(room, checkIn, checkOut)) {
            throw new IllegalArgumentException("Room is not available for the selected dates.");
        }

        String reservationId = "RES-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Reservation reservation = new Reservation(
                reservationId,
                guest,
                room,
                checkIn,
                checkOut,
                ReservationStatus.PENDING,
                addGym);

        reservation.setTotalPrice();
        reservations.add(reservation);

        // Save to database
        DatabaseSaver.saveReservation(
                reservation.getReservationId(),
                guest.getUsername(),
                room.getRoomNumber(),
                checkIn,
                checkOut,
                addGym,
                reservation.getStatus().toString());

          // deprecated check for check in date
      /*  if (checkIn.isEqual(today)) {
            reservation.setStatus(ReservationStatus.CONFIRMED);
            reservation.setDepositPaid(true);
            DatabaseSaver.updateReservationStatus(reservation.getReservationId(), ReservationStatus.CONFIRMED.toString());
        }*/

        return reservation;
    }

    public static void cancelReservation(String reservationId) {
        for (Reservation reservation : reservations) {
            if (reservation.getReservationId().equals(reservationId)) {
                
                // Refund the deposit back to the guest's balance if it was already paid
                if (reservation.isDepositPaid()) {
                    Guest guest = reservation.getGuest();
                    User freshData = UserDatabase.findUser(guest.getUsername());
                    if (freshData instanceof Guest dbGuest) {
                        guest.setBalance(dbGuest.getBalance());
                    }
                    double refundAmount = getDepositAmount(reservation);
                    guest.setBalance(guest.getBalance() + refundAmount);
                    DatabaseSaver.updateUserBalance(guest.getUsername(), guest.getBalance());
                    reservation.setDepositPaid(false);
                }

                reservation.setStatus(ReservationStatus.CANCELLED);
                DatabaseSaver.updateReservationStatus(reservationId, ReservationStatus.CANCELLED.toString());
                return;
            }
        }
        throw new IllegalArgumentException("Reservation not found.");
    }

    public static double getDepositAmount(Reservation reservation) {
        return reservation.getDepositAmount();
    }

    public static double getRemainingAmount(Reservation reservation) {
        return reservation.getRemainingAmount();
    }

    public static double getPaidAmount(Reservation reservation) {
        if (reservation == null) {
            return 0.0;
        }
        DatabaseSaver.ensureInvoiceSchema();

        String sql = """
            SELECT
                COALESCE(SUM(CASE
                    WHEN paid = TRUE AND UPPER(payment_method) NOT LIKE 'EARLY_CHECKOUT_REFUND%' THEN total_amount
                    ELSE 0
                END), 0) AS paid_total,
                COALESCE(MAX(CASE
                    WHEN paid = TRUE AND UPPER(payment_method) LIKE 'DEPOSIT%' THEN 1
                    ELSE 0
                END), 0) AS has_deposit_marker
            FROM invoices
            WHERE (
                    reservation_id = ?
                    OR (reservation_id IS NULL AND guest_username = ? AND room_number = ?)
                  )
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, reservation.getReservationId());
            stmt.setString(2, reservation.getGuest().getUsername());
            stmt.setString(3, reservation.getRoom().getRoomNumber());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return paidAmountWithDepositFallback(
                            reservation,
                            rs.getDouble("paid_total"),
                            rs.getInt("has_deposit_marker") == 1);
                }
            }
        } catch (Exception ex) {
            return reservation.isDepositPaid() ? reservation.getDepositAmount() : 0.0;
        }
        return paidAmountWithDepositFallback(reservation, 0.0, false);
    }

    public static double getEarlyCheckOutRefundAmount(Reservation reservation) {
        if (reservation == null) {
            return 0.0;
        }
        DatabaseSaver.ensureInvoiceSchema();

        String sql = """
            SELECT COALESCE(SUM(CASE
                WHEN total_amount < 0 THEN -total_amount
                ELSE total_amount
            END), 0) AS refund_total
            FROM invoices
            WHERE reservation_id = ?
              AND UPPER(payment_method) LIKE 'EARLY_CHECKOUT_REFUND%'
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, reservation.getReservationId());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Math.max(0, rs.getDouble("refund_total"));
                }
            }
        } catch (Exception ex) {
            System.out.println("Could not load early checkout refund: " + ex.getMessage());
        }
        return 0.0;
    }

    public static double getGrossPaidAmountBeforeRefunds(Reservation reservation) {
        if (reservation == null) {
            return 0.0;
        }
        DatabaseSaver.ensureInvoiceSchema();

        String sql = """
            SELECT COALESCE(SUM(CASE
                WHEN paid = TRUE AND total_amount > 0 THEN total_amount
                ELSE 0
            END), 0) AS paid_total
            FROM invoices
            WHERE (
                    reservation_id = ?
                    OR (reservation_id IS NULL AND guest_username = ? AND room_number = ?)
                  )
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, reservation.getReservationId());
            stmt.setString(2, reservation.getGuest().getUsername());
            stmt.setString(3, reservation.getRoom().getRoomNumber());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return paidAmountWithDepositFallback(reservation, rs.getDouble("paid_total"), false);
                }
            }
        } catch (Exception ex) {
            return getPaidAmount(reservation) + getEarlyCheckOutRefundAmount(reservation);
        }
        return getPaidAmount(reservation) + getEarlyCheckOutRefundAmount(reservation);
    }

    private static double paidAmountWithDepositFallback(Reservation reservation, double paidTotal, boolean hasDepositMarker) {
        double deposit = getDepositAmount(reservation);
        boolean depositWasPaid = reservation.isDepositPaid()
                || hasDepositMarker
                || statusImpliesDepositPaid(reservation.getStatus());

        if (depositWasPaid && paidTotal < deposit) {
            return deposit;
        }
        return paidTotal;
    }

    private static boolean statusImpliesDepositPaid(ReservationStatus status) {
        return status == ReservationStatus.CONFIRMED
                || status == ReservationStatus.CHECKING_IN
                || status == ReservationStatus.ONGOING
                || status == ReservationStatus.CHECKING_OUT;
    }

    public static boolean payDeposit(Reservation reservation, Guest guest) {
        return payDeposit(reservation, guest, PaymentMethod.ONLINE);
    }

    public static boolean payDeposit(Reservation reservation, Guest guest, PaymentMethod paymentMethod) {
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

        // Fetch fresh data from DB to reflect external SQL updates instantly
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
        DatabaseSaver.ensureInvoiceSchema();
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

        // reservation.setDepositPaid(true);
        // reservation.setStatus(ReservationStatus.CONFIRMED);
        // return true;
    }

    public static boolean requestCheckOut(Reservation reservation, Guest guest) {
        if (reservation == null || guest == null) {
            throw new IllegalArgumentException("Reservation and guest are required.");
        }
        if (reservation.getStatus() != ReservationStatus.ONGOING) {
            throw new IllegalArgumentException("Only ongoing reservations can request check-out.");
        }
        if (!reservation.getGuest().getUsername().equalsIgnoreCase(guest.getUsername())) {
            throw new IllegalArgumentException("This reservation does not belong to the current guest.");
        }

        applyActualCheckOutDateIfEarly(reservation);
        reservation.setStatus(ReservationStatus.CHECKING_OUT);
        DatabaseSaver.updateReservationStatus(reservation.getReservationId(), ReservationStatus.CHECKING_OUT.toString());
        return true;
    }

    public static boolean checkInGuest(Reservation reservation, Guest guest) {
        if (reservation.getStatus() != ReservationStatus.CONFIRMED
                && reservation.getStatus() != ReservationStatus.CHECKING_IN) {
            throw new IllegalArgumentException("Reservation must be CONFIRMED or CHECKING_IN to check in.");
        }

        if (!SystemTime.getToday().isEqual(reservation.getCheckInDate())) {
            throw new IllegalArgumentException("Check-in date has not arrived yet.");
        }

        reservation.setStatus(ReservationStatus.ONGOING);
        DatabaseSaver.updateReservationStatus(reservation.getReservationId(), ReservationStatus.ONGOING.toString());
        return true;
    }

    private static ReservationStatus depositPaidStatus(Reservation reservation) {
        return reservation.getCheckInDate() != null && reservation.getCheckInDate().isEqual(SystemTime.getToday())
                ? ReservationStatus.CHECKING_IN
                : ReservationStatus.CONFIRMED;
    }

    public static void markTodaysConfirmedReservationsCheckingIn() {
        LocalDate today = SystemTime.getToday();
        for (Reservation reservation : new ArrayList<>(Database.getReservations())) {
            if (reservation.getStatus() == ReservationStatus.CONFIRMED
                    && reservation.getCheckInDate() != null
                    && reservation.getCheckInDate().isEqual(today)) {
                reservation.setStatus(ReservationStatus.CHECKING_IN);
                DatabaseSaver.updateReservationStatus(reservation.getReservationId(), ReservationStatus.CHECKING_IN.toString());
            }
        }
    }

    public static boolean checkOutGuest(Reservation reservation, PaymentMethod paymentMethod) {
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
        
        // Fetch fresh data from DB to reflect external SQL updates instantly
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
                DatabaseSaver.saveInvoice(reservation.getReservationId(), guest.getUsername(), reservation.getRoom().getRoomNumber(), remaining, paymentMethod.toString(), true);
            } catch (InvalidPaymentException e) {
                guest.setBalance(guest.getBalance() + remaining);
                DatabaseSaver.updateUserBalance(guest.getUsername(), guest.getBalance()); // Rollback DB on fail
                throw new IllegalArgumentException("Checkout payment failed: " + e.getMessage());
            }
        }

        reservation.setFullPaid(true);
        reservation.setStatus(ReservationStatus.COMPLETED);
        DatabaseSaver.updateReservationStatus(reservation.getReservationId(), ReservationStatus.COMPLETED.toString());
        return true;
    }

    private static void applyActualCheckOutDateIfEarly(Reservation reservation) {
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

    private static void refundOverpaymentIfNeeded(Reservation reservation, Guest guest) {
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
        DatabaseSaver.saveInvoice(
                reservation.getReservationId(),
                guest.getUsername(),
                reservation.getRoom().getRoomNumber(),
                refund,
                "EARLY_CHECKOUT_REFUND",
                false);
    }

    public static boolean extendStay(Reservation reservation, LocalDate newCheckOut) {
        if (reservation.getStatus() != ReservationStatus.ONGOING) {
            throw new IllegalArgumentException("Only ongoing stays can be extended.");
        }
        if (newCheckOut == null || !newCheckOut.isAfter(reservation.getCheckOutDate())) {
            throw new IllegalArgumentException("New check-out date must be after the current check-out date.");
        }

        for (Reservation other : Database.getReservations()) {
            if (other == reservation || other.getReservationId().equals(reservation.getReservationId())) {
                continue;
            }
            if (!other.getRoom().getRoomNumber().equals(reservation.getRoom().getRoomNumber())) {
                continue;
            }
            if (other.getStatus() == ReservationStatus.CANCELLED || other.getStatus() == ReservationStatus.COMPLETED) {
                continue;
            }
            boolean overlap = reservation.getCheckOutDate().isBefore(other.getCheckOutDate())
                    && newCheckOut.isAfter(other.getCheckInDate());
            if (overlap) {
                throw new IllegalArgumentException("Room is already reserved during the requested extension.");
            }
        }

        reservation.setCheckOutDate(newCheckOut);
        reservation.setTotalPrice();
        DatabaseSaver.updateReservationDates(reservation.getReservationId(), reservation.getCheckInDate(), newCheckOut);
        return true;
    }

    public static List<Room> searchAvailableRooms(LocalDate checkIn, LocalDate checkOut,
            RoomType requestedType, int guests, List<Amenity> requestedAmenities) {
        ArrayList<Room> availableRooms = new ArrayList<>();
        if (!isDateRangeValid(checkIn, checkOut)) {
            return availableRooms;
        }

        for (Room room : Database.getRooms()) {
            if (requestedType == null
                    || room.getRoomType() == null
                    || !room.getRoomType().getName().equalsIgnoreCase(requestedType.getName())) {
                continue;
            }

            if (room.getRoomType().getCapacity() < guests) {
                continue;
            }

            // Filter by requested amenities
            if (requestedAmenities != null && !requestedAmenities.isEmpty()) {
                boolean hasAll = true;
                List<String> roomAmenityNames = room.getAmenities().stream().map(Amenity::getName).toList();
                for (Amenity reqAm : requestedAmenities) {
                    if (!roomAmenityNames.contains(reqAm.getName())) {
                        hasAll = false;
                        break;
                    }
                }
                if (!hasAll) continue;
            }

            if (isRoomAvailable(room, checkIn, checkOut)) {
                availableRooms.add(room);
            }
        }

        return availableRooms;
    }

    // Cancel reservations whose check-in date has passed and are not ongoing/completed/cancelled
    public static void cancelOverdueReservations() {
        LocalDate today = SystemTime.getToday();
        for (Reservation reservation : new ArrayList<>(reservations)) {

            // Only confirm PENDING reservations for today if deposit has been paid
            if (reservation.getStatus() == ReservationStatus.PENDING && reservation.getCheckInDate().isEqual(today)) {
                if (reservation.isDepositPaid()) {
                    reservation.setStatus(ReservationStatus.CONFIRMED);
                    DatabaseSaver.updateReservationStatus(reservation.getReservationId(), ReservationStatus.CONFIRMED.toString());
                }
            }

            // Cancel if deposit is overdue (after a day)
            if (reservation.getStatus() == ReservationStatus.PENDING && reservation.isDepositOverdue()) {
                reservation.setStatus(ReservationStatus.CANCELLED);
                DatabaseSaver.updateReservationStatus(reservation.getReservationId(), ReservationStatus.CANCELLED.toString());
                System.out.println("Reservation " + reservation.getReservationId() + " has been cancelled due to unpaid deposit.");
            }

            if (reservation.getStatus() == ReservationStatus.ONGOING
                    && reservation.getCheckOutDate() != null
                    && reservation.getCheckOutDate().isEqual(today)) {
                reservation.setStatus(ReservationStatus.CHECKING_OUT);
                DatabaseSaver.updateReservationStatus(reservation.getReservationId(), ReservationStatus.CHECKING_OUT.toString());
            }

            if (reservation.getStatus() == ReservationStatus.CANCELLED
                    || reservation.getStatus() == ReservationStatus.ONGOING
                    || reservation.getStatus() == ReservationStatus.CHECKING_OUT
                    || reservation.getStatus() == ReservationStatus.COMPLETED) {
                continue;
            }

            LocalDate checkIn = reservation.getCheckInDate();
            if (checkIn != null && checkIn.isBefore(today)) {
                reservation.setStatus(ReservationStatus.CANCELLED);
                DatabaseSaver.updateReservationStatus(reservation.getReservationId(), ReservationStatus.CANCELLED.toString());
                System.out.println("Reservation " + reservation.getReservationId() + " has been cancelled due to missed check-in (date passed: " + checkIn + ").");
            }
        }
    }
    public static boolean hasOverlappingReservation(Room room, LocalDate checkIn, LocalDate checkOut) {
    for (Reservation r : Database.getReservations()) {

        if (!r.getRoom().equals(room)) {
            continue;
        }

        if (r.getStatus() == ReservationStatus.CANCELLED ||
            r.getStatus() == ReservationStatus.COMPLETED) {
            continue;
        }

        boolean overlap = checkIn.isBefore(r.getCheckOutDate()) &&
                          checkOut.isAfter(r.getCheckInDate());

        if (overlap) {
            return true;
        }
    }

    return false;
}
}
