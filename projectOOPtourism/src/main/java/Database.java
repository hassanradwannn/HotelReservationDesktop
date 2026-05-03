import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import database.DatabaseConnection;

public class Database {

    private static List<RoomType> roomTypes = new ArrayList<>();
    private static List<Amenity> amenities = new ArrayList<>();
    private static List<Room> rooms = new ArrayList<>();
    private static List<Guest> guests = new ArrayList<>();
    private static List<Staff> staffMembers = new ArrayList<>();
    private static List<Reservation> reservations = new ArrayList<>();
    private static List<Invoice> invoices = new ArrayList<>();

    public static List<RoomType> getRoomTypes() { return roomTypes; }
    public static List<Amenity> getAmenities() { return amenities; }
    public static List<Room> getRooms() { return rooms; }
    public static List<Guest> getGuests() { return guests; }
    public static List<Staff> getStaffMembers() { return staffMembers; }
    public static List<Reservation> getReservations() { return reservations; }
    public static List<Invoice> getInvoices() { return invoices; }

    // --- Table Check Helper ---
    private static boolean isTableEmpty(String tableName) {
        try (Connection conn = database.DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
            if (rs.next()) {
                return rs.getInt(1) == 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    // --- Rebuilt Startup Sync ---
    public static void syncDefaultDataToDatabase() {
        // 1. Seed Room Types only if empty
        if (isTableEmpty("room_types")) {
            String sql = "INSERT IGNORE INTO room_types (name, price_per_night, capacity) VALUES (?, ?, ?)";
            try (Connection conn = database.DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                Object[][] types = {
                        {"The Mendle Classic", 100.0, 1},
                        {"The Lobby Deluxe", 150.0, 2},
                        {"The Alpine Grand Suite", 300.0, 4},
                        {"The Gustave Penthouse", 500.0, 6}
                };
                for (Object[] type : types) {
                    stmt.setString(1, (String) type[0]);
                    stmt.setDouble(2, (Double) type[1]);
                    stmt.setInt(3, (Integer) type[2]);
                    stmt.executeUpdate();
                }
            } catch (SQLException e) { e.printStackTrace(); }
        }

        // 2. Seed Amenities only if empty
        if (isTableEmpty("amenities")) {
            String sql = "INSERT IGNORE INTO amenities (name, price) VALUES (?, ?)";
            try (Connection conn = database.DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                Object[][] amens = {
                        {"WiFi", 10.0},
                        {"Smart TV", 15.0},
                        {"Mini-bar", 50.0},
                        {"Jacuzzi", 100.0},
                        {"Gym Membership", 20.0},
                        {"Sea View", 75.0}
                };
                for (Object[] a : amens) {
                    stmt.setString(1, (String) a[0]);
                    stmt.setDouble(2, (Double) a[1]);
                    stmt.executeUpdate();
                }
            } catch (SQLException e) { e.printStackTrace(); }
        }

        // 3. ONLY GENERATE ROOMS IF TABLE IS EMPTY
        // This prevents the foreign key crash and stops duplicates
        if (isTableEmpty("rooms")) {
            System.out.println("Rooms table empty. Generating hotel floors...");

            String roomSql = "INSERT IGNORE INTO rooms (room_number, room_type_name) VALUES (?, ?)";
            String amenitySql = "INSERT IGNORE INTO room_amenities (room_number, amenity_name) VALUES (?, ?)";

            try (Connection conn = database.DatabaseConnection.getConnection();
                 PreparedStatement roomStmt = conn.prepareStatement(roomSql);
                 PreparedStatement amenityStmt = conn.prepareStatement(amenitySql)) {

                for (int floor = 1; floor <= 6; floor++) {
                    for (int r = 0; r <= 20; r++) {
                        String roomNumber = String.format("%d%02d", floor, r);
                        String type = switch (floor) {
                            case 1, 2 -> "The Mendle Classic";
                            case 3, 4 -> "The Lobby Deluxe";
                            case 5 -> "The Alpine Grand Suite";
                            default -> "The Gustave Penthouse";
                        };

                        roomStmt.setString(1, roomNumber);
                        roomStmt.setString(2, type);
                        roomStmt.executeUpdate();

                        // Assign tiered amenities
                        List<String> assignedAmenities = new ArrayList<>();
                        assignedAmenities.add("WiFi");
                        assignedAmenities.add("Smart TV");
                        if (floor >= 3) assignedAmenities.add("Mini-bar");
                        if (floor >= 5) assignedAmenities.add("Jacuzzi");
                        if (floor == 6) assignedAmenities.add("Gym Membership");

                        for (String amenityName : assignedAmenities) {
                            amenityStmt.setString(1, roomNumber);
                            amenityStmt.setString(2, amenityName);
                            amenityStmt.executeUpdate();
                        }
                    }
                }
            } catch (SQLException e) { e.printStackTrace(); }
        }
    }
    public static void deleteAmenityFromDB(Amenity amenity) {
        String deleteRoomAmenitiesSql = "DELETE FROM room_amenities WHERE amenity_name = ?";
        String deleteAmenitySql = "DELETE FROM amenities WHERE name = ?";
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement roomAmenitiesStmt = conn.prepareStatement(deleteRoomAmenitiesSql);
                 PreparedStatement amenityStmt = conn.prepareStatement(deleteAmenitySql)) {
                roomAmenitiesStmt.setString(1, amenity.getName());
                roomAmenitiesStmt.executeUpdate();

                amenityStmt.setString(1, amenity.getName());
                int deletedRows = amenityStmt.executeUpdate();

                conn.commit();
                if (deletedRows > 0) {
                    amenities.removeIf(a -> a.getName().equalsIgnoreCase(amenity.getName()));
                    for (Room room : rooms) {
                        room.removeAmenityByName(amenity.getName());
                    }
                    loadAllAmenities();
                    loadAllRooms();
                }
                notifyDataChanged();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            System.out.println("Failed to delete amenity from DB: " + e.getMessage());
        }
    }

    public static void deleteRoomFromDB(Room room) {
        String sql = "DELETE FROM rooms WHERE room_number = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, room.getRoomNumber());
            if (stmt.executeUpdate() > 0) {
                notifyDataChanged();
            }
        } catch (Exception e) {
            System.out.println("Failed to delete room from DB: " + e.getMessage());
        }
    }

    public static void deleteRoomTypeFromDB(RoomType roomType) {
        String sql = "DELETE FROM room_types WHERE name = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, roomType.getName());
            if (stmt.executeUpdate() > 0) {
                notifyDataChanged();
            }
        } catch (Exception e) {
            System.out.println("Failed to delete room type from DB: " + e.getMessage());
        }
    }

