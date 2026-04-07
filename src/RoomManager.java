import java.util.ArrayList;

public class RoomManager {
    private ArrayList<Room> rooms;
    private ArrayList<RoomType> roomTypes;
    private ArrayList<Amenity> amenities;

    public RoomManager() {
        rooms = new ArrayList<>();
        roomTypes = new ArrayList<>();
        amenities = new ArrayList<>();
    }

    public ArrayList<Room> getRooms() {
        return rooms;
    }

    public ArrayList<RoomType> getRoomTypes() {
        return roomTypes;
    }

    public ArrayList<Amenity> getAmenities() {
        return amenities;
    }

    // ---------------- ROOM METHODS ----------------

    public void addRoom(Room room) {
        if (room != null) {
            rooms.add(room);
        }
    }

    public boolean updateRoom(int roomId, String newRoomNumber, RoomType newRoomType, boolean newAvailability) {
        Room room = findRoomById(roomId);
        if (room != null) {
            room.setRoomNumber(newRoomNumber);
            room.setRoomType(newRoomType);
            room.setAvailable(newAvailability);
            return true;
        }
        return false;
    }

    public boolean deleteRoom(int roomId) {
        Room room = findRoomById(roomId);
        if (room != null) {
            rooms.remove(room);
            return true;
        }
        return false;
    }

    public Room findRoomById(int roomId) {
        for (Room room : rooms) {
            if (room.getRoomId() == roomId) {
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
        Room room = findRoomById(roomId);
        if (room != null && room.isAvailable()) {
            room.setAvailable(false);
            return true;
        }
        return false;
    }

    public void releaseRoom(int roomId) {
        Room room = findRoomById(roomId);
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
            roomType.setPricePerNight(newPrice);
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
        Room room = findRoomById(roomId);
        Amenity amenity = findAmenityById(amenityId);

        if (room != null && amenity != null) {
            room.addAmenity(amenity);
            return true;
        }
        return false;
    }

    public boolean removeAmenityFromRoom(int roomId, int amenityId) {
        Room room = findRoomById(roomId);
        if (room != null) {
            room.removeAmenityById(amenityId);
            return true;
        }
        return false;
    }
}