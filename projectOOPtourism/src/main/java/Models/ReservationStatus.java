package Models;


import Controllers.*;
import Repositories.*;
import Services.*;
import Utils.*;
import Utils.exceptions.*;
import Repositories.database.DatabaseConnection;
import Repositories.DatabaseInitializer.*;
public enum ReservationStatus {
    PENDING,
    CONFIRMED,
    CHECKING_IN,
    ONGOING,
    CHECKING_OUT,
    CANCELLED,
    COMPLETED,
}
