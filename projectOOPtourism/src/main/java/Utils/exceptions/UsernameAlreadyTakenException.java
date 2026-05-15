package Utils.exceptions;


import Controllers.*;
import Models.*;
import Repositories.*;
import Services.*;
import Utils.*;
import Repositories.database.DatabaseConnection;
import Repositories.DatabaseInitializer.*;
public class UsernameAlreadyTakenException extends InvalidCredentialsException {
    public UsernameAlreadyTakenException() {
        super("Username is already taken");
    }
}
