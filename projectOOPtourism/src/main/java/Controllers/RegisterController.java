package Controllers;


import Utils.AppContext;
import Models.*;
import Repositories.*;
import Services.*;
import Utils.*;
import Utils.exceptions.*;
import Repositories.database.DatabaseConnection;
import Repositories.DatabaseInitializer.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.util.StringConverter;

public class RegisterController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private DatePicker dobPicker;
    @FXML private TextField balanceField;
    @FXML private TextField addressField;
    @FXML private ComboBox<Gender> genderBox;
    @FXML private FlowPane prefsPillsPane;
    @FXML private Label messageLabel;

    private AppContext mainApp;
    private final Set<String> selectedPreferences = new LinkedHashSet<>();
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public void setMainApp(AppContext mainApp) {
        this.mainApp = mainApp;
    }

    @FXML
    public void initialize() {
        genderBox.setItems(FXCollections.observableArrayList(Gender.values()));

        dobPicker.setConverter(new StringConverter<LocalDate>() {
            @Override
            public String toString(LocalDate date) {
                return (date != null) ? dtf.format(date) : "";
            }
            @Override
            public LocalDate fromString(String string) {
                try {
                    return (string != null && !string.isEmpty()) ? LocalDate.parse(string, dtf) : null;
                } catch (Exception e) {
                    return null;
                }
            }
        });

        dobPicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isAfter(LocalDate.now()));
            }
        });

        buildPreferencePills();
    }

    private void buildPreferencePills() {
        prefsPillsPane.getChildren().clear();
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
            });

            prefsPillsPane.getChildren().add(pill);
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

    @FXML
    private void handleRegister() {
        try {
            Authentication.validatePasswordStrength(passwordField.getText().trim());

            LocalDate dob = dobPicker.getValue();
            if (dob == null) {
                messageLabel.setText("Please select a valid Date of Birth.");
                return;
            }

            Guest guest = new Guest(
                    usernameField.getText().trim(),
                    passwordField.getText().trim(),
                    dob,
                    Double.parseDouble(balanceField.getText().trim()),
                    addressField.getText().trim(),
                    genderBox.getValue(),
                    String.join(", ", selectedPreferences)
            );

            guest.register();
            mainApp.alert("Success", "Account created successfully. You can now log in.");
            mainApp.showLoginScreen();

        } catch (NumberFormatException ex) {
            messageLabel.setText("Balance must be a number.");
        } catch (Exception ex) {
            messageLabel.setText(ex.getMessage() != null ? ex.getMessage() : "Please fill all fields correctly.");
        }
    }

    @FXML
    private void handleBack() {
        mainApp.showLoginScreen();
    }

    @FXML
    private void handleSignInLink() {
        mainApp.showLoginScreen();
    }
}
