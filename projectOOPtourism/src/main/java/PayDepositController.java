import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;

public class PayDepositController implements DashboardContentController {

    @FXML private ComboBox<Reservation> pendingCombo;
    @FXML private Label detailsLabel;

    private Main mainApp;
    private Guest guest;

    @Override
    public void initData(Main mainApp, Object data) {
        this.mainApp = mainApp;
        if (data instanceof Guest g) {
            this.guest = g;
            loadPendingReservations();
        }
    }

    private void loadPendingReservations() {
        pendingCombo.setItems(FXCollections.observableArrayList(
                Database.getReservations().stream()
                        .filter(r -> r.getGuest().getUsername().equalsIgnoreCase(guest.getUsername()))
                        .filter(r -> r.getStatus() == ReservationStatus.PENDING)
                        .toList()
        ));
    }

    @FXML
    private void handleSelection() {
        Reservation r = pendingCombo.getValue();
        if (r != null) {
            detailsLabel.setText("Deposit: $" + mainApp.money(ReservationService.getDepositAmount(r))
                    + " | Your balance: $" + mainApp.money(guest.getBalance()));
        } else {
            detailsLabel.setText("Choose a reservation.");
        }
    }

    @FXML
    private void handlePay() {
        try {
            Reservation selected = pendingCombo.getValue();
            if (selected == null) {
                mainApp.alert("Error", "Please select a reservation first.");
                return;
            }
            ReservationService.payDeposit(selected, guest);
            mainApp.alert("Success", "Deposit paid. Reservation confirmed.\nNew Balance: $" + mainApp.money(guest.getBalance()));
            pendingCombo.getItems().remove(selected);
            pendingCombo.setValue(null);
            handleSelection();
        } catch (Exception ex) {
            mainApp.alert("Payment Failed", ex.getMessage());
        }
    }
}