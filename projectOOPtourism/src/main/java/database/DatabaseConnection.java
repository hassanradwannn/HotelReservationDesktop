package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

/*    private static final String URL = "jdbc:mysql://100.109.233.122:3306/hotel_db";
    private static final String USER = "hassan";
    private static final String PASSWORD = "hassan123"; */

    private static final String URL = "jdbc:mysql://localhost/hotel_db";
    private static final String USER = "root";
    private static final String PASSWORD = "password";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}