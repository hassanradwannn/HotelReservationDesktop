package Controllers;


import Utils.AppContext;
import Models.*;
import Repositories.*;
import Services.*;
import Utils.*;
import Utils.exceptions.*;
import Repositories.database.DatabaseConnection;
import Repositories.DatabaseInitializer.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

public class ManageAmenitiesController implements DashboardContentController {

    @FXML private TextField nameField;
    @FXML private TextField priceField;
    @FXML private ListView<String> listView;

    private AppContext mainApp;

    @Override
    public void initData(AppContext mainApp, Object data) {
        this.mainApp = mainApp;
        loadList();
        mainApp.setCurrentViewRefresher(this::loadList);
    }

    private void loadList() {
        listView.setItems(FXCollections.observableArrayList(
            Database.getAmenities().stream().map(Object::toString).toList()
        ));
    }

    @FXML
    private void handleAdd() {
        try {
            Amenity amenity = new Amenity(
                    nameField.getText().trim(),
                    Double.parseDouble(priceField.getText().trim()));
            Database.insertAmenity(amenity);
            Database.loadAllAmenities();
            
            mainApp.alert("Success", "Amenity added.");
            nameField.clear();
            priceField.clear();
            loadList();

        } catch (Exception ex) {
            mainApp.alert("Error", ex.getMessage());
        }
    }
}
