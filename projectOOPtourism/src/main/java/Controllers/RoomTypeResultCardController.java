package Controllers;


import Utils.AppContext;
import Models.*;
import Repositories.*;
import Services.*;
import Utils.*;
import Utils.exceptions.*;
import Repositories.database.DatabaseConnection;
import Repositories.DatabaseInitializer.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

public class RoomTypeResultCardController {

    @FXML private HBox root;
    @FXML private StackPane visual;
    @FXML private Label typeIconLabel;
    @FXML private Label titleLabel;
    @FXML private Label availabilityLabel;
    @FXML private Label descriptionLabel;
    @FXML private FlowPane amenityChips;
    @FXML private Label priceLabel;
    @FXML private Label totalLabel;
    @FXML private Button reserveButton;

    private AppContext mainApp;
    private Guest guest;
    private List<Room> typeRooms;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private int numGuests;
    private List<Amenity> amenityFilter;
    private RoomType selectedType;
    private Double maxPrice;

    public void setData(
            AppContext mainApp,
            Guest guest,
            List<Room> typeRooms,
            LocalDate checkIn,
            LocalDate checkOut,
            int numGuests,
            List<Amenity> amenityFilter,
            RoomType selectedType,
            Double maxPrice) {
        this.mainApp = mainApp;
        this.guest = guest;
        this.typeRooms = typeRooms;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.numGuests = numGuests;
        this.amenityFilter = amenityFilter;
        this.selectedType = selectedType;
        this.maxPrice = maxPrice;

        render();
    }

    private void render() {
        RoomType roomType = typeRooms.get(0).getRoomType();
        Room pricedRoom = typeRooms.get(0);
        boolean hasPreferenceMatch = typeRooms.stream()
                .anyMatch(room -> GuestPreferenceRanker.getPreferenceMatchScore(room, guest) > 0);

        root.getStyleClass().remove("preferred-room-result-row");
        if (hasPreferenceMatch) {
            root.getStyleClass().add("preferred-room-result-row");
        }

        if (!visual.getStyleClass().contains("room-type-result-visual-2")) {
            visual.getStyleClass().add("room-type-result-visual-2");
        }

        typeIconLabel.setText(roomTypeIcon(roomType.getName()));
        typeIconLabel.setStyle(roomTypeIconStyle(roomType.getName()));
        titleLabel.setText(roomType.getName());
        availabilityLabel.setText(typeRooms.size() + " ROOMS");
        descriptionLabel.setText(getRoomTypeDescription(roomType));

        amenityChips.getChildren().clear();
        amenityChips.getChildren().addAll(createAmenityChipLabels(typeRooms));

        priceLabel.setText("$" + formatWholeMoney(roomType.getPricePerNight()));
        totalLabel.setText("Total: $" + formatWholeMoney(calculateStayTotal(pricedRoom)));

        reserveButton.setOnAction(e -> {
            mainApp.setSelectedRoomForReservation(null);
            mainApp.setSelectedRoomTypeForReservation(roomType);
            ReservationSearchContext context = new ReservationSearchContext(
                    guest, roomType, checkIn, checkOut, numGuests, amenityFilter, selectedType, maxPrice);
            mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/MakeReservation.fxml", context);
        });
    }

    private List<Label> createAmenityChipLabels(List<Room> rooms) {
        List<String> amenityNames = CatalogService.uniqueFilterAmenityNames(rooms.stream()
                .flatMap(room -> {
                    List<String> names = new ArrayList<>();
                    names.add(room.getViewName());
                    names.addAll(room.getAmenities().stream().map(Amenity::getName).toList());
                    return names.stream();
                })
                .toList());
        amenityNames.sort((left, right) -> Integer.compare(pillPriority(left), pillPriority(right)));

        if (amenityNames.isEmpty()) {
            amenityNames = List.of("Amenities vary by room");
        }

        List<Label> chips = new ArrayList<>();
        for (String amenityName : amenityNames) {
            Label chip = new Label(amenityName);
            chip.getStyleClass().add("room-type-result-amenity-chip");
            if (GuestPreferenceRanker.isPreferredAmenity(guest, amenityName)) {
                chip.getStyleClass().add("preferred-amenity-chip");
            }
            chips.add(chip);
        }
        return chips;
    }

    private String getRoomTypeDescription(RoomType roomType) {
        String name = roomType.getName().toLowerCase();
        if (name.contains("mendle")) {
            return "Cozy single room with writing desk, garden-facing window. Smart TV and high-speed WiFi included";
        }
        if (name.contains("lobby")) {
            return "Elevated classic with king bed, reading alcove, copper bath fixtures, and a view of the inner courtyard fountain.";
        }
        if (name.contains("alpine")) {
            return "Spacious two-room suite with fireplace, velvet chaises, a private terrace, and direct mountain views.";
        }
        if (name.contains("gustave")) {
            return "The crown jewel of the hotel. Grand parlor, two dressing rooms, rooftop terrace, Gym, and jacuzzi.";
        }
        return "A carefully appointed suite with refined finishes, attentive service, and selected hotel amenities.";
    }

    private boolean isViewName(String name) {
        return "Sea View".equalsIgnoreCase(name) || "Mountain View".equalsIgnoreCase(name);
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

    private String roomTypeIcon(String name) {
        String n = name.toLowerCase();
        if (n.contains("suite") || n.contains("grand") || n.contains("alpine")) return "\u2726";
        if (n.contains("penthouse") || n.contains("gustave")) return "\u2767";
        if (n.contains("deluxe") || n.contains("lobby")) return "\u25C6";
        if (n.contains("classic") || n.contains("mendle")) return "\u2299";
        return "\u25C8";
    }

    private String roomTypeIconStyle(String name) {
        String n = name.toLowerCase();
        String color = (n.contains("deluxe") || n.contains("lobby")) ? "#C8A97E" : "#F9F3EA";
        String size = (n.contains("classic") || n.contains("mendle")) ? "40" : "34";
        return "-fx-font-family: 'Georgia';"
                + "-fx-font-size: " + size + "px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: " + color + ";";
    }

    private double calculateStayTotal(Room room) {
        long nights = Math.max(1, ChronoUnit.DAYS.between(checkIn, checkOut));
        double total = room.getPricePerNight() * nights;
        for (Amenity amenity : room.getAmenities()) {
            total += amenity.getPrice();
        }
        return total;
    }

    private String formatWholeMoney(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return mainApp.money(value);
    }
}
