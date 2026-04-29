package DatabaseInitializer;

import java.sql.*;

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

            // Create reservations table
            String reservationsTable = """
                CREATE TABLE IF NOT EXISTS reservations (
                    id INT AUTO_INCREMENT PRIMARY KEY,
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

            System.out.println("All tables created successfully.");
        }
    }

    private static void executeUpdate(Connection conn, String sql) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }
}
