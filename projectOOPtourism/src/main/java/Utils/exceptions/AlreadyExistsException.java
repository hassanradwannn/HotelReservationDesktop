package Utils.exceptions;


import Controllers.*;
import Models.*;
import Repositories.*;
import Services.*;
import Utils.*;
import Repositories.database.DatabaseConnection;
import Repositories.DatabaseInitializer.*;
public class AlreadyExistsException extends Exception {
    public AlreadyExistsException(String item, String identifier) {
        super(item + " " + identifier + " already exists.");
    }
}
