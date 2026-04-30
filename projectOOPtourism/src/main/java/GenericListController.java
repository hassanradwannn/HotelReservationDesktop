import java.util.List;
import java.util.function.Supplier;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

public class GenericListController implements DashboardContentController {
    @FXML private Label titleLabel;
    @FXML private ListView<Object> listView;
    private Main mainApp;
    private Supplier<List<?>> supplier;

    @Override
    public void initData(Main mainApp, Object data) {
        this.mainApp = mainApp;
        Object[] args = (Object[]) data;
        String title = (String) args[0];
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
}