import javafx.fxml.FXML;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javafx.util.StringConverter;

public class AddReceptionistController implements DashboardContentController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private DatePicker dobPicker;
    @FXML private TextField hoursField;

    private Main mainApp;
    private Admin admin;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void initData(Main mainApp, Object data) {
        this.mainApp = mainApp;
        this.admin = (Admin) data;
        setupDobPicker();
    }

    private void setupDobPicker() {
        dobPicker.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                return date == null ? "" : dtf.format(date);
            }

            @Override
            public LocalDate fromString(String text) {
                if (text == null || text.trim().isEmpty()) {
                    return null;
                }
                return LocalDate.parse(text.trim(), dtf);
            }
        });

        dobPicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isAfter(SystemTime.getToday()));
            }
        });
    }

    @FXML
    private void handleRegister() {
        try {
            LocalDate dob = dobPicker.getValue();
            if (dob == null) {
                mainApp.alert("Error", "Please select a valid date of birth.");
                return;
            }

            admin.registerStaff(usernameField.getText().trim(),
                    passwordField.getText().trim(),
                    dob,
                    Integer.parseInt(hoursField.getText().trim()),
                    Role.RECEPTIONIST);

            mainApp.alert("Success", "Receptionist registered.");
            usernameField.clear();
            passwordField.clear();
            dobPicker.setValue(null);
            hoursField.clear();

        } catch (Exception ex) {
            mainApp.alert("Error", ex.getMessage());
        }
    }
}
