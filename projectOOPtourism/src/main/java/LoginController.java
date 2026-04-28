import exceptions.InvalidCredentialsException;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    // We keep a reference to Main to navigate to other screens
    private Main mainApp;

    public void setMainApp(Main mainApp) {
        this.mainApp = mainApp;
    }

    @FXML
    private void handleLogin() {
        try {
            // Call the static Authentication logic just like before
            User currentUser = Authentication.login(usernameField.getText().trim(), passwordField.getText().trim());
            
            // Update the global state in Main
            mainApp.setCurrentUser(currentUser);

            // Navigate based on Role
            if (currentUser instanceof Admin admin) {
                mainApp.showAdminDashboard(admin);
            } else if (currentUser instanceof Receptionist receptionist) {
                mainApp.showReceptionistDashboard(receptionist);
            } else if (currentUser instanceof Guest guest) {
                mainApp.showGuestDashboard(guest);
            }

        } catch (InvalidCredentialsException ex) {
            messageLabel.setText("Login failed: " + ex.getMessage());
        }
    }

    @FXML
    private void handleRegister() {
        mainApp.showGuestRegisterScreen();
    }
}