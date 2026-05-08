import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

public class ReceptionistReservationsController implements DashboardContentController {
    private static long lastReservationRefreshVersion = -1;

    enum ViewMode {
        CHECKING_IN,
        CHECKING_OUT,
        RESIDING,
        ALL
    }

    // Combo display strings (must match order below)
    private static final String OPT_ALL       = "All Reservations";
    private static final String OPT_CHECKIN   = "Check Ins Today";
    private static final String OPT_CHECKOUT  = "Check Out Requests";
    private static final String OPT_RESIDING  = "Guests Residing";

    @FXML private Label titleLabel;
    @FXML private ComboBox<String> filterComboBox;
    @FXML private ScrollPane scrollPane;
    @FXML private VBox cardsContainer;

    // Hidden labels kept for controller contract (counts / tab state still computed internally)
    @FXML private VBox checkInTab;
    @FXML private VBox checkOutTab;
    @FXML private VBox residingTab;
    @FXML private Label checkInCountLabel;
    @FXML private Label checkOutCountLabel;
    @FXML private Label residingCountLabel;

    private Main mainApp;
    private ViewMode mode = ViewMode.ALL;
    private final Map<String, Node> reservationCardNodes = new LinkedHashMap<>();
    private final Map<String, ReceptionistReservationCardController> reservationCardControllers = new LinkedHashMap<>();
    private Task<ReservationLoadResult> reservationLoadTask;
    private int reservationLoadRequestId;
    private int renderRequestId;

    @Override
    public void initData(Main mainApp, Object data) {
        this.mainApp = mainApp;

        // Populate combo
        if (filterComboBox != null) {
            filterComboBox.setItems(FXCollections.observableArrayList(
                    OPT_ALL, OPT_CHECKIN, OPT_CHECKOUT, OPT_RESIDING));
            filterComboBox.setValue(OPT_ALL);
        }

        if (data instanceof ViewMode viewMode) {
            this.mode = viewMode;
            syncComboToMode();
        }

        loadReservations();
        mainApp.setCurrentViewRefresher(this::loadReservations);
    }

    /** Called when the ComboBox selection changes (onAction="#onFilterChanged" in FXML). */
    @FXML
    private void onFilterChanged() {
        if (filterComboBox == null) return;
        String selected = filterComboBox.getValue();
        mode = switch (selected) {
            case OPT_CHECKIN  -> ViewMode.CHECKING_IN;
            case OPT_CHECKOUT -> ViewMode.CHECKING_OUT;
            case OPT_RESIDING -> ViewMode.RESIDING;
            default           -> ViewMode.ALL;
        };
        loadReservations();
    }

    /** Keep the three FXML-linked show* handlers so the controller still compiles
     *  if anything calls them directly. */
    @FXML private void showCheckingIn()     { setMode(ViewMode.CHECKING_IN); }
    @FXML private void showCheckingOut()    { setMode(ViewMode.CHECKING_OUT); }
    @FXML private void showResidingGuests() { setMode(ViewMode.RESIDING); }
    @FXML private void showAllReservations(){ setMode(ViewMode.ALL); }

    private void setMode(ViewMode mode) {
        this.mode = mode;
        syncComboToMode();
        loadReservations();
    }

    private void syncComboToMode() {
        if (filterComboBox == null) return;
        String label = switch (mode) {
            case CHECKING_IN  -> OPT_CHECKIN;
            case CHECKING_OUT -> OPT_CHECKOUT;
            case RESIDING     -> OPT_RESIDING;
            case ALL          -> OPT_ALL;
        };
        filterComboBox.setValue(label);
    }

