
package Repositories.DatabaseInitializer;


import Controllers.*;
import Models.*;
import Repositories.*;
import Services.*;
import Utils.*;
import Utils.exceptions.*;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnection {

    public static Connection getConnection() throws SQLException {
        return Repositories.database.DatabaseConnection.getConnection();
    }
}
