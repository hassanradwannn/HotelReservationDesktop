import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;

public class GenericListController implements DashboardContentController {
    @FXML private Label titleLabel;
    @FXML private ListView<Object> listView;
    @FXML private HBox actionBar;
    private Main mainApp;
    private Supplier<List<?>> supplier;
    private String title;

    @Override
    public void initData(Main mainApp, Object data) {
        this.mainApp = mainApp;
        Object[] args = (Object[]) data;
        this.title = (String) args[0];
        titleLabel.setText(title);
        this.supplier = (Supplier<List<?>>) args[1];

        Runnable dataRefresher = () -> {
            List<?> list = supplier.get();
            @SuppressWarnings("unchecked")
            List<Object> typedList = (List<Object>) list;
            listView.setItems(FXCollections.observableArrayList(typedList));
        };
        
        dataRefresher.run();
        mainApp.setCurrentViewRefresher(dataRefresher);

        // Add context-specific action buttons
        if (actionBar != null) {
            switch (title) {
                case "Rooms" -> {
                    Button addBtn = new Button("ADD ROOM");
                    addBtn.getStyleClass().add("primary-action-btn");
                    addBtn.setPrefHeight(38);
                    addBtn.setOnAction(e -> mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/AddRoom.fxml", null));

                    Button deleteBtn = new Button("DELETE SELECTED");
                    deleteBtn.getStyleClass().add("danger-action-btn");
                    deleteBtn.setPrefHeight(38);
                    deleteBtn.setOnAction(e -> handleDeleteRoom(dataRefresher));

                    actionBar.getChildren().addAll(deleteBtn, addBtn);
                }
                case "Room Types" -> {
                    Button addBtn = new Button("ADD ROOM TYPE");
                    addBtn.getStyleClass().add("primary-action-btn");
                    addBtn.setPrefHeight(38);
                    addBtn.setOnAction(e -> mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/ManageRoomTypes.fxml", null));

                    Button deleteBtn = new Button("DELETE SELECTED");
                    deleteBtn.getStyleClass().add("danger-action-btn");
                    deleteBtn.setPrefHeight(38);
                    deleteBtn.setOnAction(e -> handleDeleteRoomType(dataRefresher));

                    actionBar.getChildren().addAll(deleteBtn, addBtn);
                }
            }
        }
        
        listView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && listView.getSelectionModel().getSelectedItem() != null) {
                Object selectedItem = listView.getSelectionModel().getSelectedItem();
                switch (title) {
                    case "Rooms" -> mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/EditRoom.fxml", selectedItem);
                    case "Room Types" -> mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/EditRoomType.fxml", selectedItem);
                    case "Amenities" -> mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/EditAmenity.fxml", selectedItem);
                }
            }
        });
    }

    private void handleDeleteRoom(Runnable refresher) {
        Object selected = listView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            mainApp.alert("No Selection", "Please select a room to delete.");
            return;
        }
        Room room = (Room) selected;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Room");
        confirm.setHeaderText(null);
        confirm.setContentText("Delete room " + room.getRoomNumber() + "? This cannot be undone.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                CatalogService.deleteRoom(room);
                Database.deleteRoomFromDB(room);
                refresher.run();
            } catch (Exception ex) {
                mainApp.alert("Error", ex.getMessage());
            }
        }
    }

    private void handleDeleteRoomType(Runnable refresher) {
        Object selected = listView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            mainApp.alert("No Selection", "Please select a room type to delete.");
            return;
        }
        RoomType rt = (RoomType) selected;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Room Type");
        confirm.setHeaderText(null);
        confirm.setContentText("Delete room type \"" + rt.getName() + "\"? This cannot be undone.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                CatalogService.deleteRoomType(rt);
                Database.deleteRoomTypeFromDB(rt);
                refresher.run();
            } catch (Exception ex) {
                mainApp.alert("Error", ex.getMessage());
            }
        }
    }
}
