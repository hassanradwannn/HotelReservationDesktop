package Controllers;


import Utils.AppContext;
import Models.*;
import Repositories.*;
import Services.*;
import Utils.*;
import Utils.exceptions.*;
import Repositories.database.DatabaseConnection;
import Repositories.DatabaseInitializer.*;
import java.util.List;
import java.util.function.Supplier;

import javafx.fxml.FXML;

public class AdminMenuController {
    private AppContext mainApp;
    private Admin admin;

    public void initData(AppContext mainApp, Admin admin) {
        this.mainApp = mainApp;
        this.admin = admin;
    }

    public void loadDefaultView() {
        showGuests();
    }

    @FXML private void showGuests() {
        mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/GenericList.fxml", new Object[]{"Guests", (Supplier<List<?>>) () -> {
            Database.refreshUsersFromDatabase();
            return Database.getGuests();
        }});
    }
    
    @FXML private void showRooms() {
        mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/GenericList.fxml", new Object[]{"Rooms", (Supplier<List<?>>) () -> Database.getRooms()});
    }
    
    @FXML private void showReservations() {
        mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/GenericList.fxml", new Object[]{"Reservations", (Supplier<List<?>>) () -> {
            Database.refreshReservationsFromDatabase();
            return Database.getReservations();
        }});
    }
    
    @FXML private void showRoomTypes() {
        mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/GenericList.fxml", new Object[]{"Room Types", (Supplier<List<?>>) () -> Database.getRoomTypes()});
    }

    @FXML private void showAmenities() {
        mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/AdminAmenities.fxml", null);
    }

    @FXML private void showAddReceptionist() {
        mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/AddReceptionist.fxml", admin);
    }

    @FXML private void showChat() {
        mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/Chat.fxml", admin);
    }
    
    @FXML private void doLogout() { 
        mainApp.showLoginScreen(); 
    }
}
