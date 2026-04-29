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

    @FXML
    public void initialize() {
        usernameField.setOnAction(e -> handleLogin());
        passwordField.setOnAction(e -> handleLogin());
    }

    public void setMainApp(Main mainApp) {
        this.mainApp = mainApp;
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Please enter both username and password.");
            return;
        }

        try {
            // Call the static Authentication logic just like before
            User currentUser = Authentication.login(username, password);
            
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

        } catch (Exception ex) {
            messageLabel.setText("Login failed: " + ex.getMessage());
        }
    }

    @FXML
    private void handleRegister() {
        mainApp.showGuestRegisterScreen();
    }

    @FXML
    private void handleForgotPassword() {
        mainApp.showForgotPasswordScreen();
    }
}