    private void loadReservations() {
        double previousVvalue = currentScrollPosition();
        int requestId = ++reservationLoadRequestId;
        ++renderRequestId;
        ViewMode modeSnapshot = mode;

        if (titleLabel != null) titleLabel.setText(titleForMode(modeSnapshot));
        showLoadingMessage(modeSnapshot);

        if (reservationLoadTask != null && reservationLoadTask.isRunning()) {
            reservationLoadTask.cancel();
        }

        reservationLoadTask = new Task<>() {
            @Override
            protected ReservationLoadResult call() {
                List<Reservation> allReservations;
                long latestDataVersion = Database.getLatestDataVersion();
                synchronized (Database.class) {
                    if (lastReservationRefreshVersion != latestDataVersion || Database.getReservations().isEmpty()) {
                        Database.refreshReservationsFromDatabase();
                        lastReservationRefreshVersion = latestDataVersion;
                    }
                    allReservations = new ArrayList<>(Database.getReservations());
                }
                List<Reservation> visibleReservations = filterReservations(allReservations, modeSnapshot);
                Map<String, ReservationPaymentSummary> paymentSummaries =
                        ReservationService.getPaymentSummaries(visibleReservations);
                return new ReservationLoadResult(allReservations, visibleReservations, paymentSummaries);
            }
        };

        reservationLoadTask.setOnSucceeded(event -> {
            if (requestId != reservationLoadRequestId) return;
            ReservationLoadResult result = reservationLoadTask.getValue();
            updateCounts(result.allReservations);
            renderReservations(result.visibleReservations, result.paymentSummaries, previousVvalue, modeSnapshot);
        });

        reservationLoadTask.setOnFailed(event -> {
            Throwable error = reservationLoadTask.getException();
            if (error != null) error.printStackTrace();
        });

        Thread thread = new Thread(reservationLoadTask, "receptionist-reservation-load");
        thread.setDaemon(true);
        thread.start();
    }

    private void updateCounts(List<Reservation> reservations) {
        if (checkInCountLabel != null)
            checkInCountLabel.setText(String.valueOf(reservations.stream().filter(this::isCheckingInToday).count()));
        if (checkOutCountLabel != null)
            checkOutCountLabel.setText(String.valueOf(countStatus(reservations, ReservationStatus.CHECKING_OUT)));
        if (residingCountLabel != null)
            residingCountLabel.setText(String.valueOf(reservations.stream()
                    .filter(r -> r.getStatus() == ReservationStatus.ONGOING)
                    .map(r -> r.getGuest().getUsername())
                    .distinct()
                    .count()));
    }

    private long countStatus(List<Reservation> reservations, ReservationStatus status) {
        return reservations.stream().filter(r -> r.getStatus() == status).count();
    }

    private String titleForMode(ViewMode mode) {
        return switch (mode) {
            case CHECKING_IN  -> "Check Ins Today";
            case CHECKING_OUT -> "Check Out Requests";
            case RESIDING     -> "Guests Residing";
            case ALL          -> "All Reservations";
        };
    }

    private List<Reservation> filterReservations(List<Reservation> reservations, ViewMode mode) {
        return switch (mode) {
            case CHECKING_IN  -> reservations.stream().filter(this::isCheckingInToday).toList();
            case CHECKING_OUT -> reservations.stream().filter(r -> r.getStatus() == ReservationStatus.CHECKING_OUT).toList();
            case RESIDING     -> reservations.stream().filter(r -> r.getStatus() == ReservationStatus.ONGOING).toList();
            case ALL          -> reservations.stream()
                    .sorted(Comparator
                            .comparingInt((Reservation r) -> statusPriority(r.getStatus()))
                            .thenComparing(Reservation::getCheckInDate)
                            .thenComparing(Reservation::getReservationId))
                    .toList();
        };
    }

    private int statusPriority(ReservationStatus status) {
        return switch (status) {
            case CHECKING_IN  -> 0;
            case CHECKING_OUT -> 1;
            case ONGOING      -> 2;
            case CONFIRMED    -> 3;
            case PENDING      -> 4;
            default           -> 5;
        };
    }

    private boolean isCheckingInToday(Reservation reservation) {
        return reservation.getStatus() == ReservationStatus.CHECKING_IN
                && reservation.getCheckInDate().isEqual(SystemTime.getToday());
    }

    // ── rendering ────────────────────────────────────────────────────────────

