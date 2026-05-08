package Controllers;


import Utils.AppContext;
import Models.*;
import Repositories.*;
import Services.*;
import Utils.*;
import Utils.exceptions.*;
import Repositories.database.DatabaseConnection;
import Repositories.DatabaseInitializer.*;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class EditAmenityController implements DashboardContentController {

    @FXML private TextField nameField;
    @FXML private TextField priceField;

    private AppContext mainApp;
    private Amenity amenity;

    @Override
    public void initData(AppContext mainApp, Object data) {
        this.mainApp = mainApp;
        this.amenity = (Amenity) data;
        
        if (amenity != null) {
            nameField.setText(amenity.getName());
            priceField.setText(String.valueOf(amenity.getPrice()));
        }
    }
    
    @FXML
    private void handleSave() {
        String oldName = amenity.getName();
        double oldPrice = amenity.getPrice();
        try {
            String newName = nameField.getText();
            double newPrice = Double.parseDouble(priceField.getText());
            amenity.setName(newName);
            amenity.setPrice(newPrice);
            
            Database.updateAmenity(amenity);
            
            mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/AdminAmenities.fxml", null);
        } catch (NumberFormatException e) {
            amenity.setName(oldName);
            amenity.setPrice(oldPrice);
            mainApp.alert("Invalid Input", "Please enter a valid number for price.");
        } catch (Exception e) {
            amenity.setName(oldName);
            amenity.setPrice(oldPrice);
            mainApp.alert("Error", e.getMessage());
        }
    }
    
    @FXML
    private void handleCancel() {
        mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/AdminAmenities.fxml", null);
    }
}
