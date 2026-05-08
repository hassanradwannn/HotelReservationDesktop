package Controllers;


import Utils.AppContext;
import Models.*;
import Repositories.*;
import Services.*;
import Utils.*;
import Utils.exceptions.*;
import Repositories.database.DatabaseConnection;
import Repositories.DatabaseInitializer.*;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class DashboardController {
    @FXML private Label titleLabel;
    @FXML private Label userLabel;
    @FXML private HBox topBar;
    @FXML private VBox sideMenu;
    @FXML private VBox contentArea;
    @FXML private HBox navBar;

    public void setTitle(String title) {
        titleLabel.setText(title);
    }

    public void setUserInfo(String info) {
        userLabel.setText(info);
    }

    public HBox getTopBar()      { return topBar; }
    public VBox getSideMenu()    { return sideMenu; }
    public VBox getContentArea() { return contentArea; }

    /** Slot used by the receptionist dashboard for its horizontal menu. */
    public HBox getNavBar() { return navBar; }

    /** Turns the default title bar into the receptionist date and role strip. */
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
