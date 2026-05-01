import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;

public class RegisterController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private DatePicker dobPicker; // Replaces dobField[cite: 28]
    @FXML private TextField balanceField;
    @FXML private TextField addressField;
    @FXML private ComboBox<Gender> genderBox; // Reverted to use your Gender enum
    @FXML private TextField prefsField;
    @FXML private Label messageLabel;

    private Main mainApp;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("d/M/yyyy");

    public void setMainApp(Main mainApp) {
        this.mainApp = mainApp;
    }

    @FXML
    public void initialize() {
        // Reverted to your original Enum-based items
        genderBox.setItems(FXCollections.observableArrayList(Gender.values()));

        // Setup DatePicker for D/M/YYYY display
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

        // Blackout future dates for birthdate
        dobPicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isAfter(LocalDate.now()));
            }
        });
    }

    @FXML
    private void handleRegister() {
        try {
            // Reverted to your original functionality: manual password validation check
            Authentication.validatePasswordStrength(passwordField.getText().trim());

            LocalDate dob = dobPicker.getValue();
            if (dob == null) {
                messageLabel.setText("Please select a valid Date of Birth.");
                return;
            }

            // Reverted to your original functional flow: Create Guest object -> .register()
            Guest guest = new Guest(
                    usernameField.getText().trim(),
                    passwordField.getText().trim(),
                    dob,
                    Double.parseDouble(balanceField.getText().trim()),
                    addressField.getText().trim(),
                    genderBox.getValue(),
                    prefsField.getText().trim()
            );

            guest.register(); // Calling your original method
            mainApp.alert("Success", "Account created successfully. You can now log in.");
            mainApp.showLoginScreen(); // Navigating back to login[cite: 22, 28]

        } catch (NumberFormatException ex) {
            messageLabel.setText("Balance must be a number.");
        } catch (Exception ex) {
            // Displaying your original custom exception messages
            messageLabel.setText(ex.getMessage() != null ? ex.getMessage() : "Please fill all fields correctly.");
        }
    }

    @FXML
    private void handleBack() {
        mainApp.showLoginScreen(); // Fixed method name
    }

    @FXML
    private void handleSignInLink() {
        mainApp.showLoginScreen(); // Fixed method name[cite: 22]
    }
}