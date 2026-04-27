import java.util.ArrayList;

public class Room {
    private String roomNumber;
    private RoomType roomType;
    private ArrayList<Amenity> amenities = new ArrayList<>();
    

    public Room(String roomNumber, RoomType roomType) {
        this.roomNumber = roomNumber;
        this.roomType = roomType;
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

    public void update(RoomType roomType) {
        this.roomType = roomType;
    }

    public boolean removeAmenityByName(String name) {
        return this.amenities.removeIf(a -> a.getName().equalsIgnoreCase(name));
    }

    @Override
    public String toString() {
        return "Room Number: " + roomNumber +
                ", Type: " + roomType.getName();
    }


    // show amenities please argook yarab
}