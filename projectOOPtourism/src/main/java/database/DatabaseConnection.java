package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {


    /* private static final String URL = "jdbc:mysql://192.168.86.25:3306/hotel_db";
    private static final String USER = "hassan";
    private static final String PASSWORD = "admin"; */

    private static final String URL = configuredValue("hotel.db.url", "HOTEL_DB_URL", "jdbc:mysql://localhost/hotel_db");
    private static final String USER = configuredValue("hotel.db.user", "HOTEL_DB_USER", "root");
    private static final String PASSWORD = configuredValue("hotel.db.password", "HOTEL_DB_PASSWORD", "password");

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

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
