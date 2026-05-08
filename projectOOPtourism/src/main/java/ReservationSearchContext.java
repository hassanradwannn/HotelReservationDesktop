import java.time.LocalDate;
import java.util.List;

public class ReservationSearchContext {
    private final Guest guest;
    private final RoomType roomType;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final int guests;
    private final List<Amenity> requestedAmenities;
    private final RoomType searchRoomType;
    private final Double maxPrice;
    private final boolean gymPass;

    public ReservationSearchContext(
            Guest guest,
            RoomType roomType,
            LocalDate checkIn,
            LocalDate checkOut,
            int guests,
            List<Amenity> requestedAmenities) {
        this(guest, roomType, checkIn, checkOut, guests, requestedAmenities, roomType, null);
    }

    public ReservationSearchContext(
            Guest guest,
            RoomType roomType,
            LocalDate checkIn,
            LocalDate checkOut,
            int guests,
            List<Amenity> requestedAmenities,
            RoomType searchRoomType,
            Double maxPrice) {
        this(guest, roomType, checkIn, checkOut, guests, requestedAmenities, searchRoomType, maxPrice, false);
    }

    public ReservationSearchContext(
            Guest guest,
            RoomType roomType,
            LocalDate checkIn,
            LocalDate checkOut,
            int guests,
            List<Amenity> requestedAmenities,
            RoomType searchRoomType,
            Double maxPrice,
            boolean gymPass) {
        this.guest = guest;
        this.roomType = roomType;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.requestedAmenities = requestedAmenities == null ? List.of() : List.copyOf(requestedAmenities);
        this.searchRoomType = searchRoomType;
        this.maxPrice = maxPrice;
        this.gymPass = gymPass;
    }

    public Guest getGuest() {
        return guest;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public LocalDate getCheckIn() {
        return checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
    }

    public int getGuests() {
        return guests;
    }

    public List<Amenity> getRequestedAmenities() {
        return requestedAmenities;
    }

    public RoomType getSearchRoomType() {
        return searchRoomType;
    }

    public Double getMaxPrice() {
        return maxPrice;
    }

    public boolean hasGymPass() {
        return gymPass;
    }
}
