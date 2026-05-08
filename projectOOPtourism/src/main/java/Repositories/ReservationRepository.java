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
import java.time.LocalDate;

import Repositories.database.DatabaseConnection;

public class ReservationRepository {

    public void saveReservation(String reservationId, String guestUsername, String roomNumber,
                                LocalDate checkIn, LocalDate checkOut,
                                boolean hasGymPass, String status) {
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
            if (stmt.executeUpdate() > 0 && !DatabaseSaver.silentSync) {
                Database.notifyDataChanged();
            }
        } catch (Exception e) {
            System.out.println("Reservation database save failed: " + e.getMessage());
        }
    }

    public void updateStatus(String reservationId, String newStatus) {
        String sql = "UPDATE reservations SET status = ? WHERE reservation_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newStatus);
            stmt.setString(2, reservationId);
            if (stmt.executeUpdate() > 0 && !DatabaseSaver.silentSync) {
                Database.notifyDataChanged();
            }
        } catch (Exception e) {
            System.out.println("Failed to update reservation status: " + e.getMessage());
        }
    }

    public void updateDates(String reservationId, LocalDate checkIn, LocalDate checkOut) {
        String sql = "UPDATE reservations SET check_in_date = ?, check_out_date = ? WHERE reservation_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, java.sql.Date.valueOf(checkIn));
            stmt.setDate(2, java.sql.Date.valueOf(checkOut));
            stmt.setString(3, reservationId);
            if (stmt.executeUpdate() > 0 && !DatabaseSaver.silentSync) {
                Database.notifyDataChanged();
            }
        } catch (Exception e) {
            System.out.println("Failed to update reservation dates: " + e.getMessage());
        }
    }
}
