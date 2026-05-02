import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import database.DatabaseConnection;

public class DatabaseSaver {

    public static boolean silentSync = false;

    public static void ensureInvoiceSchema() {
        String createSql = """
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

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createSql);
            addInvoiceColumnIfMissing(stmt, "guest_username VARCHAR(100) NOT NULL DEFAULT '' AFTER id");
            addInvoiceColumnIfMissing(stmt, "room_number VARCHAR(50) NOT NULL DEFAULT '' AFTER guest_username");
            addInvoiceColumnIfMissing(stmt, "reservation_id VARCHAR(50) AFTER room_number");
            addInvoiceColumnIfMissing(stmt, "total_amount DOUBLE NOT NULL DEFAULT 0 AFTER reservation_id");
            addInvoiceColumnIfMissing(stmt, "payment_method VARCHAR(50) NOT NULL DEFAULT 'ONLINE' AFTER total_amount");
            addInvoiceColumnIfMissing(stmt, "paid BOOLEAN DEFAULT TRUE AFTER payment_method");
            addInvoiceColumnIfMissing(stmt, "payment_date DATE AFTER paid");
        } catch (Exception e) {
            System.out.println("Invoice schema check failed: " + e.getMessage());
        }
    }

    private static void addInvoiceColumnIfMissing(Statement stmt, String columnDefinition) {
        try {
            stmt.executeUpdate("ALTER TABLE invoices ADD COLUMN " + columnDefinition);
        } catch (SQLException ignored) {
            // Column already exists.
        }
    }

    public static void saveUser(User user) {
        String sql = """
            INSERT IGNORE INTO users
            (username, password, role, date_of_birth, gender, address, salary, balance, room_preferences)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPassword());
            stmt.setString(3, user.getClass().getSimpleName());

            if (user.getDateOfBirth() != null) {
                stmt.setDate(4, java.sql.Date.valueOf(user.getDateOfBirth()));
            } else {
                stmt.setNull(4, java.sql.Types.DATE);
            }

            if (user instanceof Guest guest) {
                stmt.setString(5, guest.getGender() != null ? guest.getGender().toString() : null);
                stmt.setString(6, guest.getAddress());
                stmt.setDouble(8, guest.getBalance());
                stmt.setString(9, guest.getRoomPreferences());
            } else {
                stmt.setNull(5, java.sql.Types.VARCHAR);
                stmt.setNull(6, java.sql.Types.VARCHAR);
                stmt.setNull(8, java.sql.Types.DOUBLE);
                stmt.setNull(9, java.sql.Types.VARCHAR);
            }

            // Salary is not in your current User/Staff models, safely set to null
            stmt.setNull(7, java.sql.Types.DOUBLE);

