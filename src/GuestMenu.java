import exceptions.RoomNotAvailableException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class GuestMenu {
    private static final Scanner scanner = new Scanner(System.in);

    public static void show(Guest guest) {
        boolean active = true;
        while (active) {
            System.out.println("\n--- Guest Menu [" + guest.getUsername() + "] ---");
            System.out.println("(Current date: " + SystemTime.getDate() + ")");
            System.out.println("1. View Profile");
            System.out.println("2. View Available Rooms");
            System.out.println("3. Make a Reservation");
            System.out.println("4. View My Reservations");
            System.out.println("5. Payments");
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
            Reservation res = ReservationService.createReservation(guest, selectedRoom, checkIn, checkOut, addGym);
            
            LocalDate today = SystemTime.getToday();
            long daysUntilCheckIn = today.until(checkIn).getDays();
            
            System.out.println("Reservation created! ID: " + res.getReservationId());
            System.out.println("  Status: PENDING");
            System.out.println("  Total Price: $" + String.format("%.2f", res.getTotalPrice()));
            
            // Determine payment requirement based on days until check-in
            if (daysUntilCheckIn == 0) {
                System.out.println("  Payment: FULL AMOUNT REQUIRED (same-day booking)");
                System.out.println("  Pay now? (Y/N): ");
                String payNow = scanner.nextLine().trim();
                if (payNow.equalsIgnoreCase("Y")) {
                    try {
                        ReservationService.payInFull(res, guest);
                        System.out.println("✓ Full amount paid! Reservation is CONFIRMED.");
                        System.out.println("New balance: $" + String.format("%.2f", guest.getBalance()));
                        Receipt receipt = Receipt.createFullPaymentReceipt(res);
                        receipt.print();
                    } catch (IllegalArgumentException e) {
                        System.out.println("Payment failed: " + e.getMessage());
                    }
                } else {
                    System.out.println("Please pay the full amount to confirm your reservation.");
                }
            } else if (daysUntilCheckIn == 1) {
                double deposit = ReservationService.getDepositAmount(res);
                System.out.println("  Deposit Due (First Night): $" + String.format("%.2f", deposit));
                System.out.println("  Deposit Deadline: " + res.getDepositDeadline());
                System.out.println("  Pay deposit now? (Y/N): ");
                String payNow = scanner.nextLine().trim();
                if (payNow.equalsIgnoreCase("Y")) {
                    try {
                        ReservationService.payDeposit(res, guest);
                        System.out.println("✓ Deposit paid! Reservation is CONFIRMED.");
                        System.out.println("New balance: $" + String.format("%.2f", guest.getBalance()));
                        Receipt receipt = Receipt.createDepositReceipt(res);
                        receipt.print();
                    } catch (IllegalArgumentException e) {
                        System.out.println("Payment failed: " + e.getMessage());
                    }
                } else {
                    System.out.println("Please pay the deposit to confirm your reservation.");
                }
            } else {
                System.out.println("  Deposit Due (First Night): $" + String.format("%.2f", ReservationService.getDepositAmount(res)));
                System.out.println("  Deposit Deadline: " + res.getDepositDeadline());
                System.out.println("  Please pay the deposit to confirm your reservation.");
            }
        } catch (RoomNotAvailableException e) {
            System.out.println("Reservation failed: Room is no longer available. " + e.getMessage());
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
                    + " | canExtend: " + (r.canManage() || r.canOnlyExtend() || r.canExtend()));
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
                    if (!selected.canManage() && !selected.canOnlyExtend() && !selected.canExtend()) {
                        System.out.println("Cannot extend: reservation cannot be modified at this time.");
                    } else {
                        System.out.println("Current check-out: " + selected.getCheckOutDate());
                        System.out.print("Enter number of days to extend: ");
                        int daysToAdd;
                        try {
                            daysToAdd = Integer.parseInt(scanner.nextLine().trim());
                            if (daysToAdd <= 0) {
                                System.out.println("Please enter a positive number of days.");
                            } else {
                                LocalDate oldCheckOut = selected.getCheckOutDate();
                                LocalDate newCheckOut = oldCheckOut.plusDays(daysToAdd);
                                double oldPrice = selected.getTotalPrice();
                                ReservationService.extendStay(selected.getReservationId(), newCheckOut);
                                double newPrice = selected.getTotalPrice();
                                double additionalCost = newPrice - oldPrice;
                                System.out.println("✓ Stay extended by " + daysToAdd + " days!");
                                System.out.println("  Old check-out: " + oldCheckOut);
                                System.out.println("  New check-out: " + newCheckOut);
                                System.out.println("  Additional cost: $" + String.format("%.2f", additionalCost));
                                System.out.println("  (Will be paid at checkout)");
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid input. Please enter a number.");
                        } catch (IllegalArgumentException e) {
                            System.out.println("Error: " + e.getMessage());
                        }
                    }
                }
                case "3" -> manageAddOns(guest, selected);
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
        
        System.out.println("\nAvailable add-ons to add:");
        List<String> availableOptions = new ArrayList<>();
        int idx = 1;
        
        if (!reservation.hasGymPass()) {
            System.out.println("  " + idx + ". Gym Pass ($200) - PAID UPFRONT");
            availableOptions.add("Gym");
            idx++;
        }
        
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
                    reservation.update();
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
        List<Reservation> activeReservations = Database.getReservations().stream()
                .filter(r -> r.getGuest().getUsername().equalsIgnoreCase(guest.getUsername())
                        && r.getStatus() != ReservationStatus.COMPLETED
                        && r.getStatus() != ReservationStatus.CANCELLED)
                .toList();

        if (activeReservations.isEmpty()) {
            System.out.println("You have no active reservations.");
            return;
        }

        boolean inPaymentMenu = true;
        while (inPaymentMenu) {
            System.out.println("\n--- Payment & Status ---");
            System.out.println("Your Reservations:");
            for (int i = 0; i < activeReservations.size(); i++) {
                Reservation r = activeReservations.get(i);
                String paidStatus;
                double outstanding = r.getActualOutstanding();
                if (outstanding <= 0) {
                    paidStatus = "Full";
                } else if (r.isDepositPaid()) {
                    paidStatus = "Deposit";
                } else {
                    paidStatus = "None";
                }
                System.out.println((i + 1) + ". " + r.getReservationId()
                        + " | Room " + r.getRoom().getRoomNumber()
                        + " | " + r.getCheckInDate() + " → " + r.getCheckOutDate()
                        + " | Status: " + r.getStatus()
                        + " | Paid: " + paidStatus);
            }

            System.out.println("\n1. Pay Deposit (for PENDING reservations)");
            System.out.println("2. Pay Full Amount");
            System.out.println("3. View Outstanding Fees & Add-ons");
            System.out.println("0. Back");
            System.out.print("Choose: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> payDeposit(guest, activeReservations);
                case "2" -> payFullAmount(guest, activeReservations);
                case "3" -> viewOutstandingFees(guest, activeReservations);
                case "0" -> inPaymentMenu = false;
                default -> System.out.println("Invalid option.");
            }
        }
    }
    
    private static void payDeposit(Guest guest, List<Reservation> reservations) {
        List<Reservation> pending = reservations.stream()
                .filter(r -> r.getStatus() == ReservationStatus.PENDING && !r.isDepositPaid())
                .toList();

        if (pending.isEmpty()) {
            System.out.println("No pending reservations requiring deposit.");
            return;
        }

        System.out.println("\n--- Pay Deposit ---");
        for (int i = 0; i < pending.size(); i++) {
            Reservation r = pending.get(i);
            double deposit = ReservationService.getDepositAmount(r);
            System.out.println((i + 1) + ". " + r.getReservationId()
                    + " | Room " + r.getRoom().getRoomNumber()
                    + " | Check-in: " + r.getCheckInDate()
                    + " | Deposit: $" + String.format("%.2f", deposit)
                    + " | Deadline: " + r.getDepositDeadline());
            if (r.isDepositOverdue()) {
                System.out.println("   ⚠️ OVERDUE!");
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

        System.out.println("\nDeposit: $" + String.format("%.2f", deposit));
        System.out.println("Your Balance: $" + String.format("%.2f", guest.getBalance()));

        if (guest.getBalance() < deposit) {
            System.out.println("Insufficient balance.");
            return;
        }

        System.out.print("Confirm payment? (Y/N): ");
        if (!scanner.nextLine().trim().equalsIgnoreCase("Y")) {
            System.out.println("Payment cancelled.");
            return;
        }

        try {
            ReservationService.payDeposit(res, guest);
            System.out.println("✓ Deposit paid! Reservation is now CONFIRMED.");
            System.out.println("New balance: $" + String.format("%.2f", guest.getBalance()));
            Receipt depositReceipt = Receipt.createDepositReceipt(res);
            depositReceipt.print();
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
    
    private static void payFullAmount(Guest guest, List<Reservation> reservations) {
        List<Reservation> confirmed = reservations.stream()
                .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED && !r.isFullPaid())
                .toList();

        if (confirmed.isEmpty()) {
            System.out.println("No reservations requiring full payment.");
            return;
        }

        System.out.println("\n--- Pay Full Amount ---");
        for (int i = 0; i < confirmed.size(); i++) {
            Reservation r = confirmed.get(i);
            double remaining = r.getRemainingBalance();
            System.out.println((i + 1) + ". " + r.getReservationId()
                    + " | Room " + r.getRoom().getRoomNumber()
                    + " | Check-in: " + r.getCheckInDate()
                    + " | Due: $" + String.format("%.2f", remaining));
        }

        System.out.print("Select reservation (number): ");
        int choice;
        try {
            choice = Integer.parseInt(scanner.nextLine().trim()) - 1;
            if (choice < 0 || choice >= confirmed.size())
                throw new NumberFormatException();
        } catch (NumberFormatException e) {
            System.out.println("Invalid selection.");
            return;
        }

        Reservation res = confirmed.get(choice);
        double remaining = res.getRemainingBalance();

        System.out.println("\nAmount Due: $" + String.format("%.2f", remaining));
        System.out.println("Your Balance: $" + String.format("%.2f", guest.getBalance()));

        if (guest.getBalance() < remaining) {
            System.out.println("Insufficient balance.");
            return;
        }

        System.out.print("Confirm payment? (Y/N): ");
        if (!scanner.nextLine().trim().equalsIgnoreCase("Y")) {
            System.out.println("Payment cancelled.");
            return;
        }

        try {
            ReservationService.payFullAmount(res, guest);
            System.out.println("✓ Full amount paid!");
            System.out.println("New balance: $" + String.format("%.2f", guest.getBalance()));
            Receipt fullReceipt = Receipt.createFullPaymentReceipt(res);
            fullReceipt.print();
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
    
    private static void viewOutstandingFees(Guest guest, List<Reservation> reservations) {
        System.out.println("\n=== Outstanding Fees & Add-ons ===");
        System.out.println("Your Balance: $" + String.format("%.2f", guest.getBalance()));
        System.out.println();
        
        boolean hasAnything = false;
        
        for (Reservation r : reservations) {
            System.out.println("Reservation: " + r.getReservationId() + " | Room " + r.getRoom().getRoomNumber());
            
            double actualOutstanding = r.getActualOutstanding();
            double totalPrice = r.getTotalPrice();
            double paidAmount = r.getPaidAmount();
            
            if (r.isDepositPaid()) {
                System.out.println("  ✓ Deposit Paid");
            } else if (r.getStatus() == ReservationStatus.PENDING) {
                double deposit = ReservationService.getDepositAmount(r);
                System.out.println("  ⚠️ Deposit Due: $" + String.format("%.2f", deposit) + " (Deadline: " + r.getDepositDeadline() + ")");
                hasAnything = true;
            }
            
            if (actualOutstanding <= 0) {
                System.out.println("  ✓ Fully Paid");
            } else if (r.getStatus() == ReservationStatus.ONGOING) {
                System.out.println("  ⚠️ Extended Stay Balance Due: $" + String.format("%.2f", actualOutstanding));
                hasAnything = true;
            } else if (r.getStatus() == ReservationStatus.CONFIRMED) {
                double remaining = r.getRemainingBalance();
                System.out.println("  ⚠️ Balance Due (at check-in): $" + String.format("%.2f", remaining));
                hasAnything = true;
            }
            
            if (r.getOriginalCheckOutDate() != null && !r.getOriginalCheckOutDate().equals(r.getCheckOutDate())) {
                System.out.println("  ℹ️ Stay Extended: Original $" + String.format("%.2f", paidAmount) 
                    + " paid, Total $" + String.format("%.2f", totalPrice) + ", Outstanding $" + String.format("%.2f", actualOutstanding));
            }
            
            System.out.println("  Add-ons:");
            if (r.hasGymPass()) {
                System.out.println("    ✓ Gym Pass (included in total)");
            }
            if (r.hasRestaurant()) {
                System.out.println("  ⚠️ Restaurant (due at checkout): $150.00");
                hasAnything = true;
            }
            
            System.out.println("  Room Amenities:");
            for (Amenity a : r.getRoom().getAmenities()) {
                System.out.println("    - " + a.getName() + " ($" + a.getPrice() + ")");
            }
            
            System.out.println("  Total Price: $" + String.format("%.2f", r.getTotalPrice()));
            System.out.println();
        }
        
        if (!hasAnything) {
            System.out.println("No outstanding fees. All payments up to date!");
        }
    }

    private static void printReservation(Reservation r) {
        System.out.println("ID: " + r.getReservationId()
                + " | Room: " + r.getRoom().getRoomNumber()
                + " | " + r.getCheckInDate() + " → " + r.getCheckOutDate()
                + " | Status: " + r.getStatus());
    }
}