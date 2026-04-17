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
    public void createRoom(String room) { System.out.println("Admin created room: " + room); }
    public void updateRoom(String oldRoom, String newRoom) { System.out.println("Updated room " + oldRoom + " to " + newRoom); }
    public void deleteRoom(String room) { System.out.println("Admin deleted room: " + room); }
}