package exceptions;

public class AlreadyExistsException extends Exception{
    public AlreadyExistsException(String item, String identifier) {
        super(item + " " + identifier + " already exists.");
    }
}
