package exceptions;

public class UserNotFoundException extends InvalidCredentialsException {
    public UserNotFoundException() {
        super("Username doesn't exist, try creating a new account instead");
    }
}
