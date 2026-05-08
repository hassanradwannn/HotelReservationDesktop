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
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;

import java.util.Optional;

public class AdminAmenitiesController implements DashboardContentController {

    @FXML private ListView<Amenity> listView;
    private AppContext mainApp;

    @Override
    public void initData(AppContext mainApp, Object data) {
        this.mainApp = mainApp;
        loadAmenities();

        // Dashboard polling calls this after another user changes amenity data.
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

    @FXML
    private void handleDeleteAmenity() {
        Amenity selected = listView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            mainApp.alert("No Selection", "Please select an amenity to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Amenity");
        confirm.setHeaderText(null);
        confirm.setContentText("Delete amenity \"" + selected.getName() + "\"? This cannot be undone.");
        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                Database.deleteAmenityFromDB(selected);
                loadAmenities();
            } catch (Exception ex) {
                mainApp.alert("Error", ex.getMessage());
            }
        }
    }
}
