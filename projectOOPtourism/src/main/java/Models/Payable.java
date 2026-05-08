package Models;


import Controllers.*;
import Repositories.*;
import Services.*;
import Utils.*;
import Utils.exceptions.*;
import Repositories.database.DatabaseConnection;
import Repositories.DatabaseInitializer.*;
import Utils.exceptions.InvalidPaymentException;

public interface Payable {
    boolean processPayment() throws InvalidPaymentException;
}
