import java.util.ArrayList;
public class Room {
    private int roomNumber;
    private RoomType roomType;
    private ArrayList<Amenity> amenities = new ArrayList<>();
    private boolean available;

    public Room(int roomNumber, RoomType roomType) {
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.available = true;
        addAmenity(Database.getAmenities().get(0), getAmenities().get(1));
    }

    public int getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(int roomNumber) {
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

    public void update(int roomNumber, RoomType roomType, boolean available) {
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.available = available;
    }

    public void udpate(int roomNumber) {
        this.roomNumber = roomNumber;
    }

    public void update(RoomType roomType) {
        this.roomType = roomType;
    }

    public void update(boolean available) {
        this.available = available;
    }

    @Override
    public String toString() {
        return "Room Number: " + roomNumber +
               ", Type: " + roomType.getName() +
               ", Available: " + available;
    }
}
