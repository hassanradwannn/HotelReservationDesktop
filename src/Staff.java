import java.time.LocalDate;
import java.util.List;

public abstract class Staff extends User {
    private Role role;
    private int workingHours;

    public Staff(String username, String password, LocalDate dateOfBirth, Role role, int workingHours) {
        super(username, password, dateOfBirth);
        this.role = role;
        this.workingHours = workingHours;
    }

    public abstract void viewGuests(List<Guest> guests);
    public abstract void viewRooms(List<String> rooms);
    public abstract void viewReservations(List<String> reservations);

    
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public int getWorkingHours() { return workingHours; }
    public void setWorkingHours(int workingHours) { this.workingHours = workingHours; }
}