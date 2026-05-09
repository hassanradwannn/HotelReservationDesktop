package Repositories;


import Controllers.*;
import Models.*;
import Services.*;
import Utils.*;
import Utils.exceptions.*;
import Repositories.database.DatabaseConnection;
import Repositories.DatabaseInitializer.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import Repositories.database.DatabaseConnection;

public class SystemSettingsRepository {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public boolean updateCurrentDate(LocalDate date) {
        String sql = "UPDATE system_settings SET setting_value = ? WHERE setting_key = 'current_date'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, date.format(FORMATTER));
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public LocalDate loadCurrentDateCatchingUp() {
        String sql = "SELECT setting_value FROM system_settings WHERE setting_key = 'current_date'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                LocalDate dbDate = LocalDate.parse(rs.getString("setting_value"), FORMATTER);

                if (dbDate.isBefore(LocalDate.now())) {
                    LocalDate realToday = LocalDate.now();
                    String updateSql = "UPDATE system_settings SET setting_value = ? WHERE setting_key = 'current_date'";
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setString(1, realToday.format(FORMATTER));
                        updateStmt.executeUpdate();
                    }
                    return realToday;
                }

                return dbDate;
            }
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        return null;
    }

    public boolean isFirstInstance() {
        String checkSql = "SELECT setting_value FROM system_settings WHERE setting_key = 'active_instances' FOR UPDATE";
        String insertSql = "INSERT INTO system_settings (setting_key, setting_value) VALUES ('active_instances', '1')";
        String updateSql = "UPDATE system_settings SET setting_value = ? WHERE setting_key = 'active_instances'";
        try (Connection conn = DatabaseConnection.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                int current = 0;
                boolean exists = false;
                try (PreparedStatement stmt = conn.prepareStatement(checkSql);
                     ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        exists = true;
                        current = Integer.parseInt(rs.getString("setting_value"));
                    }
                }

                if (exists) {
                    try (PreparedStatement update = conn.prepareStatement(updateSql)) {
                        update.setString(1, String.valueOf(current + 1));
                        update.executeUpdate();
                    }
                } else {
                    try (PreparedStatement insert = conn.prepareStatement(insertSql)) {
                        insert.executeUpdate();
                    }
                }

                conn.commit();
                conn.setAutoCommit(originalAutoCommit);
                return current == 0;
            } catch (Exception e) {
                conn.rollback();
                conn.setAutoCommit(originalAutoCommit);
                throw e;
            }
        } catch (Exception e) {
            System.out.println("Could not register instance: " + e.getMessage());
            e.printStackTrace();
            return true;
        }
    }

    public void unregisterInstance() {
        String checkSql = "SELECT setting_value FROM system_settings WHERE setting_key = 'active_instances'";
        String updateSql = "UPDATE system_settings SET setting_value = ? WHERE setting_key = 'active_instances'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(checkSql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                int instances = Integer.parseInt(rs.getString("setting_value"));
                if (instances > 0) {
                    try (PreparedStatement update = conn.prepareStatement(updateSql)) {
                        update.setString(1, String.valueOf(instances - 1));
                        update.executeUpdate();
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Could not unregister instance: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public long getLatestDataVersion() {
        String sql = "SELECT setting_value FROM system_settings WHERE setting_key = 'last_update'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return Long.parseLong(rs.getString("setting_value"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void notifyDataChanged() {
        String sql = "UPDATE system_settings SET setting_value = setting_value + 1 WHERE setting_key = 'last_update'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
