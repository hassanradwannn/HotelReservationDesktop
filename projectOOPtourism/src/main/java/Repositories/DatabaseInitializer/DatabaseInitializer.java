package Repositories.DatabaseInitializer;


import Controllers.*;
import Models.*;
import Repositories.*;
import Services.*;
import Utils.*;
import Utils.exceptions.*;
import Repositories.database.DatabaseConnection;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;

/**
 * Creates and upgrades the database schema used by the desktop app.
 */
public class DatabaseInitializer {
    
    public static boolean initializeDatabase() {
        try {
            createTables();
            insertDemoUsers();
            System.out.println("Database initialization completed successfully.");
            return true;
        } catch (SQLException e) {
            System.out.println("Error initializing database: " + e.getMessage());
            return false;
        }
    }

    private static void createTables() throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String usersTable = """
                CREATE TABLE IF NOT EXISTS users (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(100) UNIQUE NOT NULL,
                    password VARCHAR(255) NOT NULL,
                    role VARCHAR(50) NOT NULL,
                    date_of_birth DATE,
                    gender VARCHAR(20),
                    address VARCHAR(255),
                    salary DOUBLE,
                    balance DOUBLE DEFAULT 0.0,
                    is_logged_in BOOLEAN DEFAULT FALSE,
                    room_preferences TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """;
            executeUpdate(conn, usersTable);

            String roomTypesTable = """
                CREATE TABLE IF NOT EXISTS room_types (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(50) UNIQUE NOT NULL,
                    price_per_night DOUBLE NOT NULL,
                    capacity INT NOT NULL
                )
            """;
            executeUpdate(conn, roomTypesTable);

            String roomsTable = """
                CREATE TABLE IF NOT EXISTS rooms (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    room_number VARCHAR(50) UNIQUE NOT NULL,
                    room_type_name VARCHAR(50) NOT NULL,
                    FOREIGN KEY (room_type_name) REFERENCES room_types(name)
                )
            """;
            executeUpdate(conn, roomsTable);

            String amenitiesTable = """
                CREATE TABLE IF NOT EXISTS amenities (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(100) UNIQUE NOT NULL,
                    price DOUBLE NOT NULL
                )
            """;
            executeUpdate(conn, amenitiesTable);

            String roomAmenitiesTable = """
                CREATE TABLE IF NOT EXISTS room_amenities (
                    room_number VARCHAR(50) NOT NULL,
                    amenity_name VARCHAR(100) NOT NULL,
                    PRIMARY KEY (room_number, amenity_name),
                    FOREIGN KEY (room_number) REFERENCES rooms(room_number) ON DELETE CASCADE,
                    FOREIGN KEY (amenity_name) REFERENCES amenities(name) ON DELETE CASCADE
                )
            """;
            executeUpdate(conn, roomAmenitiesTable);

            String reservationsTable = """
                CREATE TABLE IF NOT EXISTS reservations (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    reservation_id VARCHAR(50) UNIQUE NOT NULL,
                    guest_username VARCHAR(100) NOT NULL,
                    room_number VARCHAR(50) NOT NULL,
                    check_in_date DATE NOT NULL,
                    check_out_date DATE NOT NULL,
                    has_gym_pass BOOLEAN DEFAULT FALSE,
                    status VARCHAR(50) DEFAULT 'PENDING',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (guest_username) REFERENCES users(username) ON UPDATE CASCADE,
                    FOREIGN KEY (room_number) REFERENCES rooms(room_number)
                )
            """;
            executeUpdate(conn, reservationsTable);

            String invoicesTable = """
                CREATE TABLE IF NOT EXISTS invoices (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    guest_username VARCHAR(100) NOT NULL,
                    room_number VARCHAR(50) NOT NULL,
                    reservation_id VARCHAR(50),
                    total_amount DOUBLE NOT NULL,
                    payment_method VARCHAR(50) NOT NULL,
                    paid BOOLEAN DEFAULT TRUE,
                    payment_date DATE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """;
            executeUpdate(conn, invoicesTable);

            String systemSettingsTable = """
                CREATE TABLE IF NOT EXISTS system_settings (
                    setting_key VARCHAR(50) PRIMARY KEY,
                    setting_value VARCHAR(255) NOT NULL
                )
            """;
            executeUpdate(conn, systemSettingsTable);


            System.out.println("All tables created successfully.");

