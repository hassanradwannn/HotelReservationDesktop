import java.time.LocalDate;

public abstract class User {
    private String username;
    private String password;
    private LocalDate dateOfBirth;

    User(String username, String password, LocalDate dateOfBirth) {
        this.username = username;
        this.password = password;
        this.dateOfBirth = dateOfBirth;
    }
   
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }
    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public void register() throws InvalidCredentialsException {
        Database.register(this);
    }

    protected static User userLogin(String username, String password, boolean isGuest) throws InvalidCredentialsException {
        return Database.authenticate(username, password, isGuest);
    }

    
}
