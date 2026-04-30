import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class AddReceptionistController implements DashboardContentController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField dobField;
    @FXML private TextField hoursField;

    private Main mainApp;
    private Admin admin;

    @Override
    public void initData(Main mainApp, Object data) {
        this.mainApp = mainApp;
        this.admin = (Admin) data;
    }

    @FXML
    private void handleRegister() {
        try {
            admin.registerStaff(usernameField.getText().trim(),
                    passwordField.getText().trim(),
                    LocalDate.parse(dobField.getText().trim(), DateTimeFormatter.ofPattern("d-M-yyyy")),
                    Integer.parseInt(hoursField.getText().trim()),
                    Role.RECEPTIONIST);

            mainApp.alert("Success", "Receptionist registered.");
            usernameField.clear();
            passwordField.clear();
            dobField.clear();
            hoursField.clear();

        } catch (Exception ex) {
            mainApp.alert("Error", ex.getMessage());
        }
    }
}