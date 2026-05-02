import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;

public class GuestHomeController implements DashboardContentController {

    @FXML private VBox heroArea;
    @FXML private VBox searchArea;
    @FXML private DatePicker checkInPicker;
    @FXML private DatePicker checkOutPicker;
    @FXML private Spinner<Integer> guestsSpinner;
    @FXML private TextField maxPriceField;
    @FXML private ComboBox<RoomType> typeCombo;
    @FXML private FlowPane amenityPillsPane;
    @FXML private Label msgLabel;
    @FXML private VBox resultsArea;
    @FXML private VBox resultsPane;
    @FXML private HBox quickCardsArea;
    @FXML private Label heroSubLabel;
    @FXML private Label summaryCheckInLabel;
    @FXML private Label summaryCheckOutLabel;
    @FXML private Label summaryGuestsLabel;
    @FXML private Label summaryBudgetLabel;
    @FXML private Label summaryTypeLabel;
    @FXML private Label summaryAmenitiesLabel;
    @FXML private Label resultsCountLabel;
    @FXML private Label heroDateLabel;

    private Main mainApp;
    private Guest guest;
    private final Set<String> selectedAmenities = new HashSet<>();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void initData(Main mainApp, Object data) {
        this.mainApp = mainApp;

        ReservationSearchContext context = null;
        if (data instanceof ReservationSearchContext searchContext) {
            context = searchContext;
            this.guest = searchContext.getGuest();
        } else {
            this.guest = (Guest) data;
        }

        heroSubLabel.setText("Welcome back, " + guest.getUsername() + ". Begin curating your stay.");
        updateHeroDate();

        setupDatePickerFormat(checkInPicker);
        setupDatePickerFormat(checkOutPicker);
        setupBlackoutDates();
        setupRoomTypeCombo();

        guestsSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 2)
        );

        buildAmenityPills();

        mainApp.setCurrentViewRefresher(() -> {
            updateHeroDate();
            buildAmenityPills();
            setupRoomTypeCombo();
        });

        if (context != null) {
            applySearchContext(context);
            handleSearch();
        }
    }

    private void updateHeroDate() {
        heroDateLabel.setText("Date: " + SystemTime.getToday().format(FMT));
    }

    private void setupRoomTypeCombo() {
        typeCombo.setItems(FXCollections.observableArrayList(Database.getRoomTypes()));
        typeCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(RoomType roomType) {
                return roomType == null ? "" : roomType.getName();
            }

            @Override
            public RoomType fromString(String text) {
                return Database.getRoomTypes().stream()
                        .filter(type -> type.getName().equals(text))
                        .findFirst()
                        .orElse(null);
            }
        });

        typeCombo.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(RoomType item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
        typeCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(RoomType item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
    }

    private void setupDatePickerFormat(DatePicker picker) {
        picker.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                return date == null ? "" : FMT.format(date);
            }

            @Override
            public LocalDate fromString(String text) {
                if (text == null || text.trim().isEmpty()) {
                    return null;
                }
                return LocalDate.parse(text.trim(), FMT);
            }
        });
    }

    private void setupBlackoutDates() {
        checkInPicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(SystemTime.getToday()));
            }
        });

        checkInPicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                return;
            }

            checkOutPicker.setDayCellFactory(picker -> new DateCell() {
                @Override
                public void updateItem(LocalDate date, boolean empty) {
                    super.updateItem(date, empty);
                    setDisable(empty || date.isBefore(newVal.plusDays(1)));
                }
            });

            if (checkOutPicker.getValue() != null && checkOutPicker.getValue().isBefore(newVal.plusDays(1))) {
                checkOutPicker.setValue(null);
            }
        });
    }

    private void applySearchContext(ReservationSearchContext context) {
        checkInPicker.setValue(context.getCheckIn());
        checkOutPicker.setValue(context.getCheckOut());
        guestsSpinner.getValueFactory().setValue(context.getGuests());
        maxPriceField.setText(context.getMaxPrice() == null ? "" : formatWholeMoney(context.getMaxPrice()));
        typeCombo.setValue(findRoomType(context.getSearchRoomType()));

        selectedAmenities.clear();
        selectedAmenities.addAll(context.getRequestedAmenities().stream().map(Amenity::getName).toList());
        buildAmenityPills();
    }

    private RoomType findRoomType(RoomType roomType) {
        if (roomType == null) {
            return null;
        }
        return Database.getRoomTypes().stream()
                .filter(type -> type.getName().equals(roomType.getName()))
                .findFirst()
                .orElse(roomType);
    }

    private void buildAmenityPills() {
        amenityPillsPane.getChildren().clear();
        selectedAmenities.retainAll(
                Database.getAmenities().stream().map(Amenity::getName).toList()
        );

        for (Amenity amenity : Database.getAmenities()) {
            ToggleButton pill = new ToggleButton(amenity.getName());
            pill.getStyleClass().add("amenity-pill");
            pill.setSelected(selectedAmenities.contains(amenity.getName()));
            if (pill.isSelected()) {
                pill.getStyleClass().add("amenity-pill-active");
            }

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
            });

            amenityPillsPane.getChildren().add(pill);
        }
    }

    @FXML
    private void handleSearch() {
        msgLabel.setText("");
        Database.refreshReservationsFromDatabase();

        try {
            LocalDate checkIn = checkInPicker.getValue();
            if (checkIn == null) {
                checkIn = SystemTime.getToday();
                checkInPicker.setValue(checkIn);
            }

            LocalDate checkOut = checkOutPicker.getValue();
            if (checkOut == null) {
                msgLabel.setText("Please enter a check-out date.");
                return;
            }

            if (!ReservationService.isDateRangeValid(checkIn, checkOut)) {
                msgLabel.setText("Invalid dates. Check-out must be after check-in and not in the past.");
                return;
            }

            int numGuests = guestsSpinner.getValue();
            RoomType selectedType = typeCombo.getValue();
            List<Amenity> amenityFilter = getSelectedAmenityObjects();
            Double maxPrice;
            try {
                maxPrice = parseBudget();
            } catch (NumberFormatException ex) {
                msgLabel.setText("Budget must be a valid number.");
                return;
            }

            List<RoomType> typesToSearch = selectedType != null
                    ? List.of(selectedType)
                    : new ArrayList<>(Database.getRoomTypes());

            List<Room> available = new ArrayList<>();
            for (RoomType type : typesToSearch) {
                if (type.getCapacity() < numGuests) {
                    continue;
                }
                if (maxPrice != null && type.getPricePerNight() > maxPrice) {
                    continue;
                }
                available.addAll(ReservationService.searchAvailableRooms(
                        checkIn, checkOut, type, numGuests, amenityFilter));
            }

            GuestPreferenceRanker.sortRoomsByGuestPreferences(available, guest);

            if (available.isEmpty()) {
                msgLabel.setText("No rooms available for the selected criteria.");
                showResults(false);
                return;
            }

            buildRoomTypeResults(available, checkIn, checkOut, numGuests, amenityFilter, maxPrice, selectedType);
            showResults(true);
        } catch (RuntimeException ex) {
            msgLabel.setText("Use date format DD/MM/YYYY (e.g. 15/06/2026).");
        }
    }

    private List<Amenity> getSelectedAmenityObjects() {
        List<Amenity> amenityFilter = new ArrayList<>();
        for (String name : selectedAmenities) {
            Amenity amenity = CatalogService.findAmenity(name);
            if (amenity != null) {
                amenityFilter.add(amenity);
            }
        }
        return amenityFilter;
    }

    private Double parseBudget() {
        String budgetText = maxPriceField.getText().trim();
        if (budgetText.isEmpty()) {
            return null;
        }
        return Double.parseDouble(budgetText);
    }

    private void buildRoomTypeResults(
            List<Room> rooms,
            LocalDate checkIn,
            LocalDate checkOut,
            int numGuests,
            List<Amenity> amenityFilter,
            Double maxPrice,
            RoomType selectedType) {
        resultsPane.getChildren().clear();
        updateResultsSummary(rooms, checkIn, checkOut, numGuests, amenityFilter, maxPrice, selectedType);

        Map<String, List<Room>> roomsByType = new LinkedHashMap<>();
        for (Room room : rooms) {
            roomsByType.computeIfAbsent(room.getRoomType().getName(), key -> new ArrayList<>()).add(room);
        }

        int rowIndex = 0;
        for (List<Room> typeRooms : roomsByType.values()) {
            RoomType roomType = typeRooms.get(0).getRoomType();
            Room pricedRoom = typeRooms.get(0);

            HBox row = new HBox(0);
            row.getStyleClass().add("room-type-result-row");
            row.setMinHeight(196);
            row.setPrefHeight(196);

            StackPane visual = new StackPane();
            visual.getStyleClass().addAll("room-type-result-visual", "room-type-result-visual-2");
            visual.setMinWidth(226);
            visual.setPrefWidth(226);
            visual.setMaxWidth(226);
            Label typeIcon = new Label(roomTypeIcon(roomType.getName()));
            typeIcon.setStyle(roomTypeIconStyle(roomType.getName()));
            visual.getChildren().add(typeIcon);

            VBox details = new VBox(12);
            details.getStyleClass().add("room-type-result-details");
            details.setPadding(new Insets(32, 42, 24, 42));
            HBox.setHgrow(details, Priority.ALWAYS);

            HBox titleRow = new HBox(16);
            titleRow.setAlignment(Pos.TOP_LEFT);

            Label title = new Label(roomType.getName());
            title.getStyleClass().add("room-type-result-title");
            title.setWrapText(true);
            HBox.setHgrow(title, Priority.ALWAYS);

            Label availability = new Label(typeRooms.size() + " ROOMS");
            availability.getStyleClass().add("room-count-badge");
            availability.setMinWidth(146);
            availability.setAlignment(Pos.CENTER);
            titleRow.getChildren().addAll(title, availability);

            Label description = new Label(getRoomTypeDescription(roomType));
            description.getStyleClass().add("room-type-result-description");
            description.setWrapText(true);

            FlowPane amenityChips = new FlowPane(10, 8);
            amenityChips.getChildren().addAll(createAmenityChipLabels(typeRooms));

            Region spacer = new Region();
            VBox.setVgrow(spacer, Priority.ALWAYS);

            VBox pricePanel = new VBox(0);
            pricePanel.getStyleClass().add("room-type-result-price-panel");
            pricePanel.setMinWidth(202);
            pricePanel.setPrefWidth(202);
            pricePanel.setMaxWidth(202);

            VBox priceBox = new VBox(0);
            priceBox.getStyleClass().add("room-type-result-price-box");
            priceBox.setAlignment(Pos.CENTER);
            VBox.setVgrow(priceBox, Priority.ALWAYS);

            Label price = new Label("$" + formatWholeMoney(roomType.getPricePerNight()));
            price.getStyleClass().add("room-type-result-price");
            Label perNight = new Label("per night");
            perNight.getStyleClass().add("room-type-result-price-sub");
            Label total = new Label("Total: $" + formatWholeMoney(calculateStayTotal(pricedRoom, checkIn, checkOut)));
            total.getStyleClass().add("room-type-result-total");
            VBox.setMargin(total, new Insets(14, 0, 0, 0));
            priceBox.getChildren().addAll(price, perNight, total);

            Button reserveBtn = new Button("RESERVE");
            reserveBtn.getStyleClass().add("room-type-result-reserve-btn");
            reserveBtn.setMaxWidth(Double.MAX_VALUE);
            reserveBtn.setPrefHeight(72);
            reserveBtn.setOnAction(e -> {
                mainApp.setSelectedRoomForReservation(null);
                mainApp.setSelectedRoomTypeForReservation(roomType);
                ReservationSearchContext context = new ReservationSearchContext(
                        guest, roomType, checkIn, checkOut, numGuests, amenityFilter, selectedType, maxPrice);
                mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/MakeReservation.fxml", context);
            });

            pricePanel.getChildren().addAll(priceBox, reserveBtn);
            details.getChildren().addAll(titleRow, description, amenityChips, spacer);
            row.getChildren().addAll(visual, details, pricePanel);
            resultsPane.getChildren().add(row);
            rowIndex++;
        }
    }

    private void updateResultsSummary(
            List<Room> rooms,
            LocalDate checkIn,
            LocalDate checkOut,
            int numGuests,
            List<Amenity> amenityFilter,
            Double maxPrice,
            RoomType selectedType) {
        summaryCheckInLabel.setText(checkIn.format(FMT));
        summaryCheckOutLabel.setText(checkOut.format(FMT));
        summaryGuestsLabel.setText(numGuests + " " + (numGuests == 1 ? "guest" : "guests"));
        summaryBudgetLabel.setText(maxPrice == null ? "ANY" : "$" + formatWholeMoney(maxPrice));
        summaryTypeLabel.setText(selectedType == null ? "ANY" : selectedType.getName().toUpperCase());
        summaryAmenitiesLabel.setText(amenityFilter == null || amenityFilter.isEmpty()
                ? "ANY"
                : amenityFilter.size() + " " + (amenityFilter.size() == 1 ? "Amenity" : "Amenities"));
        resultsCountLabel.setText(rooms.size() + " rooms available");
    }

    private List<Label> createAmenityChipLabels(List<Room> rooms) {
        List<String> amenityNames = rooms.stream()
                .flatMap(room -> room.getAmenities().stream())
                .map(Amenity::getName)
                .distinct()
                .sorted((left, right) -> Boolean.compare(
                        GuestPreferenceRanker.isPreferredAmenity(guest, right),
                        GuestPreferenceRanker.isPreferredAmenity(guest, left)))
                .limit(4)
                .toList();

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
            return "The crown jewel of the hotel. Grand parlor, two dressing rooms, rooftop terrace, gym membership, and jacuzzi.";
        }
        return "A carefully appointed suite with refined finishes, attentive service, and selected hotel amenities.";
    }

    private String roomTypeIcon(String name) {
        String n = name.toLowerCase();
        if (n.contains("suite") || n.contains("grand") || n.contains("alpine")) return "✦";
        if (n.contains("penthouse") || n.contains("gustave")) return "❧";
        if (n.contains("deluxe") || n.contains("lobby")) return "◆";
        if (n.contains("classic") || n.contains("mendle")) return "⊙";
        return "◈";
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

    private double calculateStayTotal(Room room, LocalDate checkIn, LocalDate checkOut) {
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

    private void showResults(boolean show) {
        heroArea.setVisible(!show);
        heroArea.setManaged(!show);
        searchArea.setVisible(!show);
        searchArea.setManaged(!show);
        quickCardsArea.setVisible(!show);
        quickCardsArea.setManaged(!show);
        resultsArea.setVisible(show);
        resultsArea.setManaged(show);
    }

    @FXML
    private void handleModifySearch() {
        showResults(false);
    }

    @FXML private void showProfile()      { mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/GuestProfile.fxml", guest); }
    @FXML private void showReservations() { mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/GuestReservations.fxml", guest); }
    @FXML private void showDeposit()      { mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/PayDeposit.fxml", guest); }
    @FXML private void showCancel()       { mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/CancelReservation.fxml", guest); }
    @FXML private void showTime()         { mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/AdvanceTime.fxml", null); }
    @FXML private void showChat()         { mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/Chat.fxml", guest); }
    @FXML private void doLogout()         { mainApp.showLoginScreen(); }
}
