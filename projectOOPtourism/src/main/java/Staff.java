import java.time.LocalDate;

public abstract class Staff extends User {
    private Role role;
    private int workingHours;

    public Staff(String username, String password, LocalDate dateOfBirth, Role role, int workingHours) {
        super(username, password, dateOfBirth);
        this.role = role;
        this.workingHours = workingHours;
    }

    public void viewGuests() {
        Database.getGuests().forEach(System.out::println);
    }
    public void viewRooms() {
        Database.getRooms().forEach(System.out::println);
    }
    public void viewReservations() {
        Database.getReservations().forEach(System.out::println);
    }
    
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public int getWorkingHours() { return workingHours; }
    public void setWorkingHours(int workingHours) { this.workingHours = workingHours; }

}