            // These migrations keep older local databases usable after schema changes.
            try {
                executeUpdate(conn, "ALTER TABLE users ADD COLUMN balance DOUBLE DEFAULT 0.0");
                System.out.println("Added missing 'balance' column to users table.");
            } catch (SQLException ignored) {
            }
            try {
                executeUpdate(conn, "ALTER TABLE users ADD COLUMN is_logged_in BOOLEAN DEFAULT FALSE");
                System.out.println("Added missing 'is_logged_in' column to users table.");
            } catch (SQLException ignored) {
            }
            try {
                executeUpdate(conn, "ALTER TABLE users ADD COLUMN room_preferences TEXT");
                System.out.println("Added missing 'room_preferences' column to users table.");
            } catch (SQLException ignored) {
            }
            try {
                executeUpdate(conn, "ALTER TABLE reservations ADD COLUMN reservation_id VARCHAR(50) UNIQUE NOT NULL AFTER id");
                System.out.println("Added missing 'reservation_id' column to reservations table.");
            } catch (SQLException ignored) {
            }
            try {
                executeUpdate(conn, "ALTER TABLE reservations ADD COLUMN has_gym_pass BOOLEAN DEFAULT FALSE AFTER check_out_date");
                System.out.println("Added missing 'has_gym_pass' column to reservations table.");
            } catch (SQLException ignored) {
            }
            try {
                executeUpdate(conn, "ALTER TABLE invoices ADD COLUMN guest_username VARCHAR(100) NOT NULL AFTER id");
                System.out.println("Added missing 'guest_username' column to invoices table.");
            } catch (SQLException ignored) {
            }
            try {
                executeUpdate(conn, "ALTER TABLE invoices ADD COLUMN room_number VARCHAR(50) NOT NULL AFTER guest_username");
                System.out.println("Added missing 'room_number' column to invoices table.");
            } catch (SQLException ignored) {
            }
            try {
                executeUpdate(conn, "ALTER TABLE invoices ADD COLUMN reservation_id VARCHAR(50) AFTER room_number");
                System.out.println("Added missing 'reservation_id' column to invoices table.");
            } catch (SQLException ignored) {
            }
            try {
                executeUpdate(conn, "ALTER TABLE invoices ADD COLUMN paid BOOLEAN DEFAULT TRUE AFTER payment_method");
                System.out.println("Added missing 'paid' column to invoices table.");
            } catch (SQLException ignored) {
            }
            try {
                executeUpdate(conn, "ALTER TABLE invoices ADD COLUMN payment_date DATE AFTER paid");
                System.out.println("Added missing 'payment_date' column to invoices table.");
            } catch (SQLException ignored) {
            }
            try {
                executeUpdate(conn, "INSERT IGNORE INTO system_settings (setting_key, setting_value) VALUES ('current_date', '" + java.time.LocalDate.now().toString() + "')");
                System.out.println("System date tracking initialized.");
            } catch (SQLException ignored) {
            }
            try {
                executeUpdate(conn, "INSERT IGNORE INTO system_settings (setting_key, setting_value) VALUES ('last_update', '0')");
                System.out.println("System data versioning initialized.");
            } catch (SQLException ignored) {
            }
            try {
                executeUpdate(conn, "INSERT IGNORE INTO system_settings (setting_key, setting_value) VALUES ('active_instances', '0')");
                System.out.println("Active instance tracking initialized.");
            } catch (SQLException ignored) {
            }
        }
    }

    private static void executeUpdate(Connection conn, String sql) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    private static void insertDemoUsers() throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement checkStmt = conn.prepareStatement("SELECT COUNT(*) FROM users")) {
                var rs = checkStmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    System.out.println("Demo users already exist, skipping insertion.");
                    return;
                }
            }

            String insertUserSql = """
                INSERT INTO users (username, password, role, date_of_birth, gender, address, balance, room_preferences)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

            try (PreparedStatement stmt = conn.prepareStatement(insertUserSql)) {
                // Demo login: Admin / Admin@123.
                stmt.setString(1, "Admin");
                stmt.setString(2, "Admin@123");
                stmt.setString(3, "Admin");
                stmt.setDate(4, Date.valueOf(LocalDate.now().minusYears(30)));
                stmt.setString(5, null);
                stmt.setString(6, null);
                stmt.setDouble(7, 0.0);
                stmt.setString(8, null);
                stmt.executeUpdate();
                System.out.println("Demo admin user 'Admin' created.");

                // Demo login: Manar / Manar2002.
                stmt.setString(1, "Manar");
                stmt.setString(2, "Manar2002");
                stmt.setString(3, "Receptionist");
                stmt.setDate(4, Date.valueOf(LocalDate.now().minusYears(25)));
                stmt.setString(5, null);
                stmt.setString(6, null);
                stmt.setDouble(7, 0.0);
                stmt.setString(8, null);
                stmt.executeUpdate();
                System.out.println("Demo receptionist user 'Manar' created.");

                // Demo login: Hassan / Hassan123.
                stmt.setString(1, "Hassan");
                stmt.setString(2, "Hassan123");
                stmt.setString(3, "Guest");
                stmt.setDate(4, Date.valueOf(LocalDate.now().minusYears(28)));
                stmt.setString(5, "MALE");
                stmt.setString(6, "2 Haram");
                stmt.setDouble(7, 100000.0);
                stmt.setString(8, "WiFi, Smart TV");
                stmt.executeUpdate();
                System.out.println("Demo guest user 'Hassan' created.");
            }
        } catch (SQLException e) {
            System.out.println("Error inserting demo users: " + e.getMessage());
            throw e;
        }
    }
}
