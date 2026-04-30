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
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.util.List;

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
            noRooms.setStyle("-fx-text-fill: #2B2421; -fx-font-size: 15px;");
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
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 14; -fx-border-color: #C9AA7C; -fx-border-radius: 14; -fx-effect: dropshadow(gaussian, rgba(80,45,35,0.10), 12, 0.2, 0, 4);");

        StackPane imageBox = new StackPane();
        imageBox.setPrefSize(170, 120);
        imageBox.setStyle("-fx-background-color: linear-gradient(to bottom right, #EFE4D6, #E6A4B4); -fx-background-radius: 12; -fx-border-color: #C9AA7C; -fx-border-radius: 12;");

        Label imageText = new Label("ROOM\n" + room.getRoomNumber());
        imageText.setFont(Font.font("Georgia", FontWeight.NORMAL, 22));
        imageText.setStyle("-fx-text-fill: #2B2421;");
        imageText.setAlignment(Pos.CENTER);
        imageBox.getChildren().add(imageText);

        VBox details = new VBox(7);
        details.setPrefWidth(560);
        Label roomTitle = new Label("Room " + room.getRoomNumber() + " — " + room.getRoomType().getName());
        roomTitle.setFont(Font.font("Georgia", FontWeight.NORMAL, 22));
        roomTitle.setStyle("-fx-text-fill: #2B2421;");
        Label price = new Label("Price per night: $" + mainApp.money(room.getRoomType().getPricePerNight()));
        price.setStyle("-fx-text-fill: #8F1D3F; -fx-font-weight: bold; -fx-font-size: 14px;");
        Label capacity = new Label("Capacity: " + room.getRoomType().getCapacity() + " guests");
        capacity.setStyle("-fx-text-fill: #2B2421; -fx-font-size: 14px;");
        Label amenities = new Label("Amenities: " + room.getAmenities());
        amenities.setStyle("-fx-text-fill: #8A726B; -fx-font-size: 13px;");
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