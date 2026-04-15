import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Random;
import exceptions.*;

public class DBOLD {
    // Static lists acting as our in-memory tables
    public static ArrayList<Guest> guests = new ArrayList<>();
    public static ArrayList<Staff> staffMembers = new ArrayList<>();
    public static ArrayList<Room> rooms = new ArrayList<>();
    public static ArrayList<Reservation> reservations = new ArrayList<>();
    public static ArrayList<Invoice> invoices = new ArrayList<>();

    // Method to pre-populate dummy data
    // Method to pre-populate dummy data
    // Method to pre-populate dummy data
    public static void initializeDummyData() {
        // 1. Create Dummy Guests
        Guest guest1 = new Guest("Radwan", "pass123", LocalDate.of(1990, 5, 15), 120.0, "123 main", Gender.MALE, "High Floor");
        guest1.setBalance(500.0);
        guests.add(guest1);

        Guest guest2 = new Guest("Zein", "pass", LocalDate.of(1992, 8, 20), 4200.0, "Zayed", Gender.FEMALE, "Near Elevator");
        guests.add(guest2);

        // 2. Create Dummy Room Types (Standard vs Luxury)
        RoomType singleStandard = new RoomType(1, "Single Standard", 150.00, 1);
        RoomType singleLuxury = new RoomType(2, "Single Luxury", 200.00, 1);

        RoomType doubleStandard = new RoomType(3, "Double Standard", 250.00, 2);
        RoomType doubleLuxury = new RoomType(4, "Double Luxury", 320.00, 2);

        RoomType suiteStandard = new RoomType(5, "Suite Standard", 500.00, 4);
        RoomType suiteLuxury = new RoomType(6, "Suite Luxury", 650.00, 4);

        // 3. Define All Amenities
        Amenity wifi = new Amenity(1, "WiFi", "Standard wireless internet");
        Amenity tv = new Amenity(2, "Smart TV", "Smart TV with streaming services");
        Amenity minibar = new Amenity(3, "Mini-bar", "Mini-bar with drinks and snacks");
        Amenity jacuzzi = new Amenity(4, "Jacuzzi", "Private indoor jacuzzi");
        Amenity safe = new Amenity(5, "Safe", "In-room digital safe");
        Amenity coffeeMaker = new Amenity(6, "Coffee Maker", "Espresso machine");

        // New Tiered Amenities
        Amenity highSpeedWifi = new Amenity(7, "High-Speed WiFi", "Premium high-speed wireless internet");
        Amenity widescreenTv = new Amenity(8, "Widescreen TV", "75-inch Widescreen Smart TV");

        // 4. Generate 100 Rooms (Floors 2 to 6, assuming Floor 1 is the Lobby)
        int roomIdCounter = 1;

        for (int floor = 2; floor <= 6; floor++) {
            for (int roomNum = 1; roomNum <= 20; roomNum++) {
                String formattedRoomNumber = String.format("%d%02d", floor, roomNum);
                RoomType typeToAssign;

                // Even-numbered rooms will be Luxury, Odd-numbered will be Standard
                boolean isLuxury = (roomNum % 2 == 0);

                // Determine Base Room Type
                if (floor == 6) {
                    typeToAssign = isLuxury ? suiteLuxury : suiteStandard;
                } else if (floor == 5 || (floor == 4 && roomNum > 10)) {
                    typeToAssign = isLuxury ? singleLuxury : singleStandard;
                } else {
                    typeToAssign = isLuxury ? doubleLuxury : doubleStandard;
                }

                Room currentRoom = new Room(roomIdCounter++, formattedRoomNumber, typeToAssign);
                currentRoom.addAmenity(minibar);
                currentRoom.addAmenity(isLuxury ? highSpeedWifi : wifi);
                currentRoom.addAmenity(typeToAssign == suiteLuxury ? widescreenTv : tv);

                if (isLuxury) {
                    currentRoom.addAmenity(safe);
                }

                if (floor == 6) {
                    currentRoom.addAmenity(jacuzzi);
                }

                if (typeToAssign == suiteLuxury) {
                    currentRoom.addAmenity(coffeeMaker);
                }

                rooms.add(currentRoom);
            }
        }

        // 5. Create Dummy Staff
        Admin admin1 = new Admin("admin", "adminPass123", LocalDate.of(1985, 10, 10), 8);
        staffMembers.add(admin1);

        Receptionist rec1 = new Receptionist("frontdesk", "deskPass456", LocalDate.of(1995, 2, 25), 8);
        staffMembers.add(rec1);

        System.out.println("System initialized with dummy data successfully. 100 structured rooms generated with tiered amenities.");
    }

    public static void addUser(User user) {
        if (user instanceof Guest) {
            guests.add((Guest)user);
        } else {
            staffMembers.add((Staff)user);
        }
    }

    public static User findUser(String username) {
        for (Guest guest : guests) {
            if (guest.getUsername().equalsIgnoreCase(username))
                return guest;
        }
        for (Staff staff : staffMembers) {
            if (staff.getUsername().equalsIgnoreCase(username))
                return staff;
        }
        return null;
    }
}