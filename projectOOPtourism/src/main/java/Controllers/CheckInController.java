package Controllers;


import Utils.AppContext;
import Models.*;
import Repositories.*;
import Services.*;
import Utils.*;
import Utils.exceptions.*;
import Repositories.database.DatabaseConnection;
import Repositories.DatabaseInitializer.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;

import java.util.stream.Collectors;

public class CheckInController implements DashboardContentController {

    @FXML private ComboBox<Reservation> reservationCombo;
    @FXML private Label messageLabel;

    private AppContext mainApp;

    @Override
    public void initData(AppContext mainApp, Object data) {
        this.mainApp = mainApp;
        loadReservations();
    }

    private void loadReservations() {
        Database.refreshReservationsFromDatabase();
        reservationCombo.setItems(FXCollections.observableArrayList(
                Database.getReservations().stream()
                        .filter(r -> r.getStatus() == ReservationStatus.CHECKING_IN)
                        .filter(r -> r.getCheckInDate().isEqual(SystemTime.getToday()))
                        .collect(Collectors.toList())
        ));
    }

    @FXML
    private void handleCheckIn() {
        try {
            Reservation selected = reservationCombo.getValue();
            if (selected == null) {
                mainApp.alert("Error", "Please select a reservation first.");
                return;
            }

            ReservationService.checkInGuest(selected, selected.getGuest());
            mainApp.alert("Success", "Guest checked in.");
            loadReservations(); // Refresh the list
            reservationCombo.setValue(null);

        } catch (Exception ex) {
            mainApp.alert("Error", ex.getMessage());
        }
    }
}
