import java.time.LocalDate;

import exceptions.InvalidCredentialsException;

public class Guest extends User {
    private double balance;
    private String address;
    private Gender gender;
    private String roomPreferences;

    public Guest(String username, String password, LocalDate dateOfBirth,
                 double balance, String address, Gender gender, String roomPreferences) {
        super(username, password, dateOfBirth);
        this.balance = balance;
        this.address = address;
        this.gender = gender;
        this.roomPreferences = roomPreferences;
    }

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

    public void register() throws InvalidCredentialsException {
        Authentication.register(this);
    public boolean login(String inputUsername, String inputPassword) throws InvalidCredentialsException {
        Authentication.login(inputUsername, inputPassword, true);

        return true;
    }

}