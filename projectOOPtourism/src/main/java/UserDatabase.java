import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;

public class UserDatabase {

    private static final String URL = "jdbc:mysql://localhost:3306/hotel_db";
    private static final String USER = "root";
    private static final String PASSWORD = "password";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static ArrayList<User> loadUsersFromDatabase() {
        ArrayList<User> users = new ArrayList<>();

        String sql = "SELECT * FROM users";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String username = rs.getString("username");
                String password = rs.getString("password");
                String role = rs.getString("role");
                LocalDate dateOfBirth = rs.getDate("date_of_birth") != null ?
                    rs.getDate("date_of_birth").toLocalDate() : LocalDate.now();

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
                    double balance = 0.0; // Default balance since we don't store it
                    String address = rs.getString("address") != null ? rs.getString("address") : "";
                    Gender gender = rs.getString("gender") != null ?
                        Gender.valueOf(rs.getString("gender").toUpperCase()) : Gender.MALE;
                    String roomPreferences = ""; // Default since we don't store it

                    user = new Guest(username, password, dateOfBirth, balance, address, gender, roomPreferences);
                }

                users.add(user);
            }

        } catch (SQLException e) {
            System.out.println("Error loading users from database:");
            e.printStackTrace();
        }

        return users;
    }

    public static User findUser(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String password = rs.getString("password");
                String role = rs.getString("role");
                LocalDate dateOfBirth = rs.getDate("date_of_birth") != null ?
                    rs.getDate("date_of_birth").toLocalDate() : LocalDate.now();

                User user;

                if ("ADMIN".equalsIgnoreCase(role)) {
                    user = new Admin(username, password, dateOfBirth, 0);
                }
                else if ("RECEPTIONIST".equalsIgnoreCase(role)) {
                    user = new Receptionist(username, password, dateOfBirth, 0);
                }
                else {
                    double balance = 0.0;
                    String address = rs.getString("address") != null ? rs.getString("address") : "";
                    Gender gender = rs.getString("gender") != null ?
                        Gender.valueOf(rs.getString("gender").toUpperCase()) : Gender.MALE;
                    String roomPreferences = "";

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
}