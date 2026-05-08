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

public class ManageRoomTypesController implements DashboardContentController {

    @FXML private TextField nameField;
    @FXML private TextField priceField;
    @FXML private TextField capField;
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
            Database.getRoomTypes().stream().map(Object::toString).toList()
        ));
    }

    @FXML
    private void handleAdd() {
        try {
            CatalogService.createRoomType(nameField.getText().trim(),
                    Double.parseDouble(priceField.getText().trim()),
                    Integer.parseInt(capField.getText().trim()));

            mainApp.alert("Success", "Room type added.");
            nameField.clear();
            priceField.clear();
            capField.clear();
            loadList();

        } catch (Exception ex) {
            mainApp.alert("Error", ex.getMessage());
        }
    }
}
