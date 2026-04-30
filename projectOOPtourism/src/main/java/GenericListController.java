import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import java.util.List;
import java.util.function.Supplier;

public class GenericListController implements DashboardContentController {
    @FXML private Label titleLabel;
    @FXML private ListView<String> listView;
    private Main mainApp;
    private Supplier<List<?>> supplier;

    @Override
    public void initData(Main mainApp, Object data) {
        this.mainApp = mainApp;
        Object[] args = (Object[]) data;
        titleLabel.setText((String) args[0]);
        this.supplier = (Supplier<List<?>>) args[1];

        Runnable dataRefresher = () -> {
            List<?> list = supplier.get();
            listView.setItems(FXCollections.observableArrayList(list.stream().map(Object::toString).toList()));
        };
        
        dataRefresher.run();
        mainApp.setCurrentViewRefresher(dataRefresher);
    }
}