import java.time.LocalDate;
import java.util.ArrayList;

import exceptions.InvalidCredentialsException;

public class Database {
    // Static lists acting as our in-memory tables
    private static ArrayList<Guest> guests = new ArrayList<>();
    private static ArrayList<Staff> staffMembers = new ArrayList<>();
    private static ArrayList<Room> rooms = new ArrayList<>();
    private static ArrayList<RoomType> roomTypes = new ArrayList<>();
    private static ArrayList<Amenity> amenities = new ArrayList<>();
    private static ArrayList<Reservation> reservations = new ArrayList<>();
    private static ArrayList<Invoice> invoices = new ArrayList<>();

    public static ArrayList<Guest> getGuests() { return guests; }
    public static ArrayList<Staff> getStaffMembers() { return staffMembers; }
    public static ArrayList<Room> getRooms() { return rooms; }
    public static ArrayList<RoomType> getRoomTypes() { return roomTypes; }
    public static ArrayList<Amenity> getAmenities() { return amenities; }
    public static ArrayList<Reservation> getReservations() { return reservations; }
    public static ArrayList<Invoice> getInvoices() { return invoices; }

    static {
        // 1. Staff Registration
        Admin admin = new Admin("Admin", "Admin@123", LocalDate.of(1964, 4, 19), 6);
        Receptionist receptionist = new Receptionist("Manar", "Manar2002", LocalDate.of(2002, 6, 13), 8);

        try {
            Authentication.register(admin);
            Authentication.register(receptionist);
        } catch (InvalidCredentialsException e) {
            System.out.println("Error: Could not initialize staff.");
        }

        // 2. Amenities (Indices: 0=WiFi, 1=TV, 2=Mini-bar, 3=Jacuzzi, 4=Gym)
        Amenity wifi = new Amenity("WiFi", 10);
        Amenity tv = new Amenity("Smart TV", 35);
        Amenity minibar = new Amenity("Mini-bar", 75);
        Amenity jacuzzi = new Amenity("Jacuzzi", 100);
        Amenity gym = new Amenity("Gym", 200);
        addAmenity(wifi, tv, minibar, jacuzzi, gym);

        // 3. Room Types
        RoomType standard = new RoomType("Standard", 150, 1);
        RoomType deluxe = new RoomType("Deluxe", 275, 2);
        RoomType suite = new RoomType("Suite", 500, 4);
        RoomType penthouse = new RoomType("Penthouse", 1000, 8);
        addRoomType(standard, deluxe, suite, penthouse);

        // 4. Generate Room Ranges
        generateRoomRange(100, 120, standard);
        generateRoomRange(200, 220, standard);
        generateRoomRange(300, 320, deluxe);
        generateRoomRange(400, 420, deluxe);
        generateRoomRange(500, 520, suite);
        generateRoomRange(600, 620, penthouse);
    }

    private static void generateRoomRange(int start, int end, RoomType type) {
        for (int i = start; i <= end; i++) {
            Room room = new Room(String.valueOf(i), type);
            String typeName = type.getName();

            // 1. Standard in ALL rooms
            room.addAmenity(getAmenities().get(0)); // WiFi
            room.addAmenity(getAmenities().get(1)); // TV
            room.addAmenity(getAmenities().get(2)); // Mini-bar

            // 2. Jacuzzi is exclusive to Suite and Penthouse
            if (typeName.equalsIgnoreCase("Suite") || typeName.equalsIgnoreCase("Penthouse")) {
                room.addAmenity(getAmenities().get(3)); // Jacuzzi
            }

            // 3. Gym is included for free/by default ONLY in Penthouse
            if (typeName.equalsIgnoreCase("Penthouse")) {
                room.addAmenity(getAmenities().get(4)); // Gym
            }

            addRoom(room);
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

    public static void addAmenity(String name, double price) {
        addAmenity(new Amenity(name, price));
    }

    public static void addAmenity(Amenity... items) {
        for (Amenity amenity : items) {
            getAmenities().add(amenity);
        }
    }

    public static void addRoomType(RoomType... items) {
        for (RoomType roomType : items) {
            getRoomTypes().add(roomType);
        }
    }

    public static void addRoom(Room room) {
        getRooms().add(room);
    }
}