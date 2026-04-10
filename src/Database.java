import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Random;

public class Database {
    // Static lists acting as our in-memory tables
    public static ArrayList<Guest> guests = new ArrayList<>();
    public static ArrayList<Staff> staffMembers = new ArrayList<>();
    public static ArrayList<Room> rooms = new ArrayList<>();
    public static ArrayList<Reservation> reservations = new ArrayList<>();
    public static ArrayList<Invoice> invoices = new ArrayList<>();

    // Method to pre-populate dummy data
    // Method to pre-populate dummy data
    public static void initializeDummyData() {
        // 1. Create Dummy Guests
        Guest guest1 = new Guest("Radwan", "pass123", LocalDate.of(1990, 5, 15), 120.0, "123 main", Gender.MALE, "High Floor");
        guest1.setBalance(500.0);
        guests.add(guest1);

        Guest guest2 = new Guest("Zein", "pass", LocalDate.of(1992, 8, 20), 4200.0, "Zayed", Gender.FEMALE, "Near Elevator");
        guests.add(guest2);

        // 2. Create Dummy Room Types & Amenities
        RoomType singleType = new RoomType(1, "Single", 150.00, 1);
        RoomType doubleType = new RoomType(2, "Double", 250.00, 2);
        RoomType suiteType = new RoomType(3, "Suite", 500.00, 4);

        Amenity wifi = new Amenity(1, "WiFi", "High-speed wireless internet");
        Amenity tv = new Amenity(2, "Smart TV", "Smart TV with streaming services");
        Amenity minibar = new Amenity(3, "Mini-bar", "Mini-bar with drinks and snacks");
        Amenity jacuzzi = new Amenity(4, "Jacuzzi", "Private indoor jacuzzi");

        // Extra random amenities
        Amenity safe = new Amenity(5, "Safe", "In-room digital safe");
        Amenity coffeeMaker = new Amenity(6, "Coffee Maker", "Espresso machine");
        Amenity[] randomExtras = {safe, coffeeMaker};

        // 3. Generate 100 Rooms (Floors 2 to 6, assuming Floor 1 is the Lobby)
        // - Floor 6: 20 Suites (601 - 620)
        // - Floor 5: 20 Singles (501 - 520)
        // - Floor 4: 10 Singles (411 - 420), 10 Doubles (401 - 410)
        // - Floors 2, 3: 40 Doubles (201-220, 301-320)

        Random rand = new Random();
        int roomIdCounter = 1;

        for (int floor = 2; floor <= 6; floor++) {
            for (int roomNum = 1; roomNum <= 20; roomNum++) {
                // Generate room string, e.g., 201, 614
                String formattedRoomNumber = String.format("%d%02d", floor, roomNum);
                RoomType typeToAssign;

                // NEW DISTRIBUTION LOGIC
                if (floor == 6) {
                    typeToAssign = suiteType; // 20 Suites on the top floor
                } else if (floor == 5 || (floor == 4 && roomNum > 10)) {
                    typeToAssign = singleType; // 30 Singles
                } else {
                    typeToAssign = doubleType; // 50 Doubles
                }

                Room currentRoom = new Room(roomIdCounter++, formattedRoomNumber, typeToAssign);

                // Add standard amenities to all rooms
                currentRoom.addAmenity(wifi);
                currentRoom.addAmenity(tv);
                currentRoom.addAmenity(minibar);

                // Add Jacuzzi specifically to suites
                if (typeToAssign == suiteType) {
                    currentRoom.addAmenity(jacuzzi);
                }

                // Randomly assign extra amenities
                for (Amenity extra : randomExtras) {
                    if (rand.nextBoolean()) { // 50% chance to get the safe, 50% chance for coffee maker
                        currentRoom.addAmenity(extra);
                    }
                }

                rooms.add(currentRoom);
            }
        }

        // 4. Create Dummy Staff
        Admin admin1 = new Admin("admin", "adminPass123", LocalDate.of(1985, 10, 10), 8);
        staffMembers.add(admin1);

        Receptionist rec1 = new Receptionist("frontdesk", "deskPass456", LocalDate.of(1995, 2, 25), 8);
        staffMembers.add(rec1);

        System.out.println("System initialized with dummy data successfully. 100 rooms generated.");
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