    private void renderReservations(List<Reservation> reservations,
                                    Map<String, ReservationPaymentSummary> paymentSummaries,
                                    double previousVvalue,
                                    ViewMode renderMode) {
        if (renderMode == ViewMode.ALL && reservations.size() > 16) {
            renderReservationsInBatches(reservations, paymentSummaries, previousVvalue);
            return;
        }

        List<Node> orderedCards = new ArrayList<>();
        Set<String> activeKeys = new HashSet<>();
        for (Reservation reservation : reservations) {
            String key = reservation.getReservationId();
            activeKeys.add(key);
            try {
                orderedCards.add(updateReservationCard(key, reservation, paymentSummaries.get(key)));
            } catch (IOException ex) {
                ex.printStackTrace();
                Label error = new Label("Could not load reservation card.");
                error.getStyleClass().add("error-message");
                orderedCards.add(error);
            }
        }

        if (orderedCards.isEmpty()) {
            Label empty = new Label("No reservations in this view.");
            empty.getStyleClass().add("reservation-empty-message");
            orderedCards.add(empty);
        }

        reservationCardNodes.keySet().removeIf(key -> !activeKeys.contains(key));
        reservationCardControllers.keySet().removeIf(key -> !activeKeys.contains(key));
        FxNodeSync.syncChildren(cardsContainer, orderedCards);
        restoreScrollPosition(previousVvalue);
    }

    private void renderReservationsInBatches(List<Reservation> reservations,
                                             Map<String, ReservationPaymentSummary> paymentSummaries,
                                             double previousVvalue) {
        int requestId = ++renderRequestId;
        List<Node> orderedCards = new ArrayList<>();
        Set<String> activeKeys = new HashSet<>();
        cardsContainer.getChildren().clear();
        renderNextBatch(reservations, paymentSummaries, previousVvalue, orderedCards, activeKeys, 0, requestId);
    }

    private void renderNextBatch(List<Reservation> reservations,
                                 Map<String, ReservationPaymentSummary> paymentSummaries,
                                 double previousVvalue,
                                 List<Node> orderedCards,
                                 Set<String> activeKeys,
                                 int startIndex,
                                 int requestId) {
        if (requestId != renderRequestId) return;

        int batchSize = 10;
        int endIndex = Math.min(startIndex + batchSize, reservations.size());
        for (int i = startIndex; i < endIndex; i++) {
            Reservation reservation = reservations.get(i);
            String key = reservation.getReservationId();
            activeKeys.add(key);
            try {
                orderedCards.add(updateReservationCard(key, reservation, paymentSummaries.get(key)));
            } catch (IOException ex) {
                ex.printStackTrace();
                Label error = new Label("Could not load reservation card.");
                error.getStyleClass().add("error-message");
                orderedCards.add(error);
            }
        }

        FxNodeSync.syncChildren(cardsContainer, orderedCards);

        if (endIndex < reservations.size()) {
            Platform.runLater(() -> renderNextBatch(
                    reservations, paymentSummaries, previousVvalue,
                    orderedCards, activeKeys, endIndex, requestId));
            return;
        }

        reservationCardNodes.keySet().removeIf(key -> !activeKeys.contains(key));
        reservationCardControllers.keySet().removeIf(key -> !activeKeys.contains(key));
        restoreScrollPosition(previousVvalue);
    }

    private void showLoadingMessage(ViewMode mode) {
        Label loading = new Label(mode == ViewMode.ALL ? "Loading all reservations..." : "Loading reservations...");
        loading.getStyleClass().add("reservation-empty-message");
        cardsContainer.getChildren().setAll(loading);
    }

    private Node updateReservationCard(String key, Reservation reservation,
                                       ReservationPaymentSummary paymentSummary) throws IOException {
        Node card = reservationCardNodes.get(key);
        ReceptionistReservationCardController controller = reservationCardControllers.get(key);
        if (card == null || controller == null) {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ReceptionistReservationCard.fxml"));
            card = loader.load();
            controller = loader.getController();
            reservationCardNodes.put(key, card);
            reservationCardControllers.put(key, controller);
        }
        controller.setData(mainApp, reservation, paymentSummary, titleLabel.getText(), this::loadReservations);
        return card;
    }

    private double currentScrollPosition() {
        return scrollPane == null ? 0.0 : scrollPane.getVvalue();
    }

    private void restoreScrollPosition(double vvalue) {
        if (scrollPane == null) return;
        scrollPane.setVvalue(vvalue);
        Platform.runLater(() -> scrollPane.setVvalue(vvalue));
    }

    private static class ReservationLoadResult {
        final List<Reservation> allReservations;
        final List<Reservation> visibleReservations;
        final Map<String, ReservationPaymentSummary> paymentSummaries;

        ReservationLoadResult(List<Reservation> all, List<Reservation> visible,
                              Map<String, ReservationPaymentSummary> summaries) {
            this.allReservations = all;
            this.visibleReservations = visible;
            this.paymentSummaries = summaries;
        }
    }
}
