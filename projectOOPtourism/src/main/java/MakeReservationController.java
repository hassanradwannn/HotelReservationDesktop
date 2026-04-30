import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class MakeReservationController implements DashboardContentController {

    @FXML private ComboBox<RoomType> typeBox;
    @FXML private TextField guestsField;
    @FXML private TextField checkInField;
    @FXML private TextField checkOutField;
    @FXML private CheckBox gymBox;
    @FXML private Label msgLabel;

    private Main mainApp;
    private Guest guest;

    @Override
    public void initData(Main mainApp, Object data) {
        this.mainApp = mainApp;
        this.guest = (Guest) data;
        
        typeBox.setItems(FXCollections.observableArrayList(Database.getRoomTypes()));
        
        Room selectedRoom = mainApp.getSelectedRoomForReservation();
        if (selectedRoom != null) {
            typeBox.setValue(selectedRoom.getRoomType());
            msgLabel.setText("Selected Room " + selectedRoom.getRoomNumber() + " automatically.");
            msgLabel.getStyleClass().removeAll("error-message", "success-message");
            msgLabel.getStyleClass().add("success-message");
        } else if (mainApp.getSelectedRoomTypeForReservation() != null) {
            typeBox.setValue(mainApp.getSelectedRoomTypeForReservation());
            msgLabel.setText("Selected " + mainApp.getSelectedRoomTypeForReservation().getName() + " type. Select dates and guests, then search.");
            msgLabel.getStyleClass().removeAll("error-message", "success-message");
            msgLabel.getStyleClass().add("success-message");
        }
        
        // Automatically handle the Gym Pass for the Gustave Penthouse
        typeBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getName().equalsIgnoreCase("The Gustave Penthouse")) {
                gymBox.setSelected(true);
                gymBox.setDisable(true);
            } else {
                gymBox.setDisable(false);
            }
        });
        if (typeBox.getValue() != null && typeBox.getValue().getName().equalsIgnoreCase("The Gustave Penthouse")) {
            gymBox.setSelected(true);
            gymBox.setDisable(true);
        }
    }

    @FXML
    private void handleSearch() {
        Database.refreshReservationsFromDatabase();
        try {
            if (typeBox.getValue() == null) {
                msgLabel.setText("Please select a room type.");
                msgLabel.getStyleClass().removeAll("error-message", "success-message");
                msgLabel.getStyleClass().add("error-message");
                return;
            }

            int numGuests = Integer.parseInt(guestsField.getText().trim());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d-M-yyyy");
            LocalDate in = LocalDate.parse(checkInField.getText().trim(), formatter);
            LocalDate out = LocalDate.parse(checkOutField.getText().trim(), formatter);

            if (!ReservationService.isDateRangeValid(in, out)) {
                msgLabel.setText("Invalid dates. Check-out must be after check-in.");
                msgLabel.getStyleClass().removeAll("error-message", "success-message");
                msgLabel.getStyleClass().add("error-message");
                return;
            }

            List<Room> available = ReservationService.searchAvailableRooms(in, out, typeBox.getValue(), numGuests, null);

            if (available.isEmpty()) {
                msgLabel.setText("No rooms available for the selected dates.");
                msgLabel.getStyleClass().removeAll("error-message", "success-message");
                msgLabel.getStyleClass().add("error-message");
                return;
            }

            // Package the booking info and pass it to the new available rooms selection view
            Object[] bookingData = new Object[]{ guest, in, out, gymBox.isSelected(), available };
            mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/AvailableRooms.fxml", bookingData);

        } catch (NumberFormatException | DateTimeParseException ex) {
            msgLabel.setText("Check guest number and date format (DD-MM-YYYY).");
            msgLabel.getStyleClass().removeAll("error-message", "success-message");
            msgLabel.getStyleClass().add("error-message");
        }
    }
}