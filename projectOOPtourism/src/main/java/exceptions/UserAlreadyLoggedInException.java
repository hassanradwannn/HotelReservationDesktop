package exceptions;

public class UserAlreadyLoggedInException extends Exception {
    public UserAlreadyLoggedInException() {
        super("User is already logged in on another instance.");
    }
}
