import java.util.List;
import java.util.function.Supplier;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class EditRoomTypeController implements DashboardContentController {

    @FXML private TextField nameField;
    @FXML private TextField priceField;
    @FXML private TextField capacityField;

    private Main mainApp;
    private RoomType roomType;

    @Override
    public void initData(Main mainApp, Object data) {
        this.mainApp = mainApp;
        this.roomType = (RoomType) data;
        
        if (roomType != null) {
            nameField.setText(roomType.getName());
            priceField.setText(String.valueOf(roomType.getPricePerNight()));
            capacityField.setText(String.valueOf(roomType.getCapacity()));
        }
    }
    
    @FXML
    private void handleSave() {
        try {
            roomType.setName(nameField.getText());
            roomType.setPricePerNight(Double.parseDouble(priceField.getText()));
            roomType.setCapacity(Integer.parseInt(capacityField.getText()));
            
            Database.updateRoomType(roomType);
            
            mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/GenericList.fxml", new Object[]{"Room Types", (Supplier<List<?>>) () -> Database.getRoomTypes()});
        } catch (NumberFormatException e) {
            mainApp.alert("Invalid Input", "Please enter valid numbers for price and capacity.");
        }
    }
    
    @FXML
    private void handleCancel() {
        mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/GenericList.fxml", new Object[]{"Room Types", (Supplier<List<?>>) () -> Database.getRoomTypes()});
    }
}