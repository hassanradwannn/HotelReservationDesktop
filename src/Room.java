import java.util.ArrayList;
public class Room implements Manageable{
    private int roomNumber;
    private RoomType roomType;
    private ArrayList<Amenity> amenities = new ArrayList<>();
    private boolean available;
    private double pricePerNight;

    public Room() {
        amenities = new ArrayList<>();
        available = true;
    }

    public Room(int roomNumber, RoomType roomType) {
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.amenities = new ArrayList<>();
        this.available = true;
        setPricePerNight();
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

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public void addAmenity(Amenity... amenities) {
        for (Amenity amenity : amenities) {
            this.amenities.add(amenity);
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

    public double getPricePerNight() {
        return pricePerNight;
    }

    private void setPricePerNight() {
        pricePerNight = roomType.getPricePerNight();
        for (Amenity amenity : getAmenities()) {
            pricePerNight += amenity.getPrice();
        }
    }

    @Override
    public String toString() {
        return "Room Number: " + roomNumber +
               ", Type: " + roomType.getTypeName() +
               ", Available: " + available;
    }
}
