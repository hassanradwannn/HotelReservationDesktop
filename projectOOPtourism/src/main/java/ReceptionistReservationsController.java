import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Comparator;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

public class ReceptionistReservationsController implements DashboardContentController {

    enum ViewMode {
        CHECKING_IN,
        CHECKING_OUT,
        RESIDING,
        ALL
    }

    @FXML private Label titleLabel;
    @FXML private VBox checkInTab;
    @FXML private VBox checkOutTab;
    @FXML private VBox residingTab;
    @FXML private Label checkInCountLabel;
    @FXML private Label checkOutCountLabel;
    @FXML private Label residingCountLabel;
    @FXML private ScrollPane scrollPane;
    @FXML private VBox cardsContainer;

    private Main mainApp;
    private ViewMode mode = ViewMode.CHECKING_IN;
    private final Map<String, Node> reservationCardNodes = new LinkedHashMap<>();
    private final Map<String, ReceptionistReservationCardController> reservationCardControllers = new LinkedHashMap<>();
    private Task<List<Reservation>> reservationLoadTask;
    private int reservationLoadRequestId;

    @Override
    public void initData(Main mainApp, Object data) {
        this.mainApp = mainApp;
        if (data instanceof ViewMode viewMode) {
            this.mode = viewMode;
        }
        loadReservations();
        mainApp.setCurrentViewRefresher(this::loadReservations);
    }

    @FXML
    private void showCheckingIn() {
        setMode(ViewMode.CHECKING_IN);
    }

    @FXML
    private void showCheckingOut() {
        setMode(ViewMode.CHECKING_OUT);
    }

    @FXML
    private void showResidingGuests() {
        setMode(ViewMode.RESIDING);
    }

    @FXML
    private void showAllReservations() {
        mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/ReceptionistReservations.fxml", ViewMode.ALL);
    }

    private void setMode(ViewMode mode) {
        this.mode = mode;
        loadReservations();
    }

    private void loadReservations() {
        double previousVvalue = currentScrollPosition();
        int requestId = ++reservationLoadRequestId;

        if (reservationLoadTask != null && reservationLoadTask.isRunning()) {
            reservationLoadTask.cancel();
        }

        reservationLoadTask = new Task<>() {
            @Override
            protected List<Reservation> call() {
                synchronized (Database.class) {
                    Database.refreshReservationsFromDatabase();
                    return new ArrayList<>(Database.getReservations());
                }
            }
        };

        reservationLoadTask.setOnSucceeded(event -> {
            if (requestId != reservationLoadRequestId) {
                return;
            }
            List<Reservation> reservations = reservationLoadTask.getValue();
            updateCounts(reservations);
            updateActiveTab();
            renderReservations(filterReservations(reservations), previousVvalue);
        });

        reservationLoadTask.setOnFailed(event -> {
            Throwable error = reservationLoadTask.getException();
            if (error != null) {
                error.printStackTrace();
            }
        });

        Thread thread = new Thread(reservationLoadTask, "receptionist-reservation-load");
        thread.setDaemon(true);
        thread.start();
    }

    private void updateCounts(List<Reservation> reservations) {
        checkInCountLabel.setText(String.valueOf(reservations.stream()
                .filter(this::isCheckingInToday)
                .count()));
        checkOutCountLabel.setText(String.valueOf(countStatus(reservations, ReservationStatus.CHECKING_OUT)));
        residingCountLabel.setText(String.valueOf(reservations.stream()
                .filter(r -> r.getStatus() == ReservationStatus.ONGOING)
                .map(r -> r.getGuest().getUsername())
                .distinct()
                .count()));
    }

    private long countStatus(List<Reservation> reservations, ReservationStatus status) {
        return reservations.stream().filter(r -> r.getStatus() == status).count();
    }

    private List<Reservation> filterReservations(List<Reservation> reservations) {
        titleLabel.setText(switch (mode) {
            case CHECKING_IN -> "Reservations To Check In";
            case CHECKING_OUT -> "Requested Check Outs";
            case RESIDING -> "Guests Currently Residing";
            case ALL -> "All Reservations";
        });

        return switch (mode) {
            case CHECKING_IN -> reservations.stream()
                    .filter(this::isCheckingInToday)
                    .toList();
            case CHECKING_OUT -> reservations.stream()
                    .filter(r -> r.getStatus() == ReservationStatus.CHECKING_OUT)
                    .toList();
            case RESIDING -> reservations.stream()
                    .filter(r -> r.getStatus() == ReservationStatus.ONGOING)
                    .toList();
            case ALL -> reservations.stream()
                    .sorted(Comparator
                            .comparingInt((Reservation r) -> statusPriority(r.getStatus()))
                            .thenComparing(Reservation::getCheckInDate)
                            .thenComparing(Reservation::getReservationId))
                    .toList();
        };
    }

    private int statusPriority(ReservationStatus status) {
        return switch (status) {
            case CHECKING_IN -> 0;
            case CHECKING_OUT -> 1;
            case ONGOING -> 2;
            case CONFIRMED -> 3;
            case PENDING -> 4;
            default -> 5;
        };
    }

    private boolean isCheckingInToday(Reservation reservation) {
        return reservation.getStatus() == ReservationStatus.CHECKING_IN
                && reservation.getCheckInDate().isEqual(SystemTime.getToday());
    }

    private void updateActiveTab() {
        setTabActive(checkInTab, mode == ViewMode.CHECKING_IN);
        setTabActive(checkOutTab, mode == ViewMode.CHECKING_OUT);
        setTabActive(residingTab, mode == ViewMode.RESIDING);
    }

    private void setTabActive(VBox tab, boolean active) {
        tab.getStyleClass().removeAll("reception-tab-card-active");
        if (active) {
            tab.getStyleClass().add("reception-tab-card-active");
        }
    }

    private void renderReservations(List<Reservation> reservations, double previousVvalue) {
        List<Node> orderedCards = new ArrayList<>();
        Set<String> activeKeys = new HashSet<>();
        for (Reservation reservation : reservations) {
            String key = reservation.getReservationId();
            activeKeys.add(key);
            try {
                orderedCards.add(updateReservationCard(key, reservation));
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

    private Node updateReservationCard(String key, Reservation reservation) throws IOException {
        Node card = reservationCardNodes.get(key);
        ReceptionistReservationCardController controller = reservationCardControllers.get(key);
        if (card == null || controller == null) {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ReceptionistReservationCard.fxml"));
            card = loader.load();
            controller = loader.getController();
            reservationCardNodes.put(key, card);
            reservationCardControllers.put(key, controller);
        }
        controller.setData(mainApp, reservation, titleLabel.getText(), this::loadReservations);
        return card;
    }

    private double currentScrollPosition() {
        return scrollPane == null ? 0.0 : scrollPane.getVvalue();
    }

    private void restoreScrollPosition(double vvalue) {
        if (scrollPane == null) {
            return;
        }
        scrollPane.setVvalue(vvalue);
        Platform.runLater(() -> scrollPane.setVvalue(vvalue));
    }
}
