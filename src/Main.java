import exceptions.*;

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
            SystemTime.advanceDays(days);
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
            System.out.println("6. Cancel a Reservation");
            System.out.println("0. Logout");
            System.out.print("Choose: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> viewGuestProfile(guest);
                case "2" -> viewAvailableRooms();
                case "3" -> makeReservation(guest);
                case "4" -> viewGuestReservations(guest);
                case "5" -> payDepositForReservation(guest);
                case "6" -> cancelReservation(guest);
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
            // consider room available for today if no overlapping reservation exists
            if (ReservationService.isRoomAvailable(room, SystemTime.getToday(), SystemTime.getToday().plusDays(1))) {
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
        if (selectedRoom.getRoomType().getName().equalsIgnoreCase("Penthouse")) {
            // Penthouse includes gym pass by default
            addGym = true;
        } else {
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
            System.out.println("  Status: " + res.getStatus());
            System.out.println("  Total Price: $" + String.format("%.2f", res.getTotalPrice()));
            // For same-day reservations there is no deposit
            if (res.getCheckInDate() != null && res.getCheckInDate().isEqual(SystemTime.getToday())) {
                System.out.println("  No deposit required for same-day reservations. Full payment is due at checkout.");
            } else {
                System.out.println("  Deposit Due (25%): $" + String.format("%.2f", ReservationService.getDepositAmount(res)));
                System.out.println("  Deposit Deadline: " + res.getDepositDeadline());
                System.out.println("  Please pay the deposit to confirm your reservation.");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Reservation failed: " + e.getMessage());
        }
    }

    private static void viewGuestReservations(Guest guest) {
        System.out.println("\n--- My Reservations ---");
        boolean found = false;
        for (Reservation r : Database.getReservations()) {
            if (r.getGuest().getUsername().equalsIgnoreCase(guest.getUsername())) {
                System.out.println(r);
                found = true;
            }
        }
        if (!found)
            System.out.println("You have no reservations.");
    }

    private static void cancelReservation(Guest guest) {
        System.out.println("\n--- Cancel a Reservation ---");
        viewGuestReservations(guest);
        System.out.print("Enter Reservation ID to cancel: ");
        String id = scanner.nextLine().trim();

        boolean owned = Database.getReservations().stream()
                .anyMatch(r -> r.getReservationId().equals(id)
                        && r.getGuest().getUsername().equalsIgnoreCase(guest.getUsername())
                        && r.getStatus() != ReservationStatus.CANCELLED);
        if (!owned) {
            System.out.println("Reservation not found or already cancelled.");
            return;
        }

        try {
            ReservationService.cancelReservation(id);
            System.out.println("Reservation " + id + " has been cancelled.");
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
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
            System.out.println("7. View All Room Types");
            System.out.println("8. View All Amenities");
            System.out.println("9. Register Receptionist");
            System.out.println("0. Logout");
            System.out.print("Choose: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> admin.viewGuests();
                case "2" -> admin.viewRooms();
                case "3" ->
                    admin.viewReservations();
                case "7" -> admin.viewRoomTypes();
                case "8" -> admin.viewAmenities();
                case "9" -> registerReceptionistFlow(admin);
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

                try {
                    Database.createRoom(num, types.get(t));
                    System.out.println("Room " + num + " added.");
                } catch (IllegalArgumentException e) {
                    System.out.println("Could not add room: " + e.getMessage());
                }
            }
            case "2" -> {
                System.out.print("Room number to update: ");
                String num = scanner.nextLine().trim();

                Room room = Database.findRoom(num);
                if (room == null) {
                    System.out.println("Room not found.");
                    return;
                }

                // Change type and add or remove amenities
                System.out.print("Change room type? (Y/N): ");
                String changeType = scanner.nextLine().trim();
                if (changeType.equalsIgnoreCase("Y")) {
                    System.out.println("Select new Room Type:");
                    List<RoomType> types = Database.getRoomTypes();
                    for (int i = 0; i < types.size(); i++)
                        System.out.println((i + 1) + ". " + types.get(i).getName());
                    System.out.print("Choice: ");
                    int t;
                    try {
                        t = Integer.parseInt(scanner.nextLine().trim()) - 1;
                        room.update(types.get(t));
                        System.out.println("Room type updated.");
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid.");
                    }
                }

                System.out.print("Add an amenity to room? (Y/N): ");
                if (scanner.nextLine().trim().equalsIgnoreCase("Y")) {
                    System.out.println("Available amenities:");
                    for (Amenity a : Database.getAmenities()) System.out.println("- " + a.getName());
                    System.out.print("Enter amenity name to add: ");
                    String an = scanner.nextLine().trim();
                    Amenity aobj = Database.findAmenity(an);
                    if (aobj == null) {
                        System.out.println("Amenity not found.");
                    } else {
                        room.addAmenity(aobj);
                        System.out.println("Amenity added.");
                    }
                }

                System.out.print("Remove an amenity from room? (Y/N): ");
                if (scanner.nextLine().trim().equalsIgnoreCase("Y")) {
                    System.out.println("Current amenities:");
                    for (Amenity a : room.getAmenities()) System.out.println("- " + a.getName());
                    System.out.print("Enter amenity name to remove: ");
                    String an = scanner.nextLine().trim();
                    if (room.removeAmenityByName(an)) System.out.println("Amenity removed.");
                    else System.out.println("Amenity not found on room.");
                }
            }
            case "3" -> {
                System.out.print("Room number to delete: ");
                String num = scanner.nextLine().trim();

                Room room = Database.findRoom(num);
                if (room == null) {
                    System.out.println("Room not found.");
                    return;
                }
                try {
                    Database.deleteRoom(room);
                    System.out.println("Room " + num + " deleted.");
                } catch (IllegalArgumentException e) {
                    System.out.println("Could not delete room: " + e.getMessage());
                }
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

                try {
                    Database.createRoomType(name, price, cap);
                    System.out.println("Room type '" + name + "' added.");
                } catch (IllegalArgumentException e) {
                    System.out.println("Could not add RoomType: " + e.getMessage());
                }
            }
            case "2" -> {
                System.out.print("Current room type name: ");
                String name = scanner.nextLine().trim();
                RoomType rt =  Database.findRoomType(name);
                if (rt == null) {
                    System.out.println("Room type not found.");
                    return;
                }
                // Update price and capacity (Can't change name because it's the identifier)
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

                rt.update(cap);
                rt.update(price);
                System.out.println("Room type updated.");
            }
            case "3" -> {
                System.out.print("Room type name to delete: ");
                String name = scanner.nextLine().trim();
                RoomType rt = Database.findRoomType(name);
                if (rt == null) {
                    System.out.println("Room type not found.");
                    return;
                }
                try {
                    Database.deleteRoomType(rt);
                    System.out.println("Room type '" + name + "' deleted.");
                } catch (IllegalArgumentException e) {
                    System.out.println("Could not delete RoomType: " + e.getMessage());
                }
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
                try {
                    Database.createAmenity(name, price);
                    System.out.println("Amenity '" + name + "' added.");
                } catch (IllegalArgumentException e) {
                    System.out.println("Could not add Amenity: " + e.getMessage());
                }
            }
            case "2" -> {
                System.out.print("Current amenity name: ");
                String name = scanner.nextLine().trim();
                Amenity a = Database.findAmenity(name);
                if (a == null) {
                    System.out.println("Amenity not found.");
                    return;
                }
                // Cannot change identifier (name) here. Allow updating price only.
                System.out.print("New price: ");
                double price;
                try {
                    price = Double.parseDouble(scanner.nextLine().trim());
                } catch (NumberFormatException e) {
                    System.out.println("Invalid price.");
                    return;
                }
                a.update(price);
                System.out.println("Amenity updated.");
            }
            case "3" -> {
                System.out.print("Amenity name to delete: ");
                String name = scanner.nextLine().trim();
                Amenity a = Database.findAmenity(name);
                if (a == null) {
                    System.out.println("Amenity not found.");
                    return;
                }
                try {
                    Database.deleteAmenity(a);
                    System.out.println("Amenity '" + name + "' deleted.");
                } catch (IllegalArgumentException e) {
                    System.out.println("Could not delete Amenity: " + e.getMessage());
                }
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
            System.out.println("4. View All Reservations");
            System.out.println("5. View All Guests");
            System.out.println("6. View All Rooms");
            System.out.println("0. Logout");
            System.out.print("Choose: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> rec.viewReservationsForToday();
                case "2" -> receptionistCheckIn(rec);
                case "3" -> receptionistCheckOut(rec);
                case "4" -> rec.viewReservations();
                case "5" -> rec.viewGuests();
                case "6" -> rec.viewRooms();
                case "0" -> {
                    System.out.println("Logged out.");
                    active = false;
                }
                default -> System.out.println("Invalid option.");
            }
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

    private static void registerReceptionistFlow(Admin admin) {
        System.out.println("\n--- Register Receptionist ---");
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

        System.out.print("Working hours (int): ");
        int hours;
        try {
            hours = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid number.");
            return;
        }

        try {
            admin.registerStaff(username, password, dob, hours, Role.RECEPTIONIST);
            System.out.println("Receptionist '" + username + "' registered.");
        } catch (InvalidCredentialsException e) {
            System.out.println("Registration failed: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────






}