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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

public class GuestReservationsController implements DashboardContentController {

    @FXML private ScrollPane scrollPane;
    @FXML private VBox cardsContainer;

    private AppContext mainApp;
    private Guest guest;
    private final Map<String, Node> reservationCardNodes = new LinkedHashMap<>();
    private final Map<String, GuestReservationCardController> reservationCardControllers = new LinkedHashMap<>();
    private Task<List<Reservation>> reservationLoadTask;
    private int reservationLoadRequestId;

    @Override
    public void initData(AppContext mainApp, Object data) {
        this.mainApp = mainApp;
        this.guest = (Guest) data;
        loadReservations();
        mainApp.setCurrentViewRefresher(this::loadReservations);
    }

    private void loadReservations() {
        double previousVvalue = currentScrollPosition();
        int requestId = ++reservationLoadRequestId;
        showLoadingMessage();

        if (reservationLoadTask != null && reservationLoadTask.isRunning()) {
            reservationLoadTask.cancel();
        }

        reservationLoadTask = new Task<>() {
            @Override
            protected List<Reservation> call() {
                synchronized (Database.class) {
                    Database.refreshReservationsIfStale();
                    return Database.getReservations().stream()
                            .filter(r -> r.getGuest().getUsername().equals(guest.getUsername()))
                            .sorted(Comparator
                                    .comparingInt((Reservation r) -> statusPriority(r.getStatus()))
                                    .thenComparing(Reservation::getCheckInDate)
                                    .thenComparing(Reservation::getReservationId))
                            .toList();
                }
            }
        };

        reservationLoadTask.setOnSucceeded(event -> {
            if (requestId != reservationLoadRequestId) {
                return;
            }
            renderReservations(reservationLoadTask.getValue(), previousVvalue);
        });

        reservationLoadTask.setOnFailed(event -> {
            Throwable error = reservationLoadTask.getException();
            if (error != null) {
                error.printStackTrace();
            }
        });

        Thread thread = new Thread(reservationLoadTask, "guest-reservation-load");
        thread.setDaemon(true);
        thread.start();
    }

    private int statusPriority(ReservationStatus status) {
        return switch (status) {
            case PENDING -> 0;
            case CONFIRMED, CHECKING_IN -> 1;
            case ONGOING, CHECKING_OUT -> 2;
            default -> 3;
        };
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
            Label empty = new Label("No reservations available.");
            empty.getStyleClass().add("reservation-empty-message");
            orderedCards.add(empty);
        }
        reservationCardNodes.keySet().removeIf(key -> !activeKeys.contains(key));
        reservationCardControllers.keySet().removeIf(key -> !activeKeys.contains(key));
        FxNodeSync.syncChildren(cardsContainer, orderedCards);
        restoreScrollPosition(previousVvalue);
    }

    private void showLoadingMessage() {
        Label loading = new Label("Loading reservation info...");
        loading.getStyleClass().add("reservation-empty-message");
        cardsContainer.getChildren().setAll(loading);
    }

    private Node updateReservationCard(String key, Reservation reservation) throws IOException {
        Node card = reservationCardNodes.get(key);
        GuestReservationCardController controller = reservationCardControllers.get(key);
        if (card == null || controller == null) {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GuestReservationCard.fxml"));
            card = loader.load();
            controller = loader.getController();
            reservationCardNodes.put(key, card);
            reservationCardControllers.put(key, controller);
        }
        controller.setData(mainApp, guest, reservation, this::loadReservations);
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
        if (!AppLifecycle.isShuttingDown()) {
            Platform.runLater(() -> {
                if (!AppLifecycle.isShuttingDown()) {
                    scrollPane.setVvalue(vvalue);
                }
            });
        }
    }
}
