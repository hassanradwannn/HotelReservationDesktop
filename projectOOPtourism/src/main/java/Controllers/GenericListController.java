package Controllers;


import Utils.AppContext;
import Models.*;
import Repositories.*;
import Services.*;
import Utils.*;
import Utils.exceptions.*;
import Repositories.database.DatabaseConnection;
import Repositories.DatabaseInitializer.*;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;

public class GenericListController implements DashboardContentController {
    @FXML private Label titleLabel;
    @FXML private ListView<Object> listView;
    @FXML private HBox actionBar;
    private AppContext mainApp;
    private Supplier<List<?>> supplier;
    private String title;
    private final ObservableList<Object> listItems = FXCollections.observableArrayList();
    private Task<List<Object>> dataLoadTask;
    private int dataLoadRequestId;

    @Override
    public void initData(AppContext mainApp, Object data) {
        this.mainApp = mainApp;
        Object[] args = (Object[]) data;
        this.title = (String) args[0];
        titleLabel.setText(title);
        this.supplier = (Supplier<List<?>>) args[1];
        listView.setItems(listItems);
        configurePlaceholder();
        configureListCellFactory();

        Runnable dataRefresher = this::loadDataAsync;
        
        dataRefresher.run();
        mainApp.setCurrentViewRefresher(dataRefresher);

        // Receptionists need quick reservation filters without leaving the generic list screen.
        if (actionBar != null && mainApp.getCurrentUser() instanceof Receptionist && title.contains("Reservations")) {
            Button todayBtn = new Button("For Today");
            todayBtn.getStyleClass().add("outline-action-btn");
            todayBtn.setPrefHeight(38);
            todayBtn.setOnAction(e -> mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/GenericList.fxml",
                    new Object[]{"Today's Reservations", (Supplier<List<?>>) Database::getTodaysReservations}));

            Button checkingOutBtn = new Button("Checking Out");
            checkingOutBtn.getStyleClass().add("outline-action-btn");
            checkingOutBtn.setPrefHeight(38);
            checkingOutBtn.setOnAction(e -> mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/GenericList.fxml",
                    new Object[]{"Checking Out Reservations", (Supplier<List<?>>) Database::getCheckingOutReservations}));

