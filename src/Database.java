import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Random;

import exceptions.InvalidCredentialsException;

public class Database {
    // Static lists acting as our in-memory tables
    public static ArrayList<Guest> guests = new ArrayList<>();
    public static ArrayList<Staff> staffMembers = new ArrayList<>();
    public static ArrayList<Room> rooms = new ArrayList<>();
    public static ArrayList<RoomType> roomTypes = new ArrayList<>();
    public static ArrayList<Amenity> amenities = new ArrayList<>();
    public static ArrayList<Reservation> reservations = new ArrayList<>();
    public static ArrayList<Invoice> invoices = new ArrayList<>();

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
        

        Amenity wifi = new Amenity(1, "WiFi", "High-speed wireless internet", 10);
        Amenity tv = new Amenity(2, "Smart TV", "Smart TV with streaming services", 35);
        Amenity minibar = new Amenity(3, "Mini-bar", "Mini-bar with drinks and snacks", 75);
        Amenity jacuzzi = new Amenity(4, "Jacuzzi", "Jacuzzi", 75);
        addAmenity(wifi, tv, minibar, jacuzzi);

        RoomType standard = new RoomType(1, "Standard", 150, 1);
        RoomType standardDouble = new RoomType(2, "Standard", 150, 2);
        RoomType suite = new RoomType(3, "Suite", 250, 4);
        addRoomType(standard, standardDouble, suite);

         for (int i = 100; i < 200; i++) {
            int typeIndex = (i < 130) ? 0 : (i < 170) ? 1 : 2;
            Room room = new Room(i, getRoomTypes().get(typeIndex));
            
            // Add a specific number of amenities based on room type
            for (int j = 0; j < typeIndex + 1; j++) {
                int amenityIndex = new Random().nextInt(getAmenities().size());
                room.addAmenity(getAmenities().get(amenityIndex));
            }
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