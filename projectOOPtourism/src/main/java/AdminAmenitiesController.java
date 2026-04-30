import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;

public class AdminAmenitiesController implements DashboardContentController {

    @FXML private ListView<Amenity> listView;
    private Main mainApp;

    @Override
    public void initData(Main mainApp, Object data) {
        this.mainApp = mainApp;
        loadAmenities();
        
        // Register for auto-refresh via Dashboard timeline
        mainApp.setCurrentViewRefresher(this::loadAmenities);
        
        listView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && listView.getSelectionModel().getSelectedItem() != null) {
                Amenity selected = listView.getSelectionModel().getSelectedItem();
                mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/EditAmenity.fxml", selected);
            }
        });
    }

    private void loadAmenities() {
        listView.setItems(FXCollections.observableArrayList(Database.getAmenities()));
    }

    @FXML
    private void handleAddAmenity() {
        mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/AddAmenity.fxml", null);
    }
}