import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;

public class GuestReservationsController implements DashboardContentController {

    @FXML private ListView<Reservation> reservationsList;
    private Main mainApp;
    private Guest guest;

    @Override
    public void initData(Main mainApp, Object data) {
        this.mainApp = mainApp;
        this.guest = (Guest) data;
        
        loadReservations();
        mainApp.setCurrentViewRefresher(this::loadReservations);
    }

    private void loadReservations() {
        Database.refreshReservationsFromDatabase();
        reservationsList.setItems(FXCollections.observableArrayList(Database.getReservations().stream()
                .filter(r -> r.getGuest().getUsername().equals(guest.getUsername()))
                .toList()));
        reservationsList.setOnMouseClicked(event -> {
            Reservation selected = reservationsList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/ReservationDetail.fxml", selected);
            }
        });
    }
}
