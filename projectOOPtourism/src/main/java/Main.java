import DatabaseInitializer.DatabaseInitializer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Main extends Application {

    private Stage stage;
    private User currentUser;
    private Room selectedRoomForReservation;
    private RoomType selectedRoomTypeForReservation;
    private VBox currentContentArea;
    private DashboardController currentDashboardController;
    private Timeline autoRefreshTimeline;
    private Runnable currentViewRefresher;
    private volatile long localDataVersion = -1;
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
        if (Database.isFirstInstance()) {
            SystemTime.resetToRealToday();
        } else {
            SystemTime.syncFromDatabase();
        }
        
        Database.loadAll(); // Seeds empty DBs, fetches all data fresh
        
        this.stage = stage;
        stage.setTitle("Grand Budapest Hotel Reservation System");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (currentUser != null) {
                Authentication.logout(currentUser);
            }
            Database.unregisterInstance();
        }));

        stage.setOnCloseRequest(event -> {
            if (currentUser != null) {
                Authentication.logout(currentUser);
            }
            System.exit(0);
        });

        showLoginScreen();
        stage.show();
    }

    public void showLoginScreen() {
        if (autoRefreshTimeline != null) {
            autoRefreshTimeline.stop();
            autoRefreshTimeline = null;
        }
        localDataVersion = -1;

        if (currentUser != null) {
            Authentication.logout(currentUser);
            currentUser = null;
        }

        switchScene("/Login.fxml");
    }

    public void switchScene(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            javafx.scene.Parent root = loader.load();
            
            Object controller = loader.getController();
            if (controller != null) {
                try {
                    controller.getClass().getMethod("setMainApp", Main.class).invoke(controller, this);
                } catch (Exception e) {
                    // Controller doesn't require mainApp reference, safely ignore
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
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            javafx.scene.Node node = loader.load();
            
            Object controller = loader.getController();
            if (controller instanceof DashboardContentController contentController) {
                contentController.initData(this, data);
            }
            contentArea.getChildren().setAll(node);
        } catch (Exception e) {
            System.out.println("Could not load FXML: " + fxmlFile);
            e.printStackTrace();
        }
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

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public void showGuestDashboard(Guest guest) {
        try {
            this.currentUser = guest;

            if (autoRefreshTimeline != null) {
                autoRefreshTimeline.stop();
            }

            autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(2), event -> {
                new Thread(() -> {
                    long currentVersion = Database.getLatestDataVersion();
                    if (localDataVersion == -1) {
                        localDataVersion = currentVersion;
                    } else if (currentVersion > localDataVersion) {
                        System.out.println("Database changes detected! Syncing view...");
                        localDataVersion = currentVersion;

                        Database.refreshUsersFromDatabase();
                        Database.loadAllRoomTypes();
                        Database.loadAllAmenities();
                        Database.loadAllRooms();
                        Database.loadAllReservations();

                        SystemTime.syncFromDatabase();
                        javafx.application.Platform.runLater(() -> {
                            if (currentDashboardController != null && currentUser != null) {
                                currentDashboardController.setUserInfo("Logged in as: " + currentUser.getUsername() + "   |   Date: " + SystemTime.getDate());
                            }
                            Main.getInstance().refreshActiveView();
                        });
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
            controller.setUserInfo("Logged in as: " + guest.getUsername() + "   |   Date: " + SystemTime.getDate());

            VBox menu = controller.getSideMenu();
            VBox content = controller.getContentArea();
            this.currentContentArea = content;

            FXMLLoader menuLoader = new FXMLLoader(getClass().getResource("/GuestMenu.fxml"));
            VBox menuContent = menuLoader.load();
            GuestMenuController menuController = menuLoader.getController();
            menuController.initData(this, guest);
            
            menu.getChildren().setAll(menuContent);
            menuController.loadDefaultView();
            
            stage.setScene(new Scene(root, WIDTH, HEIGHT));
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void showAdminDashboard(Admin admin) {
        try {
            this.currentUser = admin;

            if (autoRefreshTimeline != null) {
                autoRefreshTimeline.stop();
            }

            autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(2), event -> {
                new Thread(() -> {
                    long currentVersion = Database.getLatestDataVersion();
                    if (localDataVersion == -1) {
                        localDataVersion = currentVersion;
                    } else if (currentVersion > localDataVersion) {
                        System.out.println("Database changes detected! Syncing view...");
                        localDataVersion = currentVersion;

                        Database.refreshUsersFromDatabase();
                        Database.loadAllRoomTypes();
                        Database.loadAllAmenities();
                        Database.loadAllRooms();
                        Database.loadAllReservations();

                        SystemTime.syncFromDatabase();
                        javafx.application.Platform.runLater(() -> {
                            if (currentDashboardController != null && currentUser != null) {
                                currentDashboardController.setUserInfo("Logged in as: " + currentUser.getUsername() + "   |   Date: " + SystemTime.getDate());
                            }
                            Main.getInstance().refreshActiveView();
                        });
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
            controller.setUserInfo("Logged in as: " + admin.getUsername() + "   |   Date: " + SystemTime.getDate());

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

            autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(2), event -> {
                new Thread(() -> {
                    long currentVersion = Database.getLatestDataVersion();
                    if (localDataVersion == -1) {
                        localDataVersion = currentVersion;
                    } else if (currentVersion > localDataVersion) {
                        System.out.println("Database changes detected! Syncing view...");
                        localDataVersion = currentVersion;

                        Database.refreshUsersFromDatabase();
                        Database.loadAllRoomTypes();
                        Database.loadAllAmenities();
                        Database.loadAllRooms();
                        Database.loadAllReservations();

                        SystemTime.syncFromDatabase();
                        javafx.application.Platform.runLater(() -> {
                            if (currentDashboardController != null && currentUser != null) {
                                currentDashboardController.setUserInfo("Logged in as: " + currentUser.getUsername() + "   |   Date: " + SystemTime.getDate());
                            }
                            Main.getInstance().refreshActiveView();
                        });
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
            controller.setUserInfo("Logged in as: " + rec.getUsername() + "   |   Date: " + SystemTime.getDate());

            VBox menu = controller.getSideMenu();
            VBox content = controller.getContentArea();
            this.currentContentArea = content;

            FXMLLoader menuLoader = new FXMLLoader(getClass().getResource("/ReceptionistMenu.fxml"));
            VBox menuContent = menuLoader.load();
            ReceptionistMenuController menuController = menuLoader.getController();
            menuController.initData(this, rec);
            
            menu.getChildren().setAll(menuContent);
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
        // DatabaseSync.syncDefaultDataToMySQL();
        launch(args);
    }
}
