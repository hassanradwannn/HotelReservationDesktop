package Services;


import Controllers.*;
import Models.*;
import Repositories.*;
import Utils.*;
import Utils.exceptions.*;
import Repositories.database.DatabaseConnection;
import Repositories.DatabaseInitializer.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import Utils.exceptions.*;

public abstract class CatalogService {

    public static List<RoomType> listRoomTypes() { return Database.getRoomTypes(); }
    public static List<Amenity> listAmenities() { return Database.getAmenities(); }

    public static List<Amenity> listFilterAmenities() {
        Map<String, Amenity> uniqueAmenities = new LinkedHashMap<>();
        for (Amenity amenity : Database.getAmenities()) {
            if (amenity == null || amenity.getName() == null) {
                continue;
            }

            String key = amenityFilterKey(amenity.getName());
            Amenity current = uniqueAmenities.get(key);
            if (current == null || shouldPreferAmenity(amenity.getName(), current.getName())) {
                uniqueAmenities.put(key, amenity);
            }
        }
        return new ArrayList<>(uniqueAmenities.values());
    }

    public static Amenity findAmenityForFilter(String name) {
        String key = amenityFilterKey(name);
        return listFilterAmenities().stream()
                .filter(amenity -> amenityFilterKey(amenity.getName()).equals(key))
                .findFirst()
                .orElse(findAmenity(name));
    }

    public static List<String> uniqueFilterAmenityNames(List<String> names) {
        Map<String, String> uniqueNames = new LinkedHashMap<>();
        for (String name : names) {
            if (name == null || name.trim().isEmpty()) {
                continue;
            }

            String key = amenityFilterKey(name);
            String displayName = displayAmenityName(name);
            String current = uniqueNames.get(key);
            if (current == null || shouldPreferAmenity(name, current)) {
                uniqueNames.put(key, displayName);
            }
        }
        return new ArrayList<>(uniqueNames.values());
    }

    public static boolean isSameAmenityFilter(String left, String right) {
        return amenityFilterKey(left).equals(amenityFilterKey(right));
    }

    public static String amenityFilterKey(String name) {
        String normalized = normalizeAmenityName(name);
        return isGymAmenityName(normalized) ? "gym" : normalized;
    }

    public static boolean isGymAmenityName(String name) {
        return normalizeAmenityName(name).contains("gym");
    }

    public static boolean roomTypeExists(String name) {
        return Database.getRoomTypes().stream().anyMatch(rt -> rt.getName().equalsIgnoreCase(name));
    }

    public static boolean amenityExists(String name) {
        return isAmenityNameTaken(name, null);
    }

    public static void validateAmenityNameAvailable(String name, Amenity currentAmenity) {
        String displayName = displayAmenityName(name);
        if (displayName.isEmpty()) {
            throw new IllegalArgumentException("Amenity name cannot be empty.");
        }
        if (isAmenityNameTaken(displayName, currentAmenity)) {
            throw new IllegalArgumentException("Amenity with name '" + displayName + "' already exists.");
        }
    }

    public static boolean isAmenityNameTaken(String name, Amenity currentAmenity) {
        String key = amenityFilterKey(name);
        return Database.getAmenities().stream()
                .filter(amenity -> amenity != null)
                .filter(amenity -> currentAmenity == null
                        || (amenity != currentAmenity
                        && (currentAmenity.getId() == 0 || amenity.getId() != currentAmenity.getId())))
                .anyMatch(amenity -> amenityFilterKey(amenity.getName()).equals(key));
    }

    public static boolean roomExists(String roomNumber) {
        return Database.getRooms().stream().anyMatch(r -> r.getRoomNumber().equalsIgnoreCase(roomNumber));
    }

    public static void createRoomType(String name, double pricePerNight, int capacity) throws AlreadyExistsException {
        if (roomTypeExists(name)) throw new AlreadyExistsException("Room Type", name);
        Database.getRoomTypes().add(new RoomType(name, pricePerNight, capacity));
    }

    public static void createAmenity(String name, double price) {
        name = displayAmenityName(name);
        validateAmenityNameAvailable(name, null);
        Database.getAmenities().add(new Amenity(name, price));
    }

    public static void createRoom(String roomNumber, RoomType type) {
        if (roomExists(roomNumber)) throw new IllegalArgumentException("Room with number '" + roomNumber + "' already exists.");
        if (!Database.getRoomTypes().contains(type)) throw new IllegalArgumentException("RoomType does not exist.");
        Database.getRooms().add(new Room(roomNumber, type));
    }

    // Room defaults mirror the seeded floor tiers in Database.syncDefaultDataToDatabase().
    public static List<Amenity> getDefaultAmenitiesForType(String typeName) {
        List<Amenity> all = Database.getAmenities();
        List<String> names = new java.util.ArrayList<>();

        names.add("WiFi");
        names.add("Smart TV");

        String lowerName = typeName.toLowerCase();
        if (lowerName.contains("deluxe") || lowerName.contains("lobby") ||
                lowerName.contains("suite") || lowerName.contains("alpine") ||
                lowerName.contains("penthouse") || lowerName.contains("gustave")) {
            names.add("Mini-bar");
        }
        if (lowerName.contains("suite") || lowerName.contains("alpine") ||
                lowerName.contains("penthouse") || lowerName.contains("gustave")) {
            names.add("Jacuzzi");
        }
        if (lowerName.contains("penthouse") || lowerName.contains("gustave")) {
            names.add("Gym");
        }

        return all.stream()
                .filter(amenity -> names.contains(amenity.getName()))
                .collect(java.util.stream.Collectors.toList());
    }

    public static void deleteRoom(Room room) throws InUseException {
        for (Reservation r : Database.getReservations()) {
            if (r.getRoom().getRoomNumber().equalsIgnoreCase(room.getRoomNumber())
                    && r.getStatus() != ReservationStatus.CANCELLED
                    && r.getStatus() != ReservationStatus.COMPLETED) {
                throw new InUseException();
            }
        }
        Database.getRooms().remove(room);
    }

    public static void deleteRoomType(RoomType roomType) throws InUseException {
        boolean inUse = Database.getRooms().stream().anyMatch(r -> r.getRoomType().getName().equalsIgnoreCase(roomType.getName()));
        if (inUse) throw new InUseException("Room Type");
        Database.getRoomTypes().remove(roomType);
    }

    public static void deleteAmenity(Amenity amenity) {
        Database.getAmenities().remove(amenity);
    }

    public static Room findRoom(String number) {
        return Database.getRooms().stream()
                .filter(r -> r.getRoomNumber().equalsIgnoreCase(number))
                .findFirst().orElse(null);
    }

    public static RoomType findRoomType(String name) {
        return Database.getRoomTypes().stream()
                .filter(rt -> rt.getName().equalsIgnoreCase(name))
                .findFirst().orElse(null);
    }

    public static Amenity findAmenity(String name) {
        return Database.getAmenities().stream()
                .filter(a -> a.getName().equalsIgnoreCase(name))
                .findFirst().orElse(null);
    }

    private static boolean shouldPreferAmenity(String candidate, String current) {
        return isGymAmenityName(candidate)
                && !isExactGymName(current)
                && isExactGymName(candidate);
    }

    private static boolean isExactGymName(String name) {
        return "gym".equals(normalizeAmenityName(name));
    }

    public static String displayAmenityName(String name) {
        String trimmed = name == null ? "" : name.trim();
        return isGymAmenityName(trimmed) ? "Gym" : trimmed;
    }

    private static String normalizeAmenityName(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }
}
