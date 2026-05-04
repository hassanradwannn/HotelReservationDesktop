import java.time.format.DateTimeFormatter;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class ReceptionistReservationCardController {

    @FXML private StackPane root;
    @FXML private HBox cardBody;
    @FXML private Region accentBar;
    @FXML private Label iconLabel;
    @FXML private TextFlow typeNameFlow;
    @FXML private Text typeNameText;
    @FXML private Label subLineLabel;
    @FXML private Label guestHeaderLabel;
    @FXML private Label guestValueLabel;
    @FXML private Label checkInHeaderLabel;
    @FXML private Label checkInValueLabel;
    @FXML private Label checkOutHeaderLabel;
    @FXML private Label checkOutValueLabel;
    @FXML private Label paidHeaderLabel;
    @FXML private Label paidValueLabel;
    @FXML private Label outstandingHeaderLabel;
    @FXML private Label outstandingValueLabel;
    @FXML private Label statusBadgeLabel;
    @FXML private HBox actionBox;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("d/M/yyyy");
    private static final String CREAM = "#F9F3EA";
    private static final String CRIMSON = "#C4415D";
    private static final String BURGUNDY = "#8B2040";
    private static final String GOLD = "#C8A97E";
    private static final String DARK = "#2C2523";
    private static final String BLUSH = "#F7D6D9";
    private static final String ROSE = "#E8879A";

    private Main mainApp;
    private Reservation reservation;
    private String sourceTitle;
    private Runnable refreshHandler;

    public void setData(Main mainApp, Reservation reservation, String sourceTitle, Runnable refreshHandler) {
        this.mainApp = mainApp;
        this.reservation = reservation;
        this.sourceTitle = sourceTitle;
        this.refreshHandler = refreshHandler;
        render();
    }

    private void render() {
        boolean isCancelled = reservation.getStatus() == ReservationStatus.CANCELLED;
        String roomTypeName = reservation.getRoom().getRoomType().getName();

        accentBar.setStyle("-fx-background-color: " + statusAccentColor(reservation.getStatus()) + ";");
        boolean isLobbyDeluxe = roomTypeName.toLowerCase().contains("deluxe")
                || roomTypeName.toLowerCase().contains("lobby");
        iconLabel.setText(roomTypeIcon(roomTypeName));
        iconLabel.setStyle(
                "-fx-font-size: " + iconFontSize(roomTypeName) + "px;"
                        + "-fx-text-fill: " + iconForeground(roomTypeName) + ";"
                        + "-fx-background-color: " + iconBackground(roomTypeName) + ";"
                        + "-fx-background-radius: 10;"
                        + "-fx-border-color: " + (isLobbyDeluxe ? GOLD : "transparent") + ";"
                        + "-fx-border-width: " + (isLobbyDeluxe ? "2.5" : "0") + ";"
                        + "-fx-border-radius: 10;"
                        + "-fx-min-width: 72px; -fx-min-height: 72px;"
                        + "-fx-max-width: 72px; -fx-max-height: 72px;"
                        + "-fx-alignment: center;"
                        + (isMendleClassic(roomTypeName) ? "-fx-font-weight: bold;" : "")
                        + (isCancelled ? "-fx-opacity: 0.6;" : ""));

        typeNameText.setText(roomTypeName);
        typeNameText.setStyle(
                "-fx-font-family: 'Georgia';"
                        + "-fx-font-size: 18px;"
                        + "-fx-fill: " + (isCancelled ? "#9e8880" : DARK) + ";");
        typeNameText.setStrikethrough(isCancelled);
        typeNameFlow.setStyle("-fx-font-family: 'Georgia';");

        subLineLabel.setText("Room " + reservation.getRoom().getRoomNumber()
                + " - " + reservation.getReservationId());
        subLineLabel.setStyle(
                "-fx-font-family: 'Georgia';"
                        + "-fx-font-size: 12px;"
                        + "-fx-text-fill: " + (isCancelled ? "#c4a098" : CRIMSON) + ";");

        styleMetricLabels(isCancelled);
        guestValueLabel.setText(reservation.getGuest().getUsername());
        checkInValueLabel.setText(reservation.getCheckInDate().format(DATE_FMT));
        checkOutValueLabel.setText(reservation.getCheckOutDate().format(DATE_FMT));

        double paid = ReservationService.getGrossPaidAmountBeforeRefunds(reservation);
        double refunded = ReservationService.getEarlyCheckOutRefundAmount(reservation);
        double refundDue = Math.max(0, paid - refunded - reservation.getTotalPrice());
        double refundDisplay = refunded > 0.009 ? refunded : refundDue;
        double outstanding = reservation.getStatus() == ReservationStatus.COMPLETED
                ? 0
                : Math.max(0, reservation.getTotalPrice() - paid);
        paidValueLabel.setText("$" + mainApp.money(reservation.getStatus() == ReservationStatus.COMPLETED
                ? Math.max(paid, reservation.getTotalPrice())
                : paid));
        if (refundDisplay > 0.009) {
            outstandingHeaderLabel.setText("REFUND");
            outstandingValueLabel.setText("$" + mainApp.money(refundDisplay));
        } else {
            outstandingHeaderLabel.setText("OUTSTANDING");
            outstandingValueLabel.setText("$" + mainApp.money(outstanding));
        }

        styleStatusBadge();
        addActionButtons();

        root.setAlignment(javafx.geometry.Pos.TOP_LEFT);
        root.setMaxWidth(Double.MAX_VALUE);
        root.setCursor(Cursor.HAND);
        cardBody.setStyle(cardStyle(isCancelled));
        root.setOnMouseEntered(e ->
                cardBody.setStyle(cardStyle(isCancelled)
                        + "-fx-effect: dropshadow(gaussian, rgba(44,37,35,0.16), 14, 0.1, 0, 4);"));
        root.setOnMouseExited(e -> cardBody.setStyle(cardStyle(isCancelled)));
        root.setOnMouseClicked(e -> {
            if (isInsideButton(e.getPickResult().getIntersectedNode())) {
                return;
            }
            openReservationDetail();
        });
    }

    private void styleMetricLabels(boolean faded) {
        styleMetricHeader(guestHeaderLabel, faded);
        styleMetricHeader(checkInHeaderLabel, faded);
        styleMetricHeader(checkOutHeaderLabel, faded);
        styleMetricHeader(paidHeaderLabel, faded);
        styleMetricHeader(outstandingHeaderLabel, faded);
        styleMetricValue(guestValueLabel, faded);
        styleMetricValue(checkInValueLabel, faded);
        styleMetricValue(checkOutValueLabel, faded);
        styleMetricValue(paidValueLabel, faded);
        styleMetricValue(outstandingValueLabel, faded);
    }

    private void styleMetricHeader(Label label, boolean faded) {
        label.setStyle(
                "-fx-font-family: 'Georgia';"
                        + "-fx-font-size: 10px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-text-fill: " + (faded ? "#c4a8a0" : BURGUNDY) + ";");
    }

    private void styleMetricValue(Label label, boolean faded) {
        label.setStyle(
                "-fx-font-family: 'Georgia';"
                        + "-fx-font-size: 15px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-text-fill: " + (faded ? "#b8a8a4" : DARK) + ";");
    }

    private void styleStatusBadge() {
        statusBadgeLabel.setText(displayStatus(reservation.getStatus()));
        String[] c = badgeColors(reservation.getStatus());
        statusBadgeLabel.setStyle(
                "-fx-background-color: " + c[0] + ";"
                        + "-fx-text-fill: " + c[1] + ";"
                        + "-fx-font-family: 'Georgia';"
                        + "-fx-font-size: 11px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-padding: 5 18 5 18;"
                        + "-fx-background-radius: 20;");
    }

    private void addActionButtons() {
        actionBox.getChildren().clear();
        switch (reservation.getStatus()) {
            case CHECKING_IN -> actionBox.getChildren().add(outlineBtn("CHECK IN", e -> handleCheckIn()));
            case CHECKING_OUT -> actionBox.getChildren().add(outlineBtn("CHECK OUT", e -> openReservationDetail()));
            case ONGOING -> actionBox.getChildren().add(outlineBtn("EXTEND STAY", e -> openReservationDetail(true)));
            default -> actionBox.getChildren().add(outlineBtn("DETAILS", e -> openReservationDetail()));
        }
    }

    private void handleCheckIn() {
        try {
            ReservationService.checkInGuest(reservation, reservation.getGuest());
            mainApp.alert("Success", "Guest checked in.");
            refreshHandler.run();
        } catch (Exception ex) {
            mainApp.alert("Error", ex.getMessage());
        }
    }

    private void openReservationDetail() {
        openReservationDetail(false);
    }

    private void openReservationDetail(boolean extendStay) {
        mainApp.switchDashboardContent(
                mainApp.getCurrentContentArea(),
                "/ReservationDetail.fxml",
                new Object[]{reservation, sourceTitle, extendStay});
    }

    private boolean isInsideButton(Node node) {
        while (node != null) {
            if (node instanceof Button) {
                return true;
            }
            node = node.getParent();
        }
        return false;
    }

    private Button outlineBtn(String text, EventHandler<ActionEvent> handler) {
        Button btn = new Button(text);
        String base =
                "-fx-background-color: transparent;"
                        + "-fx-border-color: " + GOLD + ";"
                        + "-fx-border-width: 1.5;"
                        + "-fx-border-radius: 0;"
                        + "-fx-background-radius: 0;"
                        + "-fx-text-fill: " + BURGUNDY + ";"
                        + "-fx-font-family: 'Georgia';"
                        + "-fx-font-size: 11px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-padding: 6 16 6 16;"
                        + "-fx-cursor: hand;";
        String hover =
                "-fx-background-color: " + BLUSH + ";"
                        + "-fx-border-color: " + CRIMSON + ";"
                        + "-fx-border-width: 1.5;"
                        + "-fx-border-radius: 0;"
                        + "-fx-background-radius: 0;"
                        + "-fx-text-fill: " + CRIMSON + ";"
                        + "-fx-font-family: 'Georgia';"
                        + "-fx-font-size: 11px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-padding: 6 16 6 16;"
                        + "-fx-cursor: hand;";
        btn.setStyle(base);
        btn.setFocusTraversable(false);
        btn.setMinWidth(140);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        btn.setOnAction(handler);
        return btn;
    }

    private String[] badgeColors(ReservationStatus status) {
        return switch (status) {
            case PENDING -> new String[]{"rgba(220, 158, 45, 0.85)", "#FFFFFF"};
            case CONFIRMED -> new String[]{"rgba(95, 148, 205, 0.82)", "#FFFFFF"};
            case CHECKING_IN -> new String[]{"rgba(95, 148, 205, 0.92)", "#FFFFFF"};
            case ONGOING -> new String[]{"rgba(70, 168, 95, 0.85)", "#FFFFFF"};
            case CHECKING_OUT -> new String[]{"rgba(196, 65, 93, 0.86)", "#FFFFFF"};
            case COMPLETED -> new String[]{"rgba(155, 148, 145, 0.80)", "#FFFFFF"};
            case CANCELLED -> new String[]{"rgba(208, 130, 142, 0.82)", "#FFFFFF"};
        };
    }

    private String cardStyle(boolean cancelled) {
        return "-fx-background-color: " + CREAM + ";"
                + "-fx-background-radius: 0;"
                + "-fx-border-color: " + GOLD + ";"
                + "-fx-border-radius: 0;"
                + "-fx-border-width: 1;"
                + (cancelled ? "-fx-opacity: 0.72;" : "")
                + "-fx-effect: dropshadow(gaussian, rgba(44,37,35,0.08), 8, 0.08, 0, 2);";
    }

    private String statusAccentColor(ReservationStatus status) {
        return switch (status) {
            case PENDING -> "#DCA032";
            case CONFIRMED -> "#5F94CD";
            case CHECKING_IN -> "#5F94CD";
            case ONGOING -> "#46A85F";
            case CHECKING_OUT -> CRIMSON;
            case COMPLETED -> "#9B9491";
            case CANCELLED -> "#D0828E";
        };
    }

    private String displayStatus(ReservationStatus status) {
        return status.toString().replace('_', ' ');
    }

    private String iconBackground(String name) {
        String n = name.toLowerCase();
        if (n.contains("alpine") || n.contains("grand") || n.contains("suite")) return BURGUNDY;
        if (n.contains("penthouse") || n.contains("gustave")) return CRIMSON;
        if (n.contains("deluxe") || n.contains("lobby")) return "#F3EADF";
        if (n.contains("classic") || n.contains("mendle")) return ROSE;
        return CRIMSON;
    }

    private String iconForeground(String name) {
        String n = name.toLowerCase();
        if (n.contains("deluxe") || n.contains("lobby")) return GOLD;
        return CREAM;
    }

    private String iconFontSize(String name) {
        if (isMendleClassic(name)) return "36";
        return "26";
    }

    private boolean isMendleClassic(String name) {
        String n = name.toLowerCase();
        return n.contains("classic") || n.contains("mendle");
    }

    private String roomTypeIcon(String name) {
        String n = name.toLowerCase();
        if (n.contains("suite") || n.contains("grand") || n.contains("alpine")) return "\u2726";
        if (n.contains("penthouse") || n.contains("gustave")) return "\u2767";
        if (n.contains("deluxe") || n.contains("lobby")) return "\u25C6";
        if (n.contains("classic") || n.contains("mendle")) return "\u2299";
        return "\u25C8";
    }
}
