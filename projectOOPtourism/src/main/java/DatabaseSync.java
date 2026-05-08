import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import database.DatabaseConnection;

public class DatabaseSync {

    private static boolean isTableEmpty(String tableName) {
        String sql = "SELECT COUNT(*) FROM " + tableName;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1) == 0;
            }
        } catch (Exception e) {
            System.out.println("Error checking table " + tableName + ": " + e.getMessage());
        }
        return true;
    }

    public static void syncDefaultDataToMySQL() {
        // Safe guard: only perform massive startup sync if the database is truly empty
        if (!isTableEmpty("rooms") && !isTableEmpty("amenities")) {
            return;
        }

        DatabaseSaver.silentSync = true;

        for (RoomType type : Database.getRoomTypes()) {
            DatabaseSaver.saveRoomType(type);
        }

        for (Amenity amenity : Database.getAmenities()) {
            DatabaseSaver.saveAmenity(amenity);
        }

        for (Room room : Database.getRooms()) {
            DatabaseSaver.saveRoom(room);
            DatabaseSaver.saveRoomAmenities(room);
        }

        for (Guest guest : Database.getGuests()) {
            DatabaseSaver.saveUser(guest);
        }

        for (Staff staff : Database.getStaffMembers()) {
            DatabaseSaver.saveUser(staff);
        }

        System.out.println("Default memory data synced to MySQL.");
        DatabaseSaver.silentSync = false;
    }
}