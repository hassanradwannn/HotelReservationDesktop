import exceptions.InvalidCredentialsException;
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
            SystemTime.advanceDays(days);
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
            PaymentMethod pm = PaymentMethod.CREDITCARD;
            ReservationService.checkOutGuest(res, pm);
            System.out.println("✓ Checked out! Status: " + res.getStatus());
            
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
                    System.out.println("Status: " + toCancel.getStatus());
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
                adminMenu(admin);
            } else if (user instanceof Receptionist receptionist) {
                receptionistMenu(receptionist);
            } else if (user instanceof Guest guest) {
                guestMenu(guest);
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

    private static void guestMenu(Guest guest) {
        boolean active = true;
        while (active) {
            System.out.println("\n--- Guest Menu [" + guest.getUsername() + "] ---");
            System.out.println("(Current date: " + SystemTime.getDate() + ")");
            System.out.println("1. View Profile");
            System.out.println("2. View Available Rooms");
            System.out.println("3. Make a Reservation");
            System.out.println("4. View My Reservations");
            System.out.println("5. Pay Deposit (for PENDING reservations)");
            System.out.println("6. Manage Reservation (Cancel/Extend/Add-ons)");
            System.out.println("0. Logout");
            System.out.print("Choose: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> viewGuestProfile(guest);
                case "2" -> viewAvailableRooms();
                case "3" -> makeReservation(guest);
                case "4" -> viewGuestReservations(guest);
                case "5" -> payDepositForReservation(guest);
                case "6" -> manageReservation(guest);
                case "0" -> {
                    System.out.println("Logged out.");
                    active = false;
                }
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private static void viewGuestProfile(Guest guest) {
        System.out.println("\n--- Profile ---");
        System.out.println("Username   : " + guest.getUsername());
        System.out.println("DOB        : " + guest.getDateOfBirth());
        System.out.println("Balance    : $" + guest.getBalance());
        System.out.println("Address    : " + guest.getAddress());
        System.out.println("Gender     : " + guest.getGender());
        System.out.println("Preferences: " + guest.getRoomPreferences());
    }

    private static void viewAvailableRooms() {
        System.out.println("\n--- Available Rooms ---");
        boolean found = false;
        for (Room room : Database.getRooms()) {
            if (room.isAvailable()) {
                System.out.println(room);
                System.out.println("  Amenities: " + room.getAmenities());
                found = true;
            }
        }
        if (!found)
            System.out.println("No rooms available.");
    }

    private static void makeReservation(Guest guest) {
        System.out.println("\n--- Make a Reservation ---");

        // 1. Select Room Type
        List<RoomType> types = Database.getRoomTypes();
        for (int i = 0; i < types.size(); i++) {
            System.out.println((i + 1) + ". " + types.get(i));
        }
        System.out.print("Select room type (number): ");
        int typeChoice;
        try {
            typeChoice = Integer.parseInt(scanner.nextLine().trim()) - 1;
            if (typeChoice < 0 || typeChoice >= types.size())
                throw new NumberFormatException();
        } catch (NumberFormatException e) {
            System.out.println("Invalid selection.");
            return;
        }
        RoomType selectedType = types.get(typeChoice);

        int numGuests = 0;
        boolean validGuests = false;
        while (!validGuests) {
            System.out.print("Number of guests: ");
            try {
                numGuests = Integer.parseInt(scanner.nextLine().trim());
                if (numGuests <= 0) {
                    System.out.println("Number of guests must be at least 1.");
                } else if (numGuests > selectedType.getCapacity()) {
                    System.out.println("Error: The selected " + selectedType.getName() +
                            " only has a capacity of " + selectedType.getCapacity() + " guests.");
                    System.out.println("Please enter a smaller number or restart to choose a larger room type.");
                } else {
                    validGuests = true;
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }

        // 2. Date Input Loop
        LocalDate checkIn = null;
        LocalDate checkOut = null;
        boolean datesValid = false;

        while (!datesValid) {
            try {
                System.out.print("Check-in date (YYYY-MM-DD): ");
                checkIn = LocalDate.parse(scanner.nextLine().trim());
                System.out.print("Check-out date (YYYY-MM-DD): ");
                checkOut = LocalDate.parse(scanner.nextLine().trim());

                if (!ReservationService.isDateRangeValid(checkIn, checkOut)) {
                    System.out.println("Error: Dates cannot be in the past, and Check-out must be after Check-in.");
                    System.out.println("Please enter the dates again.");
                } else {
                    datesValid = true;
                }
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date format. Please use YYYY-MM-DD.");
            }
        }

        // 3. Search for available rooms
        List<Room> available = ReservationService.searchAvailableRooms(checkIn, checkOut, selectedType, numGuests);

        if (available.isEmpty()) {
            System.out.println("No rooms available for the selected criteria or dates overlap with existing bookings.");
            return;
        }

        System.out.println("\nAvailable rooms:");
        for (Room r : available) {
            System.out.println("- Room Number: " + r.getRoomNumber() + " | Type: " + r.getRoomType().getName());
        }

        // 4. Select room by string
        System.out.print("\nSelect room (Enter Room Number): ");
        String selectedRoomNumber = scanner.nextLine().trim();

        // ... previous code ...
        Room selectedRoom = available.stream()
                .filter(r -> r.getRoomNumber().equalsIgnoreCase(selectedRoomNumber))
                .findFirst()
                .orElse(null);

        if (selectedRoom == null) {
            System.out.println("Invalid selection. That room is not in the available list.");
            return;
        }

        boolean addGym = false;
        if (!selectedRoom.getRoomType().getName().equalsIgnoreCase("Penthouse")) {
            System.out.print("Would you like to add a Gym Pass for your stay? ($200 flat fee) (Y/N): ");
            String gymChoice = scanner.nextLine().trim();
            if (gymChoice.equalsIgnoreCase("Y")) {
                addGym = true;
            }
        }

        // Finalize Reservation
        try {
            // Pass addGym to the service
            Reservation res = ReservationService.createReservation(guest, selectedRoom, checkIn, checkOut, addGym);
            System.out.println("Reservation created! ID: " + res.getReservationId());
            System.out.println("  Status: PENDING");
            System.out.println("  Total Price: $" + String.format("%.2f", res.getTotalPrice()));
            System.out.println("  Deposit Due (25%): $" + String.format("%.2f", ReservationService.getDepositAmount(res)));
            System.out.println("  Deposit Deadline: " + res.getDepositDeadline());
            System.out.println("  Please pay the deposit to confirm your reservation.");
        } catch (IllegalArgumentException e) {
            System.out.println("Reservation failed: " + e.getMessage());
        }
    }

    private static void viewGuestReservations(Guest guest) {
        System.out.println("\n--- My Reservations ---");
        boolean found = false;
        for (Reservation r : Database.getReservations()) {
            if (r.getGuest().getUsername().equalsIgnoreCase(guest.getUsername())) {
                printReservation(r);
                found = true;
            }
        }
        if (!found)
            System.out.println("You have no reservations.");
    }

    private static void manageReservation(Guest guest) {
        System.out.println("\n--- Manage Reservation ---");
        
        // Get reservations that belong to this guest and are not completed/cancelled
        List<Reservation> myReservations = Database.getReservations().stream()
                .filter(r -> r.getGuest().getUsername().equalsIgnoreCase(guest.getUsername())
                        && r.getStatus() != ReservationStatus.COMPLETED
                        && r.getStatus() != ReservationStatus.CANCELLED)
                .toList();

        if (myReservations.isEmpty()) {
            System.out.println("You have no active reservations to manage.");
            return;
        }

        System.out.println("Your active reservations:");
        for (int i = 0; i < myReservations.size(); i++) {
            Reservation r = myReservations.get(i);
            System.out.println((i + 1) + ". " + r.getReservationId()
                    + " | Room: " + r.getRoom().getRoomNumber()
                    + " | " + r.getCheckInDate() + " → " + r.getCheckOutDate()
                    + " | Status: " + r.getStatus()
                    + " | Gym: " + (r.hasGymPass() ? "Yes" : "No")
                    + " | Restaurant: " + (r.hasRestaurant() ? "Yes" : "No")
                    + " | canManage: " + r.canManage()
                    + " | canExtend: " + (r.canManage() || r.canOnlyExtend()));
        }

        System.out.print("Select reservation number: ");
        int choice;
        try {
            choice = Integer.parseInt(scanner.nextLine().trim()) - 1;
            if (choice < 0 || choice >= myReservations.size())
                throw new NumberFormatException();
        } catch (NumberFormatException e) {
            System.out.println("Invalid selection.");
            return;
        }

        Reservation selected = myReservations.get(choice);
        
        // Sub-menu for reservation management
        boolean managing = true;
        while (managing) {
            System.out.println("\n--- Managing: " + selected.getReservationId() + " ---");
            System.out.println("1. Cancel Reservation");
            System.out.println("2. Extend Stay");
            System.out.println("3. Manage Add-ons (Gym, etc.)");
            System.out.println("0. Back");
            System.out.print("Choose: ");

            String subChoice = scanner.nextLine().trim();
            switch (subChoice) {
                case "1" -> {
                    // Cancel
                    if (!selected.canManage()) {
                        System.out.println("Cannot cancel: reservation is paid or within 48 hours of check-in.");
                    } else {
                        System.out.print("Are you sure you want to cancel? (Y/N): ");
                        if (scanner.nextLine().trim().equalsIgnoreCase("Y")) {
                            try {
                                ReservationService.cancelReservation(selected.getReservationId());
                                System.out.println("✓ Reservation cancelled.");
                                managing = false;
                            } catch (IllegalArgumentException e) {
                                System.out.println("Error: " + e.getMessage());
                            }
                        }
                    }
                }
                case "2" -> {
                    // Extend stay
                    if (!selected.canManage() && !selected.canOnlyExtend()) {
                        System.out.println("Cannot extend: reservation is paid.");
                    } else {
                        System.out.println("Current check-out: " + selected.getCheckOutDate());
                        System.out.print("Enter new check-out date (YYYY-MM-DD): ");
                        try {
                            LocalDate newCheckOut = LocalDate.parse(scanner.nextLine().trim());
                            if (!newCheckOut.isAfter(selected.getCheckOutDate())) {
                                System.out.println("New check-out must be after current check-out.");
                            } else {
                                double oldPrice = selected.getTotalPrice();
                                ReservationService.extendStay(selected.getReservationId(), newCheckOut);
                                double newPrice = selected.getTotalPrice();
                                double additionalCost = newPrice - oldPrice;
                                System.out.println("✓ Stay extended!");
                                System.out.println("  Additional cost: $" + String.format("%.2f", additionalCost));
                                System.out.println("  (Will be paid at checkout)");
                            }
                        } catch (DateTimeParseException e) {
                            System.out.println("Invalid date format.");
                        } catch (IllegalArgumentException e) {
                            System.out.println("Error: " + e.getMessage());
                        }
                    }
                }
                case "3" -> {
                    // Manage add-ons
                    manageAddOns(guest, selected);
                }
                case "0" -> managing = false;
                default -> System.out.println("Invalid option.");
            }
        }
    }
    
    private static void manageAddOns(Guest guest, Reservation reservation) {
        System.out.println("\n--- Manage Add-ons ---");
        System.out.println("Current add-ons:");
        System.out.println("  Gym Pass: " + (reservation.hasGymPass() ? "Yes ($200)" : "No") + " - PAID UPFRONT");
        System.out.println("  Restaurant: " + (reservation.hasRestaurant() ? "Yes ($150)" : "No") + " - PAID AT CHECKOUT");
        
        // Show available amenities that can be added
        System.out.println("\nAvailable add-ons to add:");
        List<String> availableOptions = new ArrayList<>();
        int idx = 1;
        
        // Gym option (if not already have it)
        if (!reservation.hasGymPass()) {
            System.out.println("  " + idx + ". Gym Pass ($200) - PAID UPFRONT");
            availableOptions.add("Gym");
            idx++;
        }
        
        // Restaurant option (if not already have it)
        if (!reservation.hasRestaurant()) {
            System.out.println("  " + idx + ". Restaurant ($150) - PAID AT CHECKOUT");
            availableOptions.add("Restaurant");
            idx++;
        }
        
        if (availableOptions.isEmpty()) {
            System.out.println("No additional add-ons available.");
            return;
        }
        
        System.out.print("Select add-on to add (number) or 0 to cancel: ");
        try {
            int addChoice = Integer.parseInt(scanner.nextLine().trim());
            if (addChoice > 0 && addChoice <= availableOptions.size()) {
                String selectedOption = availableOptions.get(addChoice - 1);
                
                if (selectedOption.equals("Gym")) {
                    System.out.println("Adding Gym Pass for $200 (will be added to total upfront)");
                    reservation.setHasGymPass(true);
                    reservation.update(); // Recalculate price
                    System.out.println("✓ Gym Pass added!");
                    System.out.println("  New total: $" + String.format("%.2f", reservation.getTotalPrice()));
                } else if (selectedOption.equals("Restaurant")) {
                    System.out.println("Adding Restaurant for $150 (will be paid at checkout)");
                    reservation.setHasRestaurant(true);
                    System.out.println("✓ Restaurant added!");
                    System.out.println("  Additional $" + String.format("%.2f", 150.0) + " due at checkout");
                }
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid selection.");
        }
    }

    private static void payDepositForReservation(Guest guest) {
        System.out.println("\n--- Pay Deposit ---");
        java.util.List<Reservation> pending = Database.getReservations().stream()
                .filter(r -> r.getGuest().getUsername().equalsIgnoreCase(guest.getUsername())
                        && r.getStatus() == ReservationStatus.PENDING)
                .toList();

        if (pending.isEmpty()) {
            System.out.println("No pending reservations requiring deposit payment.");
            return;
        }

        System.out.println("Your pending reservations:");
        for (int i = 0; i < pending.size(); i++) {
            Reservation r = pending.get(i);
            double deposit = ReservationService.getDepositAmount(r);
            System.out.println((i + 1) + ". " + r.getReservationId()
                    + " | Room " + r.getRoom().getRoomNumber()
                    + " | Check-in: " + r.getCheckInDate()
                    + " | Deposit: $" + String.format("%.2f", deposit)
                    + " | Deadline: " + r.getDepositDeadline());
            if (r.isDepositOverdue()) {
                System.out.println("OVERDUE!");
            }
        }

        System.out.print("Select reservation (number): ");
        int choice;
        try {
            choice = Integer.parseInt(scanner.nextLine().trim()) - 1;
            if (choice < 0 || choice >= pending.size())
                throw new NumberFormatException();
        } catch (NumberFormatException e) {
            System.out.println("Invalid selection.");
            return;
        }

        Reservation res = pending.get(choice);
        double deposit = ReservationService.getDepositAmount(res);

        System.out.println("\nDeposit Payment Details:");
        System.out.println("Total Price: $" + String.format("%.2f", res.getTotalPrice()));
        System.out.println("Deposit (25%): $" + String.format("%.2f", deposit));
        System.out.println("Your Balance: $" + String.format("%.2f", guest.getBalance()));

        if (guest.getBalance() < deposit) {
            System.out.println("Insufficient balance. You need $" + String.format("%.2f", deposit - guest.getBalance()) + " more.");
            return;
        }

        System.out.print("Confirm payment? (Y/N): ");
        if (!scanner.nextLine().trim().equalsIgnoreCase("Y")) {
            System.out.println("Payment cancelled.");
            return;
        }

        try {
            ReservationService.payDeposit(res, guest);
            System.out.println("Deposit payment successful!");
            System.out.println("Reservation is now CONFIRMED.");
            System.out.println("Your new balance: $" + String.format("%.2f", guest.getBalance()));
        } catch (IllegalArgumentException e) {
            System.out.println("Payment failed: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // ADMIN MENU
    // ─────────────────────────────────────────────

    private static void adminMenu(Admin admin) {
        boolean active = true;
        while (active) {
            System.out.println("\n--- Admin Menu [" + admin.getUsername() + "] ---");
            System.out.println("1. View All Guests");
            System.out.println("2. View All Rooms");
            System.out.println("3. View All Reservations");
            System.out.println("4. Manage Rooms (CRUD)");
            System.out.println("5. Manage Room Types (CRUD)");
            System.out.println("6. Manage Amenities (CRUD)");
            System.out.println("0. Logout");
            System.out.print("Choose: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> admin.viewGuests();
                case "2" -> admin.viewRooms();
                case "3" ->
                    admin.viewReservations();
                case "4" -> manageRooms();
                case "5" -> manageRoomTypes();
                case "6" -> manageAmenities();
                case "0" -> {
                    System.out.println("Logged out.");
                    active = false;
                }
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private static void manageRooms() {
        System.out.println("\n--- Manage Rooms ---");
        System.out.println("1. Add Room");
        System.out.println("2. Update Room");
        System.out.println("3. Delete Room");
        System.out.print("Choose: ");

        switch (scanner.nextLine().trim()) {
            case "1" -> {
                System.out.print("Room number: ");
                String num = scanner.nextLine().trim();

                System.out.println("Select Room Type:");
                List<RoomType> types = Database.getRoomTypes();
                for (int i = 0; i < types.size(); i++)
                    System.out.println((i + 1) + ". " + types.get(i).getName());
                System.out.print("Choice: ");
                int t;
                try {
                    t = Integer.parseInt(scanner.nextLine().trim()) - 1;
                } catch (NumberFormatException e) {
                    System.out.println("Invalid.");
                    return;
                }

                Room newRoom = new Room(num, types.get(t));
                Database.addRoom(newRoom);
                System.out.println("Room " + num + " added.");
            }
            case "2" -> {
                System.out.print("Room number to update: ");
                String num = scanner.nextLine().trim();

                Room room = findRoom(num);
                if (room == null) {
                    System.out.println("Room not found.");
                    return;
                }

                System.out.print("New room number (or same): ");
                String newNum = scanner.nextLine().trim();

                System.out.println("Select new Room Type:");
                List<RoomType> types = Database.getRoomTypes();
                for (int i = 0; i < types.size(); i++)
                    System.out.println((i + 1) + ". " + types.get(i).getName());
                System.out.print("Choice: ");
                int t;
                try {
                    t = Integer.parseInt(scanner.nextLine().trim()) - 1;
                } catch (NumberFormatException e) {
                    System.out.println("Invalid.");
                    return;
                }

                room.update(newNum, types.get(t), room.isAvailable());
                System.out.println("Room updated.");
            }
            case "3" -> {
                System.out.print("Room number to delete: ");
                String num = scanner.nextLine().trim();

                Room room = findRoom(num);
                if (room == null) {
                    System.out.println("Room not found.");
                    return;
                }
                Database.getRooms().remove(room);
                System.out.println("Room " + num + " deleted.");
            }
            default -> System.out.println("Invalid option.");
        }
    }

    private static void manageRoomTypes() {
        System.out.println("\n--- Manage Room Types ---");
        System.out.println("1. Add Room Type");
        System.out.println("2. Update Room Type");
        System.out.println("3. Delete Room Type");
        System.out.print("Choose: ");

        switch (scanner.nextLine().trim()) {
            case "1" -> {
                System.out.print("Name: ");
                String name = scanner.nextLine().trim();
                System.out.print("Price per night: ");
                double price;
                try {
                    price = Double.parseDouble(scanner.nextLine().trim());
                } catch (NumberFormatException e) {
                    System.out.println("Invalid price.");
                    return;
                }
                System.out.print("Capacity: ");
                int cap;
                try {
                    cap = Integer.parseInt(scanner.nextLine().trim());
                } catch (NumberFormatException e) {
                    System.out.println("Invalid capacity.");
                    return;
                }

                Database.addRoomType(new RoomType(name, price, cap));
                System.out.println("Room type '" + name + "' added.");
            }
            case "2" -> {
                System.out.print("Current room type name: ");
                String name = scanner.nextLine().trim();
                RoomType rt = findRoomType(name);
                if (rt == null) {
                    System.out.println("Room type not found.");
                    return;
                }

                System.out.print("New name: ");
                String newName = scanner.nextLine().trim();
                System.out.print("New price per night: ");
                double price;
                try {
                    price = Double.parseDouble(scanner.nextLine().trim());
                } catch (NumberFormatException e) {
                    System.out.println("Invalid price.");
                    return;
                }
                System.out.print("New capacity: ");
                int cap;
                try {
                    cap = Integer.parseInt(scanner.nextLine().trim());
                } catch (NumberFormatException e) {
                    System.out.println("Invalid capacity.");
                    return;
                }

                rt.update(newName, cap, price);
                System.out.println("Room type updated.");
            }
            case "3" -> {
                System.out.print("Room type name to delete: ");
                String name = scanner.nextLine().trim();
                RoomType rt = findRoomType(name);
                if (rt == null) {
                    System.out.println("Room type not found.");
                    return;
                }
                Database.getRoomTypes().remove(rt);
                System.out.println("Room type '" + name + "' deleted.");
            }
            default -> System.out.println("Invalid option.");
        }
    }

    private static void manageAmenities() {
        System.out.println("\n--- Manage Amenities ---");
        System.out.println("1. Add Amenity");
        System.out.println("2. Update Amenity");
        System.out.println("3. Delete Amenity");
        System.out.print("Choose: ");

        switch (scanner.nextLine().trim()) {
            case "1" -> {
                System.out.print("Name: ");
                String name = scanner.nextLine().trim();
                System.out.print("Price: ");
                double price;
                try {
                    price = Double.parseDouble(scanner.nextLine().trim());
                } catch (NumberFormatException e) {
                    System.out.println("Invalid price.");
                    return;
                }
                Database.addAmenity(name, price);
                System.out.println("Amenity '" + name + "' added.");
            }
            case "2" -> {
                System.out.print("Current amenity name: ");
                String name = scanner.nextLine().trim();
                Amenity a = findAmenity(name);
                if (a == null) {
                    System.out.println("Amenity not found.");
                    return;
                }
                System.out.print("New name: ");
                String newName = scanner.nextLine().trim();
                System.out.print("New price: ");
                double price;
                try {
                    price = Double.parseDouble(scanner.nextLine().trim());
                } catch (NumberFormatException e) {
                    System.out.println("Invalid price.");
                    return;
                }
                a.update(newName, price);
                System.out.println("Amenity updated.");
            }
            case "3" -> {
                System.out.print("Amenity name to delete: ");
                String name = scanner.nextLine().trim();
                Amenity a = findAmenity(name);
                if (a == null) {
                    System.out.println("Amenity not found.");
                    return;
                }
                Database.getAmenities().remove(a);
                System.out.println("Amenity '" + name + "' deleted.");
            }
            default -> System.out.println("Invalid option.");
        }
    }

    // ─────────────────────────────────────────────
    // RECEPTIONIST MENU
    // ─────────────────────────────────────────────

    private static void receptionistMenu(Receptionist rec) {
        boolean active = true;
        while (active) {
            System.out.println("\n--- Receptionist Menu [" + rec.getUsername() + "] ---");
            System.out.println("(Current date: " + SystemTime.getDate() + ")");
            System.out.println("1. View Today's Reservations");
            System.out.println("2. Check-in Guest");
            System.out.println("3. Check-out Guest");
            System.out.println("4. Extend Guest Stay");
            System.out.println("5. Add Restaurant to Guest (at checkout)");
            System.out.println("6. View All Reservations");
            System.out.println("7. View All Guests");
            System.out.println("8. View All Rooms");
            System.out.println("0. Logout");
            System.out.print("Choose: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> rec.viewReservationsForToday();
                case "2" -> receptionistCheckIn(rec);
                case "3" -> receptionistCheckOut(rec);
                case "4" -> receptionistExtendStay(rec);
                case "5" -> receptionistAddRestaurant(rec);
                case "6" -> rec.viewReservations();
                case "7" -> rec.viewGuests();
                case "8" -> rec.viewRooms();
                case "0" -> {
                    System.out.println("Logged out.");
                    active = false;
                }
                default -> System.out.println("Invalid option.");
            }
        }
    }
    
    private static void receptionistAddRestaurant(Receptionist rec) {
        System.out.println("\n--- Add Restaurant to Guest (Checkout) ---");
        List<Reservation> eligible = Database.getReservations().stream()
                .filter(r -> r.getStatus() == ReservationStatus.ONGOING
                        && !r.hasRestaurant())
                .toList();

        if (eligible.isEmpty()) {
            System.out.println("No ongoing reservations without restaurant.");
            return;
        }

        System.out.println("Select a reservation to add restaurant:");
        for (int i = 0; i < eligible.size(); i++) {
            Reservation r = eligible.get(i);
            System.out.println((i + 1) + ". " + r.getReservationId()
                    + " | Guest: " + r.getGuest().getUsername()
                    + " | Room " + r.getRoom().getRoomNumber()
                    + " | Restaurant: " + (r.hasRestaurant() ? "Yes" : "No ($150)"));
        }

        int index;
        try {
            System.out.print("Choose reservation number: ");
            index = Integer.parseInt(scanner.nextLine().trim()) - 1;
            if (index < 0 || index >= eligible.size()) {
                System.out.println("Invalid selection.");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid input.");
            return;
        }

        Reservation selected = eligible.get(index);
        selected.setHasRestaurant(true);
        System.out.println("✓ Restaurant added to reservation " + selected.getReservationId());
        System.out.println("  Additional $150 will be charged at checkout.");
    }
    
    private static void receptionistExtendStay(Receptionist rec) {
        System.out.println("\n--- Extend Guest Stay ---");
        List<Reservation> eligible = Database.getReservations().stream()
                .filter(r -> r.getStatus() == ReservationStatus.ONGOING
                        || (r.getStatus() == ReservationStatus.CONFIRMED 
                            && (r.canManage() || r.canOnlyExtend())))
                .toList();

        if (eligible.isEmpty()) {
            System.out.println("No reservations eligible for stay extension.");
            return;
        }

        System.out.println("Select a reservation to extend:");
        for (int i = 0; i < eligible.size(); i++) {
            Reservation r = eligible.get(i);
            System.out.println((i + 1) + ". " + r.getReservationId()
                    + " | Guest: " + r.getGuest().getUsername()
                    + " | Room " + r.getRoom().getRoomNumber()
                    + " | Check-out: " + r.getCheckOutDate()
                    + " | canExtend: " + (r.canManage() || r.canOnlyExtend()));
        }

        int index;
        try {
            System.out.print("Choose reservation number: ");
            index = Integer.parseInt(scanner.nextLine().trim()) - 1;
            if (index < 0 || index >= eligible.size()) {
                System.out.println("Invalid selection.");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid input.");
            return;
        }

        Reservation selected = eligible.get(index);
        
        System.out.print("Enter new check-out date (YYYY-MM-DD): ");
        LocalDate newCheckOut;
        try {
            newCheckOut = LocalDate.parse(scanner.nextLine().trim());
        } catch (DateTimeParseException e) {
            System.out.println("Invalid date format.");
            return;
        }

        // Check new date is after current check-out
        if (!newCheckOut.isAfter(selected.getCheckOutDate())) {
            System.out.println("New check-out must be after current check-out date.");
            return;
        }

        double oldPrice = selected.getTotalPrice();
        try {
            ReservationService.extendStay(selected.getReservationId(), newCheckOut);
            double newPrice = selected.getTotalPrice();
            double additionalCost = newPrice - oldPrice;
            
            System.out.println("✓ Stay extended!");
            System.out.println("  Old check-out: " + selected.getCheckOutDate());
            System.out.println("  New check-out: " + newCheckOut);
            System.out.println("  Additional cost: $" + String.format("%.2f", additionalCost));
            System.out.println("  (Will be paid at checkout)");
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void receptionistCheckIn(Receptionist rec) {
        System.out.println("\n--- Check-in Guest ---");
        List<Reservation> eligible = Database.getReservations().stream()
                .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED
                        && r.getCheckInDate().isEqual(SystemTime.getToday()))
                .toList();

        if (eligible.isEmpty()) {
            System.out.println("No reservations available for check-in today.");
            return;
        }

        System.out.println("Select a reservation to check in:");
        for (int i = 0; i < eligible.size(); i++) {
            Reservation r = eligible.get(i);
            System.out.println((i + 1) + ". " + r.getReservationId()
                    + " | Guest: " + r.getGuest().getUsername()
                    + " | Room " + r.getRoom().getRoomNumber()
                    + " | " + r.getCheckInDate() + " → " + r.getCheckOutDate());
        }

        int index;
        try {
            System.out.print("Choose reservation number: ");
            index = Integer.parseInt(scanner.nextLine().trim()) - 1;
            if (index < 0 || index >= eligible.size()) {
                System.out.println("Invalid selection.");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid input.");
            return;
        }

        Reservation selected = eligible.get(index);
        rec.checkInGuest(selected.getReservationId(), selected.getGuest());
    }

    private static void receptionistCheckOut(Receptionist rec) {
        System.out.println("\n--- Check-out Guest ---");
        List<Reservation> eligible = Database.getReservations().stream()
                .filter(r -> r.getStatus() == ReservationStatus.ONGOING
                        && r.getCheckOutDate().isEqual(SystemTime.getToday()))
                .toList();

        if (eligible.isEmpty()) {
            System.out.println("No reservations available for check-out today.");
            return;
        }

        System.out.println("Select a reservation to check out:");
        for (int i = 0; i < eligible.size(); i++) {
            Reservation r = eligible.get(i);
            System.out.println((i + 1) + ". " + r.getReservationId()
                    + " | Guest: " + r.getGuest().getUsername()
                    + " | Room " + r.getRoom().getRoomNumber()
                    + " | " + r.getCheckInDate() + " → " + r.getCheckOutDate()
                    + " | Restaurant: " + (r.hasRestaurant() ? "Yes ($150)" : "No"));
        }

        int index;
        try {
            System.out.print("Choose reservation number: ");
            index = Integer.parseInt(scanner.nextLine().trim()) - 1;
            if (index < 0 || index >= eligible.size()) {
                System.out.println("Invalid selection.");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid input.");
            return;
        }

        PaymentMethod paymentMethod = selectPaymentMethod();
        if (paymentMethod == null) {
            System.out.println("Checkout cancelled.");
            return;
        }

        Reservation selected = eligible.get(index);
        rec.checkOutGuest(selected.getReservationId(), paymentMethod);
    }

    private static PaymentMethod selectPaymentMethod() {
        System.out.println("Select payment method:");
        PaymentMethod[] methods = PaymentMethod.values();
        for (int i = 0; i < methods.length; i++) {
            System.out.println((i + 1) + ". " + methods[i]);
        }
        System.out.print("Choose: ");
        try {
            int choice = Integer.parseInt(scanner.nextLine().trim()) - 1;
            if (choice < 0 || choice >= methods.length) {
                System.out.println("Invalid payment method selection.");
                return null;
            }
            return methods[choice];
        } catch (NumberFormatException e) {
            System.out.println("Invalid input.");
            return null;
        }
    }

    // ─────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────

    private static void printReservation(Reservation r) {
        System.out.println("ID: " + r.getReservationId()
                + " | Room: " + r.getRoom().getRoomNumber()
                + " | " + r.getCheckInDate() + " → " + r.getCheckOutDate()
                + " | Status: " + r.getStatus());
    }

    private static Room findRoom(String number) {
        return Database.getRooms().stream()
                .filter(r -> r.getRoomNumber().equalsIgnoreCase(number))
                .findFirst().orElse(null);
    }

    private static RoomType findRoomType(String name) {
        return Database.getRoomTypes().stream()
                .filter(rt -> rt.getName().equalsIgnoreCase(name))
                .findFirst().orElse(null);
    }

    private static Amenity findAmenity(String name) {
        return Database.getAmenities().stream()
                .filter(a -> a.getName().equalsIgnoreCase(name))
                .findFirst().orElse(null);
    }
}