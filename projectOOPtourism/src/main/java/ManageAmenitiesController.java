import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

public class ManageAmenitiesController implements DashboardContentController {

    @FXML private TextField nameField;
    @FXML private TextField priceField;
    @FXML private ListView<String> listView;

    private Main mainApp;

    @Override
    public void initData(Main mainApp, Object data) {
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
            CatalogService.createAmenity(nameField.getText().trim(), Double.parseDouble(priceField.getText().trim()));
            
            mainApp.alert("Success", "Amenity added.");
            nameField.clear();
            priceField.clear();
            loadList();

        } catch (Exception ex) {
            mainApp.alert("Error", ex.getMessage());
        }
    }
}