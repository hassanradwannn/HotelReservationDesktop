public class UsernameAlreadyTakenException extends InvalidCredentialsException {
    UsernameAlreadyTakenException() {
        super("Username is already taken");
    }
}
