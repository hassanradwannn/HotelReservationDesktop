import java.sql.Connection;
import java.sql.PreparedStatement;

import database.DatabaseConnection;

public class DatabaseSaver {

    public static boolean silentSync = false;

    public static void saveUser(User user) {
        String sql = """
            INSERT IGNORE INTO users 
            (username, password, role, date_of_birth, gender, address, salary, balance)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
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
            } else {
                stmt.setNull(5, java.sql.Types.VARCHAR);
                stmt.setNull(6, java.sql.Types.VARCHAR);
                stmt.setNull(8, java.sql.Types.DOUBLE);
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
        String sql = """
            INSERT INTO invoices
            (reservation_id, guest_username, room_number, total_amount, payment_method, paid)
            VALUES (?, ?, ?, ?, ?, ?)
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
}
