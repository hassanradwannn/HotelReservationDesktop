import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import exceptions.InvalidCredentialsException;

public class Guest extends User {
    private double balance;
    private String address;
    private Gender gender;
    private String roomPreferences;
    private String password;
    private String name;
    private int id;

    public Guest(String username, String password, LocalDate dateOfBirth,
                 double balance, String address, Gender gender, String roomPreferences) {
        super(username, password, dateOfBirth);
        this.balance = balance;
        this.address = address;
        this.gender = gender;
        this.roomPreferences = roomPreferences == null ? "" : roomPreferences;
    }


    public double getBalance() { return balance; }
    public void setBalance(double balance) {
        if(balance < 0) throw new IllegalArgumentException("Balance cannot be negative.");
        this.balance = balance;
    }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }

    public String getRoomPreferences() { return roomPreferences == null ? "" : roomPreferences; }
    public void setRoomPreferences(String roomPreferences) { this.roomPreferences = roomPreferences == null ? "" : roomPreferences; }

    public List<String> getRoomPreferenceNames() {
        List<String> preferences = new ArrayList<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        String rawPreferences = getRoomPreferences();
        if (rawPreferences.trim().isEmpty()) {
            return preferences;
        }

        for (String preference : rawPreferences.split(",")) {
            String trimmed = preference.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            String key = CatalogService.amenityFilterKey(trimmed);
            if (seen.add(key)) {
                preferences.add(CatalogService.displayAmenityName(trimmed));
            }
        }
        return preferences;
    }

    public void setRoomPreferenceNames(List<String> preferenceNames) {
        if (preferenceNames == null || preferenceNames.isEmpty()) {
            setRoomPreferences("");
            return;
        }

        LinkedHashSet<String> uniqueNames = new LinkedHashSet<>();
        for (String name : preferenceNames) {
            if (name != null && !name.trim().isEmpty()) {
                uniqueNames.add(name.trim());
            }
        }
        setRoomPreferences(String.join(", ", uniqueNames));
    }

    public boolean prefersAmenity(String amenityName) {
        if (amenityName == null || amenityName.trim().isEmpty()) {
            return false;
        }

        String requested = CatalogService.amenityFilterKey(amenityName);
        return getRoomPreferenceNames().stream()
                .map(CatalogService::amenityFilterKey)
                .anyMatch(requested::equals);
    }

    public void register() throws InvalidCredentialsException {
        Authentication.register(this);
    }
}
