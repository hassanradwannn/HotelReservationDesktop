import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class DashboardController {

    @FXML
    private Label welcomeLabel;

    @FXML
    private TableView<Room> roomTable;

    @FXML
    private TableColumn<Room, String> roomNumberCol;

    @FXML
    private TableColumn<Room, String> roomTypeCol;

    @FXML
    private VBox guestMenuBox;

    @FXML
    private VBox roomsBox;

    public void initData(User user) {
        if (user != null) {
            welcomeLabel.setText("Hi, " + user.getUsername());
        }

        // If roomTable exists (meaning we are on the Guest Dashboard)
        if (roomTable != null) {
            roomNumberCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getRoomNumber()));
            roomTypeCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getRoomType().getName()));

            ObservableList<Room> rooms = FXCollections.observableArrayList(Database.getRooms());
            roomTable.setItems(rooms);

            roomTable.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) { // Trigger on double-click
                    Room selectedRoom = roomTable.getSelectionModel().getSelectedItem();
                    if (selectedRoom != null) {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION, "You clicked Room: " + selectedRoom.getRoomNumber() + "\nType: " + selectedRoom.getRoomType().getName());
                        alert.setTitle("Room Details");
                        alert.setHeaderText("Room Selected");
                        alert.showAndWait();
                    }
                }
            });
        }
    }

    @FXML
    public void showMakeReservation(ActionEvent event) {
        guestMenuBox.setVisible(false);
        roomsBox.setVisible(true);
    }

    @FXML
    public void showGuestMenu(ActionEvent event) {
        roomsBox.setVisible(false);
        guestMenuBox.setVisible(true);
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("login.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setTitle("The Grand Budapest Hotel");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}