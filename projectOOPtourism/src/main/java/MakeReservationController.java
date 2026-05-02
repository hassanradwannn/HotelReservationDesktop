import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

public class MakeReservationController implements DashboardContentController {

    @FXML private Label selectedRoomTypeLabel;
    @FXML private TextField guestsField;
    @FXML private DatePicker checkInPicker;
    @FXML private DatePicker checkOutPicker;
    @FXML private CheckBox gymBox;
    @FXML private Label msgLabel;
    @FXML private Label summaryNightsLabel;
    @FXML private Label summaryRoomTypeLabel;
    @FXML private Label summaryPricePerNightLabel;
    @FXML private VBox summaryAmenitiesBox;
    @FXML private Label summaryTotalCostLabel;

    private Main mainApp;
    private Guest guest;
    private Room selectedRoom;
    private RoomType selectedRoomType;
    private List<Amenity> requestedAmenities = List.of();
    private ReservationSearchContext sourceSearchContext;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void initData(Main mainApp, Object data) {
        this.mainApp = mainApp;

        ReservationSearchContext searchContext = null;
        if (data instanceof ReservationSearchContext context) {
            searchContext = context;
            sourceSearchContext = context;
            this.guest = context.getGuest();
            this.requestedAmenities = context.getRequestedAmenities();
        } else {
            sourceSearchContext = null;
            this.guest = (Guest) data;
            this.requestedAmenities = List.of();
        }

        setupDatePickerFormat(checkInPicker);
        setupDatePickerFormat(checkOutPicker);
        gymBox.setText("Add Gym Pass ($" + mainApp.money(Reservation.GYM_PASS_PRICE) + ")");
        setupBlackoutDates();
        resolveSelectedRoomType(searchContext);
        applyInitialValues(searchContext);
        setupSummaryListeners();
        updateGymPassState(selectedRoomType);
        updateSelectedRoomTypeDisplay();
        updateStaySummary();
    }

    private void resolveSelectedRoomType(ReservationSearchContext searchContext) {
        selectedRoom = mainApp.getSelectedRoomForReservation();
        if (selectedRoom != null) {
            selectedRoomType = findCurrentRoomType(selectedRoom.getRoomType());
            return;
        }

        if (searchContext != null) {
            selectedRoomType = findCurrentRoomType(searchContext.getRoomType());
            return;
        }

        selectedRoomType = findCurrentRoomType(mainApp.getSelectedRoomTypeForReservation());
    }

    private void applyInitialValues(ReservationSearchContext searchContext) {
        if (searchContext != null) {
            guestsField.setText(String.valueOf(searchContext.getGuests()));
            checkInPicker.setValue(searchContext.getCheckIn());
            checkOutPicker.setValue(searchContext.getCheckOut());
            gymBox.setSelected(searchContext.hasGymPass());
            setSuccess("Selected " + selectedRoomType.getName() + " type.");
        } else if (selectedRoom != null) {
            setSuccess("Selected Room " + selectedRoom.getRoomNumber() + " automatically.");
        } else if (selectedRoomType != null) {
            setSuccess("Selected " + selectedRoomType.getName() + " type.");
        } else {
            setError("Choose a room type from search before booking.");
        }
    }

    private void setupSummaryListeners() {
        guestsField.textProperty().addListener((obs, oldVal, newVal) -> updateStaySummary());
        checkInPicker.valueProperty().addListener((obs, oldVal, newVal) -> updateStaySummary());
        checkOutPicker.valueProperty().addListener((obs, oldVal, newVal) -> updateStaySummary());
        gymBox.selectedProperty().addListener((obs, oldVal, newVal) -> updateStaySummary());
    }

    private RoomType findCurrentRoomType(RoomType roomType) {
        if (roomType == null) {
            return null;
        }

        return Database.getRoomTypes().stream()
                .filter(type -> type.getName().equals(roomType.getName()))
                .findFirst()
                .orElse(roomType);
    }

    private void updateGymPassState(RoomType roomType) {
        if (roomType == null) {
            gymBox.setSelected(false);
            gymBox.setDisable(true);
            return;
        }

        if (roomType.getName().toLowerCase().contains("gustave")) {
            gymBox.setSelected(true);
            gymBox.setDisable(true);
        } else {
            gymBox.setDisable(false);
        }
    }

