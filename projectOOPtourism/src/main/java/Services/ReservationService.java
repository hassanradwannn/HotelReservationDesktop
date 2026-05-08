package Services;


import Controllers.*;
import Models.*;
import Repositories.*;
import Utils.*;
import Utils.exceptions.*;
import Repositories.database.DatabaseConnection;
import Repositories.DatabaseInitializer.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class ReservationService {

    private static final AvailabilityService AVAILABILITY_SERVICE = new AvailabilityService();
    private static final ReservationPaymentService PAYMENT_SERVICE = new ReservationPaymentService();

    private static List<Room> rooms = Database.getRooms();
    private static List<Reservation> reservations = Database.getReservations();

    public static boolean isDateRangeValid(LocalDate checkIn, LocalDate checkOut) {
        return AVAILABILITY_SERVICE.isDateRangeValid(checkIn, checkOut);
    }
    
    public static boolean isRoomAvailable(Room room, LocalDate checkIn, LocalDate checkOut) {
        return AVAILABILITY_SERVICE.isRoomAvailable(room, checkIn, checkOut);
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

        DatabaseSaver.saveReservation(
                reservation.getReservationId(),
                guest.getUsername(),
                room.getRoomNumber(),
                checkIn,
                checkOut,
                addGym,
                reservation.getStatus().toString());

        return reservation;
    }

    public static void cancelReservation(String reservationId) {
        for (Reservation reservation : reservations) {
            if (reservation.getReservationId().equals(reservationId)) {

                // Refund any paid deposit before marking the reservation cancelled.
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
        return PAYMENT_SERVICE.getPaidAmount(reservation);
    }

    public static double getEarlyCheckOutRefundAmount(Reservation reservation) {
        return PAYMENT_SERVICE.getEarlyCheckOutRefundAmount(reservation);
    }

    public static double getGrossPaidAmountBeforeRefunds(Reservation reservation) {
        return PAYMENT_SERVICE.getGrossPaidAmountBeforeRefunds(reservation);
    }

    public static java.util.Map<String, ReservationPaymentSummary> getPaymentSummaries(List<Reservation> reservations) {
        return PAYMENT_SERVICE.getPaymentSummaries(reservations);
    }

    public static boolean payDeposit(Reservation reservation, Guest guest) {
        return PAYMENT_SERVICE.payDeposit(reservation, guest);
    }

    public static boolean payDeposit(Reservation reservation, Guest guest, PaymentMethod paymentMethod) {
        return PAYMENT_SERVICE.payDeposit(reservation, guest, paymentMethod);
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

    public static void markTodayCheckingIn() {
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
        return PAYMENT_SERVICE.checkOutGuest(reservation, paymentMethod);
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
        return AVAILABILITY_SERVICE.searchAvailableRooms(checkIn, checkOut, requestedType, guests, requestedAmenities);
    }

    // Daily status cleanup for deposit deadlines, missed check-ins, and checkout transitions.
    public static void cancelOverdueReservations() {
        LocalDate today = SystemTime.getToday();
        for (Reservation reservation : new ArrayList<>(reservations)) {

            // Paid pending reservations enter the check-in queue on arrival day.
            if (reservation.getStatus() == ReservationStatus.PENDING && reservation.getCheckInDate().isEqual(today)) {
                if (reservation.isDepositPaid()) {
                    reservation.setStatus(ReservationStatus.CONFIRMED);
                    DatabaseSaver.updateReservationStatus(reservation.getReservationId(), ReservationStatus.CONFIRMED.toString());
                }
            }

            // Unpaid pending reservations expire after their deposit window.
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

            if (reservation.getStatus() == ReservationStatus.CHECKING_OUT  && reservation.getCheckOutDate() != null && reservation.getCheckOutDate().isBefore(today)){
                reservation.setStatus(ReservationStatus.COMPLETED);
                DatabaseSaver.updateReservationStatus(reservation.getReservationId(), ReservationStatus.COMPLETED.toString());

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
        return AVAILABILITY_SERVICE.hasOverlappingReservation(room, checkIn, checkOut);
    }
}
