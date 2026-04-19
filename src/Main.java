import exceptions.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("===========================================");
        System.out.println("   Welcome to the Hotel Reservation System");
        System.out.println("===========================================");

        boolean running = true;
        while (running) {
            System.out.println("\n--- Main Menu (Today: " + SystemTime.getDate() + ") ---");
            System.out.println("1. Login");
            System.out.println("2. Register as Guest");
            System.out.println("3. Advance Time");
            System.out.println("0. Exit");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> login();
                case "2" -> registerGuest();
                case "3" -> advanceTime();
                case "0" -> {
                    System.out.println("Goodbye!");
                    running = false;
                }
                default -> System.out.println("Invalid option. Please try again.");
            }
        }
    }


    private static void advanceTime() {
        System.out.print("How many days to advance? ");
        try {
            int days = Integer.parseInt(scanner.nextLine().trim());
            if (days <= 0) {
                System.out.println("Please enter a positive number.");
                return;
            }
            
            LocalDate todayBefore = SystemTime.getToday();
            SystemTime.advanceDays(days);
            LocalDate todayAfter = SystemTime.getToday();
            
            // Also check for auto-cancellation of overdue reservations
            ReservationService.cancelOverdueReservations();
            
            // Check for no-shows - reservations where check-in date has passed and guest never checked in
            ReservationService.processNoShows();
        } catch (NumberFormatException e) {
            System.out.println("Invalid input.");
        }
    }

    private static void login() {
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Password: ");
        String password = scanner.nextLine().trim();

        try {
            User user = Authentication.login(username, password);
            System.out.println("\nLogin successful! Welcome, " + user.getUsername());

            if (user instanceof Admin admin) {
                    AdminMenu.show(admin);
            } else if (user instanceof Receptionist receptionist) {
                    ReceptionistMenu.show(receptionist);
            } else if (user instanceof Guest guest) {
                    GuestMenu.show(guest);
            }
        } catch (InvalidCredentialsException e) {
            System.out.println("Login failed: " + e.getMessage());
        }
    }

    private static void registerGuest() {
        System.out.println("\n--- Guest Registration ---");
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        String password = "";
        boolean validPass = false;
        while (!validPass) {
            System.out.print("Password (min 8 chars, 1 uppercase, 1 digit): ");
            password = scanner.nextLine().trim();
            try {
                Authentication.validatePasswordStrength(password);
                validPass = true;
            } catch (WeakPasswordException e) {
                System.out.println("Error: " + e.getMessage() + ", Please try again.");
            }
        }

        System.out.print("Date of Birth (YYYY-MM-DD): ");
        LocalDate dob;
        try {
            dob = LocalDate.parse(scanner.nextLine().trim());
        } catch (DateTimeParseException e) {
            System.out.println("Invalid date format.");
            return;
        }

        System.out.print("Balance: ");
        double balance;
        try {
            balance = Double.parseDouble(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid balance.");
            return;
        }

        System.out.print("Address: ");
        String address = scanner.nextLine().trim();

        System.out.print("Gender (MALE / FEMALE): ");
        Gender gender;
        try {
            gender = Gender.valueOf(scanner.nextLine().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid gender.");
            return;
        }

        System.out.print("Room Preferences: ");
        String prefs = scanner.nextLine().trim();

        Guest guest = new Guest(username, password, dob, balance, address, gender, prefs);
        try {
            guest.register();
            System.out.println("Registration successful! You can now log in.");
        } catch (InvalidCredentialsException e) {
            System.out.println("Registration failed: " + e.getMessage());
        }
    }
}