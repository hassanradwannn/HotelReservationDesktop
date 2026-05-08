package Services;


import Controllers.*;
import Models.*;
import Repositories.*;
import Utils.*;
import Utils.exceptions.*;
import Repositories.database.DatabaseConnection;
import Repositories.DatabaseInitializer.*;
import Utils.exceptions.InvalidCredentialsException;
import Utils.exceptions.InvalidPasswordException;
import Utils.exceptions.UserAlreadyLoggedInException;
import Utils.exceptions.UserNotFoundException;
import Utils.exceptions.UsernameAlreadyTakenException;
import Utils.exceptions.WeakPasswordException;

public class AuthService {

    public User login(String username, String password)
            throws InvalidCredentialsException, UserAlreadyLoggedInException {
        User user = UserDatabase.findUser(username);
        if (user != null) {
            if (user.getPassword().equals(password)) {
                if (UserDatabase.isUserLoggedIn(username)) {
                    throw new UserAlreadyLoggedInException();
                }
                UserDatabase.setUserLoggedIn(username, true);
                updateInMemoryCache(user);
                return user;
            }
            throw new InvalidPasswordException();
        }
        throw new UserNotFoundException();
    }

    public void logout(User user) {
        if (user != null) {
            UserDatabase.setUserLoggedIn(user.getUsername(), false);
        }
    }

    public void register(User user) throws InvalidCredentialsException {
        if (UserDatabase.findUser(user.getUsername()) != null) {
            throw new UsernameAlreadyTakenException();
        }
        validatePasswordStrength(user.getPassword());
        Database.addUser(user);
        DatabaseSaver.saveUser(user);
    }

    public void validatePasswordStrength(String password) throws WeakPasswordException {
        String regex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$";
        if (password == null || !password.matches(regex)) {
            throw new WeakPasswordException();
        }
    }

    private void updateInMemoryCache(User user) {
        Database.getGuests().removeIf(g -> g.getUsername().equals(user.getUsername()));
        Database.getStaffMembers().removeIf(s -> s.getUsername().equals(user.getUsername()));
        Database.addUser(user);
    }
}
