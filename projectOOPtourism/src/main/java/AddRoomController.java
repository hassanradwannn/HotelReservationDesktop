import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

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
            if (typeCombo.getValue() == null) {
                mainApp.alert("Error", "Please select a room type.");
                return;
            }

            CatalogService.createRoom(roomNumberField.getText().trim(), typeCombo.getValue());
            mainApp.alert("Success", "Room added.");
            roomNumberField.clear();
            typeCombo.setValue(null);
            
            mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/GenericList.fxml", new Object[]{"Rooms", (java.util.function.Supplier<java.util.List<?>>) () -> Database.getRooms()});

        } catch (Exception ex) {
            mainApp.alert("Error", ex.getMessage());
        }
    }
}