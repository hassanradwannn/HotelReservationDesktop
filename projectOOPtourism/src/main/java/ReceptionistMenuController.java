import java.util.List;
import java.util.function.Supplier;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.util.Duration;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;

public class ReceptionistMenuController {
    private Main mainApp;
    private Receptionist receptionist;

    @FXML private ListView<Reservation> allReservationsList;
    @FXML private ListView<Reservation> todaysReservationsList;

    private Timeline syncTimeline;
    private long localDataVersion = -1;

    @FXML
    public void initialize() {
        Database.loadAll();
        if (allReservationsList != null) {
            allReservationsList.getItems().setAll(Database.getReservations());
        }
        if (todaysReservationsList != null) {
            todaysReservationsList.getItems().setAll(Database.getTodaysReservations());
        }
        
        if (syncTimeline != null) syncTimeline.stop();
        syncTimeline = new Timeline(new KeyFrame(Duration.seconds(2), event -> {
            new Thread(() -> {
                long currentVersion = Database.getLatestDataVersion();
                if (localDataVersion == -1) {
                    localDataVersion = currentVersion;
                } else if (currentVersion > localDataVersion) {
                    localDataVersion = currentVersion;
                    Database.refreshReservationsFromDatabase();
                    Platform.runLater(() -> {
                        if (allReservationsList != null) {
                            allReservationsList.getItems().setAll(Database.getReservations());
                        }
                        if (todaysReservationsList != null) {
                            todaysReservationsList.getItems().setAll(Database.getTodaysReservations());
                        }
                    });
                }
            }).start();
        }));
        syncTimeline.setCycleCount(Timeline.INDEFINITE);
        syncTimeline.play();
    }

    public void initData(Main mainApp, Receptionist receptionist) {
        this.mainApp = mainApp;
        this.receptionist = receptionist;
        Database.loadAll(); // Uniform initialization safety net
    }

    public void loadDefaultView() {
        showAllReservations();
    }

    @FXML private void showToday() {
        mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/GenericList.fxml", new Object[]{"Today's Reservations", (Supplier<List<?>>) () -> {
            return Database.getTodaysReservations();
        }});
    }
    
    @FXML private void showCheckIn() { mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/CheckIn.fxml", receptionist); }
    @FXML private void showCheckOut() { mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/CheckOut.fxml", receptionist); }
    
    @FXML private void showAllReservations() {
        mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/GenericList.fxml", new Object[]{"Reservations", (Supplier<List<?>>) () -> {
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
