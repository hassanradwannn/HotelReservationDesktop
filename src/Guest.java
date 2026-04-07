import java.time.LocalDate;
import java.util.List;

public class Guest {
    private String username;
    private String password;
    private LocalDate dateOfBirth;
    private double balance;
    private String address;
    private Gender gender;
    private String roomPreferences;

    public Guest(String username, String password, LocalDate dateOfBirth,
                 double balance, String address, Gender gender, String roomPreferences) {
        this.username = username;
        setPassword(password);
        this.dateOfBirth = dateOfBirth;
        this.balance = balance;
        this.address = address;
        this.gender = gender;
        this.roomPreferences = roomPreferences;
    }

    // Behaviors
    public boolean login(String username, String password) {
        return this.username.equals(username) && this.password.equals(password);
    }

    public void register() {
        System.out.println(username + " registered successfully!");
    }

    public void viewAvailableRooms(List<String> rooms) {
        System.out.println("Available Rooms:");
        rooms.forEach(System.out::println);
    }

    public void makeReservation(String room) {
        System.out.println(username + " reserved room: " + room);
    }

    public void viewReservations(List<String> reservations) {
        System.out.println("Reservations for " + username + ":");
        reservations.forEach(System.out::println);
    }

    public void cancelReservation(String room) {
        System.out.println(username + " canceled reservation for room: " + room);
    }

    public void checkoutAndPayInvoice(double amount) {
        if (balance >= amount) {
            balance -= amount;
            System.out.println(username + " has paid $" + amount + ". Remaining balance: $" + balance);
        } else {
            System.out.println("Insufficient balance.");
        }
    }

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

    public double getBalance() { return balance; }
    public void setBalance(double balance) {
        if(balance < 0) throw new IllegalArgumentException("Balance cannot be negative.");
        this.balance = balance;
    }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }

    public String getRoomPreferences() { return roomPreferences; }
    public void setRoomPreferences(String roomPreferences) { this.roomPreferences = roomPreferences; }


}