import exceptions.InvalidCredentialsException;
import exceptions.WeakPasswordException;
import exceptions.UserAlreadyLoggedInException;

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
