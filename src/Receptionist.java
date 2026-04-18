
import java.time.LocalDate;
import java.util.List;

public class Receptionist extends Staff {

    public Receptionist(String username, String password, LocalDate dateOfBirth, int workingHours) {
        super(username, password, dateOfBirth, Role.RECEPTIONIST, workingHours);
    }

    public void viewReservations(LocalDate checkIn) {
        System.out.println("Viewing reservations for " + checkIn.toString());
        Database.getReservations().stream().filter(
                r -> r.getCheckInDate().isEqual(checkIn))
                .forEach(System.out::println);
    }

    public void viewReservations() {
        System.out.println("\n=== All Reservations ===");
        if (Database.getReservations().isEmpty()) {
            System.out.println("No reservations found.");
            return;
        }
        Database.getReservations().forEach(System.out::println);
    }

    public void viewReservationsForToday() {
        LocalDate today = SystemTime.getToday();
        System.out.println("\n=== Reservations for Today (" + today + ") ===");
        List<Reservation> todayRes = Database.getReservations().stream()
                .filter(r -> r.getCheckInDate().isEqual(today) ||
                            (r.getStatus() == ReservationStatus.ONGOING &&
                             r.getCheckOutDate().isEqual(today)))
                .toList();

        if (todayRes.isEmpty()) {
            System.out.println("No reservations for today.");
            return;
        }

        System.out.println("\nCHECK-INS TODAY:");
        todayRes.stream()
                .filter(r -> r.getCheckInDate().isEqual(today))
                .forEach(r -> System.out.println("  - " + r.getReservationId() + " | " + r.getGuest().getUsername() +
                                                  " | Room " + r.getRoom().getRoomNumber() +
                                                  " | Deposit Paid: " + r.isDepositPaid()));

        System.out.println("\nCHECK-OUTS TODAY:");
        todayRes.stream()
                .filter(r -> r.getCheckOutDate().isEqual(today) && r.getStatus() == ReservationStatus.ONGOING)
                .forEach(r -> System.out.println("  - " + r.getReservationId() + " | " + r.getGuest().getUsername() +
                                                  " | Room " + r.getRoom().getRoomNumber()));
    }

    public void checkInGuest(String reservationId, Guest guest) {
        Reservation reservation = Database.getReservations().stream()
                .filter(r -> r.getReservationId().equals(reservationId))
                .findFirst()
                .orElse(null);

        if (reservation == null) {
            System.out.println("Reservation not found.");
            return;
        }

        try {
            ReservationService.checkInGuest(reservation, guest);
            System.out.println("Guest " + guest.getUsername() + " checked in to room " +
                             reservation.getRoom().getRoomNumber());
            System.out.println("  Full payment will be collected at checkout.");
        } catch (IllegalArgumentException e) {
            System.out.println("Check-in failed: " + e.getMessage());
        }
    }

    public void checkOutGuest(String reservationId, PaymentMethod paymentMethod) {
        Reservation reservation = Database.getReservations().stream()
                .filter(r -> r.getReservationId().equals(reservationId))
                .findFirst()
                .orElse(null);

        if (reservation == null) {
            System.out.println("Reservation not found.");
            return;
        }

        try {
            double remaining = ReservationService.getRemainingAmount(reservation);
            ReservationService.checkOutGuest(reservation, paymentMethod);
            System.out.println("Guest " + reservation.getGuest().getUsername() +
                             " checked out from room " + reservation.getRoom().getRoomNumber());
            System.out.println("  Collected payment: $" + String.format("%.2f", remaining));
            System.out.println("  Guest balance: $" + String.format("%.2f", reservation.getGuest().getBalance()));
        } catch (IllegalArgumentException e) {
            System.out.println("Check-out failed: " + e.getMessage());
        }
    }
}
