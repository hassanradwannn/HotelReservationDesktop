import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;

public class CancelReservationController implements DashboardContentController {

    @FXML private ComboBox<Reservation> reservationCombo;

    private Main mainApp;
    private Guest guest;

    @Override
    public void initData(Main mainApp, Object data) {
        this.mainApp = mainApp;
        if (data instanceof Guest g) {
            this.guest = g;
            loadReservations();
        }
    }

    private void loadReservations() {
        reservationCombo.setItems(FXCollections.observableArrayList(
                Database.getReservations().stream()
                        .filter(r -> r.getGuest().getUsername().equalsIgnoreCase(guest.getUsername()))
                        .filter(r -> r.getStatus() == ReservationStatus.PENDING || r.getStatus() == ReservationStatus.CONFIRMED)
                        .toList()
        ));
    }

    @FXML
    private void handleCancel() {
        try {
            Reservation selected = reservationCombo.getValue();
            if (selected == null) {
                mainApp.alert("Error", "Please select a reservation first.");
                return;
            }

            ReservationService.cancelReservation(selected.getReservationId());
            mainApp.alert("Cancelled", "Reservation cancelled successfully.");
            loadReservations();
            reservationCombo.setValue(null);

        } catch (Exception ex) {
            mainApp.alert("Error", ex.getMessage());
        }
    }
}