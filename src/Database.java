import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Random;

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

    public static ArrayList<Guest> getGuests() {
        return guests;
    }
    public static ArrayList<Staff> getStaffMembers() {
        return staffMembers;
    }

    public static ArrayList<Room> getRooms() {
        return rooms;
    }

    public static ArrayList<RoomType> getRoomTypes() {
        return roomTypes;
    }

    public static ArrayList<Amenity> getAmenities() {
        return amenities;
    }

    public static ArrayList<Reservation> getReservations() {
        return reservations;
    }

    public static ArrayList<Invoice> getInvoices() {
        return invoices;
    }

    static {
        Admin admin = new Admin("Admin", "admin@123", LocalDate.of(1964, 4, 19), 6);

        Receptionist receptionist = new Receptionist("Manar", "Manar2002", LocalDate.of(2002, 6, 13), 8);

        try {
            Authentication.register(admin);
            Authentication.register(receptionist);
        } catch (InvalidCredentialsException e) {
            System.out.println(e.getMessage());
        }
        

        Amenity wifi = new Amenity("WiFi", 10);
        Amenity tv = new Amenity("Smart TV", 35);
        Amenity minibar = new Amenity("Mini-bar", 75);
        Amenity jacuzzi = new Amenity("Jacuzzi", 100);
        addAmenity(wifi, tv, minibar, jacuzzi);

        RoomType standard = new RoomType("Standard", 150, 1);
        RoomType deluxe = new RoomType("Deluxe", 275, 2);
        RoomType suite = new RoomType("Suite", 500, 4);
        addRoomType(standard, deluxe, suite);

         for (int i = 100; i < 200; i++) {
            int typeIndex = (i < 130) ? 0 : (i < 170) ? 1 : 2;
            Room room = new Room(i, getRoomTypes().get(typeIndex));
            
            // Add a specific number of amenities based on room type
            for (int j = 0; j < typeIndex + 1; j++) {
                int amenityIndex = new Random().nextInt(getAmenities().size());
                room.addAmenity(getAmenities().get(amenityIndex));
            }
         }

public class Database {
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
            getGuests().add((Guest)user);
        } else {
            getStaffMembers().add((Staff)user);
        }
    }

    public static User findUser(String username) {
        for (Staff staff : getStaffMembers()) {
            if (staff.getUsername().equalsIgnoreCase(username)) 
        for (Guest guest : guests) {
            if (guest.getUsername().equalsIgnoreCase(username))
                return guest;
        }
        for (Staff staff : staffMembers) {
            if (staff.getUsername().equalsIgnoreCase(username))
                return staff;
        }
        for (Guest guest : getGuests()) {
            if (guest.getUsername().equalsIgnoreCase(username)) 
                return guest;
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