    private void setupDatePickerFormat(DatePicker picker) {
        picker.setConverter(new StringConverter<LocalDate>() {
            @Override
            public String toString(LocalDate date) {
                return (date != null) ? dtf.format(date) : "";
            }
            @Override
            public LocalDate fromString(String string) {
                if (string != null && !string.isEmpty()) {
                    try {
                        return LocalDate.parse(string, dtf);
                    } catch (Exception e) {
                        return null;
                    }
                }
                return null;
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
            if (newVal != null) {
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
            }
        });
    }

    @FXML
    private void handleBack() {
        if (sourceSearchContext != null) {
            mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/GuestHome.fxml", sourceSearchContext);
        } else {
            mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/GuestHome.fxml", guest);
        }
    }

    @FXML
    private void handleBookNow() {
        Database.refreshReservationsFromDatabase();
        try {
            LocalDate in = checkInPicker.getValue();
            LocalDate out = checkOutPicker.getValue();

            if (in == null || out == null || selectedRoomType == null) {
                setError("Please choose a room type from search and complete the dates.");
                return;
            }

            int numGuests = parseGuests();
            Room room = resolveRoomForBooking(in, out, numGuests);
            if (room == null) {
                return;
            }

            Reservation res = ReservationService.createReservation(guest, room, in, out, gymBox.isSelected());

            mainApp.alert("Reservation Created",
                    "ID: " + res.getReservationId()
                            + "\nRoom: " + room.getRoomNumber()
                            + "\nTotal: $" + mainApp.money(res.getTotalPrice()));

            mainApp.setSelectedRoomForReservation(null);
            mainApp.setSelectedRoomTypeForReservation(null);
            mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/GuestReservations.fxml", guest);

        } catch (NumberFormatException ex) {
            setError("Please enter a valid number of guests.");
        }
    }

    @FXML
    private void handleChooseRoom() {
        Database.refreshReservationsFromDatabase();
        LocalDate in = checkInPicker.getValue();
        LocalDate out = checkOutPicker.getValue();

        if (in == null || out == null || selectedRoomType == null) {
            setError("Please select dates after choosing a room type from search.");
            return;
        }

        try {
            int numGuests = parseGuests();
            List<Room> available = ReservationService.searchAvailableRooms(
                    in, out, selectedRoomType, numGuests, requestedAmenities);
            GuestPreferenceRanker.sortRoomsByGuestPreferences(available, guest);

            if (available.isEmpty()) {
                setError("No rooms available for the selected dates.");
                return;
            }

            ReservationSearchContext pickerContext = new ReservationSearchContext(
                    guest,
                    selectedRoomType,
                    in,
                    out,
                    numGuests,
                    requestedAmenities,
                    sourceSearchContext != null ? sourceSearchContext.getSearchRoomType() : selectedRoomType,
                    sourceSearchContext != null ? sourceSearchContext.getMaxPrice() : null,
                    gymBox.isSelected());
            Object[] bookingData = new Object[]{ guest, in, out, gymBox.isSelected(), available, pickerContext };
            mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/AvailableRooms.fxml", bookingData);

        } catch (NumberFormatException ex) {
            setError("Please enter a valid number of guests.");
        }
    }

    private int parseGuests() {
        return Integer.parseInt(guestsField.getText().trim());
    }

    private Room resolveRoomForBooking(LocalDate in, LocalDate out, int numGuests) {
        if (selectedRoom != null) {
            if (selectedRoom.getRoomType().getCapacity() < numGuests) {
                setError("Selected room cannot fit that many guests.");
                return null;
            }
            if (!roomHasRequestedAmenities(selectedRoom)) {
                setError("Selected room no longer matches the requested amenities.");
                return null;
            }
            if (!ReservationService.isRoomAvailable(selectedRoom, in, out)) {
                setError("Selected room is not available for the selected dates.");
                return null;
            }
            return selectedRoom;
        }

        List<Room> available = ReservationService.searchAvailableRooms(
                in, out, selectedRoomType, numGuests, requestedAmenities);
        GuestPreferenceRanker.sortRoomsByGuestPreferences(available, guest);

        if (available.isEmpty()) {
            setError("No rooms available for the selected dates.");
            return null;
        }

        return available.get(0);
    }

    private boolean roomHasRequestedAmenities(Room room) {
        if (requestedAmenities == null || requestedAmenities.isEmpty()) {
            return true;
        }

        List<String> roomAmenityNames = room.getAmenities().stream().map(Amenity::getName).toList();
        for (Amenity amenity : requestedAmenities) {
            if (!roomAmenityNames.contains(amenity.getName())) {
                return false;
            }
        }
        return true;
    }

    private void updateSelectedRoomTypeDisplay() {
        if (selectedRoom != null) {
            selectedRoomTypeLabel.setText("Room " + selectedRoom.getRoomNumber() + " - " + selectedRoomType.getName());
        } else if (selectedRoomType != null) {
            selectedRoomTypeLabel.setText(selectedRoomType.getName());
        } else {
            selectedRoomTypeLabel.setText("No room type selected yet.");
        }
    }

    private void updateStaySummary() {
        long nights = calculateNights(checkInPicker.getValue(), checkOutPicker.getValue());
        Room pricedRoom = findRoomForSummary();
        List<Amenity> amenities = getPricedAmenities(pricedRoom);

        summaryNightsLabel.setText(nights > 0 ? String.valueOf(nights) : "-");
        summaryRoomTypeLabel.setText(selectedRoomType == null ? "-" : selectedRoomType.getName());
        summaryPricePerNightLabel.setText(selectedRoomType == null ? "-" : "$" + mainApp.money(selectedRoomType.getPricePerNight()));
        renderAmenities(amenities);

        if (selectedRoomType == null || nights <= 0) {
            summaryTotalCostLabel.setText("-");
        } else {
            summaryTotalCostLabel.setText("$" + mainApp.money(calculateTotalCost(selectedRoomType, amenities, nights)));
        }
    }

    private Room findRoomForSummary() {
        if (selectedRoom != null) {
            return selectedRoom;
        }
        if (selectedRoomType == null) {
            return null;
        }

        LocalDate in = checkInPicker.getValue();
        LocalDate out = checkOutPicker.getValue();
        Integer guests = tryParseGuests();
        if (in != null && out != null && guests != null) {
            List<Room> available = ReservationService.searchAvailableRooms(
                    in, out, selectedRoomType, guests, requestedAmenities);
            GuestPreferenceRanker.sortRoomsByGuestPreferences(available, guest);
            if (!available.isEmpty()) {
                return available.get(0);
            }
        }

        return Database.getRooms().stream()
                .filter(room -> room.getRoomType().getName().equals(selectedRoomType.getName()))
                .findFirst()
                .orElse(null);
    }

    private Integer tryParseGuests() {
        try {
            return parseGuests();
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private long calculateNights(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            return 0;
        }
        return Math.max(1, ChronoUnit.DAYS.between(checkIn, checkOut));
    }

    private double calculateTotalCost(RoomType roomType, List<Amenity> amenities, long nights) {
        double total = roomType.getPricePerNight() * nights;
        if (amenities != null) {
            for (Amenity amenity : amenities) {
                total += amenity.getPrice();
            }
        }
        return total;
    }

    private List<Amenity> getPricedAmenities(Room pricedRoom) {
        List<Amenity> amenities = new ArrayList<>();
        List<Amenity> sourceAmenities = pricedRoom != null ? pricedRoom.getAmenities() : requestedAmenities;
        if (sourceAmenities != null) {
            for (Amenity amenity : sourceAmenities) {
                if (gymBox.isSelected() && Reservation.isGymAmenity(amenity)) {
                    continue;
                }
                amenities.add(amenity);
            }
        }
        if (gymBox.isSelected()) {
            amenities.add(new Amenity(Reservation.GYM_PASS_NAME, Reservation.GYM_PASS_PRICE));
        }
        return amenities;
    }

    private void renderAmenities(List<Amenity> amenities) {
        summaryAmenitiesBox.getChildren().clear();
        if (amenities == null || amenities.isEmpty()) {
            Label none = new Label("No paid amenities selected");
            none.getStyleClass().add("reservation-amenity-line");
            summaryAmenitiesBox.getChildren().add(none);
            return;
        }

        for (Amenity amenity : amenities) {
            Label line = new Label("* " + amenity.getName() + " | $" + mainApp.money(amenity.getPrice()));
            line.getStyleClass().add("reservation-amenity-line");
            line.setWrapText(true);
            summaryAmenitiesBox.getChildren().add(line);
        }
    }

    private void setError(String message) {
        msgLabel.setText(message);
        msgLabel.getStyleClass().removeAll("success-message", "error-message");
        msgLabel.getStyleClass().add("error-message");
    }

    private void setSuccess(String message) {
        msgLabel.setText(message);
        msgLabel.getStyleClass().removeAll("error-message", "success-message");
        msgLabel.getStyleClass().add("success-message");
    }
}
