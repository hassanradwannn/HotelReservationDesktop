import javafx.fxml.FXML;

public class GuestProfileController implements DashboardContentController {

    private Main mainApp;
    private Guest guest;

    @FXML
    public void initialize() {
        // Initialize UI components here
    }

    @Override
    public void initData(Main mainApp, Object data) {
        this.mainApp = mainApp;
        this.guest = (Guest) data;
    }
}