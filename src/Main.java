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
            System.out.println("4. TEST: Interface Features (Payable & Manageable)");
            System.out.println("0. Exit");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> login();
                case "2" -> registerGuest();
                case "3" -> advanceTime();
                case "4" -> testInterfaceFeatures();
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

    // =============================================================
    // TEST METHOD: Interface Features (Payable & Manageable)
    // =============================================================
    
    private static void testInterfaceFeatures() {
        System.out.println("\n===========================================");
        System.out.println("   TEST: Payable & Manageable Interfaces");
        System.out.println("===========================================");
        
        // Get or create a test guest
        Guest testGuest = (Guest) Database.findUser("Hassan");
        if (testGuest == null) {
            System.out.println("Test guest 'Hassan' not found. Creating...");
            try {
                testGuest = new Guest("Hassan", "Hassan123", LocalDate.of(2007, 4, 10), 
                    10000, "2 haram", Gender.MALE, "");
                Authentication.register(testGuest);
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                return;
            }
        }
        
        boolean testing = true;
        while (testing) {
            System.out.println("\n--- Interface Test Menu ---");
            System.out.println("1. Create Test Reservation & Test Payable");
            System.out.println("2. Test Manageable (Cancel/Modify)");
            System.out.println("3. Test Payment Deadlines (48hr check)");
            System.out.println("4. View All Reservations as Payable/Manageable");
            System.out.println("5. Reset: Clear All Reservations");
            System.out.println("0. Exit Test Menu");
            System.out.print("Choose: ");
            
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> testPayableInterface(testGuest);
                case "2" -> testManageableInterface(testGuest);
                case "3" -> testPaymentDeadlines(testGuest);
                case "4" -> viewAllAsInterfaces();
                case "5" -> resetReservations();
                case "0" -> testing = false;
                default -> System.out.println("Invalid option.");
            }
        }
    }
    
    private static void testPayableInterface(Guest guest) {
        System.out.println("\n--- Test Payable Interface ---");
        
        // Find an available room
        Room testRoom = Database.getRooms().stream()
            .filter(Room::isAvailable)
            .findFirst().orElse(null);
            
        if (testRoom == null) {
            System.out.println("No available rooms. Try advancing time or using different dates.");
            return;
        }
        
        // Create a reservation 5 days from now
        LocalDate checkIn = SystemTime.getToday().plusDays(5);
        LocalDate checkOut = SystemTime.getToday().plusDays(7);
        
        try {
            Reservation res = ReservationService.createReservation(guest, testRoom, checkIn, checkOut, false);
            System.out.println("✓ Reservation created: " + res.getReservationId());
            
            // Test Payable interface
            Payable payable = res;
            System.out.println("\n=== Payable Interface Tests ===");
            System.out.println("getTotalAmount(): $" + String.format("%.2f", payable.getTotalAmount()));
            System.out.println("getDueDate(): " + payable.getDueDate());
            System.out.println("isPaid(): " + payable.isPaid());
            System.out.println("isOverdue(): " + payable.isOverdue());
            
            // Pay deposit (first night)
            System.out.println("\n--- Paying Deposit (First Night) ---");
            double deposit = ReservationService.getDepositAmount(res);
            System.out.println("Deposit amount: $" + String.format("%.2f", deposit));
            System.out.println("Guest balance: $" + String.format("%.2f", guest.getBalance()));
            
            ReservationService.payDeposit(res, guest);
            System.out.println("✓ Deposit paid!");
            System.out.println("New balance: $" + String.format("%.2f", guest.getBalance()));
            System.out.println("isPaid(): " + payable.isPaid());
            System.out.println("canManage(): " + res.canManage());
            
            // Advance time to check-in and pay remaining
            System.out.println("\n--- Advancing to Check-in Date ---");
            SystemTime.advanceDays(5);
            System.out.println("Current date: " + SystemTime.getDate());
            
            System.out.println("\n--- Paying Remaining Balance ---");
            double remaining = ReservationService.getRemainingAmount(res);
            System.out.println("Remaining amount: $" + String.format("%.2f", remaining));
            System.out.println("Guest balance: $" + String.format("%.2f", guest.getBalance()));
            
            ReservationService.payFullAmount(res, guest);
            System.out.println("✓ Full amount paid!");
            System.out.println("isPaid(): " + payable.isPaid());
            System.out.println("canManage(): " + res.canManage());
            
            // Check-in
            System.out.println("\n--- Check-in ---");
            ReservationService.checkInGuest(res, guest);
            System.out.println("✓ Checked in! Status: " + res.getStatus());
            
            // Check-out (pay add-ons)
            System.out.println("\n--- Check-out (Add-ons) ---");
            PaymentMethod pm = PaymentMethod.CREDIT_CARD;
            ReservationService.checkOutGuest(res, pm);
            System.out.println("✓ Checked out! Status: " + res.getStatus());
            
        } catch (RoomNotAvailableException e) {
            System.out.println("Room not available: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
    
    private static void testManageableInterface(Guest guest) {
        System.out.println("\n--- Test Manageable Interface ---");
        
        // Show current reservations
        var reservations = Database.getReservations();
        if (reservations.isEmpty()) {
            System.out.println("No reservations. Create one first (option 1).");
            return;
        }
        
        // Show manageable reservations
        List<Manageable> manageables = ReservationService.getManageableReservations();
        System.out.println("Manageable reservations (can cancel/modify): " + manageables.size());
        
        for (Manageable m : manageables) {
            Reservation r = (Reservation) m;
            System.out.println("  - " + r.getReservationId() + " | Status: " + r.getStatus() + " | canManage: " + r.canManage());
        }
        
        // Test cancellation
        if (!manageables.isEmpty()) {
            System.out.print("\nTest cancellation? (Y/N): ");
            if (scanner.nextLine().trim().equalsIgnoreCase("Y")) {
                Manageable toCancel = manageables.get(0);
                System.out.println("Cancelling: " + toCancel.getId());
                if (toCancel.cancel()) {
                    System.out.println("✓ Cancelled successfully!");
                    System.out.println("Status: " + toCancel.getStatusString());
                } else {
                    System.out.println("✗ Could not cancel (check canManage())");
                }
            }
        }
        
        // Test 48-hour restriction
        System.out.println("\n--- Testing 48-hour Restriction ---");
        System.out.println("Current date: " + SystemTime.getDate());
        
        // Create reservation for tomorrow
        Room testRoom = Database.getRooms().stream()
            .filter(Room::isAvailable)
            .findFirst().orElse(null);
            
        if (testRoom != null) {
            LocalDate checkIn = SystemTime.getToday().plusDays(1);
            LocalDate checkOut = SystemTime.getToday().plusDays(3);
            
            try {
                Reservation res = ReservationService.createReservation(guest, testRoom, checkIn, checkOut, false);
                System.out.println("Created reservation: " + res.getReservationId());
                System.out.println("Check-in: " + checkIn + " | canManage: " + res.canManage() + " | canOnlyExtend: " + res.canOnlyExtend());
                
                // Advance to within 48 hours
                System.out.println("\nAdvancing to within 48 hours of check-in...");
                SystemTime.advanceDays(1); // Now it's check-in day - 48 hours = 0
                System.out.println("Current date: " + SystemTime.getDate());
                System.out.println("canManage(): " + res.canManage());
                System.out.println("canOnlyExtend(): " + res.canOnlyExtend());
                
            } catch (RoomNotAvailableException e) {
                System.out.println("Room not available: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }
    
    private static void testPaymentDeadlines(Guest guest) {
        System.out.println("\n--- Test Payment Deadlines ---");
        
        // Create reservation 5 days out
        Room testRoom = Database.getRooms().stream()
            .filter(Room::isAvailable)
            .findFirst().orElse(null);
            
        if (testRoom == null) {
            System.out.println("No available rooms.");
            return;
        }
        
        LocalDate checkIn = SystemTime.getToday().plusDays(5);
        LocalDate checkOut = SystemTime.getToday().plusDays(7);
        
        try {
            Reservation res = ReservationService.createReservation(guest, testRoom, checkIn, checkOut, true);
            System.out.println("✓ Reservation: " + res.getReservationId());
            System.out.println("  Total Price: $" + String.format("%.2f", res.getTotalPrice()));
            System.out.println("  First Night (Deposit): $" + String.format("%.2f", res.getFirstNightPrice()));
            System.out.println("  Remaining (due at check-in): $" + String.format("%.2f", res.getRemainingBalance()));
            System.out.println("  Add-ons (due at checkout): $" + String.format("%.2f", res.getAddOnsTotal()));
            
            System.out.println("\n  Deposit Deadline: " + res.getDepositDeadline() + " (48hrs before check-in)");
            System.out.println("  Full Payment Deadline: " + res.getFullPaymentDeadline() + " (at check-in)");
            
            // Advance past deposit deadline
            System.out.println("\n--- Advancing past deposit deadline (48hrs before check-in) ---");
            SystemTime.advanceDays(3); // Now 2 days before check-in
            System.out.println("Current date: " + SystemTime.getDate());
            System.out.println("Deposit overdue: " + res.isDepositOverdue());
            
        } catch (RoomNotAvailableException e) {
            System.out.println("Room not available: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
    
    private static void viewAllAsInterfaces() {
        System.out.println("\n--- All Reservations as Interfaces ---");
        
        List<Payable> payables = ReservationService.getPayableReservations();
        List<Manageable> manageables = ReservationService.getManageableReservations();
        List<Manageable> extendables = ReservationService.getExtendableReservations();
        
        System.out.println("Total Payable: " + payables.size());
        System.out.println("Total Manageable: " + manageables.size());
        System.out.println("Total Extendable: " + extendables.size());
        
        System.out.println("\nAll Reservations:");
        for (Reservation r : Database.getReservations()) {
            System.out.println("  " + r.getReservationId() + " | " + r.getStatus() 
                + " | canManage: " + r.canManage() 
                + " | canOnlyExtend: " + r.canOnlyExtend()
                + " | isPaid: " + r.isPaid()
                + " | Gym: " + r.hasGymPass()
                + " | Restaurant: " + r.hasRestaurant());
        }
    }
    
    private static void resetReservations() {
        System.out.print("Clear ALL reservations? (Y/N): ");
        if (scanner.nextLine().trim().equalsIgnoreCase("Y")) {
            Database.getReservations().clear();
            System.out.println("✓ All reservations cleared.");
            
            // Reset room availability
            for (Room r : Database.getRooms()) {
                r.setAvailable(true);
            }
            System.out.println("✓ All rooms set to available.");
            
            // Reset time
            SystemTime.setDate(2026, 4, 19);
            System.out.println("✓ Date reset to: " + SystemTime.getDate());
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