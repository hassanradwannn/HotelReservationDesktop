import java.time.LocalDate;
import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class RoomDetailsController implements DashboardContentController {

    @FXML private Label titleLabel;
    @FXML private Label imageTextLabel;
    @FXML private Label detailsLabel;

    private Main mainApp;
    private Guest guest;
    private Room room;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private boolean hasGymPass;
    private List<Room> availableRooms;

    @Override
    @SuppressWarnings("unchecked")
    public void initData(Main mainApp, Object data) {
        this.mainApp = mainApp;

        // Unpack the data passed from AvailableRoomsController
        Object[] arrayData = (Object[]) data;
        this.guest = (Guest) arrayData[0];
        this.room = (Room) arrayData[1];
        this.checkIn = (LocalDate) arrayData[2];
        this.checkOut = (LocalDate) arrayData[3];
        this.hasGymPass = (Boolean) arrayData[4];
        this.availableRooms = (List<Room>) arrayData[5];

        titleLabel.setText("Room " + room.getRoomNumber());
        mainApp.setSelectedRoomTypeForReservation(null); // Clear lingering general type
        mainApp.setSelectedRoomForReservation(room); // Ensure it's set for the reserve button
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
        // Repackage the search criteria to return to the specific Available Rooms list
        Object[] bookingData = new Object[]{ guest, checkIn, checkOut, hasGymPass, availableRooms };
        mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/AvailableRooms.fxml", bookingData);
    }

    @FXML
    private void handleReserve() {
        Reservation res = ReservationService.createReservation(guest, room, checkIn, checkOut, hasGymPass);
        mainApp.alert("Reservation Created", "ID: " + res.getReservationId() + "\nTotal: $" + mainApp.money(res.getTotalPrice()) + "\nDeposit: $" + mainApp.money(ReservationService.getDepositAmount(res)));
        mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/GuestReservations.fxml", guest);
    }
}