public class UserNotFoundException extends InvalidCredentialsException {
    UserNotFoundException() {
        super("Username doesn't exist, try creating a new account instead");
    }
}
