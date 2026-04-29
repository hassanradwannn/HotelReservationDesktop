import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import exceptions.*;



public class Database {
    // Static lists acting as our in-memory tables
    private static final ArrayList<Guest> guests = new ArrayList<>();
    private static final ArrayList<Staff> staffMembers = new ArrayList<>();
    private static final ArrayList<Room> rooms = new ArrayList<>();
    private static final ArrayList<RoomType> roomTypes = new ArrayList<>();
    private static final ArrayList<Amenity> amenities = new ArrayList<>();
    private static final ArrayList<Reservation> reservations = new ArrayList<>();
    private static final ArrayList<Invoice> invoices = new ArrayList<>();

    public static ArrayList<Guest> getGuests() { return guests; }
    public static ArrayList<Staff> getStaffMembers() { return staffMembers; }
    public static ArrayList<Room> getRooms() { return rooms; }
    public static ArrayList<RoomType> getRoomTypes() { return roomTypes; }
    public static ArrayList<Amenity> getAmenities() { return amenities; }
    public static ArrayList<Reservation> getReservations() { return reservations; }
    public static ArrayList<Invoice> getInvoices() { return invoices; }

    static {
        // Load users from SQL database
        loadUsersFromDatabase();

        // If database is empty, add demo users for testing
        if (guests.isEmpty() && staffMembers.isEmpty()) {
            initializeDemoUsers();
        }

        Amenity wifi = new Amenity("WiFi", 10);
        Amenity tv = new Amenity("Smart TV", 35);
        Amenity minibar = new Amenity("Mini-bar", 75);
        Amenity jacuzzi = new Amenity("Jacuzzi", 100);
        Amenity gym = new Amenity("Gym", 200);
        getAmenities().add(wifi);
        getAmenities().add(tv);
        getAmenities().add(minibar);
        getAmenities().add(jacuzzi);
        getAmenities().add(gym);

        RoomType standard = new RoomType("Standard", 500, 1);
        RoomType deluxe = new RoomType("Deluxe", 700, 2);
        RoomType suite = new RoomType("Suite", 1000, 4);
        RoomType penthouse = new RoomType("Penthouse", 2000, 8);
        getRoomTypes().add(standard);
        getRoomTypes().add(deluxe);
        getRoomTypes().add(suite);
        getRoomTypes().add(penthouse);

        generateRoomRange(100, 120, standard);
        generateRoomRange(200, 220, standard);
        generateRoomRange(300, 320, deluxe);
        generateRoomRange(400, 420, deluxe);
        generateRoomRange(500, 520, suite);
        generateRoomRange(600, 620, penthouse);
    }

    private static void loadUsersFromDatabase() {
        ArrayList<User> loadedUsers = UserDatabase.loadUsersFromDatabase();
        System.out.println("Loaded " + loadedUsers.size() + " users from database.");
        for (User user : loadedUsers) {
            addUser(user);
        }
    }

    private static void initializeDemoUsers() {
        System.out.println("No users found in database. Initializing demo users...");
        Admin admin = new Admin("Admin", "Admin@123", LocalDate.of(1964, 4, 19), 6);
        Receptionist receptionist = new Receptionist("Manar", "Manar2002", LocalDate.of(2002, 6, 13), 8);
        Guest defaultGuest = new Guest("Hassan", "Hassan123", LocalDate.of(2007, 4, 10), 10000, "2 haram", Gender.MALE, "");

        try {
            Authentication.register(admin);
            Authentication.register(receptionist);
            Authentication.register(defaultGuest);
        } catch (InvalidCredentialsException e) {
            System.out.println("Error: Could not initialize demo users.");
        }
    }

    private static void generateRoomRange(int start, int end, RoomType type) {
        Random rnd = new Random();
        for (int i = start; i <= end; i++) {
            Room room = new Room(String.valueOf(i), type);
            String typeName = type.getName();

            // Determine how many amenities
            int targetCount = 2;
            if (typeName.equalsIgnoreCase("Deluxe")) targetCount = 3;
            else if (typeName.equalsIgnoreCase("Suite")) targetCount = 3;
            else if (typeName.equalsIgnoreCase("Penthouse")) targetCount = 4;

            // Penthouse comes with gym pass
            if (typeName.equalsIgnoreCase("Penthouse")) {
                room.addAmenity(getAmenities().get(4));
            }

            int extrasNeeded = Math.max(0, targetCount - room.getAmenities().size());

            ArrayList<Integer> candidates = new ArrayList<>();
            if (!typeName.equalsIgnoreCase("Standard")) {
                candidates.add(2); // Mini-bar
            }

            // Jacuzzi is allowed only for Suite and Penthouse
            if (typeName.equalsIgnoreCase("Suite") || typeName.equalsIgnoreCase("Penthouse")) {
                candidates.add(3); // Jacuzzi
            }

            // Shuffle candidates and pick the needed number without repeats
            Collections.shuffle(candidates, rnd);
            for (int j = 0; j < extrasNeeded && j < candidates.size(); j++) {
                int amenityIndex = candidates.get(j);
                room.addAmenity(getAmenities().get(amenityIndex));
            }

            getRooms().add(room);
        }
    }

    public static void addUser(User user) {
        if (user instanceof Guest) {
            getGuests().add((Guest)user);
        } else {
            getStaffMembers().add((Staff)user);
        }
    }

    public static User findUser(String username) {
        for (Staff staff : getStaffMembers()) {
            if (staff.getUsername().equalsIgnoreCase(username)) return staff;
        }
        for (Guest guest : getGuests()) {
            if (guest.getUsername().equalsIgnoreCase(username)) return guest;
        }
        return null;
    }

    public static void refreshUsersFromDatabase() {
        // Clear current in-memory users
        getGuests().clear();
        getStaffMembers().clear();

        // Reload from database
        ArrayList<User> loadedUsers = UserDatabase.loadUsersFromDatabase();
        System.out.println("Refreshed " + loadedUsers.size() + " users from database.");
        for (User user : loadedUsers) {
            addUser(user);
        }
    }
}


