import java.util.List;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class RoomBrowserController implements DashboardContentController {
    @FXML private ComboBox<RoomType> typeFilter;
    @FXML private TextField maxPrice;
    @FXML private VBox roomCards;

    private Main mainApp;
    private Guest guest;

    @Override
    public void initData(Main mainApp, Object data) {
        this.mainApp = mainApp;
        this.guest = (Guest) data;
        
        typeFilter.setItems(FXCollections.observableArrayList(Database.getRoomTypes()));
        
        loadRooms();
        mainApp.setCurrentViewRefresher(this::loadRooms);
    }

    @FXML
    private void loadRooms() {
        Database.refreshReservationsFromDatabase();
        roomCards.getChildren().clear();

        List<Room> rooms = Database.getRooms().stream()
                .filter(r -> ReservationService.isRoomAvailable(r, SystemTime.getToday(), SystemTime.getToday().plusDays(1)))
                .filter(r -> typeFilter.getValue() == null || r.getRoomType().equals(typeFilter.getValue()))
                .filter(r -> {
                    if (maxPrice.getText().trim().isEmpty()) return true;
                    try {
                        return r.getRoomType().getPricePerNight() <= Double.parseDouble(maxPrice.getText().trim());
                    } catch (Exception e) {
                        return true;
                    }
                })
                .toList();

        if (rooms.isEmpty()) {
            Label noRooms = new Label("No available rooms found.");
            noRooms.getStyleClass().add("no-rooms-label");
            roomCards.getChildren().add(noRooms);
            return;
        }

        for (Room room : rooms) {
            roomCards.getChildren().add(createRoomCard(room));
        }
    }

    private HBox createRoomCard(Room room) {
        HBox card = new HBox(20);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(18));
        card.getStyleClass().add("room-card");

        StackPane imageBox = new StackPane();
        imageBox.setPrefSize(170, 120);
        imageBox.getStyleClass().add("room-image-box");

        Label imageText = new Label("ROOM\n" + room.getRoomNumber());
        imageText.getStyleClass().add("room-image-text");
        imageText.setAlignment(Pos.CENTER);
        imageBox.getChildren().add(imageText);

        VBox details = new VBox(7);
        details.setPrefWidth(560);
        Label roomTitle = new Label("Room " + room.getRoomNumber() + " — " + room.getRoomType().getName());
        roomTitle.getStyleClass().add("room-title");
        Label price = new Label("Price per night: $" + mainApp.money(room.getRoomType().getPricePerNight()));
        price.getStyleClass().add("room-price");
        Label capacity = new Label("Capacity: " + room.getRoomType().getCapacity() + " guests");
        capacity.getStyleClass().add("room-capacity");
        Label amenities = new Label("Amenities: " + room.getAmenities());
        amenities.getStyleClass().add("room-amenities");
        amenities.setWrapText(true);
        details.getChildren().addAll(roomTitle, price, capacity, amenities);

        VBox actions = new VBox(10);
        actions.setAlignment(Pos.CENTER);

        Button detailsBtn = new Button("DETAILS");
        detailsBtn.setPrefSize(140, 38);
        detailsBtn.getStyleClass().add("outline-button");
        detailsBtn.setOnAction(e -> { mainApp.setSelectedRoomForReservation(room); mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/RoomDetails.fxml", guest); });

        Button reserveBtn = new Button("RESERVE");
        reserveBtn.setPrefSize(140, 38);
        reserveBtn.getStyleClass().add("main-button");
        reserveBtn.setOnAction(e -> { mainApp.setSelectedRoomForReservation(room); mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/MakeReservation.fxml", guest); });
        
        actions.getChildren().addAll(detailsBtn, reserveBtn);
        card.getChildren().addAll(imageBox, details, actions);
        return card;
    }
}