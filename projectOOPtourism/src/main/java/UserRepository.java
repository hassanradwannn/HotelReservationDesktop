import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import database.DatabaseConnection;

public class UserRepository {

    public void saveUser(User user) {
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
                stmt.setString(9, String.join(", ", guest.getRoomPreferenceNames()));
            } else {
                stmt.setNull(5, java.sql.Types.VARCHAR);
                stmt.setNull(6, java.sql.Types.VARCHAR);
                stmt.setNull(8, java.sql.Types.DOUBLE);
                stmt.setNull(9, java.sql.Types.VARCHAR);
            }

            stmt.setNull(7, java.sql.Types.DOUBLE);

            if (stmt.executeUpdate() > 0 && !DatabaseSaver.silentSync) {
                Database.notifyDataChanged();
            }
        } catch (Exception e) {
            System.out.println("User database save failed: " + e.getMessage());
        }
    }

    public void updateBalance(String username, double newBalance) {
        String sql = "UPDATE users SET balance = ? WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, newBalance);
            stmt.setString(2, username);
            if (stmt.executeUpdate() > 0 && !DatabaseSaver.silentSync) {
                Database.notifyDataChanged();
            }
        } catch (Exception e) {
            System.out.println("Failed to update user balance: " + e.getMessage());
        }
    }

    public void updatePassword(String username, String newPassword) {
        String sql = "UPDATE users SET password = ? WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newPassword);
            stmt.setString(2, username);
            if (stmt.executeUpdate() > 0 && !DatabaseSaver.silentSync) {
                Database.notifyDataChanged();
            }
        } catch (Exception e) {
            System.out.println("Failed to update user password: " + e.getMessage());
        }
    }

    public void updateGuestProfile(String oldUsername, Guest guest) throws Exception {
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
                    stmt.setString(5, String.join(", ", guest.getRoomPreferenceNames()));
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

                if (!DatabaseSaver.silentSync) {
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

    private void updateUsernameReference(
            Connection conn, String tableName, String columnName, String oldUsername, String newUsername) throws SQLException {
        String sql = "UPDATE " + tableName + " SET " + columnName + " = ? WHERE " + columnName + " = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newUsername);
            stmt.setString(2, oldUsername);
            stmt.executeUpdate();
        }
    }

    private void updateUsernameReferenceIfTableExists(
            Connection conn, String tableName, String columnName, String oldUsername, String newUsername) throws SQLException {
        if (tableExists(conn, tableName)) {
            updateUsernameReference(conn, tableName, columnName, oldUsername, newUsername);
        }
    }

    private boolean tableExists(Connection conn, String tableName) throws SQLException {
        try (ResultSet rs = conn.getMetaData().getTables(conn.getCatalog(), null, tableName, new String[]{"TABLE"})) {
            if (rs.next()) {
                return true;
            }
        }
        try (ResultSet rs = conn.getMetaData().getTables(conn.getCatalog(), null, tableName.toUpperCase(), new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    private void setForeignKeyChecks(Connection conn, boolean enabled) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("SET FOREIGN_KEY_CHECKS=" + (enabled ? "1" : "0"));
        }
    }
}
