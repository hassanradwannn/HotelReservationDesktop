import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class DashboardController {
    @FXML private Label titleLabel;
    @FXML private Label userLabel;
    @FXML private HBox topBar;   // the title/user info bar at the very top
    @FXML private VBox sideMenu;
    @FXML private VBox contentArea;
    @FXML private HBox navBar;   // horizontal nav slot — used by receptionist dashboard

    public void setTitle(String title) {
        titleLabel.setText(title);
    }

    public void setUserInfo(String info) {
        userLabel.setText(info);
    }

    public HBox getTopBar()      { return topBar; }
    public VBox getSideMenu()    { return sideMenu; }
    public VBox getContentArea() { return contentArea; }

    /** Returns the horizontal nav bar slot in the top VBox. */
    public HBox getNavBar() { return navBar; }

    /** Restyles the topBar and its labels to match the receptionist info bar design. */
    public void styleAsReceptionistInfoBar() {
        if (titleLabel != null) {
            titleLabel.getStyleClass().removeAll("topbar-title");
            titleLabel.getStyleClass().add("topbar-date");
        }
        if (userLabel != null) {
            userLabel.getStyleClass().removeAll("topbar-user");
            userLabel.getStyleClass().add("topbar-role-badge");
        }
    }
}


interface DashboardContentController {
    void initData(Main mainApp, Object data);

    default void onRemoved() {
    }
}
