import java.util.List;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class RoomBrowserController implements DashboardContentController {
    @FXML private ComboBox<RoomType> typeFilter;
    @FXML private TextField maxPrice;
    @FXML private FlowPane roomCards;
    @FXML private CheckBox minibarBox;
    @FXML private CheckBox jacuzziBox;
    @FXML private CheckBox gymBox;

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

        List<String> requiredAmens = new java.util.ArrayList<>();
        if (minibarBox != null && minibarBox.isSelected()) requiredAmens.add("Mini-bar");
        if (jacuzziBox != null && jacuzziBox.isSelected()) requiredAmens.add("Jacuzzi");
        if (gymBox != null && gymBox.isSelected()) requiredAmens.add("Gym Membership");

        List<RoomType> types = Database.getRoomTypes().stream()
                .filter(t -> typeFilter.getValue() == null || t.equals(typeFilter.getValue()))
                .filter(t -> {
                    if (maxPrice.getText().trim().isEmpty()) return true;
                    try {
                        return t.getPricePerNight() <= Double.parseDouble(maxPrice.getText().trim());
                    } catch (Exception e) {
                        return true;
                    }
                })
                .filter(t -> {
                    if (requiredAmens.isEmpty()) return true;
                    // Only show this RoomType if AT LEAST ONE room of this type has all requested amenities
                    return Database.getRooms().stream()
                            .filter(r -> r.getRoomType().equals(t))
                            .anyMatch(r -> {
                                List<String> roomAmenities = r.getAmenities().stream().map(Amenity::getName).toList();
                                return roomAmenities.containsAll(requiredAmens);
                            });
                })
                .toList();

        if (types.isEmpty()) {
            Label noRooms = new Label("No room types found.");
            noRooms.getStyleClass().add("no-rooms-label");
            roomCards.getChildren().add(noRooms);
            return;
        }

        for (RoomType type : types) {
            roomCards.getChildren().add(createRoomTypeCard(type));
        }
    }

    private VBox createRoomTypeCard(RoomType type) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPrefSize(220, 360);
        card.setMaxSize(220, 360);
        card.setPadding(new Insets(0, 0, 15, 0));
        card.getStyleClass().add("room-type-card");

        StackPane imageBox = new StackPane();
        imageBox.getStyleClass().add("room-image-box");
        imageBox.setMinHeight(160);
        imageBox.setMaxHeight(160);

        Label imageText = new Label(type.getName().toUpperCase());
        imageText.getStyleClass().add("room-image-text");
        imageText.setAlignment(Pos.CENTER);
        imageBox.getChildren().add(imageText);

        VBox details = new VBox(7);
        details.setAlignment(Pos.TOP_LEFT);
        details.setPadding(new Insets(10));
        VBox.setVgrow(details, Priority.ALWAYS);
        
        Label roomTitle = new Label(type.getName());
        roomTitle.getStyleClass().add("room-title");
        roomTitle.setWrapText(true);
        Label capacity = new Label("Capacity: " + type.getCapacity() + " guests");
        capacity.getStyleClass().add("room-capacity");
        
        Region vSpacer = new Region();
        VBox.setVgrow(vSpacer, Priority.ALWAYS);

        HBox bottomRow = new HBox();
        bottomRow.setAlignment(Pos.CENTER_LEFT);
        Label price = new Label("$" + mainApp.money(type.getPricePerNight()));
        price.getStyleClass().add("room-price");
        
        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);

        Button reserveBtn = new Button("BOOK NOW");
        reserveBtn.setPrefSize(100, 38);
        reserveBtn.getStyleClass().add("primary-action-btn");
        reserveBtn.setOnAction(e -> { 
            mainApp.setSelectedRoomForReservation(null); // Clear any lingering specific room
            mainApp.setSelectedRoomTypeForReservation(type); 
            mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/MakeReservation.fxml", guest); 
        });
        
        bottomRow.getChildren().addAll(price, hSpacer, reserveBtn);
        details.getChildren().addAll(roomTitle, capacity, vSpacer, bottomRow);

        card.getChildren().addAll(imageBox, details);
        return card;
    }
}