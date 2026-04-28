import java.util.List;

import exceptions.*;

public abstract class CatalogService {

    public static List<RoomType> listRoomTypes() { return Database.getRoomTypes(); }
    public static List<Amenity> listAmenities() { return Database.getAmenities(); }

    public static boolean roomTypeExists(String name) {
        return Database.getRoomTypes().stream().anyMatch(rt -> rt.getName().equalsIgnoreCase(name));
    }

    public static boolean amenityExists(String name) {
        return Database.getAmenities().stream().anyMatch(a -> a.getName().equalsIgnoreCase(name));
    }

    public static boolean roomExists(String roomNumber) {
        return Database.getRooms().stream().anyMatch(r -> r.getRoomNumber().equalsIgnoreCase(roomNumber));
    }

    // Create
    public static void createRoomType(String name, double pricePerNight, int capacity) throws AlreadyExistsException {
        if (roomTypeExists(name)) throw new AlreadyExistsException("Room Type", name);
        Database.getRoomTypes().add(new RoomType(name, pricePerNight, capacity));
    }

    public static void createAmenity(String name, double price) {
        if (amenityExists(name)) throw new IllegalArgumentException("Amenity with name '" + name + "' already exists.");
        Database.getAmenities().add(new Amenity(name, price));
    }

    public static void createRoom(String roomNumber, RoomType type) {
        if (roomExists(roomNumber)) throw new IllegalArgumentException("Room with number '" + roomNumber + "' already exists.");
        if (!Database.getRoomTypes().contains(type)) throw new IllegalArgumentException("RoomType does not exist.");
        Database.getRooms().add(new Room(roomNumber, type));
    }

    // Delete
    public static void deleteRoom(Room room) throws InUseException {
        for (Reservation r : Database.getReservations()) {
            if (r.getRoom().getRoomNumber().equalsIgnoreCase(room.getRoomNumber())
                    && r.getStatus() != ReservationStatus.CANCELLED
                    && r.getStatus() != ReservationStatus.COMPLETED) {
                throw new InUseException();
            }
        }
        Database.getRooms().remove(room);
    }

    public static void deleteRoomType(RoomType roomType) throws InUseException {
        boolean inUse = Database.getRooms().stream().anyMatch(r -> r.getRoomType().getName().equalsIgnoreCase(roomType.getName()));
        if (inUse) throw new InUseException("Room Type");
        Database.getRoomTypes().remove(roomType);
    }

    public static void deleteAmenity(Amenity amenity) {
        Database.getAmenities().remove(amenity);
    }

    // Find
    public static Room findRoom(String number) {
        return Database.getRooms().stream()
                .filter(r -> r.getRoomNumber().equalsIgnoreCase(number))
                .findFirst().orElse(null);
    }

    public static RoomType findRoomType(String name) {
        return Database.getRoomTypes().stream()
                .filter(rt -> rt.getName().equalsIgnoreCase(name))
                .findFirst().orElse(null);
    }

    public static Amenity findAmenity(String name) {
        return Database.getAmenities().stream()
                .filter(a -> a.getName().equalsIgnoreCase(name))
                .findFirst().orElse(null);
    }
}
