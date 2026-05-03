import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
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
    private final Map<String, Node> roomTypeCardNodes = new LinkedHashMap<>();
    private final Map<String, RoomTypeResultCardController> roomTypeCardControllers = new LinkedHashMap<>();
    private boolean restoringSearchState;
    private Task<List<Room>> roomSearchTask;
    private int roomSearchRequestId;
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
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 1)
        );

        buildAmenityPills();

        mainApp.setCurrentViewRefresher(() -> {
            updateHeroDate();
            buildAmenityPills();
            setupRoomTypeCombo();
            if (resultsArea != null && resultsArea.isVisible()) {
                refreshVisibleSearchResults();
            }
        });

        if (context != null) {
            applySearchContext(context);
            handleSearch();
        }

        installSearchStateListeners();
        saveGuestHomeSearchContext();
    }

    private void updateHeroDate() {
        heroDateLabel.setText("Date: " + SystemTime.getToday().format(FMT));
    }

    private void setupRoomTypeCombo() {
        RoomType previousSelection = typeCombo.getValue();
        String selectedTypeName = previousSelection == null ? null : previousSelection.getName();
        List<RoomType> roomTypes = Database.getRoomTypes();
        if (roomTypes.isEmpty()) {
            return;
        }

        typeCombo.setItems(FXCollections.observableArrayList(roomTypes));
        if (selectedTypeName != null) {
            typeCombo.setValue(roomTypes.stream()
                    .filter(type -> type.getName().equals(selectedTypeName))
                    .findFirst()
                    .orElse(previousSelection));
        }
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
        restoringSearchState = true;
        try {
            checkInPicker.setValue(context.getCheckIn());
            checkOutPicker.setValue(context.getCheckOut());
            guestsSpinner.getValueFactory().setValue(context.getGuests());
            maxPriceField.setText(context.getMaxPrice() == null ? "" : formatWholeMoney(context.getMaxPrice()));
            typeCombo.setValue(findRoomType(context.getSearchRoomType()));

            selectedAmenities.clear();
            selectedAmenities.addAll(context.getRequestedAmenities().stream().map(Amenity::getName).toList());
            buildAmenityPills();
        } finally {
            restoringSearchState = false;
        }
    }

    private void installSearchStateListeners() {
        checkInPicker.valueProperty().addListener((obs, oldValue, newValue) -> saveGuestHomeSearchContext());
        checkOutPicker.valueProperty().addListener((obs, oldValue, newValue) -> saveGuestHomeSearchContext());
        guestsSpinner.valueProperty().addListener((obs, oldValue, newValue) -> saveGuestHomeSearchContext());
        maxPriceField.textProperty().addListener((obs, oldValue, newValue) -> saveGuestHomeSearchContext());
        typeCombo.valueProperty().addListener((obs, oldValue, newValue) -> saveGuestHomeSearchContext());
    }

    private void saveGuestHomeSearchContext() {
        if (mainApp == null || guest == null || restoringSearchState) {
            return;
        }

        RoomType selectedType = findRoomType(typeCombo.getValue());
        int guests = guestsSpinner.getValue() == null ? 1 : guestsSpinner.getValue();
        Double maxPrice = null;
        try {
            maxPrice = parseBudget();
        } catch (NumberFormatException ignored) {
        }

        mainApp.setGuestHomeSearchContext(new ReservationSearchContext(
                guest,
                selectedType,
                checkInPicker.getValue(),
                checkOutPicker.getValue(),
                guests,
                getSelectedAmenityObjects(),
                selectedType,
                maxPrice));
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
        List<Amenity> amenities = Database.getAmenities();
        if (amenities.isEmpty() && !selectedAmenities.isEmpty()) {
            return;
        }

        amenityPillsPane.getChildren().clear();
        selectedAmenities.retainAll(
                amenities.stream().map(Amenity::getName).toList()
        );

        for (Amenity amenity : amenities) {
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
                saveGuestHomeSearchContext();
            });

            amenityPillsPane.getChildren().add(pill);
        }
    }

    @FXML
    private void handleSearch() {
        runSearch(false);
    }

    private void refreshVisibleSearchResults() {
        runSearch(true);
    }

    private void runSearch(boolean preserveCurrentResultsOnEmpty) {
        msgLabel.setText("");

        try {
            LocalDate checkIn = checkInPicker.getValue();
            if (checkIn == null) {
                checkIn = SystemTime.getToday();
                checkInPicker.setValue(checkIn);
            }

            LocalDate checkOut = checkOutPicker.getValue();
            if (checkOut == null) {
                if (!preserveCurrentResultsOnEmpty) {
                    msgLabel.setText("Please enter a check-out date.");
                }
                return;
            }

            if (!ReservationService.isDateRangeValid(checkIn, checkOut)) {
                if (!preserveCurrentResultsOnEmpty) {
                    msgLabel.setText("Invalid dates. Check-out must be after check-in and not in the past.");
                }
                return;
            }

            int numGuests = guestsSpinner.getValue();
            RoomType selectedType = findRoomType(typeCombo.getValue());
            if (selectedType != null) {
                typeCombo.setValue(selectedType);
            }
            saveGuestHomeSearchContext();
            List<Amenity> amenityFilter = getSelectedAmenityObjects();
            Double maxPrice;
            try {
                maxPrice = parseBudget();
            } catch (NumberFormatException ex) {
                if (!preserveCurrentResultsOnEmpty) {
                    msgLabel.setText("Budget must be a valid number.");
                }
                return;
            }

            List<RoomType> typesToSearch = selectedType != null
                    ? List.of(selectedType)
                    : new ArrayList<>(Database.getRoomTypes());

            SearchRequest request = new SearchRequest(
                    checkIn,
                    checkOut,
                    numGuests,
                    List.copyOf(amenityFilter),
                    maxPrice,
                    selectedType,
                    List.copyOf(typesToSearch),
                    preserveCurrentResultsOnEmpty);
            runRoomSearchTask(request);
        } catch (RuntimeException ex) {
            if (!preserveCurrentResultsOnEmpty) {
                msgLabel.setText("Use date format DD/MM/YYYY (e.g. 15/06/2026).");
            }
        }
    }

    private void runRoomSearchTask(SearchRequest request) {
        int requestId = ++roomSearchRequestId;
        if (roomSearchTask != null && roomSearchTask.isRunning()) {
            roomSearchTask.cancel();
        }

        if (!request.preserveCurrentResultsOnEmpty()) {
            msgLabel.setText("Searching rooms...");
        }

        roomSearchTask = new Task<>() {
            @Override
            protected List<Room> call() {
                synchronized (Database.class) {
                    Database.refreshReservationsFromDatabase();
                    List<Room> available = new ArrayList<>();
                    for (RoomType type : request.typesToSearch()) {
                        if (isCancelled()) {
                            return List.of();
                        }
                        if (type.getCapacity() < request.numGuests()) {
                            continue;
                        }
                        if (request.maxPrice() != null && type.getPricePerNight() > request.maxPrice()) {
                            continue;
                        }
                        available.addAll(ReservationService.searchAvailableRooms(
                                request.checkIn(),
                                request.checkOut(),
                                type,
                                request.numGuests(),
                                request.amenityFilter()));
                    }
                    GuestPreferenceRanker.sortRoomsByGuestPreferences(available, guest);
                    return available;
                }
            }
        };

        roomSearchTask.setOnSucceeded(event -> {
            if (requestId != roomSearchRequestId) {
                return;
            }
            List<Room> available = roomSearchTask.getValue();
            if (available.isEmpty()) {
                if (request.preserveCurrentResultsOnEmpty()) {
                    return;
                }
                msgLabel.setText("No rooms available for the selected criteria.");
                showResults(false);
                return;
            }

            msgLabel.setText("");
            buildRoomTypeResults(
                    available,
                    request.checkIn(),
                    request.checkOut(),
                    request.numGuests(),
                    request.amenityFilter(),
                    request.maxPrice(),
                    request.selectedType());
            showResults(true);
        });

        roomSearchTask.setOnFailed(event -> {
            if (requestId != roomSearchRequestId || request.preserveCurrentResultsOnEmpty()) {
                return;
            }
            Throwable error = roomSearchTask.getException();
            msgLabel.setText(error == null ? "Could not search rooms." : error.getMessage());
        });

        Thread thread = new Thread(roomSearchTask, "room-availability-search");
        thread.setDaemon(true);
        thread.start();
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
        updateResultsSummary(rooms, checkIn, checkOut, numGuests, amenityFilter, maxPrice, selectedType);

        Map<String, List<Room>> roomsByType = new LinkedHashMap<>();
        for (Room room : rooms) {
            roomsByType.computeIfAbsent(room.getRoomType().getName(), key -> new ArrayList<>()).add(room);
        }

        List<Node> orderedCards = new ArrayList<>();
        Set<String> activeKeys = new HashSet<>();
        for (List<Room> typeRooms : roomsByType.values()) {
            String key = typeRooms.get(0).getRoomType().getName();
            activeKeys.add(key);
            try {
                orderedCards.add(updateRoomTypeResultCard(
                        key, typeRooms, checkIn, checkOut, numGuests, amenityFilter, selectedType, maxPrice));
            } catch (IOException ex) {
                ex.printStackTrace();
                Label error = new Label("Could not load room type card.");
                error.getStyleClass().add("error-message");
                orderedCards.add(error);
            }
        }

        roomTypeCardNodes.keySet().removeIf(key -> !activeKeys.contains(key));
        roomTypeCardControllers.keySet().removeIf(key -> !activeKeys.contains(key));
        FxNodeSync.syncChildren(resultsPane, orderedCards);
    }

    private Node updateRoomTypeResultCard(
            String key,
            List<Room> typeRooms,
            LocalDate checkIn,
            LocalDate checkOut,
            int numGuests,
            List<Amenity> amenityFilter,
            RoomType selectedType,
            Double maxPrice) throws IOException {
        Node card = roomTypeCardNodes.get(key);
        RoomTypeResultCardController controller = roomTypeCardControllers.get(key);
        if (card == null || controller == null) {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/RoomTypeResultCard.fxml"));
            card = loader.load();
            controller = loader.getController();
            roomTypeCardNodes.put(key, card);
            roomTypeCardControllers.put(key, controller);
        }
        controller.setData(
                mainApp, guest, typeRooms, checkIn, checkOut, numGuests, amenityFilter, selectedType, maxPrice);
        return card;
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

    private record SearchRequest(
            LocalDate checkIn,
            LocalDate checkOut,
            int numGuests,
            List<Amenity> amenityFilter,
            Double maxPrice,
            RoomType selectedType,
            List<RoomType> typesToSearch,
            boolean preserveCurrentResultsOnEmpty) {
    }
}
