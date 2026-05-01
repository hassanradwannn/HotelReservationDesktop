package DatabaseInitializer;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Initializes the database schema and creates necessary tables
 * @author LOQ
 */
public class DatabaseInitializer {
    
    public static void initializeDatabase() {
        try {
            createTables();
            System.out.println("Database initialization completed successfully.");
        } catch (SQLException e) {
            System.out.println("Error initializing database: " + e.getMessage());
        }
    }

    private static void createTables() throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Create users table
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
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """;
            executeUpdate(conn, usersTable);

            // Create room_types table
            String roomTypesTable = """
                CREATE TABLE IF NOT EXISTS room_types (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(50) UNIQUE NOT NULL,
                    price_per_night DOUBLE NOT NULL,
                    capacity INT NOT NULL
                )
            """;
            executeUpdate(conn, roomTypesTable);

            // Create rooms table
            String roomsTable = """
                CREATE TABLE IF NOT EXISTS rooms (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    room_number VARCHAR(50) UNIQUE NOT NULL,
                    room_type_name VARCHAR(50) NOT NULL,
                    FOREIGN KEY (room_type_name) REFERENCES room_types(name)
                )
            """;
            executeUpdate(conn, roomsTable);

            // Create amenities table
            String amenitiesTable = """
                CREATE TABLE IF NOT EXISTS amenities (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(100) UNIQUE NOT NULL,
                    price DOUBLE NOT NULL
                )
            """;
            executeUpdate(conn, amenitiesTable);

            // Create room_amenities junction table
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

            // Create reservations table
            String reservationsTable = """
                CREATE TABLE IF NOT EXISTS reservations (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    reservation_id VARCHAR(50) UNIQUE NOT NULL,
                    guest_username VARCHAR(100) NOT NULL,
                    room_number VARCHAR(50) NOT NULL,
                    check_in_date DATE NOT NULL,
                    check_out_date DATE NOT NULL,
                    status VARCHAR(50) DEFAULT 'PENDING',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (guest_username) REFERENCES users(username),
                    FOREIGN KEY (room_number) REFERENCES rooms(room_number)
                )
            """;
            executeUpdate(conn, reservationsTable);
            // Create invoices table
        String invoicesTable = """
            CREATE TABLE IF NOT EXISTS invoices (
                id INT AUTO_INCREMENT PRIMARY KEY,
                guest_username VARCHAR(100) NOT NULL,
                room_number VARCHAR(50) NOT NULL,
                total_amount DOUBLE NOT NULL,
                payment_method VARCHAR(50) NOT NULL,
                paid BOOLEAN DEFAULT TRUE,
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
            
            // Gracefully add missing columns to existing tables (if they were created before)
            try {
                executeUpdate(conn, "ALTER TABLE users ADD COLUMN balance DOUBLE DEFAULT 0.0");
                System.out.println("Added missing 'balance' column to users table.");
            } catch (SQLException e) {
                // Column already exists, safe to ignore
            }
            try {
                executeUpdate(conn, "ALTER TABLE users ADD COLUMN is_logged_in BOOLEAN DEFAULT FALSE");
                System.out.println("Added missing 'is_logged_in' column to users table.");
            } catch (SQLException e) {
                // Column already exists, safe to ignore
            }
            try {
                executeUpdate(conn, "ALTER TABLE reservations ADD COLUMN reservation_id VARCHAR(50) UNIQUE NOT NULL AFTER id");
                System.out.println("Added missing 'reservation_id' column to reservations table.");
            } catch (SQLException e) {
                // Column already exists, safe to ignore
            }
            try {
                executeUpdate(conn, "ALTER TABLE invoices ADD COLUMN guest_username VARCHAR(100) NOT NULL AFTER id");
                System.out.println("Added missing 'guest_username' column to invoices table.");
            } catch (SQLException e) {
                // Column already exists, safe to ignore
            }
        try {
            executeUpdate(conn, "INSERT IGNORE INTO system_settings (setting_key, setting_value) VALUES ('current_date', '" + java.time.LocalDate.now().toString() + "')");
            System.out.println("System date tracking initialized.");
        } catch (SQLException e) {
            // Safe to ignore
        }
        try {
            executeUpdate(conn, "INSERT IGNORE INTO system_settings (setting_key, setting_value) VALUES ('last_update', '0')");
            System.out.println("System data versioning initialized.");
        } catch (SQLException e) {
            // Safe to ignore
        }
        try {
            executeUpdate(conn, "INSERT IGNORE INTO system_settings (setting_key, setting_value) VALUES ('active_instances', '0')");
            System.out.println("Active instance tracking initialized.");
        } catch (SQLException e) {
            // Safe to ignore
        }
        }
    }

    private static void executeUpdate(Connection conn, String sql) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }
}
