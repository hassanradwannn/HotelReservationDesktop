import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;

import java.util.stream.Collectors;

public class CheckOutController implements DashboardContentController {

    @FXML private ComboBox<Reservation> reservationCombo;
    @FXML private ComboBox<PaymentMethod> paymentCombo;
    @FXML private Label messageLabel;

    private Main mainApp;

    @Override
    public void initData(Main mainApp, Object data) {
        this.mainApp = mainApp;
        loadReservations();
        paymentCombo.setItems(FXCollections.observableArrayList(PaymentMethod.values()));
    }

    private void loadReservations() {
        Database.refreshReservationsFromDatabase();
        reservationCombo.setItems(FXCollections.observableArrayList(
                Database.getReservations().stream()
                        .filter(r -> r.getStatus() == ReservationStatus.ONGOING
                                || r.getStatus() == ReservationStatus.CHECKING_OUT)
                        .collect(Collectors.toList())
        ));
    }

    @FXML
    private void handleCheckOut() {
        try {
            Reservation selected = reservationCombo.getValue();
            PaymentMethod paymentMethod = paymentCombo.getValue();

            if (selected == null || paymentMethod == null) {
                mainApp.alert("Error", "Please select a reservation and payment method.");
                return;
            }

            ReservationService.checkOutGuest(selected, paymentMethod);
            mainApp.alert("Success", "Guest checked out.");
            loadReservations(); // Refresh the list
            reservationCombo.setValue(null);
            paymentCombo.setValue(null);

        } catch (Exception ex) {
            mainApp.alert("Error", ex.getMessage());
        }
    }
}
