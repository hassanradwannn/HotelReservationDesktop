import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class AvailableRoomsController implements DashboardContentController {

    @FXML private VBox roomCards;

    private Main mainApp;
    private Guest guest;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private boolean hasGymPass;
    private List<Room> availableRooms;
    private ReservationSearchContext searchContext;

    @Override
    @SuppressWarnings("unchecked")
    public void initData(Main mainApp, Object data) {
        this.mainApp = mainApp;
        
        // Unpack the search criteria passed from MakeReservationController
        Object[] bookingData = (Object[]) data;
        this.guest = (Guest) bookingData[0];
        this.checkIn = (LocalDate) bookingData[1];
        this.checkOut = (LocalDate) bookingData[2];
        this.hasGymPass = (Boolean) bookingData[3];
        this.availableRooms = (List<Room>) bookingData[4];
        this.searchContext = bookingData.length > 5 && bookingData[5] instanceof ReservationSearchContext context
                ? context
                : null;

        loadRooms();
    }

    private void loadRooms() {
        if (roomCards != null) {
            roomCards.getChildren().clear();
        }

        for (Room room : availableRooms) {
            if (roomCards != null) {
                roomCards.getChildren().add(createRoomCard(room));
            }
        }
    }

    private HBox createRoomCard(Room room) {
        HBox card = new HBox(20);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(15));
        card.setMaxWidth(Double.MAX_VALUE);
        card.getStyleClass().add("room-card");

        StackPane imageBox = new StackPane();
        imageBox.getStyleClass().add("room-image-box");
        imageBox.setMinWidth(160);
        imageBox.setMaxWidth(160);
        imageBox.setMinHeight(160);
        imageBox.setMaxHeight(160);

        Label imageText = new Label("ROOM\n" + room.getRoomNumber());
        imageText.getStyleClass().add("room-image-text");
        imageText.setAlignment(Pos.CENTER);
        imageBox.getChildren().add(imageText);

        VBox details = new VBox(7);
        details.setAlignment(Pos.CENTER_LEFT);
        details.setPadding(new Insets(10));
        details.setMinWidth(400);
        HBox.setHgrow(details, Priority.ALWAYS);
        Label roomTitle = new Label("Room " + room.getRoomNumber() + " — " + room.getRoomType().getName());
        roomTitle.getStyleClass().add("room-title");
        
        double total = Reservation.calculateTotalPrice(room, checkIn, checkOut, hasGymPass);
        
        Label price = new Label("Total: $" + mainApp.money(total));
        price.getStyleClass().add("room-price");
        
        FlowPane amenityChips = new FlowPane(10, 8);
        for (String amenityName : getDisplayAmenityNames(room)) {
            Label chip = new Label(amenityName);
            chip.getStyleClass().add("room-type-result-amenity-chip");
            amenityChips.getChildren().add(chip);
        }
        
        details.getChildren().addAll(roomTitle, price, amenityChips);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox actions = new VBox(15);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button detailsBtn = new Button("DETAILS");
        detailsBtn.setPrefSize(130, 38);
        detailsBtn.setMinWidth(130);
        detailsBtn.getStyleClass().add("outline-action-btn");
        detailsBtn.setOnAction(e -> {
            Object[] detailsData = new Object[]{ guest, room, checkIn, checkOut, hasGymPass, availableRooms, searchContext };
            mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/RoomDetails.fxml", detailsData);
        });

        Button reserveBtn = new Button("BOOK NOW");
        reserveBtn.setPrefSize(130, 38);
        reserveBtn.setMinWidth(130);
        reserveBtn.getStyleClass().add("primary-action-btn");
        reserveBtn.setOnAction(e -> {
            Reservation res = ReservationService.createReservation(guest, room, checkIn, checkOut, hasGymPass);
            mainApp.alert("Reservation Created", "ID: " + res.getReservationId() + "\nTotal: $" + mainApp.money(res.getTotalPrice()) + "\nDeposit: $" + mainApp.money(ReservationService.getDepositAmount(res)));
            mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/GuestReservations.fxml", guest);
        });
        
        actions.getChildren().addAll(detailsBtn, reserveBtn);
        card.getChildren().addAll(imageBox, details, spacer, actions);
        return card;
    }

    private List<String> getDisplayAmenityNames(Room room) {
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
        return names;
    }
}
