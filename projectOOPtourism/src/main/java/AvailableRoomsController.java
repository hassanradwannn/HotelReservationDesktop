import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class AvailableRoomsController implements DashboardContentController {

    @FXML private VBox roomCards;

    private Main mainApp;
    private Guest guest;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private boolean hasGymPass;
    private List<Room> availableRooms;
    private ReservationSearchContext searchContext;
    private final Map<String, Node> roomCardNodes = new LinkedHashMap<>();
    private final Map<String, RoomCardController> roomCardControllers = new LinkedHashMap<>();
    private Task<RoomRefreshResult> roomRefreshTask;
    private int roomRefreshRequestId;

    @Override
    @SuppressWarnings("unchecked")
    public void initData(Main mainApp, Object data) {
        this.mainApp = mainApp;

        Object[] bookingData = (Object[]) data;
        this.guest = (Guest) bookingData[0];
        this.checkIn = (LocalDate) bookingData[1];
        this.checkOut = (LocalDate) bookingData[2];
        this.hasGymPass = (Boolean) bookingData[3];
        this.availableRooms = (List<Room>) bookingData[4];
        this.searchContext = bookingData.length > 5 && bookingData[5] instanceof ReservationSearchContext context
                ? context
                : null;
        GuestPreferenceRanker.sortRoomsByGuestPreferences(this.availableRooms, guest);

        mainApp.setCurrentViewRefresher(this::refreshAvailableRooms);
        loadRooms();
    }

    private void refreshAvailableRooms() {
        int requestId = ++roomRefreshRequestId;
        ReservationSearchContext contextSnapshot = searchContext;
        List<Room> roomsSnapshot = availableRooms == null ? List.of() : List.copyOf(availableRooms);

        if (roomRefreshTask != null && roomRefreshTask.isRunning()) {
            roomRefreshTask.cancel();
        }

        roomRefreshTask = new Task<>() {
            @Override
            protected RoomRefreshResult call() {
                synchronized (Database.class) {
                    List<Room> refreshedRooms;
                    ReservationSearchContext refreshedContext = contextSnapshot;

                    if (contextSnapshot != null) {
                        List<Amenity> currentRequestedAmenities = currentAmenities(contextSnapshot.getRequestedAmenities());
                        RoomType currentRoomType = CatalogService.findRoomType(contextSnapshot.getRoomType().getName());
                        RoomType currentSearchRoomType = contextSnapshot.getSearchRoomType() == null
                                ? null
                                : CatalogService.findRoomType(contextSnapshot.getSearchRoomType().getName());
                        if (currentRoomType == null) {
                            return new RoomRefreshResult(List.of(), contextSnapshot);
                        }

                        refreshedContext = new ReservationSearchContext(
                                guest,
                                currentRoomType,
                                checkIn,
                                checkOut,
                                contextSnapshot.getGuests(),
                                currentRequestedAmenities,
                                currentSearchRoomType,
                                contextSnapshot.getMaxPrice(),
                                hasGymPass);
                        refreshedRooms = ReservationService.searchAvailableRooms(
                                checkIn,
                                checkOut,
                                currentRoomType,
                                contextSnapshot.getGuests(),
                                currentRequestedAmenities);
                    } else {
                        refreshedRooms = roomsSnapshot.stream()
                                .map(room -> CatalogService.findRoom(room.getRoomNumber()))
                                .filter(room -> room != null)
                                .toList();
                    }

                    GuestPreferenceRanker.sortRoomsByGuestPreferences(refreshedRooms, guest);
                    return new RoomRefreshResult(refreshedRooms, refreshedContext);
                }
            }
        };

        roomRefreshTask.setOnSucceeded(event -> {
            if (requestId != roomRefreshRequestId) {
                return;
            }
            RoomRefreshResult result = roomRefreshTask.getValue();
            availableRooms = result.rooms();
            searchContext = result.searchContext();
            loadRooms();
        });

        roomRefreshTask.setOnFailed(event -> roomRefreshTask.getException().printStackTrace());

        Thread thread = new Thread(roomRefreshTask, "available-room-refresh");
        thread.setDaemon(true);
        thread.start();
    }

    private List<Amenity> currentAmenities(List<Amenity> amenities) {
        if (amenities == null || amenities.isEmpty()) {
            return List.of();
        }
        List<Amenity> current = new ArrayList<>();
        for (Amenity amenity : amenities) {
            Amenity fresh = CatalogService.findAmenity(amenity.getName());
            if (fresh != null) {
                current.add(fresh);
            }
        }
        return current;
    }

    private void loadRooms() {
        if (roomCards == null) {
            return;
        }
        if (availableRooms.isEmpty()) {
            roomCardNodes.clear();
            roomCardControllers.clear();
            Label empty = new Label("No rooms available for the selected criteria.");
            empty.getStyleClass().add("error-message");
            FxNodeSync.syncChildren(roomCards, List.of(empty));
            return;
        }

        List<Node> orderedCards = new ArrayList<>();
        Set<String> activeKeys = new HashSet<>();
        for (Room room : availableRooms) {
            String key = room.getRoomNumber();
            activeKeys.add(key);
            try {
                orderedCards.add(updateRoomCard(key, room));
            } catch (IOException ex) {
                ex.printStackTrace();
                Label error = new Label("Could not load room card.");
                error.getStyleClass().add("error-message");
                orderedCards.add(error);
            }
        }

        roomCardNodes.keySet().removeIf(key -> !activeKeys.contains(key));
        roomCardControllers.keySet().removeIf(key -> !activeKeys.contains(key));
        FxNodeSync.syncChildren(roomCards, orderedCards);
    }

    private Node updateRoomCard(String key, Room room) throws IOException {
        Node card = roomCardNodes.get(key);
        RoomCardController controller = roomCardControllers.get(key);
        if (card == null || controller == null) {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/RoomCard.fxml"));
            card = loader.load();
            controller = loader.getController();
            roomCardNodes.put(key, card);
            roomCardControllers.put(key, controller);
        }
        controller.setData(mainApp, guest, room, checkIn, checkOut, hasGymPass, availableRooms, searchContext);
        return card;
    }

    private record RoomRefreshResult(List<Room> rooms, ReservationSearchContext searchContext) {
    }
}
