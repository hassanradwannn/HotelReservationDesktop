package Repositories.database;


import Controllers.*;
import Models.*;
import Repositories.*;
import Services.*;
import Utils.*;
import Utils.exceptions.*;
import Repositories.DatabaseInitializer.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL = configuredValue("hotel.db.url", "HOTEL_DB_URL", "jdbc:mysql://172.20.10.6:3306/hotel_db");
    private static final String USER = configuredValue("hotel.db.user", "HOTEL_DB_USER", "H");
    private static final String PASSWORD = configuredValue("hotel.db.password", "HOTEL_DB_PASSWORD", "hassan2007_");

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // JVM properties win over environment variables, which win over the local default.
    private static String configuredValue(String propertyName, String envName, String fallback) {
        String propertyValue = System.getProperty(propertyName);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue;
        }

        String envValue = System.getenv(envName);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }

        return fallback;
    }
}
