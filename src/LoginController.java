import exceptions.InvalidCredentialsException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(AlertType.WARNING, "Validation Error", "Please enter both username and password.");
            return;
        }

        try {
            User user = Authentication.login(username, password);
            showAlert(AlertType.INFORMATION, "Login Successful", "Welcome, " + user.getUsername() + "!");
            
            if (user instanceof Admin) {
                // TODO: Switch to Admin Dashboard
            } else if (user instanceof Receptionist) {
                // TODO: Switch to Receptionist Dashboard
            } else if (user instanceof Guest) {
                // TODO: Switch to Guest Dashboard
            }
        } catch (InvalidCredentialsException e) {
            showAlert(AlertType.ERROR, "Login Failed", e.getMessage());
        }
    }

    private void showAlert(AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}