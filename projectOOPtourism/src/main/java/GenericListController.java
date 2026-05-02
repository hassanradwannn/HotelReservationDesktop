import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;

public class GenericListController implements DashboardContentController {
    private static final DateTimeFormatter RESERVATION_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML private Label titleLabel;
    @FXML private ListView<Object> listView;
    @FXML private HBox actionBar;
    private Main mainApp;
    private Supplier<List<?>> supplier;
    private String title;

    @Override
    public void initData(Main mainApp, Object data) {
        this.mainApp = mainApp;
        Object[] args = (Object[]) data;
        this.title = (String) args[0];
        titleLabel.setText(title);
        this.supplier = (Supplier<List<?>>) args[1];

        Runnable dataRefresher = () -> {
            List<?> list = supplier.get();
            @SuppressWarnings("unchecked")
            List<Object> typedList = (List<Object>) list;
            listView.setItems(FXCollections.observableArrayList(typedList));
        };
        
        dataRefresher.run();
        mainApp.setCurrentViewRefresher(dataRefresher);
        configureListCellFactory();

        // Add context-specific action buttons
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
                        Database.refreshReservationsFromDatabase();
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

    private void configureListCellFactory() {
        listView.setCellFactory(view -> new ListCell<>() {
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
                    setGraphic(createReservationRow(reservation));
                    setTooltip(new Tooltip(reservation.toString()));
                    return;
                }

                setText(item.toString());
                setGraphic(null);
                setTooltip(null);
            }
        });
    }

    private HBox createReservationRow(Reservation reservation) {
        HBox row = new HBox(8);
        row.getStyleClass().add("reservation-list-row");
        row.getChildren().addAll(
                fixedColumn("ID: " + reservation.getReservationId(), 190),
                fixedColumn("Guest: " + reservation.getGuest().getUsername(), 150),
                fixedColumn("Room: " + reservation.getRoom().getRoomNumber(), 100),
                fixedColumn(reservation.getCheckInDate().format(RESERVATION_DATE_FORMAT)
                        + " : " + reservation.getCheckOutDate().format(RESERVATION_DATE_FORMAT), 215),
                fixedColumn("Status: " + reservation.getStatus(), 160)
        );
        return row;
    }

    private Label fixedColumn(String text, double width) {
        Label label = new Label(text);
        label.getStyleClass().add("reservation-list-column");
        label.setMinWidth(width);
        label.setPrefWidth(width);
        label.setMaxWidth(width);
        label.setTextOverrun(OverrunStyle.ELLIPSIS);
        return label;
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
