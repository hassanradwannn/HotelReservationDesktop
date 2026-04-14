import java.util.ArrayList;
public class Room implements Manageable{
  private int roomId;
    private String roomNumber;
    private RoomType roomType;
    private ArrayList<Amenity> amenities;
    private boolean available;

    public Room() {
        amenities = new ArrayList<>();
        available = true;
    }

    public Room(int roomId, String roomNumber, RoomType roomType) {
        this.roomId = roomId;
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.amenities = new ArrayList<>();
        this.available = true;
    }

    public Room(int roomId, String roomNumber, RoomType roomType, boolean available) {
        this.roomId = roomId;
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.amenities = new ArrayList<>();
        this.available = available;
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public void setRoomType(RoomType roomType) {
        this.roomType = roomType;
    }

    public ArrayList<Amenity> getAmenities() {
        return amenities;
    }

    public void setAmenities(ArrayList<Amenity> amenities) {
        this.amenities = amenities;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public void addAmenity(Amenity amenity) {
        if (amenity != null) {
            amenities.add(amenity);
        }
    }

    public void removeAmenityById(int amenityId) {
        for (int i = 0; i < amenities.size(); i++) {
            if (amenities.get(i).getAmenityId() == amenityId) {
                amenities.remove(i);
                break;
            }
        }
    }

    public boolean checkAvailability() {
        return available;
    }

    public boolean reserveRoom() {
        if (available) {
            available = false;
            return true;
        }
        return false;
    }

    public void freeRoom() {
        available = true;
    }

    @Override
    public String toString() {
        return "Room ID: " + roomId +
               ", Room Number: " + roomNumber +
               ", Type: " + roomType.getTypeName() +
               ", Available: " + available;
    }
}
