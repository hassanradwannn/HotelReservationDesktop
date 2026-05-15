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

import Repositories.database.DatabaseConnection;

public class CatalogRepository {

    public void saveRoomType(RoomType type) {
        String sql = """
            INSERT IGNORE INTO room_types (name, price_per_night, capacity)
            VALUES (?, ?, ?)
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, type.getName());
            stmt.setDouble(2, type.getPricePerNight());
            stmt.setInt(3, type.getCapacity());
            if (stmt.executeUpdate() > 0 && !DatabaseSaver.silentSync) {
                Database.notifyDataChanged();
            }
        } catch (Exception e) {
            System.out.println("Room type database save failed: " + e.getMessage());
        }
    }

    public void saveRoom(Room room) {
        String sql = """
            INSERT IGNORE INTO rooms (room_number, room_type_name)
            VALUES (?, ?)
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, room.getRoomNumber());
            stmt.setString(2, room.getRoomType().getName());
            if (stmt.executeUpdate() > 0 && !DatabaseSaver.silentSync) {
                Database.notifyDataChanged();
            }
        } catch (Exception e) {
            System.out.println("Room database save failed: " + e.getMessage());
        }
    }

    public void saveAmenity(Amenity amenity) {
        amenity.setName(CatalogService.displayAmenityName(amenity.getName()));
        String sql = """
            INSERT IGNORE INTO amenities (name, price)
            VALUES (?, ?)
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, amenity.getName());
            stmt.setDouble(2, amenity.getPrice());
            if (stmt.executeUpdate() > 0 && !DatabaseSaver.silentSync) {
                Database.notifyDataChanged();
            }
        } catch (Exception e) {
            System.out.println("Amenity database save failed: " + e.getMessage());
        }
    }

    public void saveRoomAmenities(Room room) {
        String deleteSql = "DELETE FROM room_amenities WHERE room_number = ?";
        String insertSql = "INSERT IGNORE INTO room_amenities (room_number, amenity_name) VALUES (?, ?)";

        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                deleteStmt.setString(1, room.getRoomNumber());
                deleteStmt.executeUpdate();
            }
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                java.util.Set<String> savedAmenityKeys = new java.util.LinkedHashSet<>();
                for (Amenity amenity : room.getAmenities()) {
                    String amenityName = CatalogService.displayAmenityName(amenity.getName());
                    if (!savedAmenityKeys.add(CatalogService.amenityFilterKey(amenityName))) {
                        continue;
                    }
                    insertStmt.setString(1, room.getRoomNumber());
                    insertStmt.setString(2, amenityName);
                    insertStmt.executeUpdate();
                }
            }
            if (!DatabaseSaver.silentSync) {
                Database.notifyDataChanged();
            }
        } catch (Exception e) {
            System.out.println("Room amenities database save failed: " + e.getMessage());
        }
    }
}
