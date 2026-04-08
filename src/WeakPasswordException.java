public class WeakPasswordException extends InvalidCredentialsException {
    public WeakPasswordException() {
        super("Password must contain at least 8 characters and 1 digit");
    }
}
