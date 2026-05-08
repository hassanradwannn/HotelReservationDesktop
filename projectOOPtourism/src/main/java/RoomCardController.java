import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;

public class RoomCardController {

    @FXML private HBox root;
    @FXML private Label imageTextLabel;
    @FXML private Label roomTitleLabel;
    @FXML private Label priceLabel;
    @FXML private FlowPane amenityChips;
    @FXML private Button detailsButton;
    @FXML private Button reserveButton;

    private Main mainApp;
    private Guest guest;
    private Room room;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private boolean hasGymPass;
    private List<Room> availableRooms;
    private ReservationSearchContext searchContext;

    public void setData(
            Main mainApp,
            Guest guest,
            Room room,
            LocalDate checkIn,
            LocalDate checkOut,
            boolean hasGymPass,
            List<Room> availableRooms,
            ReservationSearchContext searchContext) {
        this.mainApp = mainApp;
        this.guest = guest;
        this.room = room;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.hasGymPass = hasGymPass;
        this.availableRooms = availableRooms;
        this.searchContext = searchContext;

        render();
    }

    private void render() {
        root.getStyleClass().remove("preferred-room-card");
        if (GuestPreferenceRanker.getPreferenceMatchScore(room, guest) > 0) {
            root.getStyleClass().add("preferred-room-card");
        }

        imageTextLabel.setText("ROOM\n" + room.getRoomNumber());
        roomTitleLabel.setText("Room " + room.getRoomNumber() + " - " + room.getRoomType().getName());

        double total = Reservation.calculateTotalPrice(room, checkIn, checkOut, hasGymPass);
        priceLabel.setText("Total: $" + mainApp.money(total));

        amenityChips.getChildren().clear();
        for (String amenityName : getDisplayAmenityNames()) {
            Label chip = new Label(amenityName);
            chip.getStyleClass().add("room-type-result-amenity-chip");
            if (GuestPreferenceRanker.isPreferredAmenity(guest, amenityName)) {
                chip.getStyleClass().add("preferred-amenity-chip");
            }
            amenityChips.getChildren().add(chip);
        }

        detailsButton.setOnAction(e -> {
            Object[] detailsData = new Object[]{guest, room, checkIn, checkOut, hasGymPass, availableRooms, searchContext};
            mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/RoomDetails.fxml", detailsData);
        });

        reserveButton.setOnAction(e -> {
            Reservation res = ReservationService.createReservation(guest, room, checkIn, checkOut, hasGymPass);
            mainApp.alert(
                    "Reservation Created",
                    "ID: " + res.getReservationId()
                            + "\nTotal: $" + mainApp.money(res.getTotalPrice())
                            + "\nDeposit: $" + mainApp.money(ReservationService.getDepositAmount(res)));
            mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/GuestReservations.fxml", guest);
        });
    }

    private List<String> getDisplayAmenityNames() {
        List<String> names = new ArrayList<>();
        names.add(room.getViewName());
        for (Amenity amenity : room.getAmenities()) {
            if (hasGymPass && Reservation.isGymAmenity(amenity)) {
                continue;
            }
            names.add(amenity.getName());
        }
        if (hasGymPass) {
            names.add(Reservation.GYM_PASS_NAME);
        }
        names = CatalogService.uniqueFilterAmenityNames(names);
        names.sort((left, right) -> Integer.compare(pillPriority(left), pillPriority(right)));
        return names;
    }

    private int pillPriority(String name) {
        if (GuestPreferenceRanker.isPreferredAmenity(guest, name)) {
            return 0;
        }
        if (isViewName(name)) {
            return 2;
        }
        return 1;
    }

    private boolean isViewName(String name) {
        return "Sea View".equalsIgnoreCase(name) || "Mountain View".equalsIgnoreCase(name);
    }
}