            if (stmt.executeUpdate() > 0 && !silentSync) {
                Database.notifyDataChanged();
            }

        } catch (Exception e) {
            System.out.println("User database save failed: " + e.getMessage());
        }
    }

    public static void saveRoomType(RoomType type) {
        String sql = """
            INSERT IGNORE INTO room_types (name, price_per_night, capacity)
            VALUES (?, ?, ?)
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, type.getName());
            stmt.setDouble(2, type.getPricePerNight());
            stmt.setInt(3, type.getCapacity());
            if (stmt.executeUpdate() > 0 && !silentSync) {
                Database.notifyDataChanged();
            }

        } catch (Exception e) {
            System.out.println("Room type database save failed: " + e.getMessage());
        }
    }

    public static void saveRoom(Room room) {
        String sql = """
            INSERT IGNORE INTO rooms (room_number, room_type_name)
            VALUES (?, ?)
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, room.getRoomNumber());
            stmt.setString(2, room.getRoomType().getName());
            if (stmt.executeUpdate() > 0 && !silentSync) {
                Database.notifyDataChanged();
            }

        } catch (Exception e) {
            System.out.println("Room database save failed: " + e.getMessage());
        }
    }

    public static void saveAmenity(Amenity amenity) {
        String sql = """
            INSERT IGNORE INTO amenities (name, price)
            VALUES (?, ?)
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, amenity.getName());
            stmt.setDouble(2, amenity.getPrice());
            if (stmt.executeUpdate() > 0 && !silentSync) {
                Database.notifyDataChanged();
            }

        } catch (Exception e) {
            System.out.println("Amenity database save failed: " + e.getMessage());
        }
    }

    public static void saveRoomAmenities(Room room) {
        String deleteSql = "DELETE FROM room_amenities WHERE room_number = ?";
        String insertSql = "INSERT IGNORE INTO room_amenities (room_number, amenity_name) VALUES (?, ?)";

        try (Connection conn = DatabaseConnection.getConnection()) {
            // Clear existing amenities first to prevent duplicates/sync updates
            try (PreparedStatement delStmt = conn.prepareStatement(deleteSql)) {
                delStmt.setString(1, room.getRoomNumber());
                delStmt.executeUpdate();
            }
            try (PreparedStatement insStmt = conn.prepareStatement(insertSql)) {
                for (Amenity amenity : room.getAmenities()) {
                    insStmt.setString(1, room.getRoomNumber());
                    insStmt.setString(2, amenity.getName());
                    insStmt.executeUpdate();
                }
            }
            if (!silentSync) {
                Database.notifyDataChanged();
            }
        } catch (Exception e) {
            System.out.println("Room amenities database save failed: " + e.getMessage());
        }
    }

    public static void saveReservation(String reservationId, String guestUsername, String roomNumber,
                                       java.time.LocalDate checkIn,
                                       java.time.LocalDate checkOut,
                                       boolean hasGymPass,
                                       String status) {
        String sql = """
            INSERT INTO reservations 
            (reservation_id, guest_username, room_number, check_in_date, check_out_date, has_gym_pass, status)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, reservationId);
            stmt.setString(2, guestUsername);
            stmt.setString(3, roomNumber);
            stmt.setDate(4, java.sql.Date.valueOf(checkIn));
            stmt.setDate(5, java.sql.Date.valueOf(checkOut));
            stmt.setBoolean(6, hasGymPass);
            stmt.setString(7, status);
            if (stmt.executeUpdate() > 0 && !silentSync) {
                Database.notifyDataChanged();
            }

        } catch (Exception e) {
            System.out.println("Reservation database save failed: " + e.getMessage());
        }
    }

    public static void saveInvoice(String guestUsername, String roomNumber,
                                   double totalAmount, String paymentMethod,
                                   boolean paid) {
        saveInvoice(null, guestUsername, roomNumber, totalAmount, paymentMethod, paid);
    }

    public static void saveInvoice(String reservationId, String guestUsername, String roomNumber,
                                   double totalAmount, String paymentMethod,
                                   boolean paid) {
        ensureInvoiceSchema();
        String sql = """
            INSERT INTO invoices
            (reservation_id, guest_username, room_number, total_amount, payment_method, paid, payment_date)
            VALUES (?, ?, ?, ?, ?, ?, CURRENT_DATE)
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, reservationId);
            stmt.setString(2, guestUsername);
            stmt.setString(3, roomNumber);
            stmt.setDouble(4, totalAmount);
            stmt.setString(5, paymentMethod);
            stmt.setBoolean(6, paid);
            if (stmt.executeUpdate() > 0 && !silentSync) {
                Database.notifyDataChanged();
            }

        } catch (Exception e) {
            System.out.println("Invoice database save failed: " + e.getMessage());
        }
    }

    public static void updateUserBalance(String username, double newBalance) {
        String sql = "UPDATE users SET balance = ? WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, newBalance);
            stmt.setString(2, username);
            if (stmt.executeUpdate() > 0 && !silentSync) {
                Database.notifyDataChanged();
            }

        } catch (Exception e) {
            System.out.println("Failed to update user balance: " + e.getMessage());
        }
    }

    public static void updateReservationStatus(String reservationId, String newStatus) {
        String sql = "UPDATE reservations SET status = ? WHERE reservation_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newStatus);
            stmt.setString(2, reservationId);
            if (stmt.executeUpdate() > 0 && !silentSync) {
                Database.notifyDataChanged();
            }

        } catch (Exception e) {
            System.out.println("Failed to update reservation status: " + e.getMessage());
        }
    }

    public static void updateReservationDates(String reservationId, java.time.LocalDate checkIn, java.time.LocalDate checkOut) {
        String sql = "UPDATE reservations SET check_in_date = ?, check_out_date = ? WHERE reservation_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, java.sql.Date.valueOf(checkIn));
            stmt.setDate(2, java.sql.Date.valueOf(checkOut));
            stmt.setString(3, reservationId);
            if (stmt.executeUpdate() > 0 && !silentSync) {
                Database.notifyDataChanged();
            }

        } catch (Exception e) {
            System.out.println("Failed to update reservation dates: " + e.getMessage());
        }
    }

    public static void updateUserPassword(String username, String newPassword) {
        String sql = "UPDATE users SET password = ? WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newPassword);
            stmt.setString(2, username);
            if (stmt.executeUpdate() > 0 && !silentSync) {
                Database.notifyDataChanged();
            }

        } catch (Exception e) {
            System.out.println("Failed to update user password: " + e.getMessage());
        }
    }

    public static void updateGuestProfile(String oldUsername, Guest guest) throws Exception {
        if (guest == null) {
            throw new IllegalArgumentException("Guest profile is required.");
        }

        String previousUsername = oldUsername == null ? "" : oldUsername.trim();
        String newUsername = guest.getUsername() == null ? "" : guest.getUsername().trim();
        if (previousUsername.isEmpty() || newUsername.isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty.");
        }

        String updateUserSql = """
            UPDATE users
            SET username = ?, date_of_birth = ?, gender = ?, address = ?, room_preferences = ?
            WHERE username = ?
        """;

        try (Connection conn = DatabaseConnection.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            setForeignKeyChecks(conn, false);

            try {
                try (PreparedStatement stmt = conn.prepareStatement(updateUserSql)) {
                    stmt.setString(1, newUsername);
                    if (guest.getDateOfBirth() == null) {
                        stmt.setNull(2, java.sql.Types.DATE);
                    } else {
                        stmt.setDate(2, java.sql.Date.valueOf(guest.getDateOfBirth()));
                    }
                    stmt.setString(3, guest.getGender() == null ? null : guest.getGender().toString());
                    stmt.setString(4, guest.getAddress());
                    stmt.setString(5, guest.getRoomPreferences());
                    stmt.setString(6, previousUsername);

                    if (stmt.executeUpdate() == 0) {
                        throw new IllegalArgumentException("Guest profile could not be found.");
                    }
                }

                updateUsernameReference(conn, "reservations", "guest_username", previousUsername, newUsername);
                updateUsernameReference(conn, "invoices", "guest_username", previousUsername, newUsername);
                updateUsernameReferenceIfTableExists(conn, "chats", "guest_username", previousUsername, newUsername);
                updateUsernameReferenceIfTableExists(conn, "chat_messages", "sender_username", previousUsername, newUsername);

                setForeignKeyChecks(conn, true);
                conn.commit();
                conn.setAutoCommit(originalAutoCommit);

                if (!silentSync) {
                    Database.notifyDataChanged();
                }
            } catch (Exception ex) {
                conn.rollback();
                setForeignKeyChecks(conn, true);
                conn.setAutoCommit(originalAutoCommit);
                throw ex;
            }
        }
    }

    private static void updateUsernameReference(
            Connection conn, String tableName, String columnName, String oldUsername, String newUsername) throws SQLException {
        String sql = "UPDATE " + tableName + " SET " + columnName + " = ? WHERE " + columnName + " = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newUsername);
            stmt.setString(2, oldUsername);
            stmt.executeUpdate();
        }
    }

    private static void updateUsernameReferenceIfTableExists(
            Connection conn, String tableName, String columnName, String oldUsername, String newUsername) throws SQLException {
        if (tableExists(conn, tableName)) {
            updateUsernameReference(conn, tableName, columnName, oldUsername, newUsername);
        }
    }

    private static boolean tableExists(Connection conn, String tableName) throws SQLException {
        try (ResultSet rs = conn.getMetaData().getTables(conn.getCatalog(), null, tableName, new String[]{"TABLE"})) {
            if (rs.next()) {
                return true;
            }
        }
        try (ResultSet rs = conn.getMetaData().getTables(conn.getCatalog(), null, tableName.toUpperCase(), new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    private static void setForeignKeyChecks(Connection conn, boolean enabled) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("SET FOREIGN_KEY_CHECKS=" + (enabled ? "1" : "0"));
        }
    }
}
