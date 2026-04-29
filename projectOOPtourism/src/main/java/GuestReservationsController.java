import java.util.List;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;

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
    }

    private void loadReservations() {
        List<String> list = Database.getReservations().stream()
                .filter(r -> r.getGuest().getUsername().equalsIgnoreCase(guest.getUsername()))
                .map(Object::toString).toList();
        reservationsList.setItems(FXCollections.observableArrayList(list));
    }
}