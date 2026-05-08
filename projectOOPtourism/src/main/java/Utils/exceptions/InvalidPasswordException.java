package Utils.exceptions;


import Controllers.*;
import Models.*;
import Repositories.*;
import Services.*;
import Utils.*;
import Repositories.database.DatabaseConnection;
import Repositories.DatabaseInitializer.*;
public class InvalidPasswordException extends InvalidCredentialsException {
    public InvalidPasswordException() {
        super("Invalid password");
    }
}
