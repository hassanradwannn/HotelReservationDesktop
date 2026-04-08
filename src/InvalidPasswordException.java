public class InvalidPasswordException extends InvalidCredentialsException {
    InvalidPasswordException() {
        super("Invalid password");
    }
}
