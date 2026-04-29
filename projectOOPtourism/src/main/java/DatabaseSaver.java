import DatabaseInitializer.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class DatabaseSaver {

    public static void saveUser(User user) {
        String sql = """
            INSERT IGNORE INTO users 
            (username, password, role, date_of_birth, gender, address, salary)
            VALUES (?, ?, ?, ?, ?, ?, ?)
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
            } else {
                stmt.setNull(5, java.sql.Types.VARCHAR);
                stmt.setNull(6, java.sql.Types.VARCHAR);
            }

            // Salary is not in your current User/Staff models, safely set to null
            stmt.setNull(7, java.sql.Types.DOUBLE);

            stmt.executeUpdate();

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
            stmt.executeUpdate();

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
            stmt.executeUpdate();

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
            stmt.executeUpdate();

        } catch (Exception e) {
            System.out.println("Amenity database save failed: " + e.getMessage());
        }
    }

    public static void saveReservation(String guestUsername, String roomNumber,
                                       java.time.LocalDate checkIn,
                                       java.time.LocalDate checkOut,
                                       String status) {
        String sql = """
            INSERT INTO reservations 
            (guest_username, room_number, check_in, check_out, status)
            VALUES (?, ?, ?, ?, ?)
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, guestUsername);
            stmt.setString(2, roomNumber);
            stmt.setDate(3, java.sql.Date.valueOf(checkIn));
            stmt.setDate(4, java.sql.Date.valueOf(checkOut));
            stmt.setString(5, status);
            stmt.executeUpdate();

        } catch (Exception e) {
            System.out.println("Reservation database save failed: " + e.getMessage());
        }
    }

    public static void saveInvoice(String guestUsername, String roomNumber,
                                   double totalAmount, String paymentMethod,
                                   boolean paid) {
        String sql = """
            INSERT INTO invoices
            (guest_username, room_number, total_amount, payment_method, paid)
            VALUES (?, ?, ?, ?, ?)
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, guestUsername);
            stmt.setString(2, roomNumber);
            stmt.setDouble(3, totalAmount);
            stmt.setString(4, paymentMethod);
            stmt.setBoolean(5, paid);
            stmt.executeUpdate();

        } catch (Exception e) {
            System.out.println("Invoice database save failed: " + e.getMessage());
        }
    }
}