import exceptions.InvalidCredentialsException;
import exceptions.InvalidPaymentException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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
            System.out.println("\n--- Main Menu ---");
            System.out.println("1. Login");
            System.out.println("2. Register as Guest");
            System.out.println("0. Exit");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> login();
                case "2" -> registerGuest();
                case "0" -> {
                    System.out.println("Goodbye!");
                    running = false;
                }
                default -> System.out.println("Invalid option. Please try again.");
            }
        }
    }

    // ─────────────────────────────────────────────
    // AUTH
    // ─────────────────────────────────────────────

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
        System.out.print("Password (min 8 chars, 1 uppercase, 1 digit): ");
        String password = scanner.nextLine().trim();
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
        System.out.print("Room Preferences (e.g. Suite, high floor): ");
        String prefs = scanner.nextLine().trim();

        Guest guest = new Guest(username, password, dob, balance, address, gender, prefs);
        try {
            guest.register();
            System.out.println("Registration successful! You can now log in.");
        } catch (InvalidCredentialsException e) {
            System.out.println("Registration failed: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // GUEST MENU
    // ─────────────────────────────────────────────

    private static void guestMenu(Guest guest) {
        boolean active = true;
        while (active) {
            System.out.println("\n--- Guest Menu [" + guest.getUsername() + "] ---");
            System.out.println("1. View Profile");
            System.out.println("2. View Available Rooms");
            System.out.println("3. Make a Reservation");
            System.out.println("4. View My Reservations");
            System.out.println("5. Cancel a Reservation");
            System.out.println("6. Checkout & Pay Invoice");
            System.out.println("0. Logout");
            System.out.print("Choose: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> viewGuestProfile(guest);
                case "2" -> viewAvailableRooms();
                case "3" -> makeReservation(guest);
                case "4" -> viewGuestReservations(guest);
                case "5" -> cancelReservation(guest);
                case "6" -> checkoutAndPay(guest);
                case "0" -> { System.out.println("Logged out."); active = false; }
                default  -> System.out.println("Invalid option.");
            }

            System.out.println("Login successful! Welcome, " + user.getUsername() + ".");
            runGuestFlow((Guest) user);

        } catch (InvalidCredentialsException e) {
            System.out.println("Login failed: " + e.getClass().getSimpleName());
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
        if (!found) System.out.println("No rooms available.");
    }

    private static void makeReservation(Guest guest) {
        System.out.println("\n--- Make a Reservation ---");
        System.out.println("Available Room Types:");
        List<RoomType> types = Database.getRoomTypes();
        for (int i = 0; i < types.size(); i++) {
            System.out.println((i + 1) + ". " + types.get(i));
        }
        System.out.print("Select room type (number): ");
        int typeChoice;
        try {
            typeChoice = Integer.parseInt(scanner.nextLine().trim()) - 1;
            if (typeChoice < 0 || typeChoice >= types.size()) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            System.out.println("Invalid selection.");
            return;
        }
        RoomType selectedType = types.get(typeChoice);

        System.out.print("Number of guests: ");
        int numGuests;
        try {
            numGuests = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid number.");
            return;
        }

        System.out.print("Check-in date (YYYY-MM-DD): ");
        LocalDate checkIn;
        System.out.print("Check-out date (YYYY-MM-DD): ");
        LocalDate checkOut;
        try {
            checkIn  = LocalDate.parse(scanner.nextLine().trim());
            checkOut = LocalDate.parse(scanner.nextLine().trim());
        } catch (DateTimeParseException e) {
            System.out.println("Invalid date format.");
            return;
        }

        ReservationService service = new ReservationService(Database.getRooms(), Database.getReservations());
        List<Room> available = service.searchAvailableRooms(checkIn, checkOut, selectedType, numGuests);

        if (available.isEmpty()) {
            System.out.println("No rooms available for the selected criteria.");
            return;
        }

        System.out.println("\nAvailable rooms:");
        for (int i = 0; i < available.size(); i++) {
            System.out.println((i + 1) + ". " + available.get(i));
        }
        System.out.print("Select room (number): ");
        int roomChoice;
        try {
            roomChoice = Integer.parseInt(scanner.nextLine().trim()) - 1;
            if (roomChoice < 0 || roomChoice >= available.size()) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            System.out.println("Invalid selection.");
            return;
        }

        try {
            Reservation res = service.createReservation(guest, available.get(roomChoice), checkIn, checkOut);
            System.out.println("Reservation created! ID: " + res.getReservationId());
            System.out.println("Status: " + res.getStatus());
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
        if (!found) System.out.println("You have no reservations.");
    }

    private static void cancelReservation(Guest guest) {
        System.out.println("\n--- Cancel a Reservation ---");
        viewGuestReservations(guest);
        System.out.print("Enter Reservation ID to cancel: ");
        String id = scanner.nextLine().trim();

        // Verify reservation belongs to this guest
        boolean owned = Database.getReservations().stream()
                .anyMatch(r -> r.getReservationId().equals(id)
                        && r.getGuest().getUsername().equalsIgnoreCase(guest.getUsername())
                        && r.getStatus() != ReservationStatus.CANCELLED);
        if (!owned) {
            System.out.println("Reservation not found or already cancelled.");
            return;
        }

        ReservationService service = new ReservationService(Database.getRooms(), Database.getReservations());
        try {
            service.cancelReservation(id);
            System.out.println("Reservation " + id + " has been cancelled.");
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
    //print receipt method
    private static void printReceipt(Guest guest, Reservation res, long nights, PaymentMethod method, double total) {
    System.out.println("\n===========================================");
    System.out.println("             HOTEL RECEIPT");
    System.out.println("===========================================");
    System.out.println("Guest Name   : " + guest.getUsername());
    System.out.println("Room Number  : " + res.getRoom().getRoomNumber());
    System.out.println("Check-in     : " + res.getCheckInDate());
    System.out.println("Check-out    : " + res.getCheckOutDate());
    System.out.println("Nights       : " + nights);
    System.out.println("Payment Type : " + method);
    System.out.println("-------------------------------------------");
    System.out.println("TOTAL PAID   : $" + total);
    System.out.println("===========================================");
    System.out.println("      Thank you for staying with us!");
    System.out.println("===========================================\n");
    //
}

    private static void checkoutAndPay(Guest guest) {
        System.out.println("\n--- Checkout & Pay ---");
        // Show confirmed reservations only
        List<Reservation> confirmed = Database.getReservations().stream()
                .filter(r -> r.getGuest().getUsername().equalsIgnoreCase(guest.getUsername())
                        && r.getStatus() == ReservationStatus.CONFIRMED)
                .toList();

        if (confirmed.isEmpty()) {
            System.out.println("No confirmed reservations to check out.");
            return;
        }

        for (int i = 0; i < confirmed.size(); i++) {
            System.out.println((i + 1) + ". " + confirmed.get(i).getReservationId()
                    + " | Room " + confirmed.get(i).getRoom().getRoomNumber()
                    + " | " + confirmed.get(i).getCheckInDate() + " → " + confirmed.get(i).getCheckOutDate());
        }

        System.out.print("Select reservation to checkout (number): ");
        int choice;
        try {
            choice = Integer.parseInt(scanner.nextLine().trim()) - 1;
            if (choice < 0 || choice >= confirmed.size()) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            System.out.println("Invalid selection.");
            return;
        }

        Reservation res = confirmed.get(choice);
        long nights = res.getCheckInDate().until(res.getCheckOutDate()).getDays();
        double total = nights * res.getRoom().getPricePerNight();

        System.out.println("\nTotal amount due: $" + total + " (" + nights + " night(s) × $" + res.getRoom().getPricePerNight() + ")");

        if (guest.getBalance() < total) {
            System.out.println("Insufficient balance ($" + guest.getBalance() + "). Please top up.");
            return;
        }

        System.out.println("Select payment method:");
        PaymentMethod[] methods = PaymentMethod.values();
        for (int i = 0; i < methods.length; i++) {
            System.out.println((i + 1) + ". " + methods[i]);
        }
        System.out.print("Choose: ");
        int pm;
        try {
            pm = Integer.parseInt(scanner.nextLine().trim()) - 1;
            if (pm < 0 || pm >= methods.length) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            System.out.println("Invalid selection.");
            return;
        }

        try {
            Invoice invoice = new Invoice(total, methods[pm]);
            invoice.processPayment();
            guest.setBalance(guest.getBalance() - total);
            res.setStatus(ReservationStatus.COMPLETED);
            res.getRoom().setAvailable(true);
            System.out.println("Checkout complete! New balance: $" + guest.getBalance());
            printReceipt(guest, res, nights, methods[pm], total);
        } catch (InvalidPaymentException e) {
            System.out.println("Payment error: " + e.getMessage());
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
                case "1" -> admin.viewGuests(Database.getGuests());
                case "2" -> admin.viewRooms(Database.getRooms().stream().map(Room::toString).toList());
                case "3" -> admin.viewReservations(Database.getReservations().stream().map(Main::reservationSummary).toList());
                case "4" -> manageRooms();
                case "5" -> manageRoomTypes();
                case "6" -> manageAmenities();
                case "0" -> { System.out.println("Logged out."); active = false; }
                default  -> System.out.println("Invalid option.");
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
                int num;
                try { num = Integer.parseInt(scanner.nextLine().trim()); }
                catch (NumberFormatException e) { System.out.println("Invalid number."); return; }

                System.out.println("Select Room Type:");
                List<RoomType> types = Database.getRoomTypes();
                for (int i = 0; i < types.size(); i++) System.out.println((i+1) + ". " + types.get(i).getName());
                System.out.print("Choice: ");
                int t;
                try { t = Integer.parseInt(scanner.nextLine().trim()) - 1; }
                catch (NumberFormatException e) { System.out.println("Invalid."); return; }

                Room newRoom = new Room(num, types.get(t));
                Database.addRoom(newRoom);
                System.out.println("Room " + num + " added.");
            }
            case "2" -> {
                System.out.print("Room number to update: ");
                int num;
                try { num = Integer.parseInt(scanner.nextLine().trim()); }
                catch (NumberFormatException e) { System.out.println("Invalid."); return; }

                Room room = findRoom(num);
                if (room == null) { System.out.println("Room not found."); return; }

                System.out.print("New room number (or same): ");
                int newNum;
                try { newNum = Integer.parseInt(scanner.nextLine().trim()); }
                catch (NumberFormatException e) { System.out.println("Invalid."); return; }

                System.out.println("Select new Room Type:");
                List<RoomType> types = Database.getRoomTypes();
                for (int i = 0; i < types.size(); i++) System.out.println((i+1) + ". " + types.get(i).getName());
                System.out.print("Choice: ");
                int t;
                try { t = Integer.parseInt(scanner.nextLine().trim()) - 1; }
                catch (NumberFormatException e) { System.out.println("Invalid."); return; }

                room.update(newNum, types.get(t), room.isAvailable());
                System.out.println("Room updated.");
            }
            case "3" -> {
                System.out.print("Room number to delete: ");
                int num;
                try { num = Integer.parseInt(scanner.nextLine().trim()); }
                catch (NumberFormatException e) { System.out.println("Invalid."); return; }

                Room room = findRoom(num);
                if (room == null) { System.out.println("Room not found."); return; }
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
                try { price = Double.parseDouble(scanner.nextLine().trim()); }
                catch (NumberFormatException e) { System.out.println("Invalid price."); return; }
                System.out.print("Capacity: ");
                int cap;
                try { cap = Integer.parseInt(scanner.nextLine().trim()); }
                catch (NumberFormatException e) { System.out.println("Invalid capacity."); return; }

                Database.addRoomType(new RoomType(name, price, cap));
                System.out.println("Room type '" + name + "' added.");
            }
            case "2" -> {
                System.out.print("Current room type name: ");
                String name = scanner.nextLine().trim();
                RoomType rt = findRoomType(name);
                if (rt == null) { System.out.println("Room type not found."); return; }

                System.out.print("New name: ");
                String newName = scanner.nextLine().trim();
                System.out.print("New price per night: ");
                double price;
                try { price = Double.parseDouble(scanner.nextLine().trim()); }
                catch (NumberFormatException e) { System.out.println("Invalid price."); return; }
                System.out.print("New capacity: ");
                int cap;
                try { cap = Integer.parseInt(scanner.nextLine().trim()); }
                catch (NumberFormatException e) { System.out.println("Invalid capacity."); return; }

                rt.update(newName, cap, price);
                System.out.println("Room type updated.");
            }
            case "3" -> {
                System.out.print("Room type name to delete: ");
                String name = scanner.nextLine().trim();
                RoomType rt = findRoomType(name);
                if (rt == null) { System.out.println("Room type not found."); return; }
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
                try { price = Double.parseDouble(scanner.nextLine().trim()); }
                catch (NumberFormatException e) { System.out.println("Invalid price."); return; }
                Database.addAmenity(name, price);
                System.out.println("Amenity '" + name + "' added.");
            }
            case "2" -> {
                System.out.print("Current amenity name: ");
                String name = scanner.nextLine().trim();
                Amenity a = findAmenity(name);
                if (a == null) { System.out.println("Amenity not found."); return; }
                System.out.print("New name: ");
                String newName = scanner.nextLine().trim();
                System.out.print("New price: ");
                double price;
                try { price = Double.parseDouble(scanner.nextLine().trim()); }
                catch (NumberFormatException e) { System.out.println("Invalid price."); return; }
                a.update(newName, price);
                System.out.println("Amenity updated.");
            }
            case "3" -> {
                System.out.print("Amenity name to delete: ");
                String name = scanner.nextLine().trim();
                Amenity a = findAmenity(name);
                if (a == null) { System.out.println("Amenity not found."); return; }
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
            System.out.println("1. View All Guests");
            System.out.println("2. View All Rooms");
            System.out.println("3. View All Reservations");
            System.out.println("4. Check-in Guest");
            System.out.println("5. Check-out Guest");
            System.out.println("0. Logout");
            System.out.print("Choose: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> rec.viewGuests(Database.getGuests());
                case "2" -> rec.viewRooms(Database.getRooms().stream().map(Room::toString).toList());
                case "3" -> rec.viewReservations(Database.getReservations().stream().map(Main::reservationSummary).toList());
                case "4" -> {
                    System.out.print("Guest username: ");
                    String uname = scanner.nextLine().trim();
                    Guest g = findGuest(uname);
                    if (g == null) { System.out.println("Guest not found."); break; }
                    System.out.print("Room number: ");
                    String room = scanner.nextLine().trim();
                    rec.checkInGuest(g, room);
                }
                case "5" -> {
                    System.out.print("Guest username: ");
                    String uname = scanner.nextLine().trim();
                    Guest g = findGuest(uname);
                    if (g == null) { System.out.println("Guest not found."); break; }
                    System.out.print("Room number: ");
                    String room = scanner.nextLine().trim();
                    rec.checkOutGuest(g, room);
                }
                case "0" -> { System.out.println("Logged out."); active = false; }
                default  -> System.out.println("Invalid option.");
            }
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

    private static String reservationSummary(Reservation r) {
        return "ID: " + r.getReservationId()
                + " | Guest: " + r.getGuest().getUsername()
                + " | Room: " + r.getRoom().getRoomNumber()
                + " | " + r.getCheckInDate() + " → " + r.getCheckOutDate()
                + " | Status: " + r.getStatus();
    }

    private static Room findRoom(int number) {
        return Database.getRooms().stream()
                .filter(r -> r.getRoomNumber() == number)
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

    private static Guest findGuest(String username) {
        return Database.getGuests().stream()
                .filter(g -> g.getUsername().equalsIgnoreCase(username))
                .findFirst().orElse(null);
    }
}