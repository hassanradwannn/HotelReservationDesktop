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
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import Repositories.database.DatabaseConnection;

public class UserDatabase {

    public static ArrayList<User> loadUsersFromDatabase() {
        ArrayList<User> users = new ArrayList<>();

        String sql = "SELECT * FROM users";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                try {
                String username = rs.getString("username");
                String password = rs.getString("password");
                String role = rs.getString("role");

                LocalDate dateOfBirth = LocalDate.now();
                try {
                    if (rs.getDate("date_of_birth") != null) {
                        dateOfBirth = rs.getDate("date_of_birth").toLocalDate();
                    }
                } catch (SQLException ex) {
                    System.out.println("Warning: Invalid date for user " + username);
                }

                User user;

                if ("ADMIN".equalsIgnoreCase(role)) {
                    // Admin constructor: (username, password, dateOfBirth, workingHours)
                    // We don't store working hours, so use default of 0
                    user = new Admin(username, password, dateOfBirth, 0);
                }
                else if ("RECEPTIONIST".equalsIgnoreCase(role)) {
                    // Receptionist constructor: (username, password, dateOfBirth, workingHours)
                    // We don't store working hours, so use default of 0
                    user = new Receptionist(username, password, dateOfBirth, 0);
                }
                else {
                    // Guest constructor: (username, password, dateOfBirth, balance, address, gender, roomPreferences)
                    double balance = rs.getDouble("balance");
                    String address = rs.getString("address") != null ? rs.getString("address") : "";
                    
                    Gender gender = Gender.MALE;
                    String genderStr = rs.getString("gender");
                    if (genderStr != null && !genderStr.trim().isEmpty()) {
                        try {
                            gender = Gender.valueOf(genderStr.trim().toUpperCase());
                        } catch (IllegalArgumentException ex) {
                            System.out.println("Warning: Invalid gender '" + genderStr + "' for user " + username);
                        }
                    }
                    
                    String roomPreferences = readRoomPreferences(rs);

                    user = new Guest(username, password, dateOfBirth, balance, address, gender, roomPreferences);
                }

                users.add(user);
                } catch (Exception e) {
                    System.out.println("Skipped a user due to error: " + e.getMessage());
                }
            }

        } catch (SQLException e) {
            System.out.println("Error loading users from database:");
            e.printStackTrace();
        }

        return users;
    }

    public static User findUser(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String password = rs.getString("password");
                String role = rs.getString("role");
                
                LocalDate dateOfBirth = LocalDate.now();
                try {
                    if (rs.getDate("date_of_birth") != null) {
                        dateOfBirth = rs.getDate("date_of_birth").toLocalDate();
                    }
                } catch (SQLException ex) {
                    System.out.println("Warning: Invalid date for user " + username);
                }

                User user;

                if ("ADMIN".equalsIgnoreCase(role)) {
                    user = new Admin(username, password, dateOfBirth, 0);
                }
                else if ("RECEPTIONIST".equalsIgnoreCase(role)) {
                    user = new Receptionist(username, password, dateOfBirth, 0);
                }
                else {
                    double balance = rs.getDouble("balance");
                    String address = rs.getString("address") != null ? rs.getString("address") : "";
                    
                    Gender gender = Gender.MALE;
                    String genderStr = rs.getString("gender");
                    if (genderStr != null && !genderStr.trim().isEmpty()) {
                        try {
                            gender = Gender.valueOf(genderStr.trim().toUpperCase());
                        } catch (IllegalArgumentException ex) {
                            System.out.println("Warning: Invalid gender '" + genderStr + "' for user " + username);
                        }
                    }
                    
                    String roomPreferences = readRoomPreferences(rs);

                    user = new Guest(username, password, dateOfBirth, balance, address, gender, roomPreferences);
                }

                return user;
            }

        } catch (SQLException e) {
            System.out.println("Error finding user in database:");
            e.printStackTrace();
        }

        return null;
    }

    public static boolean isUserLoggedIn(String username) {
        String sql = "SELECT is_logged_in FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getBoolean("is_logged_in");
            }
        } catch (SQLException e) {
            System.out.println("Error checking user logged in status:");
            e.printStackTrace();
        }
        return false;
    }

    public static void setUserLoggedIn(String username, boolean loggedIn) {
        String sql = "UPDATE users SET is_logged_in = ? WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, loggedIn);
            stmt.setString(2, username);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error updating user logged in status:");
            e.printStackTrace();
        }
    }

    public static void clearLoggedInUsers() {
        String sql = "UPDATE users SET is_logged_in = FALSE";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error clearing logged in users:");
            e.printStackTrace();
        }
    }

    private static String readRoomPreferences(ResultSet rs) {
        try {
            String roomPreferences = rs.getString("room_preferences");
            return roomPreferences == null ? "" : roomPreferences;
        } catch (SQLException ex) {
            return "";
        }
    }
}
