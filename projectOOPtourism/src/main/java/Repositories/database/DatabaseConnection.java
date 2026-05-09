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

    private static final String URL = withRequiredOptions(configuredValue(
            "hotel.db.url",
            "HOTEL_DB_URL",
            "jdbc:mysql://172.20.10.2:3306/hotel_db"));
    private static final String USER = configuredValue("hotel.db.user", "HOTEL_DB_USER", "H");
    private static final String PASSWORD = configuredValue("hotel.db.password", "HOTEL_DB_PASSWORD", "hassan2007_");
    private static final int LOGIN_TIMEOUT_SECONDS = 3;

    static {
        DriverManager.setLoginTimeout(LOGIN_TIMEOUT_SECONDS);
    }

    public static Connection getConnection() throws SQLException {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            throw new SQLException(
                    "Could not connect to MySQL at " + urlForDisplay()
                            + ". Check that MySQL is running, the host/IP is reachable, port 3306 is open, "
                            + "and HOTEL_DB_URL/hotel.db.url points to the correct machine.",
                    e.getSQLState(),
                    e.getErrorCode(),
                    e);
        }
    }

    public static String urlForDisplay() {
        int queryStart = URL.indexOf('?');
        return queryStart >= 0 ? URL.substring(0, queryStart) : URL;
    }

    public static String hostForDisplay() {
        String displayUrl = urlForDisplay();
        String prefix = "jdbc:mysql://";
        if (!displayUrl.startsWith(prefix)) {
            return "127.0.0.1";
        }

        String hostAndRest = displayUrl.substring(prefix.length());
        int slashIndex = hostAndRest.indexOf('/');
        String hostAndPort = slashIndex >= 0 ? hostAndRest.substring(0, slashIndex) : hostAndRest;
        int portSeparator = hostAndPort.lastIndexOf(':');
        String host = portSeparator >= 0 ? hostAndPort.substring(0, portSeparator) : hostAndPort;
        return host.isBlank() ? "127.0.0.1" : host;
    }

    // JVM properties win over environment variables, which win over the local default.
    public static String configuredValue(String propertyName, String envName, String fallback) {
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

    private static String withRequiredOptions(String url) {
        if (url == null || !url.startsWith("jdbc:mysql://")) {
            return url;
        }

        String updated = appendOptionIfMissing(url, "connectTimeout", "3000");
        updated = appendOptionIfMissing(updated, "socketTimeout", "5000");
        updated = appendOptionIfMissing(updated, "useSSL", "false");
        updated = appendOptionIfMissing(updated, "allowPublicKeyRetrieval", "true");
        updated = appendOptionIfMissing(updated, "serverTimezone", "UTC");
        return updated;
    }

    private static String appendOptionIfMissing(String url, String key, String value) {
        String lowerUrl = url.toLowerCase();
        if (lowerUrl.contains("?" + key.toLowerCase() + "=") || lowerUrl.contains("&" + key.toLowerCase() + "=")) {
            return url;
        }

        String separator = url.contains("?") ? "&" : "?";
        return url + separator + key + "=" + value;
    }
}
