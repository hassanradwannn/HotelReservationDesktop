import Controllers.*;
import Models.*;
import Repositories.*;
import Services.*;
import Utils.*;
import Utils.exceptions.*;
import Repositories.database.DatabaseConnection;
import Repositories.DatabaseInitializer.*;
import Repositories.DatabaseInitializer.DatabaseInitializer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.image.Image;
import java.util.List;

import java.util.concurrent.atomic.AtomicBoolean;

public class Main extends Application implements AppContext {

    private Stage stage;
    private User currentUser;
    private Room selectedRoomForReservation;
    private RoomType selectedRoomTypeForReservation;
    private ReservationSearchContext guestHomeSearchContext;
    private List<Room> lastGuestSearchResults;
    private VBox currentContentArea;
    private DashboardController currentDashboardController;
    private Timeline autoRefreshTimeline;
    private Runnable currentViewRefresher;
    private volatile long localDataVersion = -1;
    private final AtomicBoolean refreshInProgress = new AtomicBoolean(false);
    private String currentView = "";
    private static Main instance;

    private final int WIDTH = 1280;
    private final int HEIGHT = 720;

    public static Main getInstance() { return instance; }

    public void setCurrentViewRefresher(Runnable currentViewRefresher) {
        this.currentViewRefresher = currentViewRefresher;
    }

    public Runnable getCurrentViewRefresher() {
        return currentViewRefresher;
    }

    private static final String BG = "#F7EFE5";
    private static final String CARD = "#EFE4D6";
    private static final String CARD_LIGHT = "#F9F3EA";
    private static final String ROSE = "#C74261";
    private static final String ROSE_SOFT = "#E6A4B4";
    private static final String BURGUNDY = "#8F1D3F";
    private static final String GOLD = "#C9AA7C";
    private static final String TEXTDARK = "#2B2421";
    private static final String TEXT = "#FFFFFF";
    private static final String MUTED = "#8A726B";
    private static final String WHITE = "#FFFFFF";

