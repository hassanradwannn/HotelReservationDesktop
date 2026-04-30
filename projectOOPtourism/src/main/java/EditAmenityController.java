import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class EditAmenityController implements DashboardContentController {

    @FXML private TextField nameField;
    @FXML private TextField priceField;

    private Main mainApp;
    private Amenity amenity;

    @Override
    public void initData(Main mainApp, Object data) {
        this.mainApp = mainApp;
        this.amenity = (Amenity) data;
        
        if (amenity != null) {
            nameField.setText(amenity.getName());
            priceField.setText(String.valueOf(amenity.getPrice()));
        }
    }
    
    @FXML
    private void handleSave() {
        try {
            amenity.setName(nameField.getText());
            amenity.setPrice(Double.parseDouble(priceField.getText()));
            
            Database.updateAmenity(amenity);
            
            mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/AdminAmenities.fxml", null);
        } catch (NumberFormatException e) {
            mainApp.alert("Invalid Input", "Please enter a valid number for price.");
        }
    }
    
    @FXML
    private void handleCancel() {
        mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/AdminAmenities.fxml", null);
    }
}