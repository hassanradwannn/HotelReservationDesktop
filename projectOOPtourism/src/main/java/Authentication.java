import exceptions.InvalidCredentialsException;
import exceptions.InvalidPasswordException;
import exceptions.UserNotFoundException;
import exceptions.UsernameAlreadyTakenException;
import exceptions.WeakPasswordException;

public class Authentication {
    public static User login(String username, String password)
            throws InvalidCredentialsException {
        // Always query database directly for real-time data sharing across instances
        User user = UserDatabase.findUser(username);
        if (user != null) {
            if (user.getPassword().equals(password)) {
                // Update in-memory cache with the latest user data
                updateInMemoryCache(user);
                return user;
            }
            throw new InvalidPasswordException();
        }
        throw new UserNotFoundException();
    }

    private static void updateInMemoryCache(User user) {
        // Remove existing user if present
        Database.getGuests().removeIf(g -> g.getUsername().equals(user.getUsername()));
        Database.getStaffMembers().removeIf(s -> s.getUsername().equals(user.getUsername()));

        // Add the updated user to in-memory cache
        Database.addUser(user);
    }

    public static void register(User user) throws InvalidCredentialsException {
        // Check database directly to ensure real-time validation across instances
        if (UserDatabase.findUser(user.getUsername()) != null) {
            throw new UsernameAlreadyTakenException();
        }
        validatePasswordStrength(user.getPassword());
        Database.addUser(user); // Add to in-memory cache first
        DatabaseSaver.saveUser(user); // Then save to database
    }

    public static void validatePasswordStrength(String password) throws WeakPasswordException {
        String regex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$";
        if (password == null || !password.matches(regex)) {
            throw new WeakPasswordException();
        }
    }
}
