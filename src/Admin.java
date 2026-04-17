import java.time.LocalDate;
import java.util.List;
import exceptions.InvalidCredentialsException;

public class Admin extends Staff {

    public Admin(String username, String password, LocalDate dateOfBirth, int workingHours) {
        super(username, password, dateOfBirth, Role.ADMIN, workingHours);
    }

    @Override
    public void viewGuests(List<Guest> guests) {
        System.out.println("Admin viewing all guests:");
        guests.forEach(g -> System.out.println(g.getUsername()));
    }

    @Override
    public void viewRooms(List<String> rooms) {
        System.out.println("Admin viewing all rooms:");
        rooms.forEach(System.out::println);
    }

    @Override
    public void viewReservations(List<String> reservations) {
        System.out.println("Admin viewing all reservations:");
        reservations.forEach(System.out::println);
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