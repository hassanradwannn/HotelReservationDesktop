import java.util.ArrayList;

public class Room {
    private int id;
    private String roomNumber;
    private RoomType roomType;
    private ArrayList<Amenity> amenities = new ArrayList<>();
    

    public Room(String roomNumber, RoomType roomType) {
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        // Removed unsafe hardcoded defaults. 
        // Database.java now handles fetching and linking proper amenities from SQL.
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public String getViewName() {
        return isSeaView() ? "Sea View" : "Mountain View";
    }

    public boolean isSeaView() {
        int number;
        try {
            number = Integer.parseInt(roomNumber);
        } catch (NumberFormatException ex) {
            return true;
        }

        int positionOnFloor = Math.floorMod(number, 100);
        return positionOnFloor <= 10;
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
        String amenityList = (amenities == null || amenities.isEmpty()) 
                             ? "None" 
                             : String.join(", ", CatalogService.uniqueFilterAmenityNames(
                                     amenities.stream().map(Amenity::getName).toList()));
        
        return String.format("Room Number: %s | Type: %s | Amenities: %s", 
                roomNumber, 
                roomType.getName(), 
                amenityList + " | View: " + getViewName());
    }
}
