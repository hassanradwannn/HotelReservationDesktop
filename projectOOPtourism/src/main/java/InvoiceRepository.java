import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import database.DatabaseConnection;

public class InvoiceRepository {

    public static class PaidAmountRecord {
        private final double paidTotal;
        private final boolean hasDepositMarker;

        public PaidAmountRecord(double paidTotal, boolean hasDepositMarker) {
            this.paidTotal = paidTotal;
            this.hasDepositMarker = hasDepositMarker;
        }

        public double getPaidTotal() {
            return paidTotal;
        }

        public boolean hasDepositMarker() {
            return hasDepositMarker;
        }
    }

    public void ensureSchema() {
        String createSql = """
            CREATE TABLE IF NOT EXISTS invoices (
                id INT AUTO_INCREMENT PRIMARY KEY,
                guest_username VARCHAR(100) NOT NULL,
                room_number VARCHAR(50) NOT NULL,
                reservation_id VARCHAR(50),
                total_amount DOUBLE NOT NULL,
                payment_method VARCHAR(50) NOT NULL,
                paid BOOLEAN DEFAULT TRUE,
                payment_date DATE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createSql);
            addColumnIfMissing(stmt, "guest_username VARCHAR(100) NOT NULL DEFAULT '' AFTER id");
            addColumnIfMissing(stmt, "room_number VARCHAR(50) NOT NULL DEFAULT '' AFTER guest_username");
            addColumnIfMissing(stmt, "reservation_id VARCHAR(50) AFTER room_number");
            addColumnIfMissing(stmt, "total_amount DOUBLE NOT NULL DEFAULT 0 AFTER reservation_id");
            addColumnIfMissing(stmt, "payment_method VARCHAR(50) NOT NULL DEFAULT 'ONLINE' AFTER total_amount");
            addColumnIfMissing(stmt, "paid BOOLEAN DEFAULT TRUE AFTER payment_method");
            addColumnIfMissing(stmt, "payment_date DATE AFTER paid");
        } catch (Exception e) {
            System.out.println("Invoice schema check failed: " + e.getMessage());
        }
    }

    public PaidAmountRecord findPaidAmount(Reservation reservation) throws SQLException {
        String sql = """
            SELECT
                COALESCE(SUM(CASE
                    WHEN paid = TRUE AND UPPER(payment_method) NOT LIKE 'EARLY_CHECKOUT_REFUND%' THEN total_amount
                    ELSE 0
                END), 0) AS paid_total,
                COALESCE(MAX(CASE
                    WHEN paid = TRUE AND UPPER(payment_method) LIKE 'DEPOSIT%' THEN 1
                    ELSE 0
                END), 0) AS has_deposit_marker
            FROM invoices
            WHERE (
                    reservation_id = ?
                    OR (reservation_id IS NULL AND guest_username = ? AND room_number = ?)
                  )
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, reservation.getReservationId());
            stmt.setString(2, reservation.getGuest().getUsername());
            stmt.setString(3, reservation.getRoom().getRoomNumber());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new PaidAmountRecord(
                            rs.getDouble("paid_total"),
                            rs.getInt("has_deposit_marker") == 1);
                }
            }
        }
        return new PaidAmountRecord(0.0, false);
    }

    public double findEarlyCheckOutRefundAmount(Reservation reservation) throws SQLException {
        String sql = """
            SELECT COALESCE(SUM(CASE
                WHEN total_amount < 0 THEN -total_amount
                ELSE total_amount
            END), 0) AS refund_total
            FROM invoices
            WHERE reservation_id = ?
              AND UPPER(payment_method) LIKE 'EARLY_CHECKOUT_REFUND%'
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, reservation.getReservationId());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Math.max(0, rs.getDouble("refund_total"));
                }
            }
        }
        return 0.0;
    }

    public double findGrossPaidAmountBeforeRefunds(Reservation reservation) throws SQLException {
        String sql = """
            SELECT COALESCE(SUM(CASE
                WHEN paid = TRUE AND total_amount > 0 THEN total_amount
                ELSE 0
            END), 0) AS paid_total
            FROM invoices
            WHERE (
                    reservation_id = ?
                    OR (reservation_id IS NULL AND guest_username = ? AND room_number = ?)
                  )
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, reservation.getReservationId());
            stmt.setString(2, reservation.getGuest().getUsername());
            stmt.setString(3, reservation.getRoom().getRoomNumber());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("paid_total");
                }
            }
        }
        return 0.0;
    }

    public Map<String, ReservationPaymentSummary> findPaymentSummaries(List<Reservation> reservations) throws SQLException {
        Map<String, ReservationPaymentSummary> summaries = new HashMap<>();
        if (reservations == null || reservations.isEmpty()) {
            return summaries;
        }

        Map<String, Double> grossPaidByReservationId = new HashMap<>();
        Map<String, Double> refundByReservationId = new HashMap<>();
        loadReservationInvoiceTotals(reservations, grossPaidByReservationId, refundByReservationId);

        Map<String, Double> legacyGrossPaidByGuestRoom = loadLegacyGrossPaidTotals();
        for (Reservation reservation : reservations) {
            String reservationId = reservation.getReservationId();
            double grossPaid = grossPaidByReservationId.getOrDefault(reservationId, 0.0)
                    + legacyGrossPaidByGuestRoom.getOrDefault(guestRoomKey(reservation), 0.0);
            double refund = refundByReservationId.getOrDefault(reservationId, 0.0);
            summaries.put(reservationId, new ReservationPaymentSummary(grossPaid, refund));
        }

        return summaries;
    }

    private void loadReservationInvoiceTotals(
            List<Reservation> reservations,
            Map<String, Double> grossPaidByReservationId,
            Map<String, Double> refundByReservationId) throws SQLException {
        String placeholders = "?,".repeat(reservations.size());
        placeholders = placeholders.substring(0, placeholders.length() - 1);
        String sql = """
            SELECT reservation_id,
                   COALESCE(SUM(CASE
                       WHEN paid = TRUE AND total_amount > 0 THEN total_amount
                       ELSE 0
                   END), 0) AS paid_total,
                   COALESCE(SUM(CASE
                       WHEN UPPER(payment_method) LIKE 'EARLY_CHECKOUT_REFUND%' THEN
                           CASE WHEN total_amount < 0 THEN -total_amount ELSE total_amount END
                       ELSE 0
                   END), 0) AS refund_total
            FROM invoices
            WHERE reservation_id IN (%s)
            GROUP BY reservation_id
        """.formatted(placeholders);

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            int index = 1;
            for (Reservation reservation : reservations) {
                stmt.setString(index++, reservation.getReservationId());
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String reservationId = rs.getString("reservation_id");
                    grossPaidByReservationId.put(reservationId, rs.getDouble("paid_total"));
                    refundByReservationId.put(reservationId, Math.max(0, rs.getDouble("refund_total")));
                }
            }
        }
    }

    private Map<String, Double> loadLegacyGrossPaidTotals() throws SQLException {
        Map<String, Double> grossPaidByGuestRoom = new HashMap<>();
        String sql = """
            SELECT guest_username,
                   room_number,
                   COALESCE(SUM(CASE
                       WHEN paid = TRUE AND total_amount > 0 THEN total_amount
                       ELSE 0
                   END), 0) AS paid_total
            FROM invoices
            WHERE reservation_id IS NULL
            GROUP BY guest_username, room_number
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                grossPaidByGuestRoom.put(
                        guestRoomKey(rs.getString("guest_username"), rs.getString("room_number")),
                        rs.getDouble("paid_total"));
            }
        }
        return grossPaidByGuestRoom;
    }

    private String guestRoomKey(Reservation reservation) {
        return guestRoomKey(reservation.getGuest().getUsername(), reservation.getRoom().getRoomNumber());
    }

    private String guestRoomKey(String guestUsername, String roomNumber) {
        return guestUsername + "\u0000" + roomNumber;
    }

    public void saveInvoice(String guestUsername, String roomNumber,
                            double totalAmount, String paymentMethod,
                            boolean paid) {
        saveInvoice(null, guestUsername, roomNumber, totalAmount, paymentMethod, paid);
    }

    public void saveInvoice(String reservationId, String guestUsername, String roomNumber,
                            double totalAmount, String paymentMethod,
                            boolean paid) {
        ensureSchema();
        String sql = """
            INSERT INTO invoices
            (reservation_id, guest_username, room_number, total_amount, payment_method, paid, payment_date)
            VALUES (?, ?, ?, ?, ?, ?, CURRENT_DATE)
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, reservationId);
            stmt.setString(2, guestUsername);
            stmt.setString(3, roomNumber);
            stmt.setDouble(4, totalAmount);
            stmt.setString(5, paymentMethod);
            stmt.setBoolean(6, paid);
            if (stmt.executeUpdate() > 0 && !DatabaseSaver.silentSync) {
                Database.notifyDataChanged();
            }
        } catch (Exception e) {
            System.out.println("Invoice database save failed: " + e.getMessage());
        }
    }

    private void addColumnIfMissing(Statement stmt, String columnDefinition) {
        try {
            stmt.executeUpdate("ALTER TABLE invoices ADD COLUMN " + columnDefinition);
        } catch (SQLException ignored) {
            // Column already exists.
        }
    }
}
