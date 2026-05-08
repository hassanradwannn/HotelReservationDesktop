package exceptions;

public class UsernameAlreadyTakenException extends InvalidCredentialsException {
    public UsernameAlreadyTakenException() {
        super("Username is already taken");
    }
}
