import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;

public class MakeReservationController implements DashboardContentController {

    @FXML private ComboBox<RoomType> typeBox;
    @FXML private TextField guestsField;
    @FXML private DatePicker checkInPicker;
    @FXML private DatePicker checkOutPicker;
    @FXML private CheckBox gymBox;
    @FXML private Label msgLabel;

    private Main mainApp;
    private Guest guest;
    private List<Amenity> requestedAmenities = List.of();
    private ReservationSearchContext sourceSearchContext;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("d/M/yyyy");

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
        setupBlackoutDates();

        typeBox.setItems(FXCollections.observableArrayList(Database.getRoomTypes()));

        Room selectedRoom = mainApp.getSelectedRoomForReservation();
        if (searchContext != null) {
            typeBox.setValue(findCurrentRoomType(searchContext.getRoomType()));
            guestsField.setText(String.valueOf(searchContext.getGuests()));
            checkInPicker.setValue(searchContext.getCheckIn());
            checkOutPicker.setValue(searchContext.getCheckOut());
            setSuccess("Selected " + searchContext.getRoomType().getName() + " type.");
        } else if (selectedRoom != null) {
            typeBox.setValue(findCurrentRoomType(selectedRoom.getRoomType()));
            setSuccess("Selected Room " + selectedRoom.getRoomNumber() + " automatically.");
        } else if (mainApp.getSelectedRoomTypeForReservation() != null) {
            typeBox.setValue(findCurrentRoomType(mainApp.getSelectedRoomTypeForReservation()));
            setSuccess("Selected " + mainApp.getSelectedRoomTypeForReservation().getName() + " type.");
        }

        typeBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            updateGymPassState(newVal);
        });
        updateGymPassState(typeBox.getValue());
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
        if (roomType != null && roomType.getName().equalsIgnoreCase("The Gustave Penthouse")) {
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

            if (in == null || out == null || typeBox.getValue() == null) {
                setError("Please complete all fields (Room Type and Dates).");
                return;
            }

            int numGuests = Integer.parseInt(guestsField.getText().trim());
            List<Room> available = ReservationService.searchAvailableRooms(
                    in, out, typeBox.getValue(), numGuests, requestedAmenities);

            if (available.isEmpty()) {
                setError("No rooms available for the selected dates.");
                return;
            }

            Room room = available.get(0);
            Reservation res = ReservationService.createReservation(guest, room, in, out, gymBox.isSelected());

            mainApp.alert("Reservation Created",
                    "ID: " + res.getReservationId() + "\nRoom: " + room.getRoomNumber());

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

        if (in == null || out == null || typeBox.getValue() == null) {
            setError("Please select dates and room type first.");
            return;
        }

        try {
            int numGuests = Integer.parseInt(guestsField.getText().trim());
            List<Room> available = ReservationService.searchAvailableRooms(
                    in, out, typeBox.getValue(), numGuests, requestedAmenities);

            if (available.isEmpty()) {
                setError("No rooms available for the selected dates.");
                return;
            }

            Object[] bookingData = new Object[]{ guest, in, out, gymBox.isSelected(), available };
            mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/AvailableRooms.fxml", bookingData);

        } catch (NumberFormatException ex) {
            setError("Please enter a valid number of guests.");
        }
    }

    private void setError(String message) {
        msgLabel.setText(message);
        msgLabel.getStyleClass().removeAll("success-message");
        msgLabel.getStyleClass().add("error-message");
    }

    private void setSuccess(String message) {
        msgLabel.setText(message);
        msgLabel.getStyleClass().removeAll("error-message");
        msgLabel.getStyleClass().add("success-message");
    }
}
