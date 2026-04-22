import java.time.LocalDate;
import exceptions.InvalidCredentialsException;

public class Admin extends Staff {

    public Admin(String username, String password, LocalDate dateOfBirth, int workingHours) {
        super(username, password, dateOfBirth, Role.ADMIN, workingHours);
    }

    public void registerGuest(String username, String password, LocalDate dob,
                              double balance, String address, Gender gender, String preferences)
            throws InvalidCredentialsException {
        Guest guest = new Guest(username, password, dob, balance, address, gender, preferences);
        Authentication.register(guest);
        System.out.println("Guest '" + username + "' registered successfully.");
    }

    public void registerStaff(String username, String password, LocalDate dob,
                              int workingHours, Role role)
            throws InvalidCredentialsException {
        Staff newStaff = switch (role) {
            case ADMIN        -> new Admin(username, password, dob, workingHours);
            case RECEPTIONIST -> new Receptionist(username, password, dob, workingHours);
        };
        Authentication.register(newStaff);
        System.out.println("Staff member '" + username + "' (" + newStaff.getClass().getSimpleName() + ") registered successfully.");
    }

    // CRUD
    public void viewRoomTypes() { Database.getRoomTypes().forEach(System.out::println); }
    public void viewAmenities() { Database.getAmenities().forEach(System.out::println); }

    public void createRoomType(String name, double pricePerNight, int capacity) {
        Database.createRoomType(name, pricePerNight, capacity);
        System.out.println("RoomType '" + name + "' created.");
    }

    public void createAmenity(String name, double price) {
        Database.createAmenity(name, price);
        System.out.println("Amenity '" + name + "' created.");
    }

    public void createRoom(String roomNumber, RoomType type) {
        Database.createRoom(roomNumber, type);
        System.out.println("Room '" + roomNumber + "' created.");
    }

    public void deleteRoom(Room room) {
        Database.deleteRoom(room);
        System.out.println("Room '" + room.getRoomNumber() + "' deleted.");
    }

    public void deleteRoomType(RoomType roomType) {
        Database.deleteRoomType(roomType);
        System.out.println("RoomType '" + roomType.getName() + "' deleted.");
    }

    public void deleteAmenity(Amenity amenity) {
        Database.deleteAmenity(amenity);
        System.out.println("Amenity '" + amenity.getName() + "' deleted.");
    }
}