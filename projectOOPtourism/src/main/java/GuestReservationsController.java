import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class GuestReservationsController implements DashboardContentController {

    @FXML private ScrollPane scrollPane;
    @FXML private VBox cardsContainer;

    private Main mainApp;
    private Guest guest;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("d/M/yyyy");

    // Palette from styles.css
    private static final String CREAM    = "#F9F3EA";
    private static final String CRIMSON  = "#C4415D";
    private static final String BURGUNDY = "#8B2040";
    private static final String GOLD     = "#C8A97E";
    private static final String DARK     = "#2C2523";
    private static final String BLUSH    = "#F7D6D9";
    private static final String ROSE     = "#E8879A";
    private static final double ACTION_COLUMN_WIDTH = 182;

    @Override
    public void initData(Main mainApp, Object data) {
        this.mainApp = mainApp;
        this.guest = (Guest) data;
        loadReservations();
        mainApp.setCurrentViewRefresher(this::loadReservations);
    }

    private void loadReservations() {
        double previousVvalue = currentScrollPosition();
        Database.refreshReservationsFromDatabase();
        List<Reservation> reservations = Database.getReservations().stream()
                .filter(r -> r.getGuest().getUsername().equals(guest.getUsername()))
                .toList();
        cardsContainer.getChildren().clear();
        for (Reservation r : reservations) {
            cardsContainer.getChildren().add(buildCard(r));
        }
        restoreScrollPosition(previousVvalue);
    }



    private StackPane buildCard(Reservation reservation) {
        boolean isCancelled = reservation.getStatus() == ReservationStatus.CANCELLED;


        Region accentBar = new Region();
        accentBar.setPrefWidth(5);
        accentBar.setMinWidth(5);
        accentBar.setMaxWidth(5);
        accentBar.setStyle("-fx-background-color: " + statusAccentColor(reservation.getStatus()) + ";");


        String roomTypeName = reservation.getRoom().getRoomType().getName();
        boolean isLobbyDeluxe = roomTypeName.toLowerCase().contains("deluxe") || roomTypeName.toLowerCase().contains("lobby");
        Label iconLabel = new Label(roomTypeIcon(roomTypeName));
        iconLabel.setStyle(
                "-fx-font-size: " + iconFontSize(roomTypeName) + "px;" +
                        "-fx-text-fill: " + iconForeground(roomTypeName) + ";" +
                        "-fx-background-color: " + iconBackground(roomTypeName) + ";" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: " + (isLobbyDeluxe ? GOLD : "transparent") + ";" +
                        "-fx-border-width: " + (isLobbyDeluxe ? "2.5" : "0") + ";" +
                        "-fx-border-radius: 10;" +
                        "-fx-min-width: 72px; -fx-min-height: 72px;" +
                        "-fx-max-width: 72px; -fx-max-height: 72px;" +
                        "-fx-alignment: center;" +
                        (isMendleClassic(roomTypeName) ? "-fx-font-weight: bold;" : "") +
                        (isCancelled ? "-fx-opacity: 0.6;" : "")
        );


        TextFlow typeNameFlow = new TextFlow();
        Text typeNameText = new Text(roomTypeName);
        typeNameText.setStyle(
                "-fx-font-family: 'Georgia';" +
                        "-fx-font-size: 18px;" +
                        "-fx-fill: " + (isCancelled ? "#9e8880" : DARK) + ";"
        );
        typeNameText.setStrikethrough(isCancelled);
        typeNameFlow.getChildren().add(typeNameText);
        typeNameFlow.setStyle("-fx-font-family: 'Georgia';");


        Label subLine = new Label("Room " + reservation.getRoom().getRoomNumber()
                + " · " + reservation.getReservationId());
        subLine.setStyle(
                "-fx-font-family: 'Georgia';" +
                        "-fx-font-size: 12px;" +
                        "-fx-text-fill: " + (isCancelled ? "#c4a098" : CRIMSON) + ";"
        );


        VBox nameBlock = new VBox(3, typeNameFlow, subLine);
        nameBlock.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(nameBlock, Priority.ALWAYS);


        HBox topRow = new HBox(14, iconLabel, nameBlock);
        topRow.setAlignment(Pos.CENTER_LEFT);


        HBox datesRow = new HBox(32);
        datesRow.setAlignment(Pos.CENTER_LEFT);
        datesRow.setPadding(new Insets(10, 0, 0, 0));
        datesRow.getChildren().addAll(
                dateBlock("CHECK-IN",  reservation.getCheckInDate().format(DATE_FMT),  isCancelled),
                dateBlock("CHECK-OUT", reservation.getCheckOutDate().format(DATE_FMT), isCancelled),
                dateBlock("TOTAL",     "$" + String.format("%,.0f", reservation.getTotalPrice()), isCancelled)
        );


        VBox content = new VBox(0, topRow, datesRow);
        content.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(content, Priority.ALWAYS);

        Region actionSpacer = new Region();
        actionSpacer.setPrefWidth(ACTION_COLUMN_WIDTH);
        actionSpacer.setMinWidth(ACTION_COLUMN_WIDTH);
        actionSpacer.setMaxWidth(ACTION_COLUMN_WIDTH);

        HBox innerRow = new HBox(0, content, actionSpacer);
        innerRow.setAlignment(Pos.CENTER_LEFT);
        innerRow.setPadding(new Insets(20, 24, 20, 20));
        innerRow.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(content, Priority.ALWAYS);

        HBox cardBody = new HBox(0, accentBar, innerRow);
        cardBody.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(innerRow, Priority.ALWAYS);
        cardBody.setMaxWidth(Double.MAX_VALUE);
        cardBody.setStyle(cardStyle(isCancelled));
        cardBody.setMouseTransparent(true);

        // ── Status pill overlaid in top-right corner ──────────────────────────
        Label badge = buildStatusBadge(reservation.getStatus());
        StackPane.setAlignment(badge, Pos.TOP_RIGHT);
        StackPane.setMargin(badge, new Insets(20, 24, 0, 0));

        HBox actionBox = new HBox(8);
        actionBox.setAlignment(Pos.CENTER_RIGHT);
        addActionButtons(actionBox, reservation);
        StackPane.setAlignment(actionBox, Pos.TOP_RIGHT);
        StackPane.setMargin(actionBox, new Insets(74, 24, 0, 0));

        // ── Wrap in StackPane so pill and action buttons can float on top ─────
        StackPane card = new StackPane(cardBody, badge, actionBox);
        card.setAlignment(Pos.TOP_LEFT);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setPickOnBounds(true);
        card.setCursor(Cursor.HAND);
        card.setOnMouseEntered(e ->
                cardBody.setStyle(cardStyle(isCancelled) +
                        "-fx-effect: dropshadow(gaussian, rgba(44,37,35,0.16), 14, 0.1, 0, 4);"));
        card.setOnMouseExited(e -> cardBody.setStyle(cardStyle(isCancelled)));
        card.setOnMouseClicked(e -> {
            if (isInsideButton(e.getPickResult().getIntersectedNode())) return;
            openReservationDetail(reservation);
        });

        return card;
    }

    // ── Action buttons ────────────────────────────────────────────────────────

    private void addActionButtons(HBox panel, Reservation reservation) {
        switch (reservation.getStatus()) {
            case PENDING -> {
                Button payBtn = outlineBtn("PAY DEPOSIT", e -> openReservationDetail(reservation));
                Button cancelBtn = outlineBtn("CANCEL", null);
                cancelBtn.setOnAction(e -> showConfirmCancel(panel, cancelBtn, reservation));
                panel.getChildren().addAll(payBtn, cancelBtn);
            }
            case CONFIRMED -> {
                Button cancelBtn = outlineBtn("CANCEL", null);
                cancelBtn.setOnAction(e -> showConfirmCancel(panel, cancelBtn, reservation));
                panel.getChildren().add(cancelBtn);
            }
            case ONGOING -> {
                Button checkoutBtn = outlineBtn("REQUEST CHECK OUT", e -> handleRequestCheckOut(reservation));
                Button extendBtn = outlineBtn("EXTEND STAY", e ->
                        mainApp.switchDashboardContent(mainApp.getCurrentContentArea(),
                                "/ReservationDetail.fxml",
                                new Object[]{reservation, "My Reservations"}));
                panel.getChildren().addAll(checkoutBtn, extendBtn);
            }
            default -> { }
        }
    }

    private void openReservationDetail(Reservation reservation) {
        mainApp.switchDashboardContent(
                mainApp.getCurrentContentArea(),
                "/ReservationDetail.fxml",
                new Object[]{reservation, "My Reservations"});
    }

    private void handleRequestCheckOut(Reservation reservation) {
        try {
            ReservationService.requestCheckOut(reservation, guest);
            mainApp.alert("Check-out Requested", "Your check-out request has been sent to the front desk.");
            loadReservations();
        } catch (Exception ex) {
            mainApp.alert("Request Failed", ex.getMessage());
        }
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

    private void showConfirmCancel(HBox panel, Button originalBtn, Reservation reservation) {
        String confirm =
                "-fx-background-color: " + CRIMSON + ";" +
                        "-fx-border-color: " + CRIMSON + ";" +
                        "-fx-border-width: 1.5;" +
                        "-fx-border-radius: 0;" +
                        "-fx-background-radius: 0;" +
                        "-fx-text-fill: #FFFFFF;" +
                        "-fx-font-family: 'Georgia';" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 6 16 6 16;" +
                        "-fx-cursor: hand;";
        String confirmHover =
                "-fx-background-color: " + BURGUNDY + ";" +
                        "-fx-border-color: " + BURGUNDY + ";" +
                        "-fx-border-width: 1.5;" +
                        "-fx-border-radius: 0;" +
                        "-fx-background-radius: 0;" +
                        "-fx-text-fill: #FFFFFF;" +
                        "-fx-font-family: 'Georgia';" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 6 16 6 16;" +
                        "-fx-cursor: hand;";

        Button areYouSure = new Button("ARE YOU SURE?");
        areYouSure.setFocusTraversable(false);
        areYouSure.setStyle(confirm);
        areYouSure.setOnMouseEntered(e -> areYouSure.setStyle(confirmHover));
        areYouSure.setOnMouseExited(e -> areYouSure.setStyle(confirm));
        areYouSure.setOnAction(e -> {
            try {
                ReservationService.cancelReservation(reservation.getReservationId());
                loadReservations();
            } catch (Exception ex) {
                mainApp.alert("Error", ex.getMessage());
            }
        });
        preserveScrollPosition(() -> {
            int idx = panel.getChildren().indexOf(originalBtn);
            if (idx >= 0) {
                panel.getChildren().set(idx, areYouSure);
            } else {
                panel.getChildren().add(areYouSure);
            }
        });
    }



    private VBox dateBlock(String header, String value, boolean faded) {
        Label h = new Label(header);
        h.setStyle(
                "-fx-font-family: 'Georgia';" +
                        "-fx-font-size: 10px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + (faded ? "#c4a8a0" : BURGUNDY) + ";" +
                        "-fx-letter-spacing: 1;"
        );
        Label v = new Label(value);
        v.setStyle(
                "-fx-font-family: 'Georgia';" +
                        "-fx-font-size: 15px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + (faded ? "#b8a8a4" : DARK) + ";"
        );
        return new VBox(2, h, v);
    }

    private Label buildStatusBadge(ReservationStatus status) {
        Label badge = new Label(displayStatus(status));
        String[] c = badgeColors(status);
        badge.setStyle(
                "-fx-background-color: " + c[0] + ";" +
                        "-fx-text-fill: " + c[1] + ";" +
                        "-fx-font-family: 'Georgia';" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 5 18 5 18;" +
                        "-fx-background-radius: 20;" +
                        "-fx-letter-spacing: 1;"
        );
        return badge;
    }

    private String[] badgeColors(ReservationStatus status) {
        return switch (status) {
            case PENDING   -> new String[]{"rgba(220, 158, 45, 0.85)",  "#FFFFFF"};
            case CONFIRMED -> new String[]{"rgba(95,  148, 205, 0.82)", "#FFFFFF"};
            case ONGOING   -> new String[]{"rgba(70,  168, 95,  0.85)", "#FFFFFF"};
            case CHECKING_OUT -> new String[]{"rgba(196, 65, 93, 0.86)", "#FFFFFF"};
            case COMPLETED -> new String[]{"rgba(155, 148, 145, 0.80)", "#FFFFFF"};
            case CANCELLED -> new String[]{"rgba(208, 130, 142, 0.82)", "#FFFFFF"};
        };
    }

    private String displayStatus(ReservationStatus status) {
        return status.toString().replace('_', ' ');
    }

    /** Sharp-cornered outline button — gold border, burgundy text */
    private Button outlineBtn(String text, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button btn = new Button(text);
        String base =
                "-fx-background-color: transparent;" +
                        "-fx-border-color: " + GOLD + ";" +
                        "-fx-border-width: 1.5;" +
                        "-fx-border-radius: 0;" +
                        "-fx-background-radius: 0;" +
                        "-fx-text-fill: " + BURGUNDY + ";" +
                        "-fx-font-family: 'Georgia';" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 6 16 6 16;" +
                        "-fx-cursor: hand;";
        String hover =
                "-fx-background-color: " + BLUSH + ";" +
                        "-fx-border-color: " + CRIMSON + ";" +
                        "-fx-border-width: 1.5;" +
                        "-fx-border-radius: 0;" +
                        "-fx-background-radius: 0;" +
                        "-fx-text-fill: " + CRIMSON + ";" +
                        "-fx-font-family: 'Georgia';" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 6 16 6 16;" +
                        "-fx-cursor: hand;";
        btn.setStyle(base);
        btn.setFocusTraversable(false);
        btn.setMinWidth(140);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        btn.setOnAction(handler);
        return btn;
    }

    private double currentScrollPosition() {
        return scrollPane == null ? 0.0 : scrollPane.getVvalue();
    }

    private void preserveScrollPosition(Runnable action) {
        double previousVvalue = currentScrollPosition();
        action.run();
        restoreScrollPosition(previousVvalue);
    }

    private void restoreScrollPosition(double vvalue) {
        if (scrollPane == null) {
            return;
        }
        scrollPane.setVvalue(vvalue);
        Platform.runLater(() -> scrollPane.setVvalue(vvalue));
    }

    private String cardStyle(boolean cancelled) {
        return  "-fx-background-color: " + CREAM + ";" +
                "-fx-background-radius: 0;" +
                "-fx-border-color: " + GOLD + ";" +
                "-fx-border-radius: 0;" +
                "-fx-border-width: 1;" +
                (cancelled ? "-fx-opacity: 0.72;" : "") +
                "-fx-effect: dropshadow(gaussian, rgba(44,37,35,0.08), 8, 0.08, 0, 2);";
    }

    // ── Colour mapping ────────────────────────────────────────────────────────

    private String statusAccentColor(ReservationStatus status) {
        return switch (status) {
            case PENDING   -> "#DCA032";
            case CONFIRMED -> "#5F94CD";
            case ONGOING   -> "#46A85F";
            case CHECKING_OUT -> CRIMSON;
            case COMPLETED -> "#9B9491";
            case CANCELLED -> "#D0828E";
        };
    }

    // Lobby Deluxe: inverted → cream bg, gold symbol, gold border
    // Mendle Classic: rose bg, cream symbol, larger font
    private String iconBackground(String name) {
        String n = name.toLowerCase();
        if (n.contains("alpine") || n.contains("grand") || n.contains("suite")) return BURGUNDY;
        if (n.contains("penthouse") || n.contains("gustave"))                   return CRIMSON;
        if (n.contains("deluxe") || n.contains("lobby"))                        return "#F3EADF";
        if (n.contains("classic") || n.contains("mendle"))                      return ROSE;
        return CRIMSON;
    }

    /** Lobby Deluxe inverted: gold symbol on cream bg; all others cream on dark */
    private String iconForeground(String name) {
        String n = name.toLowerCase();
        if (n.contains("deluxe") || n.contains("lobby")) return GOLD;
        return CREAM;
    }

    /** Mendle Classic ⊙ is a small glyph — bump it up to fill the tile */
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
        if (n.contains("suite") || n.contains("grand") || n.contains("alpine")) return "✦";
        if (n.contains("penthouse") || n.contains("gustave"))                   return "❧";
        if (n.contains("deluxe") || n.contains("lobby"))                        return "◆";
        if (n.contains("classic") || n.contains("mendle"))                      return "⊙";
        return "◈";
    }
}
