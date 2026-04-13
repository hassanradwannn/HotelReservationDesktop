import java.util.ArrayList;

public class RoomManager {

    ArrayList<Room> rooms = Database.rooms;
    ArrayList<RoomType> roomTypes = Database.roomTypes;
    ArrayList<Amenity> amenities = Database.amenities;

    public ArrayList<Room> getRooms() {
        return rooms;
    }

    public ArrayList<RoomType> getRoomTypes() {
        return Database.roomTypes;
    }

    public ArrayList<Amenity> getAmenities() {
        return Database.amenities;
    }

    // ---------------- ROOM METHODS ----------------

    public void addRoom(Room room) {
        if (room != null) {
            rooms.add(room);
        }
    }

    public boolean updateRoom(int roomNumber, RoomType newRoomType, boolean newAvailability) {
        Room room = findRoomByNumber(roomNumber);
        if (room != null) {
            room.setRoomType(newRoomType);
            room.setAvailable(newAvailability);
            return true;
        }
        return false;
    }

    public boolean deleteRoom(int roomId) {
        Room room = findRoomByNumber(roomId);
        if (room != null) {
            rooms.remove(room);
            return true;
        }
        return false;
    }

    public Room findRoomByNumber(int roomNumber) {
        for (Room room : rooms) {
            if (room.getRoomNumber() == roomNumber) {
                return room;
            }
        }
        return null;
    }

    public ArrayList<Room> getAvailableRooms() {
        ArrayList<Room> availableRooms = new ArrayList<>();
        for (Room room : rooms) {
            if (room.isAvailable()) {
                availableRooms.add(room);
            }
        }
        return availableRooms;
    }

    public boolean useRoom(int roomId) {
        Room room = findRoomByNumber(roomId);
        if (room != null && room.isAvailable()) {
            room.setAvailable(false);
            return true;
        }
        return false;
    }

    public void releaseRoom(int roomId) {
        Room room = findRoomByNumber(roomId);
        if (room != null) {
            room.setAvailable(true);
        }
    }

    // ---------------- ROOM TYPE METHODS ----------------

    public void addRoomType(RoomType roomType) {
        if (roomType != null) {
            roomTypes.add(roomType);
        }
    }

    public boolean updateRoomType(int typeId, String newName, double newPrice, int newCapacity) {
        RoomType roomType = findRoomTypeById(typeId);
        if (roomType != null) {
            roomType.setTypeName(newName);
            roomType.setStandardPrice(newPrice);
            roomType.setCapacity(newCapacity);
            return true;
        }
        return false;
    }

    public boolean deleteRoomType(int typeId) {
        RoomType roomType = findRoomTypeById(typeId);
        if (roomType != null) {
            roomTypes.remove(roomType);
            return true;
        }
        return false;
    }

    public RoomType findRoomTypeById(int typeId) {
        for (RoomType type : roomTypes) {
            if (type.getTypeId() == typeId) {
                return type;
            }
        }
        return null;
    }

    // ---------------- AMENITY METHODS ----------------

    public void addAmenity(Amenity amenity) {
        if (amenity != null) {
            amenities.add(amenity);
        }
    }

    public boolean updateAmenity(int amenityId, String newName, String newDescription) {
        Amenity amenity = findAmenityById(amenityId);
        if (amenity != null) {
            amenity.setName(newName);
            amenity.setDescription(newDescription);
            return true;
        }
        return false;
    }

    public boolean deleteAmenity(int amenityId) {
        Amenity amenity = findAmenityById(amenityId);
        if (amenity != null) {
            amenities.remove(amenity);
            return true;
        }
        return false;
    }

    public Amenity findAmenityById(int amenityId) {
        for (Amenity amenity : amenities) {
            if (amenity.getAmenityId() == amenityId) {
                return amenity;
            }
        }
        return null;
    }

    // ---------------- ROOM + AMENITY LINK ----------------

    public boolean addAmenityToRoom(int roomId, int amenityId) {
        Room room = findRoomByNumber(roomId);
        Amenity amenity = findAmenityById(amenityId);

        if (room != null && amenity != null) {
            room.addAmenity(amenity);
            return true;
        }
        return false;
    }

    public boolean removeAmenityFromRoom(int roomId, int amenityId) {
        Room room = findRoomByNumber(roomId);
        if (room != null) {
            room.removeAmenityById(amenityId);
            return true;
        }
        return false;
    }
}