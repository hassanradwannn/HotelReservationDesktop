import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.util.StringConverter;

public class GuestProfileController implements DashboardContentController {
    @FXML private TextField usernameField;
    @FXML private DatePicker dobPicker;
    @FXML private TextField addressField;
    @FXML private ComboBox<Gender> genderBox;
    @FXML private FlowPane preferencePillsPane;
    @FXML private Label balanceLabel;
    @FXML private Label profileMessageLabel;
    @FXML private Label summaryUsernameLabel;
    @FXML private Label summaryDobLabel;
    @FXML private Label summaryBalanceLabel;
    @FXML private Label summaryGenderLabel;
    @FXML private Label summaryPreferencesLabel;

    private Main mainApp;
    private Guest guest;
    private final Set<String> selectedPreferences = new LinkedHashSet<>();
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void initData(Main mainApp, Object data) {
        this.mainApp = mainApp;
        this.guest = (Guest) data;

        setupDatePicker();
        genderBox.setItems(FXCollections.observableArrayList(Gender.values()));
        usernameField.textProperty().addListener((obs, oldValue, newValue) -> updateSummaryPreview());
        dobPicker.valueProperty().addListener((obs, oldValue, newValue) -> updateSummaryPreview());
        addressField.textProperty().addListener((obs, oldValue, newValue) -> updateSummaryPreview());
        genderBox.valueProperty().addListener((obs, oldValue, newValue) -> updateSummaryPreview());

        refreshProfileFromDatabase();
        mainApp.setCurrentViewRefresher(this::refreshProfileFromDatabase);
    }

    private void setupDatePicker() {
        dobPicker.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                return date == null ? "" : DISPLAY_DATE.format(date);
            }

