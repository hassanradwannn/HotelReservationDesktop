package Models;


import Controllers.*;
import Repositories.*;
import Services.*;
import Utils.*;
import Utils.exceptions.*;
import Repositories.database.DatabaseConnection;
import Repositories.DatabaseInitializer.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class Receptionist extends Staff {

    public Receptionist(String username, String password, LocalDate dateOfBirth, int workingHours) {
        super(username, password, dateOfBirth, Role.RECEPTIONIST, workingHours);
    }

    public void viewReservations(LocalDate checkIn) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        System.out.println("Viewing reservations for " + checkIn.format(formatter));
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
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        System.out.println("\n=== Reservations for Today (" + today.format(formatter) + ") ===");
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
            printFormattedInvoice(reservation, remaining, paymentMethod);
        } catch (IllegalArgumentException e) {
            System.out.println("Check-out failed: " + e.getMessage());
        }
    }

    private void printFormattedInvoice(Reservation reservation, double amountPaid, PaymentMethod paymentMethod) {
        Guest guest = reservation.getGuest();
        Room room = reservation.getRoom();
        LocalDate paymentDate = SystemTime.getToday();

        System.out.println("\n" + "=".repeat(60));
        System.out.println(String.format("%20s %s %20s", "", "HOTEL CHECKOUT INVOICE", ""));
        System.out.println("=".repeat(60));

        System.out.println("\nRESERVATION DETAILS:");
        System.out.println(String.format("  %-25s: %s", "Reservation ID", reservation.getReservationId()));
        System.out.println(String.format("  %-25s: %s", "Guest Name", guest.getUsername()));
        System.out.println(String.format("  %-25s: %s", "Room Number", room.getRoomNumber()));
        System.out.println(String.format("  %-25s: %s", "Room Type", room.getRoomType()));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        System.out.println("\nSTAY DETAILS:");
        System.out.println(String.format("  %-25s: %s", "Check-In Date", reservation.getCheckInDate().format(formatter)));
        System.out.println(String.format("  %-25s: %s", "Check-Out Date", reservation.getCheckOutDate().format(formatter)));
        long nights = reservation.getCheckInDate().until(reservation.getCheckOutDate()).getDays();
        System.out.println(String.format("  %-25s: %d night(s)", "Number of Nights", nights));

        System.out.println("\nPAYMENT DETAILS:");
        System.out.println(String.format("  %-25s: $%.2f", "Total Stay Cost", reservation.getTotalPrice()));
        System.out.println(String.format("  %-25s: $%.2f", "Amount Paid at Checkout", amountPaid));
        System.out.println(String.format("  %-25s: %s", "Payment Method", paymentMethod));
        System.out.println(String.format("  %-25s: %s", "Payment Date", paymentDate.format(formatter)));

        System.out.println("\nACCOUNT INFORMATION:");
        System.out.println(String.format("  %-25s: $%.2f", "Guest Balance", guest.getBalance()));

        System.out.println("\n" + "=".repeat(60));
        System.out.println(String.format("%15s %s %15s", "", "Thank You for Your Stay!", ""));
        System.out.println("=".repeat(60) + "\n");
    }
}
