import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import exceptions.InvalidPaymentException;

public abstract class ReservationService {

    private static ArrayList<Room> rooms = Database.getRooms();
    private static ArrayList<Reservation> reservations = Database.getReservations();

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

    // Update the method signature to include the boolean
    public static Reservation createReservation(Guest guest, Room room,
                                         LocalDate checkIn, LocalDate checkOut, boolean addGym) {

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

        // For same-day reservations, confirm immediately and no deposit required
        if (checkIn.isEqual(today)) {
            reservation.setStatus(ReservationStatus.CONFIRMED);
        }

        return reservation;
    }

    public static void cancelReservation(String reservationId) {
        for (Reservation reservation : reservations) {
            if (reservation.getReservationId().equals(reservationId)) {
                reservation.setStatus(ReservationStatus.CANCELLED);
                return;
            }
        }
        throw new IllegalArgumentException("Reservation not found.");
    }

    public static double getDepositAmount(Reservation reservation) {
        if (reservation.getCheckInDate() != null && reservation.getCheckInDate().isEqual(SystemTime.getToday())) {
            return 0.0;
        }
        return reservation.getTotalPrice() * 0.25;
    }

    public static double getRemainingAmount(Reservation reservation) {
        if (reservation.getCheckInDate() != null && reservation.getCheckInDate().isEqual(SystemTime.getToday())) {
            return reservation.getTotalPrice();
        }
        return reservation.getTotalPrice() * 0.75;
    }

    public static boolean payDeposit(Reservation reservation, Guest guest) {
        if (reservation.isDepositPaid()) {
            throw new IllegalArgumentException("Deposit already paid.");
        }

        double deposit = getDepositAmount(reservation);
        if (guest.getBalance() < deposit) {
            throw new IllegalArgumentException("Insufficient balance. Need $" + deposit + ", have $" + guest.getBalance());
        }

        guest.setBalance(guest.getBalance() - deposit);
        reservation.setDepositPaid(true);
        reservation.setStatus(ReservationStatus.CONFIRMED);
        return true;
    }

    public static boolean checkInGuest(Reservation reservation, Guest guest) {
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new IllegalArgumentException("Reservation must be CONFIRMED to check in.");
        }

        if (!SystemTime.getToday().isEqual(reservation.getCheckInDate())) {
            throw new IllegalArgumentException("Check-in date has not arrived yet.");
        }

        double requiredAtCheckIn = getRemainingAmount(reservation);
        if (guest.getBalance() < requiredAtCheckIn)  {
            cancelReservation(reservation.getReservationId());
            throw new IllegalArgumentException("Insufficient balance, you need $" + (requiredAtCheckIn - guest.getBalance()) + " more to check in.");
        }

        reservation.setStatus(ReservationStatus.ONGOING);
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
        double remaining = getRemainingAmount(reservation);
        if (guest.getBalance() < remaining) {
            throw new IllegalArgumentException("Insufficient balance for checkout payment. Need $" + remaining + ", have $" + guest.getBalance());
        }

        guest.setBalance(guest.getBalance() - remaining);
        try {
            Invoice invoice = new Invoice(remaining, paymentMethod);
            invoice.processPayment();
        } catch (InvalidPaymentException e) {
            guest.setBalance(guest.getBalance() + remaining);
            throw new IllegalArgumentException("Checkout payment failed: " + e.getMessage());
        }

        reservation.setFullPaid(true);
        reservation.setStatus(ReservationStatus.COMPLETED);
        return true;
    }

    public static List<Room> searchAvailableRooms(LocalDate checkIn, LocalDate checkOut,
            RoomType requestedType, int guests) {
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

            if (isRoomAvailable(room, checkIn, checkOut)) {
                availableRooms.add(room);
            }
        }

        return availableRooms;
    }
}