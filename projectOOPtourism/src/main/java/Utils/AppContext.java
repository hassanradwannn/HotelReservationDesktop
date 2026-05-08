package Utils;

import java.util.List;
import javafx.scene.layout.VBox;
import Models.*;

public interface AppContext {
    void showLoginScreen();
    void switchScene(String fxmlFile);
    void switchDashboardContent(VBox contentArea, String fxmlFile, Object data);
    VBox getCurrentContentArea();
    User getCurrentUser();
    void setCurrentUser(User user);
    void setCurrentViewRefresher(Runnable currentViewRefresher);
    Room getSelectedRoomForReservation();
    void setSelectedRoomForReservation(Room room);
    RoomType getSelectedRoomTypeForReservation();
    void setSelectedRoomTypeForReservation(RoomType type);
    void setGuestHomeSearchContext(ReservationSearchContext guestHomeSearchContext);
    List<Room> getLastGuestSearchResults();
    void setLastGuestSearchResults(List<Room> lastGuestSearchResults);
    void showGuestDashboard(Guest guest);
    void showAdminDashboard(Admin admin);
    void showReceptionistDashboard(Receptionist receptionist);
    String money(double value);
    void alert(String title, String msg);
}
