import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
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

    private Main mainApp;
    private final Set<String> selectedPreferences = new LinkedHashSet<>();
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public void setMainApp(Main mainApp) {
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
        selectedPreferences.retainAll(Database.getAmenities().stream().map(Amenity::getName).toList());

        for (Amenity amenity : Database.getAmenities()) {
            ToggleButton pill = new ToggleButton(amenity.getName());
            pill.getStyleClass().add("amenity-pill");
            pill.setSelected(selectedPreferences.contains(amenity.getName()));
            if (pill.isSelected()) {
                pill.getStyleClass().add("amenity-pill-active");
            }

            pill.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                if (isSelected) {
                    selectedPreferences.add(amenity.getName());
                    if (!pill.getStyleClass().contains("amenity-pill-active")) {
                        pill.getStyleClass().add("amenity-pill-active");
                    }
                } else {
                    selectedPreferences.remove(amenity.getName());
                    pill.getStyleClass().remove("amenity-pill-active");
                }
            });

            prefsPillsPane.getChildren().add(pill);
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

            guest.register(); // Calling your original method
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
