import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import DatabaseInitializer.DatabaseConnection;

public abstract class SystemTime {
    private static LocalDate today = LocalDate.now();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static LocalDate getToday() {
        return today;
    }

    public static String getDate() {
        return today.format(formatter);
    }

    public static void advanceDays(int days) {
        today = today.plusDays(days);
        String sql = "UPDATE system_settings SET setting_value = ? WHERE setting_key = 'current_date'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, today.format(formatter));
            if (stmt.executeUpdate() > 0) {
                Database.notifyDataChanged();
            }
        } catch (Exception e) {
            System.out.println("Failed to sync date to database: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("FAST FORWARD: The date is now " + getDate());
    }

    public static void resetToRealToday() {
        today = LocalDate.now();
        String sql = "UPDATE system_settings SET setting_value = ? WHERE setting_key = 'current_date'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, today.format(formatter));
            if (stmt.executeUpdate() > 0) {
                Database.notifyDataChanged();
            }
        } catch (Exception e) {
            System.out.println("Failed to reset date to database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void syncFromDatabase() {
        String sql = "SELECT setting_value FROM system_settings WHERE setting_key = 'current_date'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                LocalDate dbDate = LocalDate.parse(rs.getString("setting_value"), formatter);
                
                // Automatically catch up if the database date is stuck in the past
                if (dbDate.isBefore(LocalDate.now())) {
                    today = LocalDate.now();
                    String updateSql = "UPDATE system_settings SET setting_value = ? WHERE setting_key = 'current_date'";
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setString(1, today.format(formatter));
                        updateStmt.executeUpdate();
                    }
                } else {
                    today = dbDate;
                }
            }
        } catch (Exception e) {
            System.out.println("Could not fetch date from database: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
