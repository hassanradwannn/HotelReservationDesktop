import javafx.fxml.FXML;

public class GuestMenuController {
    private Main mainApp;
    private Guest guest;

    public void initData(Main mainApp, Guest guest) {
        this.mainApp = mainApp;
        this.guest = guest;
    }

    public void loadDefaultView() {
        showProfile();
    }

    @FXML private void showProfile() { mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/GuestProfile.fxml", guest); }
    @FXML private void showRooms() { mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/RoomBrowser.fxml", guest); }
    @FXML private void showReserve() { mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/MakeReservation.fxml", guest); }
    @FXML private void showReservations() { mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/GuestReservations.fxml", guest); }
    @FXML private void showDeposit() { mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/PayDeposit.fxml", guest); }
    @FXML private void showCancel() { mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/CancelReservation.fxml", guest); }
    @FXML private void showTime() { mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/AdvanceTime.fxml", null); }
    @FXML private void showChat() { mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/Chat.fxml", guest); }
    
    @FXML 
    private void doLogout() { 
        mainApp.showLoginScreen(); 
    }
}