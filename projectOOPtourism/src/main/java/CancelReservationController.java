import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;

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

        javafx.application.Platform.runLater(() -> {
            if (reservationCombo.getScene() != null) {
                for (javafx.scene.Node node : reservationCombo.getScene().getRoot().lookupAll(".label")) {
                    if (node instanceof Label label && label.getText() != null && label.getText().toLowerCase().contains("cancel")) {
                        String currentStyle = label.getStyle() == null ? "" : label.getStyle();
                        label.setStyle(currentStyle + "; -fx-text-fill: #2B2421;");
                    }
                }
            }
        });
    }

    private void loadReservations() {
        reservationCombo.setItems(FXCollections.observableArrayList(
                Database.getReservations().stream()
                        .filter(r -> r.getGuest().getUsername().equalsIgnoreCase(guest.getUsername()))
                        .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
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