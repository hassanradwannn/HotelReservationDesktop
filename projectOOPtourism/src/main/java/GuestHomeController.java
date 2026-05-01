import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

public class GuestHomeController implements DashboardContentController {

    @FXML private DatePicker checkInPicker;
    @FXML private DatePicker checkOutPicker;
    @FXML private Spinner<Integer> guestsSpinner;
    @FXML private TextField maxPriceField;
    @FXML private ComboBox<RoomType> typeCombo;
    @FXML private FlowPane amenityPillsPane;
    @FXML private Label msgLabel;
    @FXML private VBox heroArea;
    @FXML private VBox resultsArea;
    @FXML private VBox resultsPane;
    @FXML private HBox quickCardsArea;
    @FXML private Label heroSubLabel;
    @FXML private Label summaryCheckInLabel;
    @FXML private Label summaryCheckOutLabel;
    @FXML private Label summaryGuestsLabel;
    @FXML private Label summaryPriceLabel;
    @FXML private Label summaryTypeLabel;
    @FXML private Label summaryAmenitiesLabel;
    @FXML private Label resultsCountLabel;

    private Main mainApp;
    private Guest guest;
    private final Set<String> selectedAmenities = new HashSet<>();

    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("d/M/yyyy");
    private static final DateTimeFormatter SUMMARY_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final List<DateTimeFormatter> ACCEPTED_DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("d-M-yyyy"),
            DateTimeFormatter.ISO_LOCAL_DATE
    );

    @Override
    public void initData(Main mainApp, Object data) {
        this.mainApp = mainApp;
        ReservationSearchContext returnContext = null;
        if (data instanceof ReservationSearchContext context) {
            returnContext = context;
            this.guest = context.getGuest();
        } else {
            this.guest = (Guest) data;
        }

        if (guest != null) {
            heroSubLabel.setText("Welcome back, " + guest.getUsername() + ". Begin curating your stay.");
        }

        setupRoomTypeCombo();
        setupDatePickers();
        refreshRoomTypeChoices();
        applySearchContext(returnContext);
        buildAmenityPills();

        mainApp.setCurrentViewRefresher(() -> {
            refreshCatalogData();
            refreshDatePickers();
            refreshRoomTypeChoices();
            buildAmenityPills();
        });

        if (returnContext != null) {
            handleSearch();
        }
    }

    private void applySearchContext(ReservationSearchContext context) {
        if (context == null) {
            checkInPicker.setValue(SystemTime.getToday());
            return;
        }

        checkInPicker.setValue(context.getCheckIn());
        checkOutPicker.setValue(context.getCheckOut());
        if (guestsSpinner.getValueFactory() != null) {
            guestsSpinner.getValueFactory().setValue(context.getGuests());
        }
        if (context.getMaxPrice() == null) {
            maxPriceField.clear();
        } else {
            maxPriceField.setText(formatWholeMoney(context.getMaxPrice()));
        }

        selectedAmenities.clear();
        selectedAmenities.addAll(context.getRequestedAmenities().stream().map(Amenity::getName).toList());

        RoomType searchType = context.getSearchRoomType();
        if (searchType == null) {
            typeCombo.setValue(null);
        } else {
            Database.getRoomTypes().stream()
                    .filter(type -> type.getName().equals(searchType.getName()))
                    .findFirst()
                    .ifPresentOrElse(typeCombo::setValue, () -> typeCombo.setValue(searchType));
        }
    }

    private void refreshCatalogData() {
        Database.loadAllRoomTypes();
        Database.loadAllAmenities();
        Database.loadAllRooms();
    }

    private void setupRoomTypeCombo() {
        typeCombo.setPromptText("Any type");
        typeCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(RoomType roomType) {
                return roomType == null ? "" : roomType.getName();
            }

            @Override
            public RoomType fromString(String value) {
                return CatalogService.findRoomType(value);
            }
        });
        typeCombo.setCellFactory(listView -> roomTypeCell());
        typeCombo.setButtonCell(roomTypeCell());
    }

    private void setupDatePickers() {
        setupDatePickerFormat(checkInPicker);
        setupDatePickerFormat(checkOutPicker);

        checkInPicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(SystemTime.getToday()));
            }
        });

        checkInPicker.valueProperty().addListener((obs, oldValue, newValue) -> {
            updateCheckOutDateRules();
            if (newValue != null
                    && checkOutPicker.getValue() != null
                    && !checkOutPicker.getValue().isAfter(newValue)) {
                checkOutPicker.setValue(null);
            }
        });
        updateCheckOutDateRules();
    }

    private void setupDatePickerFormat(DatePicker picker) {
        picker.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                return date == null ? "" : date.format(DISPLAY_FMT);
            }

            @Override
            public LocalDate fromString(String value) {
                if (value == null || value.isBlank()) {
                    return null;
                }

                for (DateTimeFormatter formatter : ACCEPTED_DATE_FORMATS) {
                    try {
                        return LocalDate.parse(value.trim(), formatter);
                    } catch (DateTimeParseException ignored) {
                    }
                }
                return null;
            }
        });
    }

    private void updateCheckOutDateRules() {
        LocalDate earliestCheckOut = checkInPicker.getValue() == null
                ? SystemTime.getToday().plusDays(1)
                : checkInPicker.getValue().plusDays(1);

        checkOutPicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(earliestCheckOut));
            }
        });
    }

    private void refreshDatePickers() {
        LocalDate today = SystemTime.getToday();
        if (checkInPicker.getValue() == null || checkInPicker.getValue().isBefore(today)) {
            checkInPicker.setValue(today);
        }
        updateCheckOutDateRules();
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
        String selectedName = typeCombo.getValue() == null ? null : typeCombo.getValue().getName();
        typeCombo.setItems(FXCollections.observableArrayList(Database.getRoomTypes()));
        if (selectedName != null) {
            Database.getRoomTypes().stream()
                    .filter(type -> type.getName().equals(selectedName))
                    .findFirst()
                    .ifPresent(typeCombo::setValue);
        }
    }

    private void buildAmenityPills() {
        amenityPillsPane.getChildren().clear();
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
            });

            if (pill.isSelected() && !pill.getStyleClass().contains("amenity-pill-active")) {
                pill.getStyleClass().add("amenity-pill-active");
            }
            amenityPillsPane.getChildren().add(pill);
        }
    }

    @FXML
    private void handleSearch() {
        msgLabel.setText("");
        refreshCatalogData();
        refreshRoomTypeChoices();
        buildAmenityPills();
        Database.refreshReservationsFromDatabase();

        LocalDate checkIn = checkInPicker.getValue();
        LocalDate checkOut = checkOutPicker.getValue();

        if (checkIn == null || checkOut == null) {
            msgLabel.setText("Please choose check-in and check-out dates.");
            return;
        }

        if (!ReservationService.isDateRangeValid(checkIn, checkOut)) {
            msgLabel.setText("Invalid dates. Check-out must be after check-in and not in the past.");
            return;
        }

        int numGuests = guestsSpinner.getValue();
        RoomType selectedType = typeCombo.getValue();

        List<Amenity> amenityFilter = new ArrayList<>();
        for (String name : selectedAmenities) {
            Amenity amenity = CatalogService.findAmenity(name);
            if (amenity != null) {
                amenityFilter.add(amenity);
            }
        }

        Double maxPrice = null;
        String priceText = maxPriceField.getText().trim();
        if (!priceText.isEmpty()) {
            try {
                maxPrice = Double.parseDouble(priceText);
            } catch (NumberFormatException ignored) {
            }
        }
        final Double maxPriceFinal = maxPrice;

        List<RoomType> typesToSearch = selectedType != null
                ? List.of(selectedType)
                : Database.getRoomTypes();

        List<Room> available = new ArrayList<>();
        for (RoomType type : typesToSearch) {
            if (type.getCapacity() < numGuests) {
                continue;
            }
            if (maxPriceFinal != null && type.getPricePerNight() > maxPriceFinal) {
                continue;
            }
            available.addAll(ReservationService.searchAvailableRooms(
                    checkIn, checkOut, type, numGuests, amenityFilter));
        }

        if (available.isEmpty()) {
            msgLabel.setText("No rooms available for the selected criteria.");
            showResults(false);
            return;
        }

        buildRoomTypeResults(available, checkIn, checkOut, numGuests, amenityFilter, maxPriceFinal, selectedType);
        showResults(true);
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

            HBox row = new HBox(0);
            row.getStyleClass().add("room-type-result-row");
            row.setMinHeight(196);
            row.setPrefHeight(196);

            StackPane visual = new StackPane();
            visual.getStyleClass().addAll("room-type-result-visual", "room-type-result-visual-" + (rowIndex % 4));
            visual.setMinWidth(226);
            visual.setPrefWidth(226);
            visual.setMaxWidth(226);
            Region marker = new Region();
            marker.getStyleClass().add("room-type-result-marker");
            if (rowIndex % 4 == 1) {
                marker.getStyleClass().add("room-type-result-marker-diamond");
                marker.setRotate(45);
            } else if (rowIndex % 4 == 2) {
                marker.getStyleClass().add("room-type-result-marker-star");
            } else if (rowIndex % 4 == 3) {
                marker.getStyleClass().add("room-type-result-marker-cluster");
            }
            visual.getChildren().add(marker);

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

            Room pricedRoom = typeRooms.get(0);

            Label price = new Label("$" + formatWholeMoney(roomType.getPricePerNight()));
            price.getStyleClass().add("room-type-result-price");
            Label perNight = new Label("per night");
            perNight.getStyleClass().add("room-type-result-price-sub");
            Label total = new Label("Total: $" + formatWholeMoney(calculateStayTotal(pricedRoom, checkIn, checkOut)));
            total.getStyleClass().add("room-type-result-total");
            VBox.setMargin(total, new Insets(14, 0, 0, 0));
            priceBox.getChildren().addAll(price, perNight, total);

            Button bookBtn = new Button("RESERVE");
            bookBtn.getStyleClass().add("room-type-result-reserve-btn");
            bookBtn.setMaxWidth(Double.MAX_VALUE);
            bookBtn.setPrefHeight(72);

            bookBtn.setOnAction(e -> {
                mainApp.setSelectedRoomForReservation(null);
                mainApp.setSelectedRoomTypeForReservation(roomType);
                ReservationSearchContext context = new ReservationSearchContext(
                        guest, roomType, checkIn, checkOut, numGuests, amenityFilter, selectedType, maxPrice);
                mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/MakeReservation.fxml", context);
            });

            pricePanel.getChildren().addAll(priceBox, bookBtn);
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
        summaryCheckInLabel.setText(checkIn.format(SUMMARY_FMT));
        summaryCheckOutLabel.setText(checkOut.format(SUMMARY_FMT));
        summaryGuestsLabel.setText(numGuests + " " + (numGuests == 1 ? "guest" : "guests"));
        summaryPriceLabel.setText(maxPrice == null ? "ANY" : "$" + formatWholeMoney(maxPrice));
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
                .limit(4)
                .toList();

        if (amenityNames.isEmpty()) {
            amenityNames = List.of("Amenities vary by room");
        }

        List<Label> chips = new ArrayList<>();
        for (String amenityName : amenityNames) {
            Label chip = new Label(amenityName);
            chip.getStyleClass().add("room-type-result-amenity-chip");
            chips.add(chip);
        }
        return chips;
    }

    private String getRoomTypeDescription(RoomType roomType) {
        String name = roomType.getName().toLowerCase();
        if (name.contains("mendle")) {
            return "Cozy single room with writing desk, garden-facing window, Smart TV, and high-speed WiFi included.";
        }
        if (name.contains("lobby")) {
            return "Elevated classic with king bed, reading alcove, copper bath fixtures, and a view of the inner courtyard fountain.";
        }
        if (name.contains("alpine")) {
            return "Spacious two-room suite with fireplace, velvet chaises, a private terrace, and direct mountain views.";
        }
        if (name.contains("gustave")) {
            return "The crown jewel of the hotel. Grand parlor, two dressing rooms, rooftop terrace, and gym membership.";
        }
        return "A carefully appointed suite with refined finishes, attentive service, and selected hotel amenities.";
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
        quickCardsArea.setVisible(!show);
        quickCardsArea.setManaged(!show);
        resultsArea.setVisible(show);
        resultsArea.setManaged(show);
    }

    @FXML
    private void handleModifySearch() {
        showResults(false);
    }

    @FXML private void showProfile() { mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/GuestProfile.fxml", guest); }
    @FXML private void showReservations() { mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/GuestReservations.fxml", guest); }
    @FXML private void showDeposit() { mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/PayDeposit.fxml", guest); }
    @FXML private void showCancel() { mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/CancelReservation.fxml", guest); }
    @FXML private void showTime() { mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/AdvanceTime.fxml", null); }
    @FXML private void showChat() { mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/Chat.fxml", guest); }
    @FXML private void doLogout() { mainApp.showLoginScreen(); }
}
