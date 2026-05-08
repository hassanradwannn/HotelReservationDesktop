package Utils.exceptions;


import Controllers.*;
import Models.*;
import Repositories.*;
import Services.*;
import Utils.*;
import Repositories.database.DatabaseConnection;
import Repositories.DatabaseInitializer.*;
public class UserNotFoundException extends InvalidCredentialsException {
    public UserNotFoundException() {
        super("Username doesn't exist, try creating a new account instead");
    }
}
