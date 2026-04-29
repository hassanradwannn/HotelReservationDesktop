import java.time.LocalDate;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;

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
            msgLabel.setTextFill(Color.web("#8F1D3F"));
        }
    }

    @FXML
    private void handleSearch() {
        try {
            if (typeBox.getValue() == null) {
                msgLabel.setText("Please select a room type.");
                msgLabel.setTextFill(Color.web("#C74261"));
                return;
            }

            int numGuests = Integer.parseInt(guestsField.getText().trim());
            LocalDate in = LocalDate.parse(checkInField.getText().trim());
            LocalDate out = LocalDate.parse(checkOutField.getText().trim());

            if (!ReservationService.isDateRangeValid(in, out)) {
                msgLabel.setText("Invalid dates. Check-out must be after check-in.");
                msgLabel.setTextFill(Color.web("#C74261"));
                return;
            }

            List<Room> available = ReservationService.searchAvailableRooms(in, out, typeBox.getValue(), numGuests);
            roomBox.setItems(FXCollections.observableArrayList(available));

            Room selectedRoom = mainApp.getSelectedRoomForReservation();
            if (selectedRoom != null && available.contains(selectedRoom)) {
                roomBox.setValue(selectedRoom);
            }

            msgLabel.setText(available.isEmpty() ? "No rooms available." : available.size() + " rooms found.");
            msgLabel.setTextFill(available.isEmpty() ? Color.web("#C74261") : Color.web("#8F1D3F"));

        } catch (NumberFormatException ex) {
            msgLabel.setText("Number of guests must be a number.");
            msgLabel.setTextFill(Color.web("#C74261"));
        } catch (Exception ex) {
            msgLabel.setText("Enter valid room type, guests, and dates YYYY-MM-DD.");
            msgLabel.setTextFill(Color.web("#C74261"));
        }
    }

    @FXML
    private void handleCreate() {
        try {
            if (roomBox.getValue() == null) {
                msgLabel.setText("Please choose a room first.");
                msgLabel.setTextFill(Color.web("#C74261"));
                return;
            }

            LocalDate in = LocalDate.parse(checkInField.getText().trim());
            LocalDate out = LocalDate.parse(checkOutField.getText().trim());

            if (!ReservationService.isDateRangeValid(in, out)) {
                msgLabel.setText("Invalid date range.");
                msgLabel.setTextFill(Color.web("#C74261"));
                return;
            }
            if (ReservationService.hasOverlappingReservation(roomBox.getValue(), in, out)) {
                msgLabel.setText("This room is already reserved during the selected dates.");
                msgLabel.setTextFill(Color.web("#C74261"));
                return;
            }
            
            Reservation res = ReservationService.createReservation(guest, roomBox.getValue(), in, out, gymBox.isSelected());

            mainApp.setSelectedRoomForReservation(null);

            mainApp.alert("Reservation Created",
                    "ID: " + res.getReservationId()
                            + "\nRoom: " + res.getRoom().getRoomNumber()
                            + "\nStatus: " + res.getStatus()
                            + "\nTotal: $" + mainApp.money(res.getTotalPrice())
                            + "\nDeposit: $" + mainApp.money(ReservationService.getDepositAmount(res)));

            // Redirect right back to their reservations view to see it
            mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/GuestReservations.fxml", guest);

        } catch (Exception ex) {
            msgLabel.setText("Reservation failed: " + ex.getMessage());
            msgLabel.setTextFill(Color.web("#C74261"));
        }
    }
}