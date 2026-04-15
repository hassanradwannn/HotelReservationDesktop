package exceptions;
public class InvalidPasswordException extends InvalidCredentialsException {
    public InvalidPasswordException() {
        super("Invalid password");
    }
}
