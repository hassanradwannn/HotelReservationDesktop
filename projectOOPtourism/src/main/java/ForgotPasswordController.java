import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class ForgotPasswordController {
    @FXML private TextField usernameField;
    @FXML private TextField dobField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label messageLabel;

    private Main mainApp;

    public void setMainApp(Main mainApp) {
        this.mainApp = mainApp;
    }

    @FXML
    public void initialize() {
        dobField.setPromptText("DOB DD-MM-YYYY");
    }

    @FXML
    private void handleResetPassword() {
        String username = usernameField.getText().trim();
        String dobStr = dobField.getText().trim();
        String newPass = newPasswordField.getText().trim();
        String confPass = confirmPasswordField.getText().trim();

        if (username.isEmpty() || dobStr.isEmpty() || newPass.isEmpty() || confPass.isEmpty()) {
            messageLabel.setText("Please fill all fields.");
            return;
        }

        if (!newPass.equals(confPass)) {
            messageLabel.setText("Passwords do not match.");
            return;
        }

        try {
            Authentication.validatePasswordStrength(newPass);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d-M-yyyy");
            LocalDate dob = LocalDate.parse(dobStr, formatter);

            User user = UserDatabase.findUser(username);
            if (user == null || !user.getDateOfBirth().isEqual(dob)) {
                messageLabel.setText("Invalid Username or Date of Birth.");
                return;
            }

            // Real-time update in MySQL
            DatabaseSaver.updateUserPassword(username, newPass);

            mainApp.alert("Success", "Password reset successfully. You can now log in.");
            mainApp.showLoginScreen();

        } catch (DateTimeParseException ex) {
            messageLabel.setText("Invalid date format. Use DD-MM-YYYY.");
        } catch (Exception ex) {
            messageLabel.setText(ex.getMessage() != null ? ex.getMessage() : "Failed to reset password.");
        }
    }

    @FXML
    private void handleBack() {
        mainApp.showLoginScreen();
    }
}