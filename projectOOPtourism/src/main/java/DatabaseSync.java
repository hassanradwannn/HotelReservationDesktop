public class DatabaseSync {

    public static void syncDefaultDataToMySQL() {

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