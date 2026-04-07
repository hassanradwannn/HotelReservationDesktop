import java.time.LocalDate;
import java.util.ArrayList;

public class HotelDatabase {
    // Static lists acting as our in-memory tables
    public static ArrayList<Guest> guests = new ArrayList<>();
    public static ArrayList<Room> rooms = new ArrayList<>();
    public static ArrayList<Reservation> reservations = new ArrayList<>();
    public static ArrayList<Invoice> invoices = new ArrayList<>();

    // Optional: A list to hold staff if you want to test staff logins
    public static ArrayList<Staff> staffMembers = new ArrayList<>();

    // Method to pre-populate dummy data
    public static void initializeDummyData() {
        // 1. Create Dummy Guests
        Guest guest1 = new Guest("Radwan", "pass123", LocalDate.of(1990, 5, 15), 120.0, "123 main", Gender.MALE, "High Floor");
        guest1.setBalance(500.0);
        guests.add(guest1);

        Guest guest2 = new Guest("Zein", "pass", LocalDate.of(1992, 8, 20), 4200.0, "Zayed", Gender.FEMALE, "Near Elevator");
        guests.add(guest2);

        // 2. Create Dummy Room Types & Amenities
        // Updated to match constructor: (int typeId, String typeName, double pricePerNight, int capacity)
        RoomType singleType = new RoomType(1, "Single", 150.00, 1);
        RoomType doubleType = new RoomType(2, "Double", 250.00, 2);
        RoomType suiteType = new RoomType(3, "Suite", 500.00, 4);

        Amenity wifi = new Amenity(1, "WiFi", "High-speed wireless internet");
        Amenity tv = new Amenity(2, "Smart TV", "Smart TV with streaming services");
        Amenity minibar = new Amenity(3, "Mini-bar", "Mini-bar with drinks and snacks");

        // 3. Create Dummy Rooms and add Amenities
        Room room101 = new Room(101, "101", singleType);
        room101.addAmenity(wifi);
        room101.addAmenity(tv);
        rooms.add(room101);

        Room room205 = new Room(205, "205", doubleType);
        room205.addAmenity(wifi);
        room205.addAmenity(tv);
        rooms.add(room205);

        Room room501 = new Room(501, "501", suiteType);
        room501.addAmenity(wifi);
        room501.addAmenity(tv);
        room501.addAmenity(minibar);
        rooms.add(room501);

        // 4. Create Dummy Staff
        Admin admin1 = new Admin("admin", "adminPass123", LocalDate.of(1985, 10, 10), 8);
        staffMembers.add(admin1);

        Receptionist rec1 = new Receptionist("frontdesk", "deskPass456", LocalDate.of(1995, 2, 25), 8);
        staffMembers.add(rec1);

        System.out.println("System initialized with dummy data successfully.");
    }
}