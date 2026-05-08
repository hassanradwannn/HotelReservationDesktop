package Services;


import Controllers.*;
import Models.*;
import Repositories.*;
import Utils.*;
import Utils.exceptions.*;
import Repositories.database.DatabaseConnection;
import Repositories.DatabaseInitializer.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class GuestPreferenceRanker {

    private GuestPreferenceRanker() {}

    public static void sortRoomsByGuestPreferences(List<Room> rooms, Guest guest) {
        Set<String> preferences = getNormalizedPreferenceNames(guest);
        if (rooms == null || rooms.size() < 2 || preferences.isEmpty()) {
            return;
        }

        rooms.sort(Comparator.comparingInt((Room room) -> getPreferenceMatchScore(room, preferences)).reversed());
    }

    public static int getPreferenceMatchScore(Room room, Guest guest) {
        return getPreferenceMatchScore(room, getNormalizedPreferenceNames(guest));
    }

    private static int getPreferenceMatchScore(Room room, Set<String> preferences) {
        if (room == null || room.getAmenities() == null) {
            return 0;
        }

        if (preferences.isEmpty()) {
            return 0;
        }

        int score = 0;
        if (preferences.contains(normalize(room.getViewName()))) {
            score++;
        }
        for (Amenity amenity : room.getAmenities()) {
            if (amenity != null && preferences.contains(normalize(amenity.getName()))) {
                score++;
            }
        }
        return score;
    }

    public static List<String> getMatchingPreferenceNames(Room room, Guest guest) {
        List<String> matches = new ArrayList<>();
        if (room == null || room.getAmenities() == null) {
            return matches;
        }

        Set<String> preferences = getNormalizedPreferenceNames(guest);
        if (preferences.contains(normalize(room.getViewName()))) {
            matches.add(room.getViewName());
        }
        for (Amenity amenity : room.getAmenities()) {
            if (amenity != null && preferences.contains(normalize(amenity.getName()))) {
                matches.add(amenity.getName());
            }
        }
        return matches;
    }

    public static boolean isPreferredAmenity(Guest guest, String amenityName) {
        return guest != null && guest.prefersAmenity(amenityName);
    }

    private static List<String> getPreferenceNames(Guest guest) {
        return guest == null ? List.of() : guest.getRoomPreferenceNames();
    }

    private static Set<String> getNormalizedPreferenceNames(Guest guest) {
        Set<String> names = new HashSet<>();
        for (String preference : getPreferenceNames(guest)) {
            names.add(normalize(preference));
        }
        return names;
    }

    private static String normalize(String value) {
        return CatalogService.amenityFilterKey(value);
    }
}
