package Controllers;


import Utils.AppContext;
import Models.*;
import Repositories.*;
import Services.*;
import Utils.*;
import Utils.exceptions.*;
import Repositories.database.DatabaseConnection;
import Repositories.DatabaseInitializer.*;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    private AppContext mainApp;

    @FXML
    public void initialize() {
        usernameField.setOnAction(e -> handleLogin());
        passwordField.setOnAction(e -> handleLogin());
    }

    public void setMainApp(AppContext mainApp) {
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
            User currentUser = Authentication.login(username, password);
            mainApp.setCurrentUser(currentUser);

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
        mainApp.switchScene("/Register.fxml");
    }

    @FXML
    private void handleForgotPassword() {
        mainApp.switchScene("/ForgotPassword.fxml");
    }
}
