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
import javafx.scene.layout.VBox;

public class ReceptionistReservationCardController {

    @FXML private StackPane root;
    @FXML private HBox cardBody;
    @FXML private Region accentBar;
    @FXML private StackPane avatarPane;
    @FXML private Label iconLabel;
    @FXML private Label typeNameText;
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

    // Palette
    private static final String CREAM   = "#F9F3EA";
    private static final String CRIMSON = "#C4415D";
    private static final String BURGUNDY= "#8B2040";
    private static final String GOLD    = "#C8A97E";
    private static final String DARK    = "#2C2523";
    private static final String BLUSH   = "#F7D6D9";
    private static final String ROSE    = "#E8879A";

    private Main mainApp;
    private Reservation reservation;
    private ReservationPaymentSummary paymentSummary;
    private String sourceTitle;
    private Runnable refreshHandler;

    public void setData(Main mainApp, Reservation reservation, String sourceTitle, Runnable refreshHandler) {
        setData(mainApp, reservation, null, sourceTitle, refreshHandler);
    }

    public void setData(Main mainApp, Reservation reservation, ReservationPaymentSummary paymentSummary,
                        String sourceTitle, Runnable refreshHandler) {
        this.mainApp = mainApp;
        this.reservation = reservation;
        this.paymentSummary = paymentSummary;
        this.sourceTitle = sourceTitle;
        this.refreshHandler = refreshHandler;
        render();
    }