    @Override
    public void start(Stage stage) {
        instance = this;
        DatabaseInitializer.initializeDatabase();
        ChatServer.startInBackgroundIfAvailable();
        UserDatabase.clearLoggedInUsers();
        if (Database.isFirstInstance()) {
            SystemTime.resetToRealToday();
        } else {
            SystemTime.syncFromDatabase();
        }

        Database.loadAll();

        this.stage = stage;
        stage.setTitle("Grand Budapest Hotel Reservation System");
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/grandbudapestlogo.jpg")));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Authentication.logout(currentUser);
            Database.unregisterInstance();
        }));

        stage.setOnCloseRequest(event -> {
            System.exit(0);
        });


        showLoginScreen();
        stage.show();
    }

    public void showLoginScreen() {
        Authentication.logout(currentUser);
        currentUser = null;
        selectedRoomForReservation = null;
        selectedRoomTypeForReservation = null;
        guestHomeSearchContext = null;
        lastGuestSearchResults = null;

        // Logging out resets view state and stops any dashboard polling tied to the previous user.
        if (autoRefreshTimeline != null) {
            autoRefreshTimeline.stop();
            autoRefreshTimeline = null;
        }
        localDataVersion = -1;

        switchScene("/Login.fxml");
    }

    public void switchScene(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            javafx.scene.Parent root = loader.load();

            Object controller = loader.getController();
            if (controller != null) {
                try {
                    // Standalone controllers expose setMainApp; simple FXML views can skip it.
                    controller.getClass().getMethod("setMainApp", AppContext.class).invoke(controller, this);
                } catch (Exception e) {
                }
            }
            stage.setScene(new Scene(root, WIDTH, HEIGHT));
        } catch (Exception e) {
            System.out.println("Could not load scene: " + fxmlFile);
            e.printStackTrace();
        }
    }

    public void switchDashboardContent(VBox contentArea, String fxmlFile, Object data) {
        this.currentView = fxmlFile;
        this.currentContentArea = contentArea;
        this.currentViewRefresher = null;
        try {
            Object viewData = data;
            if (currentUser instanceof Guest && "/GuestHome.fxml".equals(fxmlFile)
                    && data instanceof Guest
                    && guestHomeSearchContext != null) {
                viewData = new GuestHomeController.HomeSearchState(guestHomeSearchContext, false);
            } else if (currentUser instanceof Guest && "/GuestHome.fxml".equals(fxmlFile)
                    && data instanceof GuestHomeController.HomeSearchState) {
                viewData = data;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            javafx.scene.Node node = loader.load();

            Object controller = loader.getController();
            if (controller instanceof DashboardContentController contentController) {
                contentController.initData(this, viewData);
            }

            if (currentUser instanceof Guest && !"/GuestHome.fxml".equals(fxmlFile)) {
                VBox guestPage = new VBox(14);
                guestPage.getStyleClass().add("guest-subpage-shell");
                javafx.scene.layout.VBox.setVgrow(guestPage, javafx.scene.layout.Priority.ALWAYS);
                guestPage.setMaxHeight(Double.MAX_VALUE);

                ReservationSearchContext reservationBackContext = extractReservationBackContext(fxmlFile, data);
                boolean returningToReservation = reservationBackContext != null;
                Button backHome = new Button(returningToReservation ? "BACK TO RESERVATION MENU" : "HOME");
                backHome.getStyleClass().addAll("outline-action-btn", "guest-back-home-btn");
                if (returningToReservation) {
                    backHome.getStyleClass().add("guest-reservation-back-btn");
                }
                backHome.setOnAction(event -> {
                    if (returningToReservation) {
                        selectedRoomForReservation = null;
                        switchDashboardContent(getCurrentContentArea(), "/MakeReservation.fxml", reservationBackContext);
                    } else {
                        switchDashboardContent(getCurrentContentArea(), "/GuestHome.fxml", currentUser);
                    }
                });

                HBox backRow = new HBox(backHome);
                backRow.getStyleClass().add("guest-back-row");
                javafx.scene.layout.VBox.setVgrow(node, javafx.scene.layout.Priority.ALWAYS);
                node.setStyle(node.getStyle() != null ? node.getStyle() : "");
                guestPage.getChildren().addAll(backRow, node);
                contentArea.getChildren().setAll(guestPage);
                this.currentContentArea = contentArea;
            } else {
                javafx.scene.layout.VBox.setVgrow(node, javafx.scene.layout.Priority.ALWAYS);
                contentArea.getChildren().setAll(node);
                this.currentContentArea = contentArea;
            }
        } catch (Exception e) {
            System.out.println("Could not load FXML: " + fxmlFile);
            e.printStackTrace();
        }
    }

    private ReservationSearchContext extractReservationBackContext(String fxmlFile, Object data) {
        if (!"/AvailableRooms.fxml".equals(fxmlFile) && !"/RoomDetails.fxml".equals(fxmlFile)) {
            return null;
        }
        if (data instanceof Object[] args) {
            for (Object arg : args) {
                if (arg instanceof ReservationSearchContext context) {
                    return context;
                }
            }
        }
        return null;
    }

    public void refreshActiveView() {
        if (currentView.equals("AllReservations") || currentView.equals("/GenericList.fxml")) {
            if (currentViewRefresher != null) currentViewRefresher.run();
        } else if (currentView.equals("LiveChat") || currentView.equals("/Chat.fxml")) {
            if (currentViewRefresher != null) currentViewRefresher.run();
        } else {
            if (currentViewRefresher != null) currentViewRefresher.run();
        }
    }

    public VBox getCurrentContentArea() {
        return currentContentArea;
    }

    public Room getSelectedRoomForReservation() {
        return selectedRoomForReservation;
    }

    public void setSelectedRoomForReservation(Room room) {
        this.selectedRoomForReservation = room;
    }

    public RoomType getSelectedRoomTypeForReservation() {
        return selectedRoomTypeForReservation;
    }

    public void setSelectedRoomTypeForReservation(RoomType type) {
        this.selectedRoomTypeForReservation = type;
    }

    public ReservationSearchContext getGuestHomeSearchContext() {
        return guestHomeSearchContext;
    }

    public void setGuestHomeSearchContext(ReservationSearchContext guestHomeSearchContext) {
        this.guestHomeSearchContext = guestHomeSearchContext;
    }

    public List<Room> getLastGuestSearchResults() {
        return lastGuestSearchResults;
    }

    public void setLastGuestSearchResults(List<Room> lastGuestSearchResults) {
        this.lastGuestSearchResults = lastGuestSearchResults;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    private String userInfoText(User user) {
        return "Logged in as: " + user.getUsername()
                + "   |   Date: "
                + SystemTime.getToday().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private String receptionistDateText() {
        return "Today's Date: "
                + SystemTime.getToday().format(java.time.format.DateTimeFormatter.ofPattern("d/M/yyyy"));
    }

    private void refreshRuntimeDataFromDatabase() {
        synchronized (Database.class) {
            Database.refreshUsersFromDatabase();
            Database.loadAllRoomTypes();
            Database.loadAllAmenities();
            Database.loadAllRooms();
            Database.loadAllReservations();
        }
    }

    public void showGuestDashboard(Guest guest) {
        try {
            this.currentUser = guest;

            if (autoRefreshTimeline != null) {
                autoRefreshTimeline.stop();
            }

            autoRefreshTimeline = new Timeline(new KeyFrame(Duration.millis(150), event -> {
                new Thread(() -> {
                    long currentVersion = Database.getLatestDataVersion();
                    if (localDataVersion == -1) {
                        localDataVersion = currentVersion;
                    } else if (currentVersion > localDataVersion && refreshInProgress.compareAndSet(false, true)) {
                        try {
                            System.out.println("Database changes detected! Syncing view...");
                            localDataVersion = currentVersion;
                            refreshRuntimeDataFromDatabase();
                            SystemTime.syncFromDatabase();
                            javafx.application.Platform.runLater(() -> Main.getInstance().refreshActiveView());
                        } finally {
                            refreshInProgress.set(false);
                        }
                    }
                }).start();
            }));
            autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
            autoRefreshTimeline.play();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Dashboard.fxml"));
            BorderPane root = loader.load();

            DashboardController controller = loader.getController();
            this.currentDashboardController = controller;
            controller.setTitle("Guest Dashboard");
            SystemTime.syncFromDatabase();
            controller.setUserInfo(userInfoText(guest));

            VBox menu = controller.getSideMenu();
            VBox content = controller.getContentArea();
            content.setPadding(Insets.EMPTY);
            this.currentContentArea = content;

            root.setLeft(null);
            root.setTop(null);
            menu.getChildren().clear();
            switchDashboardContent(content, "/GuestHome.fxml", guest);

            stage.setScene(new Scene(root, WIDTH, HEIGHT));
        } catch (Exception e) { e.printStackTrace(); }
    }
    public void showAdminDashboard(Admin admin) {
        try {
            this.currentUser = admin;

            if (autoRefreshTimeline != null) {
                autoRefreshTimeline.stop();
            }

            autoRefreshTimeline = new Timeline(new KeyFrame(Duration.millis(500), event -> {
                new Thread(() -> {
                    long currentVersion = Database.getLatestDataVersion();
                    if (localDataVersion == -1) {
                        localDataVersion = currentVersion;
                    } else if (currentVersion > localDataVersion && refreshInProgress.compareAndSet(false, true)) {
                        try {
                            System.out.println("Database changes detected! Syncing view...");
                            localDataVersion = currentVersion;

                            refreshRuntimeDataFromDatabase();

                            SystemTime.syncFromDatabase();
                            javafx.application.Platform.runLater(() -> {
                                if (currentDashboardController != null && currentUser != null) {
                                    currentDashboardController.setUserInfo(userInfoText(currentUser));
                                }
                                Main.getInstance().refreshActiveView();
                            });
                        } finally {
                            refreshInProgress.set(false);
                        }
                    }
                }).start();
            }));
            autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
            autoRefreshTimeline.play();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Dashboard.fxml"));
            BorderPane root = loader.load();

            DashboardController controller = loader.getController();
            this.currentDashboardController = controller;
            controller.setTitle("Admin Dashboard");
            SystemTime.syncFromDatabase();
            controller.setUserInfo(userInfoText(admin));

            VBox menu = controller.getSideMenu();
            VBox content = controller.getContentArea();
            this.currentContentArea = content;

            FXMLLoader menuLoader = new FXMLLoader(getClass().getResource("/AdminMenu.fxml"));
            VBox menuContent = menuLoader.load();
            AdminMenuController menuController = menuLoader.getController();
            menuController.initData(this, admin);

            menu.getChildren().setAll(menuContent);
            menuController.loadDefaultView();

            stage.setScene(new Scene(root, WIDTH, HEIGHT));
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void showReceptionistDashboard(Receptionist rec) {
        try {
            this.currentUser = rec;

            if (autoRefreshTimeline != null) {
                autoRefreshTimeline.stop();
            }

            autoRefreshTimeline = new Timeline(new KeyFrame(Duration.millis(500), event -> {
                new Thread(() -> {
                    long currentVersion = Database.getLatestDataVersion();
                    if (localDataVersion == -1) {
                        localDataVersion = currentVersion;
                    } else if (currentVersion > localDataVersion && refreshInProgress.compareAndSet(false, true)) {
                        try {
                            System.out.println("Database changes detected! Syncing view...");
                            localDataVersion = currentVersion;
                            refreshRuntimeDataFromDatabase();
                            SystemTime.syncFromDatabase();
                            javafx.application.Platform.runLater(() -> {
                                if (currentDashboardController != null && currentUser != null) {
                                    currentDashboardController.setUserInfo(userInfoText(currentUser));
                                }
                                Main.getInstance().refreshActiveView();
                            });
                        } finally {
                            refreshInProgress.set(false);
                        }
                    }
                }).start();
            }));
            autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
            autoRefreshTimeline.play();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Dashboard.fxml"));
            BorderPane root = loader.load();

            DashboardController controller = loader.getController();
            this.currentDashboardController = controller;
            controller.setTitle("Receptionist Dashboard");
            SystemTime.syncFromDatabase();
            controller.setUserInfo(userInfoText(rec));

            // The receptionist dashboard reuses Dashboard.fxml but swaps the sidebar for a top nav.
            controller.styleAsReceptionistInfoBar();
            controller.setTitle(receptionistDateText());
            controller.setUserInfo("RECEPTIONIST");

            VBox sidebar = controller.getSideMenu();
            sidebar.setManaged(false);
            sidebar.setVisible(false);
            sidebar.setPrefWidth(0);

            HBox topBar = controller.getTopBar();
            if (topBar != null) {
                topBar.setManaged(true);
                topBar.setVisible(true);
                topBar.setPrefHeight(42);
                topBar.setMinHeight(42);
                topBar.setMaxHeight(42);
                topBar.getStyleClass().removeAll("topbar");
                topBar.getStyleClass().add("topbar-info");
            }

            VBox content = controller.getContentArea();
            this.currentContentArea = content;

            FXMLLoader menuLoader = new FXMLLoader(getClass().getResource("/ReceptionistMenu.fxml"));
            HBox menuContent = menuLoader.load();
            ReceptionistMenuController menuController = menuLoader.getController();
            menuController.initData(this, rec);

            HBox navBar = controller.getNavBar();
            if (navBar != null) {
                navBar.setPrefHeight(58);
                navBar.setMinHeight(58);
                navBar.setMaxHeight(58);
                menuContent.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(menuContent, javafx.scene.layout.Priority.ALWAYS);
                navBar.getChildren().setAll(menuContent);
                // Inline styles win over stylesheet rules, so clear this before applying .topnav.
                navBar.setStyle(null);
            }

            menuController.loadDefaultView();

            stage.setScene(new Scene(root, WIDTH, HEIGHT));
        } catch (Exception e) { e.printStackTrace(); }
    }

    public String money(double value) {
        return String.format("%.2f", value);
    }

    public void alert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
