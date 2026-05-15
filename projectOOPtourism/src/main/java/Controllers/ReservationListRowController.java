package Controllers;


import Utils.AppContext;
import Models.*;
import Repositories.*;
import Services.*;
import Utils.*;
import Utils.exceptions.*;
import Repositories.database.DatabaseConnection;
import Repositories.DatabaseInitializer.*;
import java.time.format.DateTimeFormatter;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.HBox;

public class ReservationListRowController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML private HBox root;
    @FXML private Label reservationIdLabel;
    @FXML private Label guestLabel;
    @FXML private Label roomLabel;
    @FXML private Label datesLabel;
    @FXML private Label statusLabel;

    public void setReservation(Reservation reservation) {
        setFixedColumn(reservationIdLabel, "ID: " + reservation.getReservationId(), 190);
        setFixedColumn(guestLabel, "Guest: " + reservation.getGuest().getUsername(), 150);
        setFixedColumn(roomLabel, "Room: " + reservation.getRoom().getRoomNumber(), 100);
        setFixedColumn(
                datesLabel,
                reservation.getCheckInDate().format(DATE_FORMAT)
                        + " : " + reservation.getCheckOutDate().format(DATE_FORMAT),
                215);
        setFixedColumn(statusLabel, "Status: " + reservation.getStatus(), 160);
    }

    private void setFixedColumn(Label label, String text, double width) {
        label.setText(text);
        label.setMinWidth(width);
        label.setPrefWidth(width);
        label.setMaxWidth(width);
        label.setTextOverrun(OverrunStyle.ELLIPSIS);
    }
}