    public static void loadAll() {
        // Sync logic runs first, but handles its own empty-checks
        syncDefaultDataToDatabase();

        // Clear local lists to prevent duplicates on refresh
        roomTypes.clear();
        rooms.clear();
        amenities.clear();
        reservations.clear();

        // Essential sequence for Receptionist and Admin views
        refreshUsersFromDatabase();
        loadAllRoomTypes();
        loadAllAmenities();
        loadAllRooms();
        loadAllReservations();
    }

    public static void loadAllRoomTypes() {
        roomTypes.clear();
        try (Connection conn = database.DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM room_types")) {
            while (rs.next()) {
                RoomType rt = new RoomType(rs.getString("name"), rs.getDouble("price_per_night"), rs.getInt("capacity"));
                rt.setId(rs.getInt("id"));
                roomTypes.add(rt);
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static void loadAllAmenities() {
        amenities.clear();
        try (Connection conn = database.DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM amenities")) {
            while (rs.next()) {
                Amenity a = new Amenity(rs.getString("name"), rs.getDouble("price"));
                a.setId(rs.getInt("id"));
                amenities.add(a);
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static void loadAllRooms() {
        rooms.clear();
        try (Connection conn = database.DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM rooms")) {
            while (rs.next()) {
                String roomNumber = rs.getString("room_number");
                String typeName = rs.getString("room_type_name");
                RoomType type = roomTypes.stream().filter(rt -> rt.getName().equals(typeName)).findFirst().orElse(null);
                if (type != null) {
                    Room room = new Room(roomNumber, type);
                    room.setId(rs.getInt("id"));
                    room.getAmenities().clear();

                    try (PreparedStatement amStmt = conn.prepareStatement("SELECT amenity_name FROM room_amenities WHERE room_number = ?")) {
                        amStmt.setString(1, roomNumber);
                        try (ResultSet amRs = amStmt.executeQuery()) {
                            while (amRs.next()) {
                                String amName = amRs.getString("amenity_name");
                                amenities.stream()
                                        .filter(a -> a.getName().equals(amName))
                                        .findFirst()
                                        .ifPresent(room.getAmenities()::add);
                            }
                        }
                    }
                    rooms.add(room);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static void loadAllReservations() {
        refreshReservationsFromDatabase();
    }

    public static List<Reservation> getTodaysReservations() {
        loadAllReservations();
        return reservations.stream()
                .filter(r -> r.getCheckInDate().isEqual(SystemTime.getToday()))
                .toList();
    }

    public static List<Reservation> getCheckingOutReservations() {
        loadAllReservations();
        return reservations.stream()
                .filter(r -> r.getStatus() == ReservationStatus.CHECKING_OUT)
                .toList();
    }

    public static void addUser(User user) {
        if (user instanceof Guest) {
            guests.add((Guest) user);
        } else if (user instanceof Staff) {
            staffMembers.add((Staff) user);
        }
    }

    // BUG FIX 2 & 3: If the guests list is empty when this is called (e.g. from
    // the auto-refresh timeline on a second instance, or on first receptionist
    // login), we reload users first so reservations are never silently dropped.
    public static void refreshReservationsFromDatabase() {
        if (guests.isEmpty() && staffMembers.isEmpty()) {
            refreshUsersFromDatabase();
        }

        reservations.clear();
        String sql = "SELECT * FROM reservations";
        try (Connection conn = database.DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String resId = rs.getString("reservation_id");
                String guestUsername = rs.getString("guest_username");
                String roomNumber = rs.getString("room_number");
                java.sql.Date checkInSql = rs.getDate("check_in_date");
                java.sql.Date checkOutSql = rs.getDate("check_out_date");
                boolean hasGymPass = rs.getBoolean("has_gym_pass");
                String statusStr = rs.getString("status");

                if (checkInSql == null || checkOutSql == null) continue;

                java.time.LocalDate checkIn = checkInSql.toLocalDate();
                java.time.LocalDate checkOut = checkOutSql.toLocalDate();
                ReservationStatus status = ReservationStatus.valueOf(statusStr.toUpperCase().replace(' ', '_'));

                // BUG FIX 3: If the guest still isn't in memory (registered on
                // another instance after our last user-load), fetch them live
                // from the DB rather than silently dropping the reservation.
                Guest guest = guests.stream()
                        .filter(g -> g.getUsername().equals(guestUsername))
                        .findFirst()
                        .orElse(null);

                if (guest == null) {
                    // Guest was created on another instance — load them now.
                    User freshUser = UserDatabase.findUser(guestUsername);
                    if (freshUser instanceof Guest freshGuest) {
                        guests.add(freshGuest);
                        guest = freshGuest;
                    }
                }

                Room room = rooms.stream()
                        .filter(r -> r.getRoomNumber().equals(roomNumber))
                        .findFirst()
                        .orElse(null);

                if (guest != null && room != null) {
                    Reservation r = new Reservation(resId, guest, room, checkIn, checkOut, status, hasGymPass);
                    r.setTotalPrice();
                    reservations.add(r);
                }
            }
        } catch (Exception e) {
            System.out.println("Error refreshing reservations: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void refreshUsersFromDatabase() {
        guests.clear();
        staffMembers.clear();
        List<User> users = UserDatabase.loadUsersFromDatabase();
        for (User u : users) {
            if (u instanceof Guest) {
                guests.add((Guest) u);
            } else if (u instanceof Staff) {
                staffMembers.add((Staff) u);
            }
        }
    }

    // --- CRUD Methods ---
    public static void updateRoomType(RoomType rt) {
        String sql = "UPDATE room_types SET name = ?, price_per_night = ?, capacity = ? WHERE id = ?";
        try (Connection conn = database.DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, rt.getName());
            stmt.setDouble(2, rt.getPricePerNight());
            stmt.setInt(3, rt.getCapacity());
            stmt.setInt(4, rt.getId());
            if (stmt.executeUpdate() == 0) {
                try (PreparedStatement fallback = conn.prepareStatement("UPDATE room_types SET price_per_night = ?, capacity = ? WHERE name = ?")) {
                    fallback.setDouble(1, rt.getPricePerNight());
                    fallback.setInt(2, rt.getCapacity());
                    fallback.setString(3, rt.getName());
                    fallback.executeUpdate();
                }
            }
            notifyDataChanged();
        } catch (Exception e) {
            System.out.println("Error updating room type: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void updateRoom(Room r) {
        String sql = "UPDATE rooms SET room_number = ?, room_type_name = ? WHERE id = ?";
        try (Connection conn = database.DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, r.getRoomNumber());
            stmt.setString(2, r.getRoomType().getName());
            stmt.setInt(3, r.getId());
            if (stmt.executeUpdate() == 0) {
                try (PreparedStatement fallback = conn.prepareStatement("UPDATE rooms SET room_type_name = ? WHERE room_number = ?")) {
                    fallback.setString(1, r.getRoomType().getName());
                    fallback.setString(2, r.getRoomNumber());
                    fallback.executeUpdate();
                }
            }
            notifyDataChanged();
        } catch (Exception e) {
            System.out.println("Error updating room: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void insertAmenity(Amenity a) {
        String sql = "INSERT INTO amenities (name, price) VALUES (?, ?)";
        try (Connection conn = database.DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, a.getName());
            stmt.setDouble(2, a.getPrice());
            stmt.executeUpdate();
            notifyDataChanged();
        } catch (Exception e) {
            System.out.println("Error inserting amenity: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void updateAmenity(Amenity a) {
        String sql = "UPDATE amenities SET name = ?, price = ? WHERE id = ?";
        try (Connection conn = database.DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, a.getName());
            stmt.setDouble(2, a.getPrice());
            stmt.setInt(3, a.getId());
            if (stmt.executeUpdate() == 0) {
                try (PreparedStatement fallback = conn.prepareStatement("UPDATE amenities SET price = ? WHERE name = ?")) {
                    fallback.setDouble(1, a.getPrice());
                    fallback.setString(2, a.getName());
                    fallback.executeUpdate();
                }
            }
            notifyDataChanged();
        } catch (Exception e) {
            System.out.println("Error updating amenity: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static boolean isFirstInstance() {
        // Always reset to 1 on startup — stale counters from crashes/force-closes
        // would otherwise permanently block login. Multi-instance sync still works
        // via the last_update version mechanism, so this counter is only used for
        // the date-reset logic on first launch.
        String resetSql = "UPDATE system_settings SET setting_value = '1' WHERE setting_key = 'active_instances'";
        String checkSql = "SELECT setting_value FROM system_settings WHERE setting_key = 'active_instances'";
        String insertSql = "INSERT IGNORE INTO system_settings (setting_key, setting_value) VALUES ('active_instances', '1')";
        try (Connection conn = database.DatabaseConnection.getConnection()) {
            // Try to read current value first to detect true first-instance
            try (PreparedStatement stmt = conn.prepareStatement(checkSql);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int current = Integer.parseInt(rs.getString("setting_value"));
                    // Reset counter to 1 (this instance)
                    try (PreparedStatement update = conn.prepareStatement(resetSql)) {
                        update.executeUpdate();
                    }
                    // If counter was 0, this is genuinely the first instance
                    return current == 0;
                } else {
                    try (PreparedStatement ins = conn.prepareStatement(insertSql)) {
                        ins.executeUpdate();
                    }
                    return true;
                }
            }
        } catch (Exception e) {
            return true;
        }
    }

    public static void unregisterInstance() {
        String checkSql = "SELECT setting_value FROM system_settings WHERE setting_key = 'active_instances'";
        String updateSql = "UPDATE system_settings SET setting_value = ? WHERE setting_key = 'active_instances'";
        try (Connection conn = database.DatabaseConnection.getConnection();
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

    public static long getLatestDataVersion() {
        String sql = "SELECT setting_value FROM system_settings WHERE setting_key = 'last_update'";
        try (Connection conn = database.DatabaseConnection.getConnection();
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

    public static void notifyDataChanged() {
        String sql = "UPDATE system_settings SET setting_value = setting_value + 1 WHERE setting_key = 'last_update'";
        try (Connection conn = database.DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void updateRoomAmenities(int roomId, List<Amenity> amenities) {
        String roomNumber = null;
        for (Room r : rooms) {
            if (r.getId() == roomId) {
                roomNumber = r.getRoomNumber();
                break;
            }
        }

        String deleteSql = "DELETE FROM room_amenities WHERE room_number = ?";
        String insertSql = "INSERT IGNORE INTO room_amenities (room_number, amenity_name) VALUES (?, ?)";

        try (Connection conn = database.DatabaseConnection.getConnection()) {
            if (roomNumber == null) {
                try (PreparedStatement stmt = conn.prepareStatement("SELECT room_number FROM rooms WHERE id = ?")) {
                    stmt.setInt(1, roomId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) roomNumber = rs.getString("room_number");
                    }
                }
            }

            if (roomNumber != null) {
                try (PreparedStatement delStmt = conn.prepareStatement(deleteSql)) {
                    delStmt.setString(1, roomNumber);
                    delStmt.executeUpdate();
                }

                if (amenities != null && !amenities.isEmpty()) {
                    try (PreparedStatement insStmt = conn.prepareStatement(insertSql)) {
                        for (Amenity amenity : amenities) {
                            insStmt.setString(1, roomNumber);
                            insStmt.setString(2, amenity.getName());
                            insStmt.executeUpdate();
                        }
                    }
                }
                notifyDataChanged();
            }
        } catch (Exception e) {
            System.out.println("Error updating room amenities: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static List<String> getActiveGuests() {
        List<String> activeGuests = new ArrayList<>();
        // BUG FIX 1: Query for role = 'Guest' (mixed-case) to match how
        // UserDatabase saves the role via getClass().getSimpleName().
        // The original query used 'GUEST' (uppercase) which never matched.
        String sql = "SELECT username FROM users WHERE LOWER(role) = 'guest'";
        try (Connection conn = database.DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                activeGuests.add(rs.getString("username"));
            }
        } catch (SQLException e) {
            System.out.println("Error fetching active guests: " + e.getMessage());
            e.printStackTrace();
            return guests.stream().map(Guest::getUsername).toList();
        }
        return activeGuests;
    }

    public static void updateChatStatus(String username, boolean isChatting) {
        // Empty — no is_chatting column in the schema
    }
}
