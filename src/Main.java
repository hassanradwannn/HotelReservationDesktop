import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;
import exceptions.InvalidCredentialsException;

public class Main {

    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("Initializing Database...");
        System.out.println("System loaded with " + Database.getRooms().size() + " rooms.");
        System.out.println("System loaded with " + Database.getStaffMembers().size() + " staff members.\n");

        boolean running = true;

        while (running) {
            System.out.println("\n=================================");
            System.out.println("          HOTEL SYSTEM");
            System.out.println("=================================");
            System.out.println("1. Register");
            System.out.println("2. Login");
            System.out.println("3. Exit");
            System.out.print("Select an option: ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> handleRegister();
                case "2" -> handleLogin();
                case "3" -> {
                    System.out.println("Exiting the system. Goodbye!");
                    running = false;
                }
                default -> System.out.println("Invalid option. Please try again.");
            }
        }

        scanner.close();
    }

    // ─── REGISTER ────────────────────────────────────────────────────────────────

    private static void handleRegister() {
        System.out.println("\n--- GUEST REGISTRATION ---");
        System.out.println("(Only guests can self-register. Staff accounts are created by an Admin.)");

        System.out.print("Username: ");
        String username = scanner.nextLine().trim();

        System.out.print("Password (min 8 chars, upper, lower & digit): ");
        String password = scanner.nextLine().trim();

        System.out.print("Date of birth (YYYY-MM-DD): ");
        LocalDate dob;
        try {
            dob = LocalDate.parse(scanner.nextLine().trim());
        } catch (DateTimeParseException e) {
            System.out.println("Invalid date format. Registration cancelled.");
            return;
        }

        System.out.print("Address: ");
        String address = scanner.nextLine().trim();

        System.out.print("Gender (MALE / FEMALE): ");
        Gender gender;
        try {
            gender = Gender.valueOf(scanner.nextLine().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid gender. Registration cancelled.");
            return;
        }

        System.out.print("Room preferences (e.g. High Floor, Quiet Room): ");
        String preferences = scanner.nextLine().trim();

        System.out.print("Starting balance: $");
        double balance;
        try {
            balance = Double.parseDouble(scanner.nextLine().trim());
            if (balance < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            System.out.println("Invalid balance. Registration cancelled.");
            return;
        }

        Guest newGuest = new Guest(username, password, dob, balance, address, gender, preferences);
        try {
            Authentication.register(newGuest);
            System.out.println("\nRegistration successful! Welcome, " + username + ". You can now log in as a Guest.");
        } catch (InvalidCredentialsException e) {
            System.out.println("Registration failed: " + e.getClass().getSimpleName());
        }
    }

    // ─── LOGIN ────────────────────────────────────────────────────────────────────

    private static void handleLogin() {
        System.out.println("\n--- LOGIN ---");
        System.out.println("1. Guest Login");
        System.out.println("2. Staff Login");
        System.out.println("3. Back");
        System.out.print("Select an option: ");
        String choice = scanner.nextLine().trim();

        switch (choice) {
            case "1" -> handleGuestLogin();
            case "2" -> handleStaffLogin();
            case "3" -> { /* return to main menu */ }
            default  -> System.out.println("Invalid option.");
        }
    }

    private static void handleGuestLogin() {
        System.out.println("\n--- GUEST LOGIN ---");
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Password: ");
        String password = scanner.nextLine().trim();

        try {
            User user = Authentication.login(username, password);

            if (!(user instanceof Guest)) {
                System.out.println("Access denied: This portal is for guests only. Staff should use the Staff Login.");
                return;
            }

            System.out.println("Login successful! Welcome, " + user.getUsername() + ".");
            runGuestFlow((Guest) user);

        } catch (InvalidCredentialsException e) {
            System.out.println("Login failed: " + e.getClass().getSimpleName());
        }
    }

    private static void handleStaffLogin() {
        System.out.println("\n--- STAFF LOGIN ---");
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Password: ");
        String password = scanner.nextLine().trim();

        try {
            User user = Authentication.login(username, password);

            if (!(user instanceof Staff)) {
                System.out.println("Access denied: This portal is for staff only. Guests should use the Guest Login.");
                return;
            }

            System.out.println("Login successful! Welcome, " + user.getUsername() + " (" + user.getClass().getSimpleName() + ").");

            if (user instanceof Admin) {
                runAdminFlow((Admin) user);
            } else if (user instanceof Receptionist) {
                runReceptionistFlow((Receptionist) user);
            } else {
                System.out.println("[Staff role recognised — no specific flow assigned yet.]");
            }

        } catch (InvalidCredentialsException e) {
            System.out.println("Login failed: " + e.getClass().getSimpleName());
        }
    }

    // ─── STAFF FLOWS ─────────────────────────────────────────────────────────────

    private static void runAdminFlow(Admin admin) {
        System.out.println("\n--- ADMIN PANEL ---");
        System.out.println("Total guests in database : " + Database.getGuests().size());
        System.out.println("Total rooms in database  : " + Database.getRooms().size());

        boolean inPanel = true;
        while (inPanel) {
            System.out.println("\nAdmin Options:");
            System.out.println("1. View all guests");
            System.out.println("2. View all rooms");
            System.out.println("3. Register a new guest");
            System.out.println("4. Register a new staff member");
            System.out.println("5. Logout");
            System.out.print("Select an option: ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> admin.viewGuests(Database.getGuests());
                case "2" -> {
                    java.util.List<String> roomStrings = new java.util.ArrayList<>();
                    Database.getRooms().forEach(r -> roomStrings.add(r.toString()));
                    admin.viewRooms(roomStrings);
                }
                case "3" -> adminRegisterGuestPrompt(admin);
                case "4" -> adminRegisterStaffPrompt(admin);
                case "5" -> {
                    System.out.println("Logging out Admin...");
                    inPanel = false;
                }
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private static void adminRegisterGuestPrompt(Admin admin) {
        System.out.println("\n--- Register New Guest ---");
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Password: ");
        String password = scanner.nextLine().trim();
        System.out.print("Date of birth (YYYY-MM-DD): ");
        LocalDate dob;
        try {
            dob = LocalDate.parse(scanner.nextLine().trim());
        } catch (DateTimeParseException e) {
            System.out.println("Invalid date. Cancelled.");
            return;
        }
        System.out.print("Address: ");
        String address = scanner.nextLine().trim();
        System.out.print("Gender (MALE / FEMALE): ");
        Gender gender;
        try {
            gender = Gender.valueOf(scanner.nextLine().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid gender. Cancelled.");
            return;
        }
        System.out.print("Room preferences: ");
        String prefs = scanner.nextLine().trim();
        System.out.print("Starting balance: $");
        double balance;
        try {
            balance = Double.parseDouble(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid balance. Cancelled.");
            return;
        }

        try {
            admin.registerGuest(username, password, dob, balance, address, gender, prefs);
        } catch (InvalidCredentialsException e) {
            System.out.println("Failed: " + e.getClass().getSimpleName());
        }
    }

    private static void adminRegisterStaffPrompt(Admin admin) {
        System.out.println("\n--- Register New Staff Member ---");
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Password (min 8 chars, upper, lower & digit): ");
        String password = scanner.nextLine().trim();
        System.out.print("Date of birth (YYYY-MM-DD): ");
        LocalDate dob;
        try {
            dob = LocalDate.parse(scanner.nextLine().trim());
        } catch (DateTimeParseException e) {
            System.out.println("Invalid date format. Cancelled.");
            return;
        }
        System.out.print("Working hours per week: ");
        int workingHours;
        try {
            workingHours = Integer.parseInt(scanner.nextLine().trim());
            if (workingHours <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            System.out.println("Invalid working hours. Cancelled.");
            return;
        }
        System.out.println("Select role:");
        System.out.println("1. Admin");
        System.out.println("2. Receptionist");
        System.out.print("Your choice (1/2): ");
        Role role = switch (scanner.nextLine().trim()) {
            case "1" -> Role.ADMIN;
            case "2" -> Role.RECEPTIONIST;
            default  -> null;
        };
        if (role == null) {
            System.out.println("Invalid role. Cancelled.");
            return;
        }

        try {
            admin.registerStaff(username, password, dob, workingHours, role);
        } catch (InvalidCredentialsException e) {
            System.out.println("Failed: " + e.getClass().getSimpleName());
        }
    }

    private static void runReceptionistFlow(Receptionist receptionist) {
        System.out.println("\n--- RECEPTIONIST PANEL ---");

        boolean inPanel = true;
        while (inPanel) {
            System.out.println("\nReceptionist Options:");
            System.out.println("1. View all guests");
            System.out.println("2. View all rooms");
            System.out.println("3. View all reservations");
            System.out.println("4. Check in a guest");
            System.out.println("5. Check out a guest");
            System.out.println("6. Logout");
            System.out.print("Select an option: ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> receptionist.viewGuests(Database.getGuests());
                case "2" -> {
                    java.util.List<String> roomStrings = new java.util.ArrayList<>();
                    Database.getRooms().forEach(r -> roomStrings.add(r.toString()));
                    receptionist.viewRooms(roomStrings);
                }
                case "3" -> {
                    java.util.List<String> resList = new java.util.ArrayList<>();
                    Database.getReservations().forEach(r ->
                            resList.add("ID: " + r.getReservationId() +
                                    " | Guest: " + r.getGuest().getUsername() +
                                    " | Room: " + r.getRoom().getRoomNumber() +
                                    " | Status: " + r.getStatus()));
                    receptionist.viewReservations(resList);
                }
                case "4" -> {
                    System.out.print("Guest username to check in: ");
                    String g = scanner.nextLine().trim();
                    System.out.print("Room number: ");
                    String r = scanner.nextLine().trim();
                    User u = Database.findUser(g);
                    if (u instanceof Guest) {
                        receptionist.checkInGuest((Guest) u, r);
                    } else {
                        System.out.println("Guest not found.");
                    }
                }
                case "5" -> {
                    System.out.print("Guest username to check out: ");
                    String g = scanner.nextLine().trim();
                    System.out.print("Room number: ");
                    String r = scanner.nextLine().trim();
                    User u = Database.findUser(g);
                    if (u instanceof Guest) {
                        receptionist.checkOutGuest((Guest) u, r);
                    } else {
                        System.out.println("Guest not found.");
                    }
                }
                case "6" -> {
                    System.out.println("Logging out Receptionist...");
                    inPanel = false;
                }
                default -> System.out.println("Invalid option.");
            }
        }
    }

    // ─── GUEST FLOW ───────────────────────────────────────────────────────────────

    private static void runGuestFlow(Guest guest) {
        System.out.println("\n--- GUEST DASHBOARD ---");
        System.out.println("Welcome " + guest.getUsername() + ", let's book a room!");

        if (Database.getRooms().isEmpty()) {
            System.out.println("No rooms available in the system.");
            return;
        }

        // 1. Select Room Type
        System.out.println("\nWhat type of room are you looking for?");
        System.out.println("1. Standard");
        System.out.println("2. Deluxe");
        System.out.println("3. Suite");
        System.out.print("Your choice (1/2/3): ");

        String typeChoice = scanner.nextLine().trim();
        String targetTypeName = switch (typeChoice) {
            case "1" -> "Standard";
            case "2" -> "Deluxe";
            case "3" -> "Suite";
            default  -> "";
        };

        if (targetTypeName.isEmpty()) {
            System.out.println("Invalid choice. Booking cancelled.");
            return;
        }

        // 2. Display available rooms of the chosen type
        System.out.println("\nAvailable " + targetTypeName + " Rooms:");
        int displayed = 0;
        for (Room r : Database.getRooms()) {
            if (r.isAvailable() && r.getRoomType().getName().equalsIgnoreCase(targetTypeName)) {
                System.out.println("Room " + r.getRoomNumber() + " | Price: $" + r.getPricePerNight() + "/night");
                if (++displayed >= 5) { System.out.println("... (showing top 5 options)"); break; }
            }
        }

        if (displayed == 0) {
            System.out.println("No " + targetTypeName + " rooms currently available.");
            return;
        }

        // 3. Select Specific Room
        System.out.print("\nEnter the Room Number you want to book: ");
        int roomNumChoice;
        try {
            roomNumChoice = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Booking cancelled.");
            return;
        }

        Room selectedRoom = null;
        for (Room r : Database.getRooms()) {
            if (r.getRoomNumber() == roomNumChoice && r.isAvailable()
                    && r.getRoomType().getName().equalsIgnoreCase(targetTypeName)) {
                selectedRoom = r;
                break;
            }
        }

        if (selectedRoom == null) {
            System.out.println("Invalid or unavailable room. Booking cancelled.");
            return;
        }

        // 4. Select Nights
        System.out.print("Number of nights: ");
        int nights = 1;
        try {
            nights = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Defaulting to 1 night.");
        }

        LocalDate checkInDate  = LocalDate.now().plusDays(1);
        LocalDate checkOutDate = checkInDate.plusDays(nights);

        // 5. Create Reservation
        ReservationService reservationService = new ReservationService(Database.getRooms(), Database.getReservations());
        try {
            reservationService.createReservation(guest, selectedRoom, checkInDate, checkOutDate);
            selectedRoom.setAvailable(false);
        } catch (IllegalArgumentException e) {
            System.out.println("Failed to book room: " + e.getMessage());
            return;
        }

        // 6. Select Payment Method
        double totalCost = nights * selectedRoom.getPricePerNight();
        System.out.println("\nTotal cost for " + nights + " night(s): $" + totalCost);
        System.out.println("Select Payment Method:");
        System.out.println("1. CASH");
        System.out.println("2. CREDIT CARD");
        System.out.println("3. ONLINE");
        System.out.print("Your choice (1/2/3): ");
        String payChoice = scanner.nextLine().trim();

        PaymentMethod method = switch (payChoice) {
            case "2" -> PaymentMethod.CREDITCARD;
            case "3" -> PaymentMethod.ONLINE;
            default  -> PaymentMethod.CASH;
        };

        // 7. Generate Invoice and Print Receipt
        try {
            Invoice invoice = new Invoice(totalCost, method);
            invoice.processPayment();

            System.out.println("\n=================================");
            System.out.println("           RECEIPT");
            System.out.println("=================================");
            System.out.println("Guest Name : " + guest.getUsername());
            System.out.println("Room       : " + selectedRoom.getRoomNumber() + " (" + selectedRoom.getRoomType().getName() + ")");
            System.out.println("Check-in   : " + checkInDate);
            System.out.println("Check-out  : " + checkOutDate);
            System.out.println("Nights     : " + nights);
            System.out.println("Payment    : " + method);
            System.out.println("---------------------------------");
            System.out.println("TOTAL PAID : $" + totalCost);
            System.out.println("=================================\n");
        } catch (Exception e) {
            System.out.println("Payment processing error: " + e.getMessage());
        }

        System.out.println("[Booking complete. Logging out " + guest.getUsername() + "...]");
    }
}