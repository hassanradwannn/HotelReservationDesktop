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
        this.guest = (Guest) data;
        
        // Fetch fresh data from DB to reflect any external SQL updates instantly
        Database.refreshUsersFromDatabase();
        guest = Database.getGuests().stream().filter(g -> g.getUsername().equals(guest.getUsername())).findFirst().orElse(guest);
        mainApp.setCurrentUser(guest);

        usernameLabel.setText("Username: " + guest.getUsername());
        dobLabel.setText("Date of Birth: " + guest.getDateOfBirth());
        balanceLabel.setText("Balance: $" + mainApp.money(guest.getBalance()));
        addressLabel.setText("Address: " + guest.getAddress());
        genderLabel.setText("Gender: " + guest.getGender());
        prefsLabel.setText("Room Preferences: " + guest.getRoomPreferences());
    }
}