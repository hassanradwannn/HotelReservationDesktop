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

public class AddAmenityController implements DashboardContentController {

    @FXML private TextField nameField;
    @FXML private TextField priceField;

    private AppContext mainApp;

    @Override
    public void initData(AppContext mainApp, Object data) {
        this.mainApp = mainApp;
    }

    @FXML
    private void handleAdd() {
        try {
            String name = nameField.getText().trim();
            double price = Double.parseDouble(priceField.getText().trim());

            if (name.isEmpty()) {
                mainApp.alert("Error", "Amenity name cannot be empty.");
                return;
            }

            Amenity newAmenity = new Amenity(name, price);
            Database.insertAmenity(newAmenity);

            // Reload the static list from the database to include the new item
            Database.loadAllAmenities();

            mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/AdminAmenities.fxml", null);

        } catch (NumberFormatException e) {
            mainApp.alert("Error", "Please enter a valid numeric price.");
        } catch (Exception ex) {
            mainApp.alert("Error", ex.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/AdminAmenities.fxml", null);
    }
}
