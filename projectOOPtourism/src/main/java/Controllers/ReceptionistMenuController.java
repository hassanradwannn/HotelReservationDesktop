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

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

public class ReceptionistMenuController {
    private AppContext mainApp;
    private Receptionist receptionist;

    @FXML private HBox navBar;

    @FXML private Button btnReservations;
    @FXML private Button btnGuests;
    @FXML private Button btnRooms;
    @FXML private Button btnChat;
    @FXML private Button btnTime;

    private Button activeButton;

    private Timeline syncTimeline;
    private long localDataVersion = -1;

    @FXML
    public void initialize() {
        Database.loadAll();

        if (syncTimeline != null) syncTimeline.stop();
        syncTimeline = new Timeline(new KeyFrame(Duration.seconds(2), event -> {
            new Thread(() -> {
                long currentVersion = Database.getLatestDataVersion();
                if (localDataVersion == -1) {
                    localDataVersion = currentVersion;
                } else if (currentVersion > localDataVersion) {
                    localDataVersion = currentVersion;
                    Database.refreshReservationsFromDatabase();
                }
            }).start();
        }));
        syncTimeline.setCycleCount(Timeline.INDEFINITE);
        syncTimeline.play();
    }

    public void initData(AppContext mainApp, Receptionist receptionist) {
        this.mainApp = mainApp;
        this.receptionist = receptionist;
        Database.loadAll();
        // Set initial active button to Reservations
        setActiveButton(btnReservations);
    }

    public void loadDefaultView() {
        showAllReservations();
    }


    private void setActiveButton(Button btn) {
        if (activeButton != null) {
            activeButton.getStyleClass().remove("nav-btn-active");
        }
        activeButton = btn;
        if (activeButton != null && !activeButton.getStyleClass().contains("nav-btn-active")) {
            activeButton.getStyleClass().add("nav-btn-active");
        }
    }


    @FXML private void showAllReservations() {
        setActiveButton(btnReservations);
        mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/ReceptionistReservations.fxml",
                ReceptionistReservationsController.ViewMode.ALL);
    }

    @FXML private void showGuests() {
        setActiveButton(btnGuests);
        mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/GenericList.fxml",
                new Object[]{"All Guests", (Supplier<List<?>>) () -> {
                    Database.refreshUsersFromDatabase();
                    return Database.getGuests();
                }});
    }

    @FXML private void showRooms() {
        setActiveButton(btnRooms);
        mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/GenericList.fxml",
                new Object[]{"Rooms", (Supplier<List<?>>) () -> Database.getRooms()});
    }

    @FXML private void showTime() {
        setActiveButton(btnTime);
        mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/AdvanceTime.fxml", null);
    }

    @FXML private void showChat() {
        setActiveButton(btnChat);
        mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/Chat.fxml", receptionist);
    }

    @FXML private void doLogout() {
        mainApp.showLoginScreen();
    }

    // Kept for backward compat
    @FXML private void showToday() {
        setActiveButton(btnReservations);
        mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/ReceptionistReservations.fxml",
                ReceptionistReservationsController.ViewMode.CHECKING_IN);
    }

    @FXML private void showCheckIn() {
        setActiveButton(btnReservations);
        mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/ReceptionistReservations.fxml",
                ReceptionistReservationsController.ViewMode.CHECKING_IN);
    }

    @FXML private void showCheckOut() {
        setActiveButton(btnReservations);
        mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/ReceptionistReservations.fxml",
                ReceptionistReservationsController.ViewMode.CHECKING_OUT);
    }
}
