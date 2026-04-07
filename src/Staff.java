import java.time.LocalDate;
import java.util.List;

public abstract class Staff {
    private String username;
    private String password;
    private LocalDate dateOfBirth;
    private Role role;
    private int workingHours;

    public Staff(String username, String password, LocalDate dateOfBirth, Role role, int workingHours) {
        this.username = username;
        setPassword(password);
        this.dateOfBirth = dateOfBirth;
        this.role = role;
        this.workingHours = workingHours;
    }

    public abstract void viewGuests(List<Guest> guests);
    public abstract void viewRooms(List<String> rooms);
    public abstract void viewReservations(List<String> reservations);

    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) {
        if(password == null || password.length() < 6 ||
           !password.matches(".*\\d.*") || !password.matches(".*[a-zA-Z].*")) {
            throw new IllegalArgumentException(
                "Password must be at least 6 characters long and contain letters and digits.");
        }
        this.password = password;
    }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public int getWorkingHours() { return workingHours; }
    public void setWorkingHours(int workingHours) { this.workingHours = workingHours; }
}