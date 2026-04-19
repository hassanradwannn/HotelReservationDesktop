import java.time.LocalDate;
import exceptions.InvalidCredentialsException;

public class Admin extends Staff {

    public Admin(String username, String password, LocalDate dateOfBirth, int workingHours) {
        super(username, password, dateOfBirth, Role.ADMIN, workingHours);
    }

    // CRUD
    public void createRoom(Room room) { 
        Database.addRoom(room);
    }
    
    public void updateRoom(Room room, String newRoomNumber, RoomType newRoomType, boolean available) { 
        room.update(newRoomNumber, newRoomType, available);
    }
    
    public void deleteRoom(Room room) { 
        Database.getRooms().remove(room);
    }
    
    public void createRoomType(RoomType roomType) {
        Database.addRoomType(roomType);
    }
    
    public void updateRoomType(RoomType roomType, String newName, int newCapacity, double newPrice) {
        roomType.update(newName, newCapacity, newPrice);
    }
    
    public void deleteRoomType(RoomType roomType) {
        Database.getRoomTypes().remove(roomType);
    }
    
    public void createAmenity(String name, double price) {
        Database.addAmenity(name, price);
    }
    
    public void updateAmenity(Amenity amenity, String newName, double newPrice) {
        amenity.update(newName, newPrice);
    }
    
    public void deleteAmenity(Amenity amenity) {
        Database.getAmenities().remove(amenity);
    }
}