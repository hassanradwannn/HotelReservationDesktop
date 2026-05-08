package exceptions;

public class InvalidCredentialsException extends Exception {
    public InvalidCredentialsException(String message) {
        super(message);
    }

    public InvalidCredentialsException() {
        super("Authentication error has occurred");
    }
}
