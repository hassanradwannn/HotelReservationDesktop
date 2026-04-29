import javafx.fxml.FXML;
import javafx.scene.control.Label;

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
    }

    private void populateData() {
        usernameLabel.setText("Username: " + guest.getUsername());
        dobLabel.setText("DOB: " + guest.getDateOfBirth());
        balanceLabel.setText("Balance: $" + mainApp.money(guest.getBalance()));
        addressLabel.setText("Address: " + guest.getAddress());
        genderLabel.setText("Gender: " + guest.getGender());
        prefsLabel.setText("Preferences: " + guest.getRoomPreferences());
    }
}