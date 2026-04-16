import java.time.LocalDate;
import java.util.Scanner;
import exceptions.InvalidCredentialsException;

public class Main {

    public static void main(String[] args) {
        System.out.println("Initializing Database...");
        System.out.println("System loaded with " + Database.getRooms().size() + " rooms.");
        System.out.println("System loaded with " + Database.getStaffMembers().size() + " staff members.\n");

        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.println("\n=================================");
            System.out.println("         SYSTEM LOGIN");
            System.out.println("=================================");
            System.out.print("Enter username (or type 'exit' to quit): ");
            String username = scanner.nextLine().trim();

            if (username.equalsIgnoreCase("exit")) {
                System.out.println("Exiting the system. Goodbye!");
                running = false;
                break;
            }

            System.out.print("Enter password: ");
            String password = scanner.nextLine().trim();

            System.out.println("\nAuthenticating user: '" + username + "'...");

            try {
                User loggedInUser = Authentication.login(username, password);

                System.out.println("Login SUCCESS! Welcome, " + loggedInUser.getUsername());
                System.out.println("Role: " + loggedInUser.getClass().getSimpleName());

                // Route to the correct flow and pass the scanner for interactive input
                if (loggedInUser instanceof Admin) {
                    runAdminFlow((Admin) loggedInUser);
                } else if (loggedInUser instanceof Guest) {
                    runGuestFlow((Guest) loggedInUser, scanner);
                } else {
                    System.out.println("\n[Standard Staff Confirmed - No specific tests assigned]");
                }

            } catch (InvalidCredentialsException e) {
                System.out.println("Login FAILED: " + e.getClass().getSimpleName());
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }

        scanner.close();
    }

    /**
     * Admin Flow: Registers a new user and then logs out.
     */
    private static void runAdminFlow(Admin admin) {
        System.out.println("\n--- ADMIN PANEL ---");
        System.out.println("Task: Registering new guest 'Omar'...");

        Guest newGuest = new Guest("Omar", "OmarPass123", LocalDate.of(2001, 8, 22), 1500.0, "Cairo", null, "High Floor");

        try {
            Authentication.register(newGuest);
            System.out.println("SUCCESS: Admin registered new guest '" + newGuest.getUsername() + "'.");
            System.out.println("Total guests currently in database: " + Database.getGuests().size());
            System.out.println("\n[Logging out Admin... Please log back in as 'Omar' to book a room]");
        } catch (InvalidCredentialsException e) {
            System.out.println("FAILED to add user: " + e.getClass().getSimpleName() + " (User might already exist).");
        }
    }

    /**
     * Guest Flow: Interactive booking system.
     */
    /**
     * Guest Flow: Interactive booking system with Room Type filtering.
     */
    private static void runGuestFlow(Guest guest, Scanner scanner) {
        System.out.println("\n--- GUEST DASHBOARD ---");
        System.out.println("Welcome " + guest.getUsername() + ", let's book a room!");

        if (Database.getRooms().isEmpty()) {
            System.out.println("FAILED: Database has 0 rooms available.");
            return;
        }

        // 1. Select Room Type
        System.out.println("\nWhat type of room are you looking for?");
        System.out.println("1. Standard");
        System.out.println("2. Deluxe");
        System.out.println("3. Suite");
        System.out.print("Your choice (1/2/3): ");

        String typeChoice = scanner.nextLine().trim();
        String targetTypeName = "";

        if (typeChoice.equals("1")) targetTypeName = "Standard";
        else if (typeChoice.equals("2")) targetTypeName = "Deluxe";
        else if (typeChoice.equals("3")) targetTypeName = "Suite";
        else {
            System.out.println("Invalid choice. Booking cancelled.");
            return;
        }

        // 2. Display available rooms of the chosen type
        System.out.println("\nAvailable " + targetTypeName + " Rooms:");
        int displayed = 0;
        for (Room r : Database.getRooms()) {
            if (r.isAvailable() && r.getRoomType().getName().equalsIgnoreCase(targetTypeName)) {
                System.out.println("Room " + r.getRoomNumber() + " | Price: $" + r.getPricePerNight() + "/night");
                displayed++;
                if (displayed >= 5) {
                    System.out.println("... (showing top 5 options)");
                    break;
                }
            }
        }

        if (displayed == 0) {
            System.out.println("Sorry, there are no " + targetTypeName + " rooms currently available. Please try another type.");
            return;
        }

        // 3. Select Specific Room
        System.out.print("\nEnter the Room Number you want to book: ");
        int roomNumChoice = -1;
        try {
            roomNumChoice = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Booking cancelled.");
            return;
        }

        Room selectedRoom = null;
        for (Room r : Database.getRooms()) {
            if (r.getRoomNumber() == roomNumChoice && r.isAvailable() && r.getRoomType().getName().equalsIgnoreCase(targetTypeName)) {
                selectedRoom = r;
                break;
            }
        }

        if (selectedRoom == null) {
            System.out.println("Invalid room selection or room is unavailable. Booking cancelled.");
            return;
        }

        // 4. Select Nights
        System.out.print("Enter the number of nights you want to stay: ");
        int nights = 1;
        try {
            nights = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Defaulting to 1 night.");
            nights = 1;
        }

        LocalDate checkInDate = LocalDate.now().plusDays(1);
        LocalDate checkOutDate = checkInDate.plusDays(nights);

        // 5. Create Reservation
        ReservationService reservationService = new ReservationService(Database.getRooms(), Database.getReservations());
        try {
            reservationService.createReservation(guest, selectedRoom, checkInDate, checkOutDate);
            selectedRoom.setAvailable(false); // Update availability
        } catch (IllegalArgumentException e) {
            System.out.println("FAILED to book room: " + e.getMessage());
            return;
        }

        // 6. Select Payment Method
        double totalCost = nights * selectedRoom.getPricePerNight();
        System.out.println("\nTotal Cost for " + nights + " nights: $" + totalCost);
        System.out.println("Select Payment Method:");
        System.out.println("1. CASH");
        System.out.println("2. CREDIT CARD");
        System.out.println("3. ONLINE");
        System.out.print("Your choice (1/2/3): ");
        String payChoice = scanner.nextLine().trim();

        PaymentMethod method = PaymentMethod.CASH;
        if (payChoice.equals("2")) method = PaymentMethod.CREDITCARD;
        else if (payChoice.equals("3")) method = PaymentMethod.ONLINE;

        // 7. Generate Invoice and Print Receipt
        try {
            Invoice invoice = new Invoice(totalCost, method);
            Database.getInvoices().add(invoice);

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