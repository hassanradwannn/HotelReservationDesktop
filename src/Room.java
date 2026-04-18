import java.util.ArrayList;

public class Room {
    private String roomNumber;
    private RoomType roomType;
    private ArrayList<Amenity> amenities = new ArrayList<>();
    private boolean available;

    public Room(String roomNumber, RoomType roomType) {
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.available = true;
        addAmenity(Database.getAmenities().get(0), Database.getAmenities().get(1));
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

    public double getPricePerNight() {
        return roomType.getPricePerNight();
    }

    public void addAmenity(Amenity... amenities) {
        for (Amenity amenity : amenities) {
            this.amenities.add(amenity);
        }
    }

    public void update(String roomNumber, RoomType roomType, boolean available) {
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.available = available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public String toString() {
        return "Room Number: " + roomNumber +
                ", Type: " + roomType.getName() +
                ", Available: " + available;
    }

    public boolean isAvailable() {
        return available;
    }
}