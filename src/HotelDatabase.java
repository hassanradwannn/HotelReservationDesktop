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
        Guest guest1 = new Guest("Radwan", "pass123", LocalDate.of(1990, 5, 15), "123 Main", Gender.MALE, "High floor, quiet");
        guest1.setBalance(500.0);
        guests.add(guest1);

        Guest guest2 = new Guest("Zein", "pass", LocalDate.of(1992, 8, 20), "Zayed", Gender.FEMALE, "Near elevator");
        guests.add(guest2);

        // 2. Create Dummy Room Types & Amenities
        RoomType singleType = new RoomType("Single");
        RoomType doubleType = new RoomType("Double");
        RoomType suiteType = new RoomType("Suite");

        Amenity wifi = new Amenity("WiFi");
        Amenity tv = new Amenity("Smart TV");
        Amenity minibar = new Amenity("Mini-bar");

        // 3. Create Dummy Rooms and add Amenities
        Room room101 = new Room("101", singleType);
        room101.addAmenity(wifi);
        room101.addAmenity(tv);
        rooms.add(room101);

        Room room205 = new Room("205", doubleType);
        room205.addAmenity(wifi);
        room205.addAmenity(tv);
        rooms.add(room205);

        Room room501 = new Room("501", suiteType);
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