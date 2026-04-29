import java.util.List;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.Label;

public class GuestReservationsController implements DashboardContentController {

    @FXML private ListView<String> reservationsList;

    private Main mainApp;
    private Guest guest;

    @Override
    public void initData(Main mainApp, Object data) {
        this.mainApp = mainApp;
        if (data instanceof Guest g) {
            this.guest = g;
            loadReservations();
        }

        javafx.application.Platform.runLater(() -> {
            if (reservationsList.getScene() != null) {
                for (javafx.scene.Node node : reservationsList.getScene().getRoot().lookupAll(".label")) {
                    if (node instanceof Label label && label.getText() != null && label.getText().toLowerCase().contains("reservation")) {
                        String currentStyle = label.getStyle() == null ? "" : label.getStyle();
                        label.setStyle(currentStyle + "; -fx-text-fill: #2B2421;");
                    }
                }
            }
        });
    }

    private void loadReservations() {
        List<String> list = Database.getReservations().stream()
                .filter(r -> r.getGuest().getUsername().equalsIgnoreCase(guest.getUsername()))
                .map(Object::toString).toList();
        reservationsList.setItems(FXCollections.observableArrayList(list));
    }
}