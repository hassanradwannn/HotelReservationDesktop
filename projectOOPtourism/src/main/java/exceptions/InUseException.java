package exceptions;

public class InUseException extends Exception {
    public InUseException() {
        super("Cannot delete room with active or pending reservations.");
    }

    public InUseException(String item) {
        super("Can't delete " + item + " in use by existing rooms.");
    }
}
