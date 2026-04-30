import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

public class EditRoomController implements DashboardContentController {

    @FXML private TextField roomNumberField;
    @FXML private ComboBox<RoomType> typeCombo;
    @FXML private ListView<Amenity> currentAmenitiesList;
    @FXML private ComboBox<Amenity> availableAmenitiesCombo;

    private Main mainApp;
    private Room room;

    @Override
    public void initData(Main mainApp, Object data) {
        this.mainApp = mainApp;
        this.room = (Room) data;
        
        typeCombo.setItems(FXCollections.observableArrayList(Database.getRoomTypes()));
        availableAmenitiesCombo.setItems(FXCollections.observableArrayList(Database.getAmenities()));
        
        if (room != null) {
            roomNumberField.setText(room.getRoomNumber());
            typeCombo.setValue(room.getRoomType());
            currentAmenitiesList.setItems(FXCollections.observableArrayList(room.getAmenities()));
        }
    }
    
    @FXML
    private void handleAddAmenity() {
        Amenity selectedAmenity = availableAmenitiesCombo.getValue();
        if (selectedAmenity != null && !currentAmenitiesList.getItems().contains(selectedAmenity)) {
            currentAmenitiesList.getItems().add(selectedAmenity);
        }
    }
    
    @FXML
    private void handleRemoveAmenity() {
        Amenity selectedAmenity = currentAmenitiesList.getSelectionModel().getSelectedItem();
        if (selectedAmenity != null) {
            currentAmenitiesList.getItems().remove(selectedAmenity);
        }
    }
    
    @FXML
    private void handleSave() {
        try {
            room.setRoomNumber(roomNumberField.getText());
            room.setRoomType(typeCombo.getValue());
            
            List<Amenity> updatedAmenities = new ArrayList<>(currentAmenitiesList.getItems());
            Database.updateRoomAmenities(room.getId(), updatedAmenities);
            room.setAmenities(new ArrayList<>(updatedAmenities));
            
            Database.updateRoom(room);
            
            mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/GenericList.fxml", new Object[]{"Rooms", (Supplier<List<?>>) () -> Database.getRooms()});
        } catch (Exception e) {
            mainApp.alert("Error", "Could not update room.");
        }
    }
    
    @FXML
    private void handleCancel() {
        mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/GenericList.fxml", new Object[]{"Rooms", (Supplier<List<?>>) () -> Database.getRooms()});
    }
}