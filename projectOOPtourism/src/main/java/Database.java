import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import database.DatabaseConnection;
import exceptions.InvalidCredentialsException;



public class Database {
    // Static lists acting as our in-memory tables
    private static final ArrayList<Guest> guests = new ArrayList<>();
    private static final ArrayList<Staff> staffMembers = new ArrayList<>();
    private static final ArrayList<Room> rooms = new ArrayList<>();
    private static final ArrayList<RoomType> roomTypes = new ArrayList<>();
    private static final ArrayList<Amenity> amenities = new ArrayList<>();
    private static final ArrayList<Reservation> reservations = new ArrayList<>();
    private static final ArrayList<Invoice> invoices = new ArrayList<>();

    public static ArrayList<Guest> getGuests() { return guests; }
    public static ArrayList<Staff> getStaffMembers() { return staffMembers; }
    public static ArrayList<Room> getRooms() { return rooms; }
    public static ArrayList<RoomType> getRoomTypes() { return roomTypes; }
    public static ArrayList<Amenity> getAmenities() { return amenities; }
    public static ArrayList<Reservation> getReservations() { return reservations; }
    public static ArrayList<Invoice> getInvoices() { return invoices; }

    static {
        // Load users from SQL database
        loadUsersFromDatabase();

        // If database is empty, add demo users for testing
        if (guests.isEmpty() && staffMembers.isEmpty()) {
            initializeDemoUsers();
        }

        loadAmenitiesFromDatabase();

        if (amenities.isEmpty()) {
            System.out.println("No amenities found in database. Initializing default amenities...");
            insertAmenity(new Amenity("WiFi", 10));
            insertAmenity(new Amenity("Smart TV", 35));
            insertAmenity(new Amenity("Mini-bar", 75));
            insertAmenity(new Amenity("Jacuzzi", 100));
            insertAmenity(new Amenity("Gym", 200));
        }

        RoomType standard = new RoomType("The Mendle Classic", 500, 1);
        RoomType deluxe = new RoomType("The Lobby Deluxe", 700, 2);
        RoomType suite = new RoomType("The Alpine Grand Suite", 1000, 4);
        RoomType penthouse = new RoomType("The Gustave Penthouse", 2000, 8);
        getRoomTypes().add(standard);
        getRoomTypes().add(deluxe);
        getRoomTypes().add(suite);
        getRoomTypes().add(penthouse);

        // Try to load rooms from database first
        loadRoomsFromDatabase();
        
        // If no rooms in database, generate and insert them
        if (rooms.isEmpty()) {
            System.out.println("No rooms found in database. Generating default rooms...");
            generateAndInsertRoomRange(100, 120, standard);
            generateAndInsertRoomRange(200, 220, standard);
            generateAndInsertRoomRange(300, 320, deluxe);
            generateAndInsertRoomRange(400, 420, deluxe);
            generateAndInsertRoomRange(500, 520, suite);
            generateAndInsertRoomRange(600, 620, penthouse);
        }

        // Load reservations from database
        loadReservationsFromDatabase();
    }

