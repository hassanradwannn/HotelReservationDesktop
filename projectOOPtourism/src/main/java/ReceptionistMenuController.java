import java.util.List;
import java.util.function.Supplier;

import javafx.fxml.FXML;

public class ReceptionistMenuController {
    private Main mainApp;
    private Receptionist receptionist;

    public void initData(Main mainApp, Receptionist receptionist) {
        this.mainApp = mainApp;
        this.receptionist = receptionist;
    }

    public void loadDefaultView() {
        showToday();
    }

    @FXML private void showToday() {
        mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/GenericList.fxml", new Object[]{"Today's Reservations", (Supplier<List<?>>) () -> {
            Database.refreshReservationsFromDatabase();
            return Database.getReservations().stream()
                            .filter(r -> r.getCheckInDate().isEqual(SystemTime.getToday()))
                            .toList();
        }});
    }
    
    @FXML private void showCheckIn() { mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/CheckIn.fxml", receptionist); }
    @FXML private void showCheckOut() { mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/CheckOut.fxml", receptionist); }
    
    @FXML private void showAllReservations() {
        mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/GenericList.fxml", new Object[]{"All Reservations", (Supplier<List<?>>) () -> {
            Database.refreshReservationsFromDatabase();
            return Database.getReservations();
        }});
    }
    
    @FXML private void showGuests() {
        mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/GenericList.fxml", new Object[]{"All Guests", (Supplier<List<?>>) () -> {
            Database.refreshUsersFromDatabase();
            return Database.getGuests();
        }});
    }
    
    @FXML private void showRooms() { mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/GenericList.fxml", new Object[]{"Rooms", (Supplier<List<?>>) () -> Database.getRooms()}); }
    @FXML private void showTime() { mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/AdvanceTime.fxml", null); }
    @FXML private void showChat() { mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/Chat.fxml", receptionist); }
    
    @FXML private void doLogout() { 
        mainApp.showLoginScreen(); 
    }
}