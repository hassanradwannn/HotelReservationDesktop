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
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import Repositories.database.DatabaseConnection;

public class Database {

    private static final SystemSettingsRepository SYSTEM_SETTINGS = new SystemSettingsRepository();

    private static List<RoomType> roomTypes = new ArrayList<>();
    private static List<Amenity> amenities = new ArrayList<>();
    private static List<Room> rooms = new ArrayList<>();
    private static List<Guest> guests = new ArrayList<>();
    private static List<Staff> staffMembers = new ArrayList<>();
    private static List<Reservation> reservations = new ArrayList<>();
    private static List<Invoice> invoices = new ArrayList<>();
    private static long cachedDataVersion = -1;
    private static boolean roomsCacheLoaded;
    private static boolean reservationsCacheLoaded;

    public static List<RoomType> getRoomTypes() { return roomTypes; }
    public static List<Amenity> getAmenities() { return amenities; }
    public static List<Room> getRooms() { return rooms; }
    public static List<Guest> getGuests() { return guests; }
    public static List<Staff> getStaffMembers() { return staffMembers; }
    public static List<Reservation> getReservations() { return reservations; }
    public static List<Invoice> getInvoices() { return invoices; }

    private static boolean isTableEmpty(String tableName) {
        try (Connection conn = DatabaseConnection.getConnection();
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

    // Seeds reference data only for fresh databases; existing data is left untouched.
    public static void syncDefaultDataToDatabase() {
        if (isTableEmpty("room_types")) {
            String sql = "INSERT IGNORE INTO room_types (name, price_per_night, capacity) VALUES (?, ?, ?)";
            try (Connection conn = DatabaseConnection.getConnection();
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

        ensureDefaultAmenities();
        normalizeGymAmenityData();
        if (isTableEmpty("rooms")) {
            System.out.println("Rooms table empty. Generating hotel floors...");

            String roomSql = "INSERT IGNORE INTO rooms (room_number, room_type_name) VALUES (?, ?)";
            String amenitySql = "INSERT IGNORE INTO room_amenities (room_number, amenity_name) VALUES (?, ?)";

            try (Connection conn = DatabaseConnection.getConnection();
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

                        // Higher floors get richer default amenities.
                        List<String> assignedAmenities = new ArrayList<>();
                        assignedAmenities.add("WiFi");
                        assignedAmenities.add("Smart TV");
                        if (floor >= 3) assignedAmenities.add("Mini-bar");
                        if (floor >= 5) assignedAmenities.add("Jacuzzi");
                        if (floor == 6) assignedAmenities.add("Gym");

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

    private static void normalizeGymAmenityData() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            try {
                try (PreparedStatement stmt = conn.prepareStatement(
                        "INSERT IGNORE INTO amenities (name, price) VALUES ('Gym', 20.0)")) {
                    stmt.executeUpdate();
                }

                List<String> gymRoomNumbers = new ArrayList<>();
                try (PreparedStatement stmt = conn.prepareStatement(
                        "SELECT DISTINCT room_number FROM room_amenities WHERE LOWER(amenity_name) LIKE '%gym%'");
                     ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        gymRoomNumbers.add(rs.getString("room_number"));
                    }
                }

                try (PreparedStatement stmt = conn.prepareStatement(
                        "INSERT IGNORE INTO room_amenities (room_number, amenity_name) VALUES (?, 'Gym')")) {
                    for (String roomNumber : gymRoomNumbers) {
                        stmt.setString(1, roomNumber);
                        stmt.executeUpdate();
                    }
                }

                try (PreparedStatement stmt = conn.prepareStatement(
                        "DELETE FROM room_amenities WHERE LOWER(amenity_name) LIKE '%gym%' AND LOWER(amenity_name) <> 'gym'")) {
                    stmt.executeUpdate();
                }

                try (PreparedStatement stmt = conn.prepareStatement(
                        "DELETE FROM amenities WHERE LOWER(name) LIKE '%gym%' AND LOWER(name) <> 'gym'")) {
                    stmt.executeUpdate();
                }

                normalizeGuestGymPreferences(conn);

                conn.commit();
                conn.setAutoCommit(originalAutoCommit);
            } catch (Exception ex) {
                conn.rollback();
                conn.setAutoCommit(originalAutoCommit);
                throw ex;
            }
        } catch (Exception e) {
            System.out.println("Failed to normalize Gym amenity data: " + e.getMessage());
        }
    }

    private static void normalizeGuestGymPreferences(Connection conn) throws SQLException {
        List<String[]> updates = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT username, room_preferences FROM users WHERE room_preferences IS NOT NULL AND TRIM(room_preferences) <> ''");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String username = rs.getString("username");
                String preferences = rs.getString("room_preferences");
                String normalized = normalizeRoomPreferences(preferences);
                if (!normalized.equals(preferences)) {
                    updates.add(new String[]{username, normalized});
                }
            }
        } catch (SQLException ex) {
            return;
        }

        try (PreparedStatement stmt = conn.prepareStatement(
                "UPDATE users SET room_preferences = ? WHERE username = ?")) {
            for (String[] update : updates) {
                stmt.setString(1, update[1]);
                stmt.setString(2, update[0]);
                stmt.executeUpdate();
            }
        }
    }

    private static String normalizeRoomPreferences(String preferences) {
        if (preferences == null || preferences.trim().isEmpty()) {
            return "";
        }

        Set<String> seenKeys = new LinkedHashSet<>();
        List<String> normalizedPreferences = new ArrayList<>();
        for (String preference : preferences.split(",")) {
            String trimmed = preference.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            String key = CatalogService.amenityFilterKey(trimmed);
            if (seenKeys.add(key)) {
                normalizedPreferences.add(CatalogService.displayAmenityName(trimmed));
            }
        }
        return String.join(", ", normalizedPreferences);
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

    private static void ensureDefaultAmenities() {
        String sql = "INSERT IGNORE INTO amenities (name, price) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            Object[][] amens = {
                    {"WiFi", 10.0},
                    {"Smart TV", 15.0},
                    {"Mini-bar", 50.0},
                    {"Jacuzzi", 100.0},
                    {"Gym", 20.0},
                    {"Sea View", 75.0},
                    {"Mountain View", 75.0}
            };
            for (Object[] a : amens) {
                stmt.setString(1, (String) a[0]);
                stmt.setDouble(2, (Double) a[1]);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
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
        // Rebuild the in-memory cache in dependency order after the startup seed check.
        syncDefaultDataToDatabase();

        roomTypes.clear();
        rooms.clear();
        amenities.clear();
        reservations.clear();
        roomsCacheLoaded = false;
        reservationsCacheLoaded = false;

        refreshUsersFromDatabase();
        loadAllRoomTypes();
        loadAllAmenities();
        loadAllRooms();
        loadAllReservations();
        cachedDataVersion = getLatestDataVersion();
    }

    public static void loadAllRoomTypes() {
        roomTypes.clear();
        try (Connection conn = DatabaseConnection.getConnection();
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
        try (Connection conn = DatabaseConnection.getConnection();
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
        Map<String, List<String>> amenityNamesByRoom = new HashMap<>();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            try (ResultSet amenityRs = stmt.executeQuery("SELECT room_number, amenity_name FROM room_amenities")) {
                while (amenityRs.next()) {
                    amenityNamesByRoom
                            .computeIfAbsent(amenityRs.getString("room_number"), key -> new ArrayList<>())
                            .add(amenityRs.getString("amenity_name"));
                }
            }

            try (ResultSet rs = stmt.executeQuery("SELECT * FROM rooms")) {
            while (rs.next()) {
                String roomNumber = rs.getString("room_number");
                String typeName = rs.getString("room_type_name");
                RoomType type = roomTypes.stream().filter(rt -> rt.getName().equals(typeName)).findFirst().orElse(null);
                if (type != null) {
                    Room room = new Room(roomNumber, type);
                    room.setId(rs.getInt("id"));
                    room.getAmenities().clear();

                    for (String amName : amenityNamesByRoom.getOrDefault(roomNumber, List.of())) {
                        Amenity amenity = amenities.stream()
                                .filter(a -> a.getName().equals(amName))
                                .findFirst()
                                .orElse(null);
                        if (amenity == null && CatalogService.isGymAmenityName(amName)) {
                            amenity = amenities.stream()
                                    .filter(a -> CatalogService.isGymAmenityName(a.getName()))
                                    .findFirst()
                                    .orElse(null);
                        }
                        if (amenity != null) {
                            room.getAmenities().add(amenity);
                        }
                    }
                    rooms.add(room);
                }
            }
            }
            roomsCacheLoaded = true;
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static void loadAllReservations() {
        refreshReservationsFromDatabase();
    }

    public static List<Reservation> getTodaysReservations() {
        refreshReservationsIfStale();
        return reservations.stream()
                .filter(r -> r.getStatus() == ReservationStatus.CHECKING_IN
                        && r.getCheckInDate().isEqual(SystemTime.getToday()))
                .toList();
    }

    public static List<Reservation> getCheckingOutReservations() {
        refreshReservationsIfStale();
        return reservations.stream()
                .filter(r -> r.getStatus() == ReservationStatus.CHECKING_OUT)
                .toList();
    }

    public static void refreshRuntimeCacheIfStale() {
        long latestDataVersion = getLatestDataVersion();
        if (cachedDataVersion == latestDataVersion && roomsCacheLoaded && reservationsCacheLoaded) {
            return;
        }

        refreshUsersFromDatabase();
        loadAllRoomTypes();
        loadAllAmenities();
        loadAllRooms();
        refreshReservationsFromDatabase();
        cachedDataVersion = latestDataVersion;
    }

    public static void refreshReservationsIfStale() {
        long latestDataVersion = getLatestDataVersion();
        if (cachedDataVersion == latestDataVersion && reservationsCacheLoaded) {
            return;
        }
        if (rooms.isEmpty() || !roomsCacheLoaded) {
            loadAllRoomTypes();
            loadAllAmenities();
            loadAllRooms();
        }
        refreshReservationsFromDatabase();
        cachedDataVersion = latestDataVersion;
    }

    public static void addUser(User user) {
        if (user instanceof Guest) {
            guests.add((Guest) user);
        } else if (user instanceof Staff) {
            staffMembers.add((Staff) user);
        }
    }

    // Reservations need Guest objects, so rebuild users first when this cache is cold.
    public static void refreshReservationsFromDatabase() {
        if (guests.isEmpty() && staffMembers.isEmpty()) {
            refreshUsersFromDatabase();
        }

        reservations.clear();
        String sql = "SELECT * FROM reservations";
        try (Connection conn = DatabaseConnection.getConnection();
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

                Guest guest = guests.stream()
                        .filter(g -> g.getUsername().equals(guestUsername))
                        .findFirst()
                        .orElse(null);

                // Another app instance may have registered this guest after our last refresh.
                if (guest == null) {
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
                    if (r.getStatus() == ReservationStatus.CONFIRMED && checkIn.isEqual(SystemTime.getToday())) {
                        r.setStatus(ReservationStatus.CHECKING_IN);
                        DatabaseSaver.updateReservationStatus(r.getReservationId(), ReservationStatus.CHECKING_IN.toString());
                    } else if (r.getStatus() == ReservationStatus.CHECKING_IN && !checkIn.isEqual(SystemTime.getToday())) {
                        r.setStatus(ReservationStatus.CONFIRMED);
                        DatabaseSaver.updateReservationStatus(r.getReservationId(), ReservationStatus.CONFIRMED.toString());
                    } else if (r.getStatus() == ReservationStatus.ONGOING && checkOut.isEqual(SystemTime.getToday())) {
                        r.setStatus(ReservationStatus.CHECKING_OUT);
                        DatabaseSaver.updateReservationStatus(r.getReservationId(), ReservationStatus.CHECKING_OUT.toString());
                    }
                    reservations.add(r);
                }
            }
            reservationsCacheLoaded = true;
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

    public static void updateRoomType(RoomType rt) {
        String sql = "UPDATE room_types SET name = ?, price_per_night = ?, capacity = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
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
        try (Connection conn = DatabaseConnection.getConnection();
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
        a.setName(CatalogService.displayAmenityName(a.getName()));
        CatalogService.validateAmenityNameAvailable(a.getName(), null);
        if (amenityNameExistsInDatabase(a.getName(), null)) {
            throw new IllegalArgumentException("Amenity with name '" + a.getName() + "' already exists.");
        }

        String sql = "INSERT INTO amenities (name, price) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, a.getName());
            stmt.setDouble(2, a.getPrice());
            stmt.executeUpdate();
            notifyDataChanged();
        } catch (Exception e) {
            System.out.println("Error inserting amenity: " + e.getMessage());
            throw new IllegalArgumentException("Could not add amenity: " + e.getMessage(), e);
        }
    }

    public static void updateAmenity(Amenity a) {
        a.setName(CatalogService.displayAmenityName(a.getName()));
        CatalogService.validateAmenityNameAvailable(a.getName(), a);
        if (amenityNameExistsInDatabase(a.getName(), a.getId())) {
            throw new IllegalArgumentException("Amenity with name '" + a.getName() + "' already exists.");
        }

        String sql = "UPDATE amenities SET name = ?, price = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
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
            throw new IllegalArgumentException("Could not update amenity: " + e.getMessage(), e);
        }
    }

    private static boolean amenityNameExistsInDatabase(String name, Integer excludedId) {
        String key = CatalogService.amenityFilterKey(name);
        String sql = "SELECT id, name FROM amenities";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id");
                if (excludedId != null && id == excludedId) {
                    continue;
                }
                if (CatalogService.amenityFilterKey(rs.getString("name")).equals(key)) {
                    return true;
                }
            }
        } catch (SQLException e) {
            throw new IllegalArgumentException("Could not validate amenity name: " + e.getMessage(), e);
        }
        return false;
    }

    public static boolean isFirstInstance() {
        // The first app window resets simulated dates; last_update handles data sync.
        return SYSTEM_SETTINGS.isFirstInstance();
    }

    public static void unregisterInstance() {
        SYSTEM_SETTINGS.unregisterInstance();
    }

    public static long getLatestDataVersion() {
        return SYSTEM_SETTINGS.getLatestDataVersion();
    }

    public static void notifyDataChanged() {
        SYSTEM_SETTINGS.notifyDataChanged();
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

        try (Connection conn = DatabaseConnection.getConnection()) {
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
                        Set<String> insertedAmenityKeys = new LinkedHashSet<>();
                        for (Amenity amenity : amenities) {
                            String amenityName = CatalogService.displayAmenityName(amenity.getName());
                            if (!insertedAmenityKeys.add(CatalogService.amenityFilterKey(amenityName))) {
                                continue;
                            }
                            insStmt.setString(1, roomNumber);
                            insStmt.setString(2, amenityName);
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
        // Roles are stored as Java class names, so compare case-insensitively.
        String sql = "SELECT username FROM users WHERE LOWER(role) = 'guest'";
        try (Connection conn = DatabaseConnection.getConnection();
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
    }
}
