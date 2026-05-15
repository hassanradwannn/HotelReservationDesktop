package Utils.exceptions;


import Controllers.*;
import Models.*;
import Repositories.*;
import Services.*;
import Utils.*;
import Repositories.database.DatabaseConnection;
import Repositories.DatabaseInitializer.*;
public class UserAlreadyLoggedInException extends Exception {
    public UserAlreadyLoggedInException() {
        super("User is already logged in on another instance.");
    }
}