    private void render() {
        boolean isCancelled = reservation.getStatus() == ReservationStatus.CANCELLED;
        String guestName = reservation.getGuest().getUsername();
        String accentColor = statusAccentColor(reservation.getStatus());

        // --- Accent bar ---
        accentBar.setStyle("-fx-background-color: " + accentColor + "; -fx-min-height: 120; -fx-pref-height: 120;");

        // --- Avatar ---
        String initials = getInitials(guestName);
        String avatarBg = avatarColor(reservation.getStatus());
        avatarPane.setStyle(
                "-fx-background-color: " + avatarBg + ";"
                + "-fx-background-radius: 50;"
                + "-fx-min-width: 56; -fx-min-height: 56;"
                + "-fx-max-width: 56; -fx-max-height: 56;"
                + (isCancelled ? "-fx-opacity: 0.65;" : ""));
        iconLabel.setText(initials);
        iconLabel.setStyle(
                "-fx-font-family: 'Georgia';"
                + "-fx-font-size: 18px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: white;"
                + "-fx-alignment: center;");

        // --- Guest name ---
        typeNameText.setText(guestName);
        typeNameText.setStyle(
                "-fx-font-family: 'Georgia';"
                + "-fx-font-size: 20px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: " + (isCancelled ? "#9e8880" : DARK) + ";");

        // --- Sub line: Room · Reservation ID ---
        subLineLabel.setText("Room " + reservation.getRoom().getRoomNumber()
                + " · " + reservation.getReservationId());
        subLineLabel.setStyle(
                "-fx-font-family: 'Georgia';"
                + "-fx-font-size: 12px;"
                + "-fx-text-fill: " + (isCancelled ? "#c4a098" : CRIMSON) + ";");

        // --- Metric headers ---
        String headerColor = isCancelled ? "#c4a8a0" : BURGUNDY;
        String valueColor  = isCancelled ? "#b8a8a4" : DARK;
        styleMetricLabel(checkInHeaderLabel,  "CHECK-IN",  headerColor, 10);
        styleMetricLabel(checkOutHeaderLabel, "CHECK-OUT", headerColor, 10);
        styleMetricLabel(paidHeaderLabel,     "TOTAL",     headerColor, 10);

        checkInValueLabel.setStyle(metricValueStyle(valueColor));
        checkOutValueLabel.setStyle(metricValueStyle(valueColor));
        paidValueLabel.setStyle(metricValueStyle(valueColor));

        checkInValueLabel.setText(reservation.getCheckInDate().format(DATE_FMT));
        checkOutValueLabel.setText(reservation.getCheckOutDate().format(DATE_FMT));

        // Payment total
        ReservationPaymentSummary summary = paymentSummary == null
                ? new ReservationPaymentSummary(
                        ReservationService.getGrossPaidAmountBeforeRefunds(reservation),
                        ReservationService.getEarlyCheckOutRefundAmount(reservation))
                : paymentSummary;
        double paid = summary.getGrossPaidAmountBeforeRefunds();
        double total = reservation.getTotalPrice();
        paidValueLabel.setText("$" + mainApp.money(total));

        // Hidden fields (controller contract)
        guestValueLabel.setText(guestName);
        outstandingValueLabel.setText("$" + mainApp.money(Math.max(0, total - paid)));

        // --- Status badge ---
        styleStatusBadge();

        // --- Action buttons ---
        addActionButtons();

        // --- Card shell ---
        root.setCursor(Cursor.HAND);
        cardBody.setStyle(cardStyle(isCancelled));
        root.setOnMouseEntered(e -> cardBody.setStyle(cardHoverStyle(isCancelled)));
        root.setOnMouseExited(e  -> cardBody.setStyle(cardStyle(isCancelled)));
        root.setOnMouseClicked(e -> {
            if (isInsideButton(e.getPickResult().getIntersectedNode())) return;
            openReservationDetail();
        });
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String getInitials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    private String avatarColor(ReservationStatus status) {
        return switch (status) {
            case PENDING      -> "#8B2040";
            case CONFIRMED    -> "#5F94CD";
            case CHECKING_IN  -> "#5F94CD";
            case ONGOING      -> "#46A85F";
            case CHECKING_OUT -> "#C4415D";
            case COMPLETED    -> "#9B9491";
            case CANCELLED    -> "#D0828E";
        };
    }

    private String statusAccentColor(ReservationStatus status) {
        return switch (status) {
            case PENDING      -> "#DCA032";
            case CONFIRMED    -> "#5F94CD";
            case CHECKING_IN  -> "#5F94CD";
            case ONGOING      -> "#46A85F";
            case CHECKING_OUT -> CRIMSON;
            case COMPLETED    -> "#9B9491";
            case CANCELLED    -> "#D0828E";
        };
    }

    private void styleMetricLabel(Label label, String text, String color, int size) {
        label.setText(text);
        label.setStyle(
                "-fx-font-family: 'Georgia';"
                + "-fx-font-size: " + size + "px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: " + color + ";");
    }

    private String metricValueStyle(String color) {
        return "-fx-font-family: 'Georgia';"
                + "-fx-font-size: 16px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: " + color + ";";
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

    private String[] badgeColors(ReservationStatus status) {
        return switch (status) {
            case PENDING      -> new String[]{"rgba(220,158,45,0.88)",  "#FFFFFF"};
            case CONFIRMED    -> new String[]{"rgba(95,148,205,0.85)",  "#FFFFFF"};
            case CHECKING_IN  -> new String[]{"rgba(95,148,205,0.92)",  "#FFFFFF"};
            case ONGOING      -> new String[]{"rgba(70,168,95,0.88)",   "#FFFFFF"};
            case CHECKING_OUT -> new String[]{"rgba(196,65,93,0.88)",   "#FFFFFF"};
            case COMPLETED    -> new String[]{"rgba(155,148,145,0.82)", "#FFFFFF"};
            case CANCELLED    -> new String[]{"rgba(208,130,142,0.84)", "#FFFFFF"};
        };
    }

    private String displayStatus(ReservationStatus status) {
        return status.toString().replace('_', ' ');
    }

    private void addActionButtons() {
        actionBox.getChildren().clear();
        switch (reservation.getStatus()) {
            case PENDING      -> actionBox.getChildren().add(outlineBtn("CANCEL",      e -> openReservationDetail()));
            case CONFIRMED    -> actionBox.getChildren().add(outlineBtn("CHECK IN",    e -> handleCheckIn()));
            case CHECKING_IN  -> actionBox.getChildren().add(outlineBtn("CHECK IN",    e -> handleCheckIn()));
            case ONGOING      -> actionBox.getChildren().add(outlineBtn("EXTEND STAY", e -> openReservationDetail(true)));
            case CHECKING_OUT -> actionBox.getChildren().add(outlineBtn("CHECK OUT",   e -> openReservationDetail()));
            default           -> actionBox.getChildren().add(outlineBtn("DETAILS",     e -> openReservationDetail()));
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

    private void openReservationDetail() { openReservationDetail(false); }

    private void openReservationDetail(boolean extendStay) {
        mainApp.switchDashboardContent(
                mainApp.getCurrentContentArea(),
                "/ReservationDetail.fxml",
                new Object[]{reservation, sourceTitle, extendStay});
    }

    private boolean isInsideButton(Node node) {
        while (node != null) {
            if (node instanceof Button) return true;
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
                + "-fx-padding: 7 18 7 18;"
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
                + "-fx-padding: 7 18 7 18;"
                + "-fx-cursor: hand;";
        btn.setStyle(base);
        btn.setFocusTraversable(false);
        btn.setMinWidth(110);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
        btn.setOnAction(handler);
        return btn;
    }

    private String cardStyle(boolean cancelled) {
        return "-fx-background-color: white;"
                + "-fx-border-color: #E8DDD4;"
                + "-fx-border-width: 1;"
                + "-fx-border-radius: 0;"
                + "-fx-background-radius: 0;"
                + (cancelled ? "-fx-opacity: 0.72;" : "")
                + "-fx-effect: dropshadow(gaussian, rgba(44,37,35,0.06), 6, 0.06, 0, 2);";
    }

    private String cardHoverStyle(boolean cancelled) {
        return "-fx-background-color: white;"
                + "-fx-border-color: " + GOLD + ";"
                + "-fx-border-width: 1;"
                + "-fx-border-radius: 0;"
                + "-fx-background-radius: 0;"
                + (cancelled ? "-fx-opacity: 0.72;" : "")
                + "-fx-effect: dropshadow(gaussian, rgba(44,37,35,0.14), 12, 0.1, 0, 4);";
    }
}
