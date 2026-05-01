import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import java.util.List;
import java.util.function.Supplier;

public class AddRoomController implements DashboardContentController {

    @FXML private TextField roomNumberField;
    @FXML private ComboBox<RoomType> typeCombo;

    private Main mainApp;

    @Override
    public void initData(Main mainApp, Object data) {
        this.mainApp = mainApp;
        typeCombo.setItems(FXCollections.observableArrayList(Database.getRoomTypes()));
    }

    @FXML
    private void handleAdd() {
        try {
            String roomNumber = roomNumberField.getText().trim();
            RoomType selectedType = typeCombo.getValue();

            if (roomNumber.isEmpty()) {
                mainApp.alert("Error", "Please enter a room number.");
                return;
            }
            if (selectedType == null) {
                mainApp.alert("Error", "Please select a room type.");
                return;
            }

            // Create in-memory
            CatalogService.createRoom(roomNumber, selectedType);

            // Sync to database
            Room newRoom = CatalogService.findRoom(roomNumber);
            if (newRoom != null) {
                DatabaseSaver.saveRoom(newRoom);

                // Assign default amenities based on room type
                java.util.List<Amenity> defaultAmenities = getDefaultAmenitiesForType(selectedType.getName());
                newRoom.setAmenities(new java.util.ArrayList<>(defaultAmenities));
                DatabaseSaver.saveRoomAmenities(newRoom);
            }

            mainApp.alert("Success", "Room added.");
            roomNumberField.clear();
            typeCombo.setValue(null);

            mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/GenericList.fxml",
                    new Object[]{"Rooms", (Supplier<List<?>>) () -> Database.getRooms()});

        } catch (Exception ex) {
            mainApp.alert("Error", ex.getMessage());
        }
    }

    private java.util.List<Amenity> getDefaultAmenitiesForType(String typeName) {
        java.util.List<Amenity> all = Database.getAmenities();
        java.util.List<String> names = new java.util.ArrayList<>();

        names.add("WiFi");
        names.add("Smart TV");

        String lc = typeName.toLowerCase();
        if (lc.contains("deluxe") || lc.contains("lobby") ||
                lc.contains("suite") || lc.contains("alpine") ||
                lc.contains("penthouse") || lc.contains("gustave")) {
            names.add("Mini-bar");
        }
        if (lc.contains("suite") || lc.contains("alpine") ||
                lc.contains("penthouse") || lc.contains("gustave")) {
            names.add("Jacuzzi");
        }
        if (lc.contains("penthouse") || lc.contains("gustave")) {
            names.add("Gym");
        }

        return all.stream()
                .filter(a -> names.contains(a.getName()))
                .collect(java.util.stream.Collectors.toList());
    }
}