import javafx.fxml.FXML;
import javafx.scene.control.Label;
import java.time.format.DateTimeFormatter;

public class GuestProfileController implements DashboardContentController {

    @FXML private Label usernameLabel;
    @FXML private Label dobLabel;
    @FXML private Label balanceLabel;
    @FXML private Label addressLabel;
    @FXML private Label genderLabel;
    @FXML private Label prefsLabel;

    private Main mainApp;
    private Guest guest;

    @Override
    public void initData(Main mainApp, Object data) {
        this.mainApp = mainApp;
        if (data instanceof Guest g) {
            this.guest = g;
            populateData();
        }

        javafx.application.Platform.runLater(() -> {
            if (usernameLabel.getScene() != null) {
                for (javafx.scene.Node node : usernameLabel.getScene().getRoot().lookupAll(".label")) {
                    if (node instanceof Label label && label.getText() != null && label.getText().toLowerCase().contains("profile")) {
                        String currentStyle = label.getStyle() == null ? "" : label.getStyle();
                        label.setStyle(currentStyle + "; -fx-text-fill: #2B2421;");
                    }
                }
            }
        });
    }

    private void populateData() {
        usernameLabel.setText("Username: " + guest.getUsername());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        dobLabel.setText("DOB: " + guest.getDateOfBirth().format(formatter));
        balanceLabel.setText("Balance: $" + mainApp.money(guest.getBalance()));
        addressLabel.setText("Address: " + guest.getAddress());
        genderLabel.setText("Gender: " + guest.getGender());
        prefsLabel.setText("Preferences: " + guest.getRoomPreferences());
    }
}