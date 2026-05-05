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
                java.util.List<Amenity> defaultAmenities = CatalogService.getDefaultAmenitiesForType(selectedType.getName());
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

}
