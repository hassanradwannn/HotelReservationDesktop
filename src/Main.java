import java.time.LocalDate;

public class Main {

    public static void main(String[] args) {
        // 1. Initialize the database first
        Database.initializeDummyData();

        System.out.println("\n=================================");
        System.out.println("   STARTING MILESTONE 1 TESTS");
        System.out.println("=================================");

        // 2. Display all generated rooms
        displayAllRooms();

        // 3. Run the test scenarios
        testGuestLogin();
        testRoomAvailability();
        testMakeReservation();
        testExceptionHandling();
    }

    /**
     * Display all 100 rooms and their specific amenities
     */
    public static void displayAllRooms() {
        System.out.println("\n--- COMPLETE HOTEL ROOM LIST ---");
        for (Room room : Database.rooms) {
            System.out.println(room.toString());

            System.out.print("Amenities: ");
            if (room.getAmenities().isEmpty()) {
                System.out.println("None");
            } else {
                for (int i = 0; i < room.getAmenities().size(); i++) {
                    System.out.print(room.getAmenities().get(i).getName());
                    if (i < room.getAmenities().size() - 1) {
                        System.out.print(", ");
                    }
                }
                System.out.println(); // New line after amenities
            }
            System.out.println("----------------------------------------");
        }
        System.out.println("Total rooms generated: " + Database.rooms.size());
    }

    /**
     * Test Scenario 1: Guest Authentication
     */
    public static void testGuestLogin() {
        System.out.println("\n--- Test: Guest Login (Centralized Database) ---");

        // Grab the first guest from our dummy data (Radwan)
        Guest testGuest = Database.guests.get(0);

        // 1. Attempt login with correct credentials
        try {
            boolean isSuccess = testGuest.login("Radwan", "pass123");
            System.out.println("Login attempt for 'Radwan' with correct password: " + (isSuccess ? "SUCCESS" : "FAILED"));
        } catch (InvalidCredentialsException e) {
            System.out.println("Login attempt for 'Radwan' with correct password: FAILED (" + e.getClass().getSimpleName() + ")");
        }

        // 2. Attempt login with incorrect password
        try {
            boolean isFail = testGuest.login("Radwan", "wrongpassword");
            System.out.println("Login attempt for 'Radwan' with wrong password: " + (!isFail ? "FAILED AS EXPECTED" : "SUCCESS (ERROR)"));
        } catch (InvalidCredentialsException e) {
            // It will catch your new InvalidPasswordException here!
            System.out.println("Login attempt for 'Radwan' with wrong password: FAILED AS EXPECTED (" + e.getClass().getSimpleName() + ")");
        }

        // 3. Attempt login with non-existent user
        try {
            // Calling the static Database method directly to test a user that isn't in the system
            Authentication.login("GhostUser", "pass123", true);
            System.out.println("Login attempt for 'GhostUser': SUCCESS (ERROR - should not exist)");
        } catch (InvalidCredentialsException e) {
            // It will catch your new UserNotFoundException here!
            System.out.println("Login attempt for 'GhostUser': FAILED AS EXPECTED (" + e.getClass().getSimpleName() + ")");
        }
    }

    /**
     * Test Scenario 2: Browsing Rooms
     */
    public static void testRoomAvailability() {
        System.out.println("\n--- Test: View All Rooms ---");

        if (Database.rooms.isEmpty()) {
            System.out.println("No rooms found in database.");
            return;
        }

        for (Room room : Database.rooms) {
            System.out.println("Room " + room.getRoomNumber() +
                    " | Type: " + room.getRoomType().getTypeName() +
                    " | Amenities: " + room.getAmenities().size());
        }
    }

    /**
     * Test Scenario 3: Creating a Reservation
     */
    public static void testMakeReservation() {
        System.out.println("\n--- Test: Make Reservation ---");

        try {
            Guest guest = Database.guests.get(0);
            Room room = Database.rooms.get(0); // Grab Room 101

            // Check in today, check out in 3 days
            LocalDate checkIn = LocalDate.now();
            LocalDate checkOut = LocalDate.now().plusDays(3);

            // Create the reservation
            Reservation res = new Reservation(guest, room, checkIn, checkOut);

            System.out.println("Reservation successfully created!");
            System.out.println("Guest: " + res.getGuest().getUsername());
            System.out.println("Room: " + res.getRoom().getRoomNumber());
            System.out.println("Status: " + res.getReservationStatus());

        } catch (IllegalArgumentException e) {
            System.out.println("Failed to make reservation: " + e.getMessage());
        }
    }

    public static void testExceptionHandling() {
        System.out.println("\n--- Test: Custom Exception Handling (Invoice) ---");

        // 1. Test the expected failure (Negative Amount)
        System.out.println("Attempting to generate an invoice with a negative amount (-50.0)...");
        try {
            Invoice badInvoice = new Invoice(-50.0, PaymentMethod.CREDIT_CARD);
            System.out.println("ERROR: Invoice created successfully. Validation failed.");
        } catch (InvalidPaymentException e) {
            // This is the expected outcome!
            System.out.println("Caught Expected Custom Exception -> " + e.getMessage());
        }

        // 2. Test the successful payment using the Payable interface
        System.out.println("\nAttempting to generate a valid invoice ($150.0)...");
        try {
            Invoice goodInvoice = new Invoice(150.0, PaymentMethod.CASH);
            System.out.println("Invoice generated successfully.");

            // Capture the boolean result from the interface method
            boolean isPaid = goodInvoice.processPayment();

            // Print the explicit final status
            if (isPaid) {
                System.out.println("Final Status: SUCCESS");
            } else {
                System.out.println("Final Status: FAILED");
            }

        } catch (InvalidPaymentException e) {
            System.out.println("Unexpected Error: " + e.getMessage());
        }
    }
}