            @Override
            public LocalDate fromString(String value) {
                try {
                    return value == null || value.trim().isEmpty()
                            ? null
                            : LocalDate.parse(value.trim(), DISPLAY_DATE);
                } catch (Exception ex) {
                    return null;
                }
            }
        });

        dobPicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isAfter(SystemTime.getToday()));
            }
        });
    }

    private void refreshProfileFromDatabase() {
        String username = guest == null ? "" : guest.getUsername();
        User freshUser = UserDatabase.findUser(username);
        if (freshUser instanceof Guest freshGuest) {
            guest = freshGuest;
            mainApp.setCurrentUser(guest);
        }
        renderProfile();
    }

    private void renderProfile() {
        usernameField.setText(guest.getUsername());
        dobPicker.setValue(guest.getDateOfBirth());
        addressField.setText(guest.getAddress());
        genderBox.setValue(guest.getGender());
        balanceLabel.setText("$" + mainApp.money(guest.getBalance()));

        selectedPreferences.clear();
        selectedPreferences.addAll(guest.getRoomPreferenceNames());
        buildPreferencePills();
        updateSummaryPreview();
    }

    private void buildPreferencePills() {
        preferencePillsPane.getChildren().clear();
        List<Amenity> amenities = CatalogService.listFilterAmenities();
        retainSelectedPreferenceFilters(amenities);

        for (Amenity amenity : amenities) {
            ToggleButton pill = new ToggleButton(amenity.getName());
            pill.getStyleClass().add("amenity-pill");
            pill.setSelected(isPreferenceSelected(amenity.getName()));
            if (pill.isSelected()) {
                pill.getStyleClass().add("amenity-pill-active");
            }

            pill.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                if (isSelected) {
                    setPreferenceSelected(amenity.getName(), true);
                    if (!pill.getStyleClass().contains("amenity-pill-active")) {
                        pill.getStyleClass().add("amenity-pill-active");
                    }
                } else {
                    setPreferenceSelected(amenity.getName(), false);
                    pill.getStyleClass().remove("amenity-pill-active");
                }
                updateSummaryPreview();
            });

            preferencePillsPane.getChildren().add(pill);
        }
    }

    private void retainSelectedPreferenceFilters(List<Amenity> amenities) {
        selectedPreferences.removeIf(selectedName -> amenities.stream()
                .noneMatch(amenity -> CatalogService.isSameAmenityFilter(selectedName, amenity.getName())));
    }

    private boolean isPreferenceSelected(String amenityName) {
        return selectedPreferences.stream()
                .anyMatch(selectedName -> CatalogService.isSameAmenityFilter(selectedName, amenityName));
    }

    private void setPreferenceSelected(String amenityName, boolean selected) {
        selectedPreferences.removeIf(selectedName -> CatalogService.isSameAmenityFilter(selectedName, amenityName));
        if (selected) {
            selectedPreferences.add(amenityName);
        }
    }

    private void updateSummaryPreview() {
        if (summaryUsernameLabel == null) {
            return;
        }

        String username = usernameField.getText() == null || usernameField.getText().trim().isEmpty()
                ? "-"
                : usernameField.getText().trim();
        LocalDate dob = dobPicker.getValue();
        Gender gender = genderBox.getValue();

        summaryUsernameLabel.setText(username);
        summaryDobLabel.setText(dob == null ? "-" : DISPLAY_DATE.format(dob));
        summaryBalanceLabel.setText(guest == null ? "-" : "$" + mainApp.money(guest.getBalance()));
        summaryGenderLabel.setText(gender == null ? "-" : gender.toString());
        summaryPreferencesLabel.setText(formatPreferences());
    }

    private String formatPreferences() {
        return selectedPreferences.isEmpty() ? "None selected" : String.join(", ", selectedPreferences);
    }

    @FXML
    private void handleSaveProfile() {
        try {
            String oldUsername = guest.getUsername();
            String newUsername = usernameField.getText() == null ? "" : usernameField.getText().trim();
            if (newUsername.isEmpty()) {
                setError("Username cannot be empty.");
                return;
            }

            if (!newUsername.equalsIgnoreCase(oldUsername) && UserDatabase.findUser(newUsername) != null) {
                setError("That username is already taken.");
                return;
            }

            LocalDate dob = dobPicker.getValue();
            if (dob == null || dob.isAfter(SystemTime.getToday())) {
                setError("Please select a valid birth date.");
                return;
            }

            if (genderBox.getValue() == null) {
                setError("Please select a gender.");
                return;
            }

            Guest updatedGuest = new Guest(
                    newUsername,
                    guest.getPassword(),
                    dob,
                    guest.getBalance(),
                    addressField.getText() == null ? "" : addressField.getText().trim(),
                    genderBox.getValue(),
                    "");
            updatedGuest.setRoomPreferenceNames(selectedPreferences.stream().toList());

            DatabaseSaver.updateGuestProfile(oldUsername, updatedGuest);
            Database.refreshUsersFromDatabase();
            Database.refreshReservationsFromDatabase();

            guest = Database.getGuests().stream()
                    .filter(g -> g.getUsername().equals(newUsername))
                    .findFirst()
                    .orElse(updatedGuest);
            mainApp.setCurrentUser(guest);
            mainApp.setGuestHomeSearchContext(null);
            setSuccess("Profile saved and synced.");
            renderProfile();
        } catch (Exception ex) {
            refreshProfileFromDatabase();
            setError(ex.getMessage() == null ? "Could not save profile." : ex.getMessage());
        }
    }

    @FXML
    private void handleResetProfile() {
        profileMessageLabel.setText("");
        refreshProfileFromDatabase();
    }

    private void setSuccess(String message) {
        profileMessageLabel.setText(message);
        profileMessageLabel.getStyleClass().removeAll("error-message", "success-message");
        profileMessageLabel.getStyleClass().add("success-message");
    }

    private void setError(String message) {
        profileMessageLabel.setText(message);
        profileMessageLabel.getStyleClass().removeAll("success-message", "error-message");
        profileMessageLabel.getStyleClass().add("error-message");
    }
}
