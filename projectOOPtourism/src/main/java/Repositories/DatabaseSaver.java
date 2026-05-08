package Repositories;


import Controllers.*;
import Models.*;
import Services.*;
import Utils.*;
import Utils.exceptions.*;
import Repositories.database.DatabaseConnection;
import Repositories.DatabaseInitializer.*;
public class DatabaseSaver {

    private static final InvoiceRepository INVOICE_REPOSITORY = new InvoiceRepository();
    private static final UserRepository USER_REPOSITORY = new UserRepository();
    private static final CatalogRepository CATALOG_REPOSITORY = new CatalogRepository();
    private static final ReservationRepository RESERVATION_REPOSITORY = new ReservationRepository();

    public static boolean silentSync = false;

    public static void ensureInvoiceSchema() {
        INVOICE_REPOSITORY.ensureSchema();
    }

    public static void saveUser(User user) {
        USER_REPOSITORY.saveUser(user);
    }

    public static void saveRoomType(RoomType type) {
        CATALOG_REPOSITORY.saveRoomType(type);
    }

    public static void saveRoom(Room room) {
        CATALOG_REPOSITORY.saveRoom(room);
    }

    public static void saveAmenity(Amenity amenity) {
        CATALOG_REPOSITORY.saveAmenity(amenity);
    }

    public static void saveRoomAmenities(Room room) {
        CATALOG_REPOSITORY.saveRoomAmenities(room);
    }

    public static void saveReservation(String reservationId, String guestUsername, String roomNumber,
                                       java.time.LocalDate checkIn,
                                       java.time.LocalDate checkOut,
                                       boolean hasGymPass,
                                       String status) {
        RESERVATION_REPOSITORY.saveReservation(reservationId, guestUsername, roomNumber, checkIn, checkOut, hasGymPass, status);
    }

    public static void saveInvoice(String guestUsername, String roomNumber,
                                   double totalAmount, String paymentMethod,
                                   boolean paid) {
        INVOICE_REPOSITORY.saveInvoice(guestUsername, roomNumber, totalAmount, paymentMethod, paid);
    }

    public static void saveInvoice(String reservationId, String guestUsername, String roomNumber,
                                   double totalAmount, String paymentMethod,
                                   boolean paid) {
        INVOICE_REPOSITORY.saveInvoice(reservationId, guestUsername, roomNumber, totalAmount, paymentMethod, paid);
    }

    public static void updateUserBalance(String username, double newBalance) {
        USER_REPOSITORY.updateBalance(username, newBalance);
    }

    public static void updateReservationStatus(String reservationId, String newStatus) {
        RESERVATION_REPOSITORY.updateStatus(reservationId, newStatus);
    }

    public static void updateReservationDates(String reservationId, java.time.LocalDate checkIn, java.time.LocalDate checkOut) {
        RESERVATION_REPOSITORY.updateDates(reservationId, checkIn, checkOut);
    }

    public static void updateUserPassword(String username, String newPassword) {
        USER_REPOSITORY.updatePassword(username, newPassword);
    }

    public static void updateGuestProfile(String oldUsername, Guest guest) throws Exception {
        USER_REPOSITORY.updateGuestProfile(oldUsername, guest);
    }
}
