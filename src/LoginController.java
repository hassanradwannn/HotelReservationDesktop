import exceptions.InvalidCredentialsException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;

import java.io.IOException;

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
                switchScene(event, "admin_home.fxml", "Admin Dashboard", user);
            } else if (user instanceof Receptionist) {
                switchScene(event, "receptionist_home.fxml", "Receptionist Dashboard", user);
            } else if (user instanceof Guest) {
                switchScene(event, "guest_home.fxml", "Guest Dashboard", user);
            }
        } catch (InvalidCredentialsException e) {
            showAlert(AlertType.ERROR, "Login Failed", e.getMessage());
        } catch (IOException e) {
            showAlert(AlertType.ERROR, "Navigation Error", "Could not load the dashboard page.");
            e.printStackTrace();
        }
    }

    private void showAlert(AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void switchScene(ActionEvent event, String fxmlFile, String title, User user) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
        Parent root = loader.load();
        
        DashboardController controller = loader.getController();
        controller.initData(user);
        
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setTitle(title);
        stage.setScene(scene);
        stage.show();
    }
}