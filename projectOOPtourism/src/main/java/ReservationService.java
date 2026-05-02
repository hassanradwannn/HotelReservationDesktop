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
        for (Reservation reservation : reservations) {
            if (reservation.getRoom().getRoomNumber() == room.getRoomNumber()
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
        String sql = """
            SELECT COALESCE(SUM(total_amount), 0) AS paid_total
            FROM invoices
            WHERE paid = TRUE
              AND (
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
                    return rs.getDouble("paid_total");
                }
            }
        } catch (Exception ex) {
            return reservation.isDepositPaid() ? reservation.getDepositAmount() : 0.0;
        }
        return 0.0;
    }

    public static boolean payDeposit(Reservation reservation, Guest guest) {
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

        guest.setBalance(guest.getBalance() - deposit);
        DatabaseSaver.updateUserBalance(guest.getUsername(), guest.getBalance());
        reservation.setDepositPaid(true);
        reservation.setStatus(ReservationStatus.CONFIRMED);
        DatabaseSaver.updateReservationStatus(reservation.getReservationId(), ReservationStatus.CONFIRMED.toString());

        // Log this transaction as an invoice
        try {
            Invoice depositInvoice = new Invoice(deposit, PaymentMethod.ONLINE);
            DatabaseSaver.saveInvoice(reservation.getReservationId(), guest.getUsername(), reservation.getRoom().getRoomNumber(), deposit, "DEPOSIT_ONLINE", true);
        } catch (Exception e) {}

        return true;

        // reservation.setDepositPaid(true);
        // reservation.setStatus(ReservationStatus.CONFIRMED);
        // return true;
    }

    public static boolean checkInGuest(Reservation reservation, Guest guest) {
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new IllegalArgumentException("Reservation must be CONFIRMED to check in.");
        }

        if (!SystemTime.getToday().isEqual(reservation.getCheckInDate())) {
            throw new IllegalArgumentException("Check-in date has not arrived yet.");
        }

        // Fetch fresh data from DB to reflect external SQL updates instantly
        User freshData = UserDatabase.findUser(guest.getUsername());
        if (freshData instanceof Guest dbGuest) {
            guest.setBalance(dbGuest.getBalance());
        }

        double requiredAtCheckIn = getRemainingAmount(reservation);
        if (guest.getBalance() < requiredAtCheckIn)  {
            throw new IllegalArgumentException("Insufficient balance, you need $" + (requiredAtCheckIn - guest.getBalance()) + " more to check in.");
        }

        reservation.setStatus(ReservationStatus.ONGOING);
        DatabaseSaver.updateReservationStatus(reservation.getReservationId(), ReservationStatus.ONGOING.toString());
        return true;
    }

    public static boolean checkOutGuest(Reservation reservation, PaymentMethod paymentMethod) {
        if (reservation.getStatus() != ReservationStatus.ONGOING) {
            throw new IllegalArgumentException("Reservation must be ONGOING to check out.");
        }

        if (paymentMethod == null) {
            throw new IllegalArgumentException("Payment method must be provided.");
        }

        Guest guest = reservation.getGuest();
        
        // Fetch fresh data from DB to reflect external SQL updates instantly
        User freshData = UserDatabase.findUser(guest.getUsername());
        if (freshData instanceof Guest dbGuest) {
            guest.setBalance(dbGuest.getBalance());
        }

        double remaining = Math.max(0, reservation.getTotalPrice() - getPaidAmount(reservation));
        if (guest.getBalance() < remaining) {
            throw new IllegalArgumentException("Insufficient balance for checkout payment. Need $" + remaining + ", have $" + guest.getBalance());
        }

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

        reservation.setFullPaid(true);
        reservation.setStatus(ReservationStatus.COMPLETED);
        DatabaseSaver.updateReservationStatus(reservation.getReservationId(), ReservationStatus.COMPLETED.toString());
        return true;
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

        for (Room room : rooms) {
            if (room.getRoomType() != requestedType) {
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

            if (reservation.getStatus() == ReservationStatus.CANCELLED
                    || reservation.getStatus() == ReservationStatus.ONGOING
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
