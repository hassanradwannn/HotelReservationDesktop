import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

public class RoomBrowserController implements DashboardContentController {
    @FXML private ComboBox<RoomType> typeFilter;
    @FXML private TextField maxPrice;
    @FXML private FlowPane roomCards;
    @FXML private FlowPane amenityFiltersPane;

    private Main mainApp;
    private Guest guest;
    private final Set<String> selectedAmenities = new HashSet<>();

    @Override
    public void initData(Main mainApp, Object data) {
        this.mainApp = mainApp;
        this.guest = (Guest) data;

        setupRoomTypeCombo();
        refreshCatalogData();
        refreshRoomTypeChoices();
        buildAmenityFilters();
        loadRooms();

        mainApp.setCurrentViewRefresher(() -> {
            refreshCatalogData();
            refreshRoomTypeChoices();
            buildAmenityFilters();
            loadRooms();
        });
    }

    private void refreshCatalogData() {
        Database.loadAllRoomTypes();
        Database.loadAllAmenities();
        Database.loadAllRooms();
    }

    private void setupRoomTypeCombo() {
        typeFilter.setPromptText("Any Type");
        typeFilter.setConverter(new StringConverter<>() {
            @Override
            public String toString(RoomType roomType) {
                return roomType == null ? "" : roomType.getName();
            }

            @Override
            public RoomType fromString(String value) {
                return CatalogService.findRoomType(value);
            }
        });
        typeFilter.setCellFactory(listView -> roomTypeCell());
        typeFilter.setButtonCell(roomTypeCell());
    }

    private ListCell<RoomType> roomTypeCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(RoomType item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        };
    }

    private void refreshRoomTypeChoices() {
        String selectedName = typeFilter.getValue() == null ? null : typeFilter.getValue().getName();
        typeFilter.setItems(FXCollections.observableArrayList(Database.getRoomTypes()));
        if (selectedName != null) {
            Database.getRoomTypes().stream()
                    .filter(type -> type.getName().equals(selectedName))
                    .findFirst()
                    .ifPresent(typeFilter::setValue);
        }
    }

    private void buildAmenityFilters() {
        amenityFiltersPane.getChildren().clear();
        selectedAmenities.retainAll(Database.getAmenities().stream().map(Amenity::getName).toList());

        for (Amenity amenity : Database.getAmenities()) {
            ToggleButton pill = new ToggleButton(amenity.getName());
            pill.getStyleClass().add("amenity-pill");
            pill.setSelected(selectedAmenities.contains(amenity.getName()));

            pill.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                if (isSelected) {
                    selectedAmenities.add(amenity.getName());
                    if (!pill.getStyleClass().contains("amenity-pill-active")) {
                        pill.getStyleClass().add("amenity-pill-active");
                    }
                } else {
                    selectedAmenities.remove(amenity.getName());
                    pill.getStyleClass().remove("amenity-pill-active");
                }
                loadRooms();
            });

            if (pill.isSelected() && !pill.getStyleClass().contains("amenity-pill-active")) {
                pill.getStyleClass().add("amenity-pill-active");
            }
            amenityFiltersPane.getChildren().add(pill);
        }
    }

    @FXML
    private void loadRooms() {
        Database.refreshReservationsFromDatabase();
        roomCards.getChildren().clear();

        List<String> requiredAmens = new ArrayList<>(selectedAmenities);
        String selectedTypeName = typeFilter.getValue() == null ? null : typeFilter.getValue().getName();

        List<RoomType> types = Database.getRoomTypes().stream()
                .filter(t -> selectedTypeName == null || t.getName().equals(selectedTypeName))
                .filter(t -> {
                    if (maxPrice.getText().trim().isEmpty()) {
                        return true;
                    }
                    try {
                        return t.getPricePerNight() <= Double.parseDouble(maxPrice.getText().trim());
                    } catch (Exception e) {
                        return true;
                    }
                })
                .filter(t -> {
                    if (requiredAmens.isEmpty()) {
                        return true;
                    }
                    return Database.getRooms().stream()
                            .filter(r -> r.getRoomType().getName().equals(t.getName()))
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
        imageText.setWrapText(true);
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
            mainApp.setSelectedRoomForReservation(null);
            mainApp.setSelectedRoomTypeForReservation(type);
            mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/MakeReservation.fxml", guest);
        });

        bottomRow.getChildren().addAll(price, hSpacer, reserveBtn);
        details.getChildren().addAll(roomTitle, capacity, vSpacer, bottomRow);

        card.getChildren().addAll(imageBox, details);
        return card;
    }
}
