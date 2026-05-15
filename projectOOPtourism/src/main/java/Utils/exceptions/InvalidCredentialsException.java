package Utils.exceptions;


import Controllers.*;
import Models.*;
import Repositories.*;
import Services.*;
import Utils.*;
import Repositories.database.DatabaseConnection;
import Repositories.DatabaseInitializer.*;
public class InvalidCredentialsException extends Exception {
    public InvalidCredentialsException(String message) {
        super(message);
    }

    public InvalidCredentialsException() {
        super("Authentication error has occurred");
    }
}
