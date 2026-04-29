import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class RoomDetailsController implements DashboardContentController {

    @FXML private Label titleLabel;
    @FXML private Label imageTextLabel;
    @FXML private Label detailsLabel;

    private Main mainApp;
    private Guest guest;

    @Override
    public void initData(Main mainApp, Object data) {
        this.mainApp = mainApp;
        this.guest = (Guest) data;
        Room room = mainApp.getSelectedRoomForReservation();

        titleLabel.setText("Room " + room.getRoomNumber());
        imageTextLabel.setText(room.getRoomType().getName() + "\nLuxury Suite Preview");

        detailsLabel.setText(
                "Room Number: " + room.getRoomNumber()
                        + "\nRoom Type: " + room.getRoomType().getName()
                        + "\nCapacity: " + room.getRoomType().getCapacity()
                        + "\nPrice per night: $" + mainApp.money(room.getRoomType().getPricePerNight())
                        + "\nAmenities: " + room.getAmenities()
        );
    }

    @FXML
    private void handleBack() {
        mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/RoomBrowser.fxml", guest);
    }

    @FXML
    private void handleReserve() {
        mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/MakeReservation.fxml", guest);
    }
}