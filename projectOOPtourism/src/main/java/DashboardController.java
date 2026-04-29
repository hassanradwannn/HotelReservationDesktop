import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class DashboardController {
    @FXML private Label titleLabel;
    @FXML private Label userLabel;
    @FXML private VBox sideMenu;
    @FXML private VBox contentArea;

    public void setTitle(String title) {
        titleLabel.setText(title);
    }

    public void setUserInfo(String info) {
        userLabel.setText(info);
    }

    public VBox getSideMenu() { return sideMenu; }

    public VBox getContentArea() { return contentArea; }
}


interface DashboardContentController {
    void initData(Main mainApp, Object data);
}