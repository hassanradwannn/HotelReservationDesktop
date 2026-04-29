import exceptions.InvalidCredentialsException;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class RegisterController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField dobField;
    @FXML private TextField balanceField;
    @FXML private TextField addressField;
    @FXML private ComboBox<Gender> genderBox;
    @FXML private TextField prefsField;
    @FXML private Label messageLabel;

    private Main mainApp;

    public void setMainApp(Main mainApp) {
        this.mainApp = mainApp;
    }

    @FXML
    public void initialize() {
        genderBox.setItems(FXCollections.observableArrayList(Gender.values()));
        dobField.setPromptText("DOB DD-MM-YYYY");
    }

    @FXML
    private void handleRegister() {
        try {
            Authentication.validatePasswordStrength(passwordField.getText().trim());
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d-M-yyyy");

            Guest guest = new Guest(
                    usernameField.getText().trim(),
                    passwordField.getText().trim(),
                    LocalDate.parse(dobField.getText().trim(), formatter),
                    Double.parseDouble(balanceField.getText().trim()),
                    addressField.getText().trim(),
                    genderBox.getValue(),
                    prefsField.getText().trim()
            );

            guest.register();
            mainApp.alert("Success", "Account created successfully. You can now log in.");
            mainApp.showLoginScreen();

        } catch (InvalidCredentialsException ex) {
            messageLabel.setText(ex.getMessage());
        } catch (DateTimeParseException ex) {
            messageLabel.setText("Invalid date format. Use DD-MM-YYYY.");
        } catch (NumberFormatException ex) {
            messageLabel.setText("Balance must be a number.");
        } catch (Exception ex) {
            messageLabel.setText("Please fill all fields correctly.");
        }
    }

    @FXML
    private void handleBack() {
        mainApp.showLoginScreen();
    }
}