package Services;


import Controllers.*;
import Models.*;
import Repositories.*;
import Utils.*;
import Utils.exceptions.*;
import Repositories.database.DatabaseConnection;
import Repositories.DatabaseInitializer.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AvailabilityService {

    public boolean isDateRangeValid(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            return false;
        }

        LocalDate today = SystemTime.getToday();

        if (checkIn.isBefore(today)) {
            return false;
        }
        return checkOut.isAfter(checkIn);
    }

    public boolean isRoomAvailable(Room room, LocalDate checkIn, LocalDate checkOut) {
        for (Reservation reservation : Database.getReservations()) {
            if (reservation.getRoom().getRoomNumber().equalsIgnoreCase(room.getRoomNumber())
                    && reservation.getStatus() != ReservationStatus.CANCELLED
                    && reservation.overlaps(checkIn, checkOut)) {
                return false;
            }
        }
        return true;
    }

    public List<Room> searchAvailableRooms(LocalDate checkIn, LocalDate checkOut,
            RoomType requestedType, int guests, List<Amenity> requestedAmenities) {
        ArrayList<Room> availableRooms = new ArrayList<>();
        if (!isDateRangeValid(checkIn, checkOut)) {
            return availableRooms;
        }

        for (Room room : Database.getRooms()) {
            if (requestedType == null
                    || room.getRoomType() == null
                    || !room.getRoomType().getName().equalsIgnoreCase(requestedType.getName())) {
                continue;
            }

            if (room.getRoomType().getCapacity() < guests) {
                continue;
            }

            if (requestedAmenities != null && !requestedAmenities.isEmpty()) {
                boolean hasAll = true;
                List<String> roomAmenityNames = room.getAmenities().stream().map(Amenity::getName).toList();
                for (Amenity requestedAmenity : requestedAmenities) {
                    if (!roomHasRequestedAmenity(room, roomAmenityNames, requestedAmenity)) {
                        hasAll = false;
                        break;
                    }
                }
                if (!hasAll) {
                    continue;
                }
            }

            if (isRoomAvailable(room, checkIn, checkOut)) {
                availableRooms.add(room);
            }
        }

        return availableRooms;
    }

    public boolean hasOverlappingReservation(Room room, LocalDate checkIn, LocalDate checkOut) {
        for (Reservation reservation : Database.getReservations()) {
            if (!reservation.getRoom().equals(room)) {
                continue;
            }

            if (reservation.getStatus() == ReservationStatus.CANCELLED
                    || reservation.getStatus() == ReservationStatus.COMPLETED) {
                continue;
            }

            boolean overlap = checkIn.isBefore(reservation.getCheckOutDate())
                    && checkOut.isAfter(reservation.getCheckInDate());

            if (overlap) {
                return true;
            }
        }

        return false;
    }

    private boolean roomHasRequestedAmenity(Room room, List<String> roomAmenityNames, Amenity requestedAmenity) {
        if (requestedAmenity == null || requestedAmenity.getName() == null) {
            return true;
        }

        String requestedName = requestedAmenity.getName();
        if (isViewAmenity(requestedName)) {
            return requestedName.equalsIgnoreCase(room.getViewName());
        }
        if (CatalogService.isGymAmenityName(requestedName)) {
            return roomAmenityNames.stream().anyMatch(CatalogService::isGymAmenityName);
        }

        return roomAmenityNames.stream().anyMatch(name -> name.equalsIgnoreCase(requestedName));
    }

    private boolean isViewAmenity(String name) {
        return "Sea View".equalsIgnoreCase(name) || "Mountain View".equalsIgnoreCase(name);
    }
}
