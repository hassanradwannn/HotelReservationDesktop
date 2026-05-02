import javafx.fxml.FXML;

public class GuestMenuController {
    private Main mainApp;
    private Guest guest;

    public void initData(Main mainApp, Guest guest) {
        this.mainApp = mainApp;
        this.guest = guest;
    }

    public void loadDefaultView() {
        showHome();
    }

    @FXML private void showHome() { mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/GuestHome.fxml", guest); }
    @FXML private void showChat() { mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/Chat.fxml", guest); }
    @FXML private void showReservations() { mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/GuestReservations.fxml", guest); }
    
    @FXML 
    private void doLogout() { 
        mainApp.showLoginScreen(); 
    }
}
