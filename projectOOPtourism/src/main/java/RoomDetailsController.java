import java.time.LocalDate;
import java.util.ArrayList;
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
    private ReservationSearchContext searchContext;

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
        this.searchContext = arrayData.length > 6 && arrayData[6] instanceof ReservationSearchContext context
                ? context
                : null;

        mainApp.setSelectedRoomTypeForReservation(null); // Clear lingering general type
        mainApp.setSelectedRoomForReservation(room); // Ensure it's set for the reserve button
        mainApp.setCurrentViewRefresher(this::refreshRoomDetails);
        renderRoomDetails();
    }

    private void refreshRoomDetails() {
        Room freshRoom = CatalogService.findRoom(room.getRoomNumber());
        if (freshRoom == null) {
            mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/GuestHome.fxml", guest);
            return;
        }
        room = freshRoom;
        mainApp.setSelectedRoomForReservation(room);
        availableRooms = availableRooms.stream()
                .map(existingRoom -> CatalogService.findRoom(existingRoom.getRoomNumber()))
                .filter(existingRoom -> existingRoom != null)
                .toList();
        refreshSearchContext();
        renderRoomDetails();
    }

    private void refreshSearchContext() {
        if (searchContext == null) {
            return;
        }

        List<Amenity> currentRequestedAmenities = new ArrayList<>();
        for (Amenity amenity : searchContext.getRequestedAmenities()) {
            Amenity fresh = CatalogService.findAmenity(amenity.getName());
            if (fresh != null) {
                currentRequestedAmenities.add(fresh);
            }
        }

        RoomType currentRoomType = CatalogService.findRoomType(searchContext.getRoomType().getName());
        RoomType currentSearchRoomType = searchContext.getSearchRoomType() == null
                ? null
                : CatalogService.findRoomType(searchContext.getSearchRoomType().getName());
        if (currentRoomType == null) {
            return;
        }

        searchContext = new ReservationSearchContext(
                guest,
                currentRoomType,
                checkIn,
                checkOut,
                searchContext.getGuests(),
                currentRequestedAmenities,
                currentSearchRoomType,
                searchContext.getMaxPrice(),
                hasGymPass);
    }

    private void renderRoomDetails() {
        titleLabel.setText("Room " + room.getRoomNumber());
        imageTextLabel.setText(room.getRoomType().getName() + "\nLuxury Suite Preview");
        String amenityText = String.join(", ", getDisplayAmenityNames());
        double total = Reservation.calculateTotalPrice(room, checkIn, checkOut, hasGymPass);
        detailsLabel.setText(
                "Room Number: " + room.getRoomNumber()
                        + "\nRoom Type: " + room.getRoomType().getName()
                        + "\nCapacity: " + room.getRoomType().getCapacity()
                        + "\nPrice per night: $" + mainApp.money(room.getRoomType().getPricePerNight())
                        + "\nAmenities: " + amenityText
                        + "\nTotal: $" + mainApp.money(total)
        );
    }

    private List<String> getDisplayAmenityNames() {
        List<String> names = new ArrayList<>();
        for (Amenity amenity : room.getAmenities()) {
            if (hasGymPass && Reservation.isGymAmenity(amenity)) {
                continue;
            }
            names.add(amenity.getName());
        }
        if (hasGymPass) {
            names.add(Reservation.GYM_PASS_NAME);
        }
        names.sort((left, right) -> Boolean.compare(
                GuestPreferenceRanker.isPreferredAmenity(guest, right),
                GuestPreferenceRanker.isPreferredAmenity(guest, left)));
        return names;
    }

    @FXML
    private void handleBack() {
        // Repackage the search criteria to return to the specific Available Rooms list
        Object[] bookingData = new Object[]{ guest, checkIn, checkOut, hasGymPass, availableRooms, searchContext };
        mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/AvailableRooms.fxml", bookingData);
    }

    @FXML
    private void handleReserve() {
        Reservation res = ReservationService.createReservation(guest, room, checkIn, checkOut, hasGymPass);
        mainApp.alert("Reservation Created", "ID: " + res.getReservationId() + "\nTotal: $" + mainApp.money(res.getTotalPrice()) + "\nDeposit: $" + mainApp.money(ReservationService.getDepositAmount(res)));
        mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/GuestReservations.fxml", guest);
    }
}