    private static void loadAmenitiesFromDatabase() {
        String sql = "SELECT * FROM amenities";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Amenity a = new Amenity(rs.getString("name"), rs.getDouble("price"));
                a.setId(rs.getInt("id"));
                amenities.add(a);
            }
        } catch (SQLException e) {
            System.out.println("Error loading amenities from database: " + e.getMessage());
        }
    }

    private static void loadRoomsFromDatabase() {
        String sql = "SELECT r.*, rt.name as room_type_name, rt.price_per_night, rt.capacity " +
                     "FROM rooms r JOIN room_types rt ON r.room_type_name = rt.name";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                RoomType roomType = new RoomType(
                    rs.getString("room_type_name"),
                    rs.getDouble("price_per_night"),
                    rs.getInt("capacity")
                );
                roomType.setId(rs.getInt("id"));
                
                Room room = new Room(rs.getString("room_number"), roomType);
                room.setId(rs.getInt("id"));
                
                // Load amenities for this room
                loadAmenitiesForRoom(room);
                
                rooms.add(room);
            }
        } catch (SQLException e) {
            System.out.println("Error loading rooms from database: " + e.getMessage());
        }
    }

    private static void loadAmenitiesForRoom(Room room) {
        String sql = "SELECT a.* FROM amenities a " +
                     "JOIN room_amenities ra ON a.id = ra.amenity_id " +
                     "WHERE ra.room_number = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, room.getRoomNumber());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Amenity amenity = new Amenity(rs.getString("name"), rs.getDouble("price"));
                    amenity.setId(rs.getInt("id"));
                    room.addAmenity(amenity);
                }
            }
        } catch (SQLException e) {
            System.out.println("Error loading amenities for room " + room.getRoomNumber() + ": " + e.getMessage());
        }
    }

    private static void loadUsersFromDatabase() {
        ArrayList<User> loadedUsers = UserDatabase.loadUsersFromDatabase();
        System.out.println("Loaded " + loadedUsers.size() + " users from database.");
        for (User user : loadedUsers) {
            addUser(user);
        }
    }

    private static void initializeDemoUsers() {
        System.out.println("No users found in database. Initializing demo users...");
        Admin admin = new Admin("Admin", "Admin@123", LocalDate.of(1964, 4, 19), 6);
        Receptionist receptionist = new Receptionist("Manar", "Manar2002", LocalDate.of(2002, 6, 13), 8);
        Guest defaultGuest = new Guest("Hassan", "Hassan123", LocalDate.of(2007, 4, 10), 10000, "2 haram", Gender.MALE, "");

        try {
            Authentication.register(admin);
            Authentication.register(receptionist);
            Authentication.register(defaultGuest);
        } catch (InvalidCredentialsException e) {
            System.out.println("Error: Could not initialize demo users.");
        }
    }

    private static void generateRoomRange(int start, int end, RoomType type) {
        Random rnd = new Random();
        for (int i = start; i <= end; i++) {
            Room room = new Room(String.valueOf(i), type);
            String typeName = type.getName();

            // Determine how many amenities
            int targetCount = 2;
            if (typeName.equalsIgnoreCase("The Lobby Deluxe")) targetCount = 3;
            else if (typeName.equalsIgnoreCase("The Alpine Grand Suite")) targetCount = 3;
            else if (typeName.equalsIgnoreCase("The Gustave Penthouse")) targetCount = 4;

            // Penthouse comes with gym pass
            if (typeName.equalsIgnoreCase("The Gustave Penthouse")) {
                room.addAmenity(getAmenities().get(4));
            }

            int extrasNeeded = Math.max(0, targetCount - room.getAmenities().size());

            ArrayList<Integer> candidates = new ArrayList<>();
            if (!typeName.equalsIgnoreCase("The Mendle Classic")) {
                candidates.add(2); // Mini-bar
            }

            // Jacuzzi is allowed only for Suite and Penthouse
            if (typeName.equalsIgnoreCase("The Alpine Grand Suite") || typeName.equalsIgnoreCase("The Gustave Penthouse")) {
                candidates.add(3); // Jacuzzi
            }

            // Shuffle candidates and pick the needed number without repeats
            Collections.shuffle(candidates, rnd);
            for (int j = 0; j < extrasNeeded && j < candidates.size(); j++) {
                int amenityIndex = candidates.get(j);
                room.addAmenity(getAmenities().get(amenityIndex));
            }

            getRooms().add(room);
        }
    }

    private static void generateAndInsertRoomRange(int start, int end, RoomType type) {
        Random rnd = new Random();
        for (int i = start; i <= end; i++) {
            Room room = new Room(String.valueOf(i), type);
            String typeName = type.getName();

            // Determine how many amenities
            int targetCount = 2;
            if (typeName.equalsIgnoreCase("The Lobby Deluxe")) targetCount = 3;
            else if (typeName.equalsIgnoreCase("The Alpine Grand Suite")) targetCount = 3;
            else if (typeName.equalsIgnoreCase("The Gustave Penthouse")) targetCount = 4;

            // Penthouse comes with gym pass
            if (typeName.equalsIgnoreCase("The Gustave Penthouse")) {
                room.addAmenity(getAmenities().get(4));
            }

            int extrasNeeded = Math.max(0, targetCount - room.getAmenities().size());

            ArrayList<Integer> candidates = new ArrayList<>();
            if (!typeName.equalsIgnoreCase("The Mendle Classic")) {
                candidates.add(2); // Mini-bar
            }

            // Jacuzzi is allowed only for Suite and Penthouse
            if (typeName.equalsIgnoreCase("The Alpine Grand Suite") || typeName.equalsIgnoreCase("The Gustave Penthouse")) {
                candidates.add(3); // Jacuzzi
            }

            // Shuffle candidates and pick the needed number without repeats
            Collections.shuffle(candidates, rnd);
            for (int j = 0; j < extrasNeeded && j < candidates.size(); j++) {
                int amenityIndex = candidates.get(j);
                room.addAmenity(getAmenities().get(amenityIndex));
            }

            getRooms().add(room);
            insertRoom(room);
            
            // Save room amenities to database
            if (!room.getAmenities().isEmpty()) {
                updateRoomAmenities(room.getId(), room.getAmenities());
            }
        }
    }

    public static void addUser(User user) {
        if (user instanceof Guest) {
            getGuests().add((Guest)user);
        } else {
            getStaffMembers().add((Staff)user);
        }
    }

    public static User findUser(String username) {
        for (Staff staff : getStaffMembers()) {
            if (staff.getUsername().equalsIgnoreCase(username)) return staff;
        }
        for (Guest guest : getGuests()) {
            if (guest.getUsername().equalsIgnoreCase(username)) return guest;
        }
        return null;
    }

    public static void notifyDataChanged() {
        String sql = "UPDATE system_settings SET setting_value = setting_value + 1 WHERE setting_key = 'last_update'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (Exception e) {}
    }

    public static long getLatestDataVersion() {
        String sql = "SELECT setting_value FROM system_settings WHERE setting_key = 'last_update'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getLong("setting_value");
            }
        } catch (Exception e) {}
        return 0;
    }

    public static boolean isFirstInstance() {
        String updateSql = "UPDATE system_settings SET setting_value = setting_value + 1 WHERE setting_key = 'active_instances'";
        String selectSql = "SELECT setting_value FROM system_settings WHERE setting_key = 'active_instances'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement updateStmt = conn.prepareStatement(updateSql);
             PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
            updateStmt.executeUpdate();
            ResultSet rs = selectStmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("setting_value") <= 1;
            }
        } catch (Exception e) {}
        return true; // Default to true if something fails
    }

    public static void unregisterInstance() {
        String selectSql = "SELECT setting_value FROM system_settings WHERE setting_key = 'active_instances'";
        String updateSql = "UPDATE system_settings SET setting_value = ? WHERE setting_key = 'active_instances'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement selectStmt = conn.prepareStatement(selectSql);
             PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
            ResultSet rs = selectStmt.executeQuery();
            if (rs.next()) {
                int current = rs.getInt("setting_value");
                int next = Math.max(0, current - 1);
                updateStmt.setString(1, String.valueOf(next));
                updateStmt.executeUpdate();
                
                // Automatically reset time when the last instance closes!
                if (next == 0) {
                    SystemTime.resetToRealToday();
                }
            }
        } catch (Exception e) {}
    }

    public static void refreshUsersFromDatabase() {
        // Clear current in-memory users
        getGuests().clear();
        getStaffMembers().clear();

        // Reload from database
        ArrayList<User> loadedUsers = UserDatabase.loadUsersFromDatabase();
        System.out.println("Refreshed " + loadedUsers.size() + " users from database.");
        for (User user : loadedUsers) {
            addUser(user);
        }
    }

    public static void refreshReservationsFromDatabase() {
        SystemTime.syncFromDatabase();
        refreshUsersFromDatabase(); // Ensure newly registered guests are loaded
        getReservations().clear();
        loadReservationsFromDatabase();
    }

    private static void loadReservationsFromDatabase() {
        String sql = "SELECT * FROM reservations";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String resId = rs.getString("reservation_id");
                String guestUsername = rs.getString("guest_username");
                String roomNumber = rs.getString("room_number");
                LocalDate checkIn = rs.getDate("check_in_date").toLocalDate();
                LocalDate checkOut = rs.getDate("check_out_date").toLocalDate();
                String statusStr = rs.getString("status");
                ReservationStatus status = ReservationStatus.valueOf(statusStr.toUpperCase());

                User user = findUser(guestUsername);
                Room room = null;
                for (Room r : rooms) {
                    if (r.getRoomNumber().equalsIgnoreCase(roomNumber)) {
                        room = r;
                        break;
                    }
                }

                if (user instanceof Guest guest && room != null && resId != null) {
                    Reservation res = new Reservation(resId, guest, room, checkIn, checkOut, status, false);
                    res.setTotalPrice();

                    if (status == ReservationStatus.CONFIRMED || status == ReservationStatus.ONGOING || status == ReservationStatus.COMPLETED) {
                        res.setDepositPaid(true);
                    }
                    if (status == ReservationStatus.COMPLETED) {
                        res.setFullPaid(true);
                    }

                    reservations.add(res);
                }
            }
            System.out.println("Loaded " + reservations.size() + " reservations from database.");
        } catch (SQLException e) {
            System.out.println("Error loading reservations from database: " + e.getMessage());
        }
    }

    public static void updateAmenity(Amenity a) {
        String sql = "UPDATE amenities SET name = ?, price = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, a.getName());
            stmt.setDouble(2, a.getPrice());
            stmt.setInt(3, a.getId());
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                for (int i = 0; i < getAmenities().size(); i++) {
                    if (getAmenities().get(i).getId() == a.getId() || getAmenities().get(i).getName().equals(a.getName())) {
                        getAmenities().set(i, a);
                        break;
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Error updating amenity: " + e.getMessage());
        }
    }

    public static void insertAmenity(Amenity a) {
        String sql = "INSERT INTO amenities (name, price) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, a.getName());
            stmt.setDouble(2, a.getPrice());
            stmt.executeUpdate();
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    a.setId(rs.getInt(1));
                }
            }
            
            // Check if amenity with same ID already exists in the list
            boolean exists = false;
            for (Amenity existing : getAmenities()) {
                if (existing.getId() == a.getId()) {
                    exists = true;
                    break;
                }
            }
            if (!exists) getAmenities().add(a);
        } catch (SQLException e) {
            System.out.println("Error inserting amenity: " + e.getMessage());
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
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                for (int i = 0; i < getRoomTypes().size(); i++) {
                    if (getRoomTypes().get(i).getId() == rt.getId() || getRoomTypes().get(i).getName().equals(rt.getName())) {
                        getRoomTypes().set(i, rt);
                        break;
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Error updating room type: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void insertRoomType(RoomType rt) {
        String sql = "INSERT INTO room_types (name, price_per_night, capacity) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, rt.getName());
            stmt.setDouble(2, rt.getPricePerNight());
            stmt.setInt(3, rt.getCapacity());
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error inserting room type: " + e.getMessage());
        }
    }

    public static void updateRoom(Room r) {
        String sql = "UPDATE rooms SET room_number = ?, room_type_name = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, r.getRoomNumber());
            stmt.setString(2, r.getRoomType().getName());
            stmt.setInt(3, r.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error updating room: " + e.getMessage());
        }
    }

    public static void insertRoom(Room r) {
        String sql = "INSERT INTO rooms (room_number, room_type_name) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, r.getRoomNumber());
            stmt.setString(2, r.getRoomType().getName());
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error inserting room: " + e.getMessage());
        }
    }

    public static void updateRoomAmenities(int roomId, List<Amenity> updatedAmenities) {
        // First, get the room number for this room ID
        String roomNumber = null;
        for (Room room : getRooms()) {
            if (room.getId() == roomId) {
                roomNumber = room.getRoomNumber();
                break;
            }
        }
        if (roomNumber == null) {
            System.out.println("Error: Room with ID " + roomId + " not found.");
            return;
        }
        
        String deleteSql = "DELETE FROM room_amenities WHERE room_number = ?";
        String insertSql = "INSERT INTO room_amenities (room_number, amenity_name) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement deleteStmt = conn.prepareStatement(deleteSql);
             PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
            deleteStmt.setString(1, roomNumber);
            deleteStmt.executeUpdate();
            for (Amenity amenity : updatedAmenities) {
                insertStmt.setString(1, roomNumber);
                insertStmt.setString(2, amenity.getName()); 
                insertStmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.out.println("Error syncing room amenities: " + e.getMessage());
        }
    }
}
