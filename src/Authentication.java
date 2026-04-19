import exceptions.InvalidCredentialsException;
import exceptions.InvalidPasswordException;
import exceptions.UserNotFoundException;
import exceptions.UsernameAlreadyTakenException;
import exceptions.WeakPasswordException;

public final class Authentication {
    
    private Authentication() {} // Utility class

    public static User login(String username, String password)
            throws InvalidCredentialsException {
        User user = Database.findUser(username);
        if (user != null) {
            if (user.getPassword().equals(password)) {
                return user;
            }
            throw new InvalidPasswordException();
        }
        throw new UserNotFoundException();
    }

    public static void register(User user) throws InvalidCredentialsException {
        if (Database.findUser(user.getUsername()) != null) {
            throw new UsernameAlreadyTakenException();
        }
        validatePasswordStrength(user.getPassword());
        Database.addUser(user);
    }

    public static void validatePasswordStrength(String password) throws WeakPasswordException {
        String regex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$";
        if (password == null || !password.matches(regex)) {
            throw new WeakPasswordException();
        }
    }
}
