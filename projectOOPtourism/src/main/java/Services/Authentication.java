package Services;


import Controllers.*;
import Models.*;
import Repositories.*;
import Utils.*;
import Utils.exceptions.*;
import Repositories.database.DatabaseConnection;
import Repositories.DatabaseInitializer.*;
import Utils.exceptions.InvalidCredentialsException;
import Utils.exceptions.WeakPasswordException;
import Utils.exceptions.UserAlreadyLoggedInException;

public class Authentication {
    private static final AuthService AUTH_SERVICE = new AuthService();

    public static User login(String username, String password)
            throws InvalidCredentialsException, UserAlreadyLoggedInException {
        return AUTH_SERVICE.login(username, password);
    }

    public static void logout(User user) {
        AUTH_SERVICE.logout(user);
    }

    public static void register(User user) throws InvalidCredentialsException {
        AUTH_SERVICE.register(user);
    }

    public static void validatePasswordStrength(String password) throws WeakPasswordException {
        AUTH_SERVICE.validatePasswordStrength(password);
    }
}
