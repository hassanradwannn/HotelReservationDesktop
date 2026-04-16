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
        // 1. FIXED PASSWORD (Added capital 'A')
        Admin admin = new Admin("Admin", "Admin@123", LocalDate.of(1964, 4, 19), 6);
        Receptionist receptionist = new Receptionist("Manar", "Manar2002", LocalDate.of(2002, 6, 13), 8);

        // Separated try-catch blocks so if one fails, the other still registers
        try {
            Authentication.register(admin);
        } catch (InvalidCredentialsException e) {
            System.out.println("Failed to register Admin.");
        }

        try {
            Authentication.register(receptionist);
        } catch (InvalidCredentialsException e) {
            System.out.println("Failed to register Receptionist.");
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

            // ---> THIS IS THE CRITICAL MISSING LINE <---
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