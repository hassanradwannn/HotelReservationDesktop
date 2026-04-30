import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import java.util.List;

public class GuestReservationsController implements DashboardContentController {

    @FXML private ListView<String> reservationsList;
    private Main mainApp;
    private Guest guest;

    @Override
    public void initData(Main mainApp, Object data) {
        this.mainApp = mainApp;
        this.guest = (Guest) data;
        
        loadReservations();
        mainApp.setCurrentViewRefresher(this::loadReservations);
    }

    private void loadReservations() {
        Database.refreshReservationsFromDatabase();
        List<String> res = Database.getReservations().stream()
                .filter(r -> r.getGuest().getUsername().equals(guest.getUsername()))
                .map(Object::toString)
                .toList();
        reservationsList.setItems(FXCollections.observableArrayList(res));
    }
}