            Button allBtn = new Button("ALL");
            allBtn.getStyleClass().add("outline-action-btn");
            allBtn.setPrefHeight(38);
            allBtn.setOnAction(e -> mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/GenericList.fxml",
                    new Object[]{"All Reservations", (Supplier<List<?>>) () -> {
                        Database.refreshReservationsIfStale();
                        return Database.getReservations();
                    }}));

            actionBar.getChildren().addAll(todayBtn, checkingOutBtn, allBtn);
        }

        if (actionBar != null && mainApp.getCurrentUser() instanceof Admin) {
            switch (title) {
                case "Rooms" -> {
                    Button addBtn = new Button("ADD ROOM");
                    addBtn.getStyleClass().add("primary-action-btn");
                    addBtn.setPrefHeight(38);
                    addBtn.setOnAction(e -> mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/AddRoom.fxml", null));

                    Button deleteBtn = new Button("DELETE SELECTED");
                    deleteBtn.getStyleClass().add("danger-action-btn");
                    deleteBtn.setPrefHeight(38);
                    deleteBtn.setOnAction(e -> handleDeleteRoom(dataRefresher));

                    actionBar.getChildren().addAll(deleteBtn, addBtn);
                }
                case "Room Types" -> {
                    Button addBtn = new Button("ADD ROOM TYPE");
                    addBtn.getStyleClass().add("primary-action-btn");
                    addBtn.setPrefHeight(38);
                    addBtn.setOnAction(e -> mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/ManageRoomTypes.fxml", null));

                    Button deleteBtn = new Button("DELETE SELECTED");
                    deleteBtn.getStyleClass().add("danger-action-btn");
                    deleteBtn.setPrefHeight(38);
                    deleteBtn.setOnAction(e -> handleDeleteRoomType(dataRefresher));

                    actionBar.getChildren().addAll(deleteBtn, addBtn);
                }
            }
        }
        
        listView.setOnMouseClicked(event -> {
            if (listView.getSelectionModel().getSelectedItem() instanceof Reservation selectedReservation
                    && (mainApp.getCurrentUser() instanceof Receptionist || mainApp.getCurrentUser() instanceof Guest)) {
                mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/ReservationDetail.fxml",
                        new Object[]{selectedReservation, title});
                return;
            }
            if (event.getClickCount() == 2 && listView.getSelectionModel().getSelectedItem() != null && mainApp.getCurrentUser() instanceof Admin) {
                Object selectedItem = listView.getSelectionModel().getSelectedItem();
                switch (title) {
                    case "Rooms" -> mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/EditRoom.fxml", selectedItem);
                    case "Room Types" -> mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/EditRoomType.fxml", selectedItem);
                    case "Amenities" -> mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/EditAmenity.fxml", selectedItem);
                }
            }
        });
    }

    private void loadDataAsync() {
        int requestId = ++dataLoadRequestId;
        if (title != null && title.contains("Reservations")) {
            listView.setPlaceholder(styledPlaceholder("Loading reservation info..."));
        }
        if (dataLoadTask != null && dataLoadTask.isRunning()) {
            dataLoadTask.cancel();
        }

        dataLoadTask = new Task<>() {
            @Override
            protected List<Object> call() {
                synchronized (Database.class) {
                    return new java.util.ArrayList<>(supplier.get());
                }
            }
        };

        dataLoadTask.setOnSucceeded(event -> {
            if (requestId == dataLoadRequestId) {
                syncListItems(dataLoadTask.getValue());
                configurePlaceholder();
            }
        });

        dataLoadTask.setOnFailed(event -> {
            Throwable error = dataLoadTask.getException();
            if (error != null) {
                error.printStackTrace();
            }
            if (requestId == dataLoadRequestId) {
                listView.setPlaceholder(styledPlaceholder("Could not load data. Please check the database connection."));
                listItems.clear();
            }
        });

        Thread thread = new Thread(dataLoadTask, "generic-list-load");
        thread.setDaemon(true);
        thread.start();
    }

    private void configureListCellFactory() {
        listView.setCellFactory(view -> new ListCell<>() {
            private ReservationRowView reservationRowView;

            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setTooltip(null);
                    return;
                }

                if (item instanceof Reservation reservation) {
                    setText(null);
                    try {
                        if (reservationRowView == null) {
                            reservationRowView = loadReservationRow();
                        }
                        reservationRowView.controller().setReservation(reservation);
                        setGraphic(reservationRowView.row());
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        setGraphic(new Label("Could not load reservation row."));
                    }
                    setTooltip(new Tooltip(reservation.toString()));
                    return;
                }

                setText(item.toString());
                setGraphic(null);
                setTooltip(null);
            }
        });
    }

    private void configurePlaceholder() {
        if (title != null && title.contains("Reservations")) {
            listView.setPlaceholder(styledPlaceholder("No reservations available."));
        }
    }

    private Label styledPlaceholder(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("reservation-empty-message");
        return label;
    }

    private void syncListItems(List<Object> freshItems) {
        int index = 0;
        for (; index < freshItems.size(); index++) {
            Object freshItem = freshItems.get(index);
            if (index < listItems.size()) {
                if (listItems.get(index) != freshItem) {
                    listItems.set(index, freshItem);
                }
            } else {
                listItems.add(freshItem);
            }
        }

        if (listItems.size() > freshItems.size()) {
            listItems.remove(index, listItems.size());
        }
    }

    private ReservationRowView loadReservationRow() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ReservationListRow.fxml"));
        HBox row = loader.load();
        ReservationListRowController controller = loader.getController();
        return new ReservationRowView(row, controller);
    }

    private record ReservationRowView(HBox row, ReservationListRowController controller) {
    }

    private void handleDeleteRoom(Runnable refresher) {
        Object selected = listView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            mainApp.alert("No Selection", "Please select a room to delete.");
            return;
        }
        Room room = (Room) selected;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Room");
        confirm.setHeaderText(null);
        confirm.setContentText("Delete room " + room.getRoomNumber() + "? This cannot be undone.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                CatalogService.deleteRoom(room);
                Database.deleteRoomFromDB(room);
                refresher.run();
            } catch (Exception ex) {
                mainApp.alert("Error", ex.getMessage());
            }
        }
    }

    private void handleDeleteRoomType(Runnable refresher) {
        Object selected = listView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            mainApp.alert("No Selection", "Please select a room type to delete.");
            return;
        }
        RoomType rt = (RoomType) selected;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Room Type");
        confirm.setHeaderText(null);
        confirm.setContentText("Delete room type \"" + rt.getName() + "\"? This cannot be undone.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                CatalogService.deleteRoomType(rt);
                Database.deleteRoomTypeFromDB(rt);
                refresher.run();
            } catch (Exception ex) {
                mainApp.alert("Error", ex.getMessage());
            }
        }
    }
}
