import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class MakeReservationController implements DashboardContentController {

    @FXML private ComboBox<RoomType> typeBox;
    @FXML private TextField guestsField;
    @FXML private TextField checkInField;
    @FXML private TextField checkOutField;
    @FXML private CheckBox gymBox;
    @FXML private ComboBox<Room> roomBox;
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
            roomBox.setItems(FXCollections.observableArrayList(selectedRoom));
            roomBox.setValue(selectedRoom);
            msgLabel.setText("Selected Room " + selectedRoom.getRoomNumber() + " automatically.");
            msgLabel.setStyle("-fx-text-fill: #8F1D3F;");
        }
    }

    @FXML
    private void handleSearch() {
        Database.refreshReservationsFromDatabase();
        try {
            if (typeBox.getValue() == null) {
                msgLabel.setText("Please select a room type.");
                msgLabel.setStyle("-fx-text-fill: #C74261;");
                return;
            }

            int numGuests = Integer.parseInt(guestsField.getText().trim());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d-M-yyyy");
            LocalDate in = LocalDate.parse(checkInField.getText().trim(), formatter);
            LocalDate out = LocalDate.parse(checkOutField.getText().trim(), formatter);

            if (!ReservationService.isDateRangeValid(in, out)) {
                msgLabel.setText("Invalid dates. Check-out must be after check-in.");
                msgLabel.setStyle("-fx-text-fill: #C74261;");
                return;
            }

            List<Room> available = ReservationService.searchAvailableRooms(in, out, typeBox.getValue(), numGuests);
            roomBox.setItems(FXCollections.observableArrayList(available));

            Room selectedRoom = mainApp.getSelectedRoomForReservation();
            if (selectedRoom != null && available.contains(selectedRoom)) {
                roomBox.setValue(selectedRoom);
            }

            msgLabel.setText(available.isEmpty() ? "No rooms available." : available.size() + " rooms found.");
            msgLabel.setStyle(available.isEmpty() ? "-fx-text-fill: #C74261;" : "-fx-text-fill: #8F1D3F;");

        } catch (NumberFormatException | DateTimeParseException ex) {
            msgLabel.setText("Check guest number and date format (DD-MM-YYYY).");
            msgLabel.setStyle("-fx-text-fill: #C74261;");
        }
    }

    @FXML
    private void handleCreate() {
        try {
            if (roomBox.getValue() == null) {
                msgLabel.setText("Please choose a room first.");
                msgLabel.setStyle("-fx-text-fill: #C74261;");
                return;
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d-M-yyyy");
            LocalDate in = LocalDate.parse(checkInField.getText().trim(), formatter);
            LocalDate out = LocalDate.parse(checkOutField.getText().trim(), formatter);

            Reservation res = ReservationService.createReservation(guest, roomBox.getValue(), in, out, gymBox.isSelected());
            mainApp.setSelectedRoomForReservation(null);

            mainApp.alert("Reservation Created", "ID: " + res.getReservationId() + "\nTotal: $" + mainApp.money(res.getTotalPrice()) + "\nDeposit: $" + mainApp.money(ReservationService.getDepositAmount(res)));
            mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/GuestReservations.fxml", guest);

        } catch (Exception ex) {
            msgLabel.setText("Reservation failed: " + ex.getMessage());
            msgLabel.setStyle("-fx-text-fill: #C74261;");
        }
    }
}