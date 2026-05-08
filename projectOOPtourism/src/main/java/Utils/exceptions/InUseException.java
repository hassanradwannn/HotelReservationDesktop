package Utils.exceptions;


import Controllers.*;
import Models.*;
import Repositories.*;
import Services.*;
import Utils.*;
import Repositories.database.DatabaseConnection;
import Repositories.DatabaseInitializer.*;
public class InUseException extends Exception {
    public InUseException() {
        super("Cannot delete room with active or pending reservations.");
    }

    public InUseException(String item) {
        super("Can't delete " + item + " in use by existing rooms.");
    }
}
