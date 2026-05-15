package Models;


import Controllers.*;
import Repositories.*;
import Services.*;
import Utils.*;
import Utils.exceptions.*;
import Repositories.database.DatabaseConnection;
import Repositories.DatabaseInitializer.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
public class Reservation {
    public static final String GYM_PASS_NAME = "Gym";
    public static final double GYM_PASS_PRICE = 200.0;
    private static final PricingService PRICING_SERVICE = new PricingService();

    private String reservationId;
    private Guest guest;
    private Room room;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private ReservationStatus status;
    private boolean hasGymPass;
    private double totalPrice;
    private LocalDate depositDeadline;
    private boolean depositPaid;
    private boolean fullPaid;
    private boolean isLateCheckout;

    public boolean isLateCheckout() {
        return isLateCheckout;
    }

    public void setLateCheckout(boolean lateCheckout) {
        isLateCheckout = lateCheckout;
    }

    public Reservation(String reservationId, Guest guest, Room room,
                       LocalDate checkInDate, LocalDate checkOutDate,
                       ReservationStatus status, boolean hasGymPass) {
        this.reservationId = reservationId;
        this.guest = guest;
        this.room = room;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.status = status;
        this.hasGymPass = hasGymPass;
        this.depositDeadline = SystemTime.getToday().plusDays(1);
        this.depositPaid = false;
        this.fullPaid = false;
    }

    public boolean isSameDayBooking() {
        return this.checkInDate != null && this.checkInDate.isEqual(SystemTime.getToday());
    }

    public final double getDepositAmount() {
        return PRICING_SERVICE.calculateDeposit(this.totalPrice);
    }

    public double getRemainingAmount() {
        return PRICING_SERVICE.calculateRemaining(this.totalPrice);
    }

    public double getLateFee() {return PRICING_SERVICE.calculateLateFee(this.totalPrice); }

    public boolean hasGymPass() {
        return hasGymPass;
    }

    public LocalDate getDepositDeadline() {
        return depositDeadline;
    }

    public boolean isDepositPaid() {
        return depositPaid;
    }

    public void setDepositPaid(boolean paid) {
        this.depositPaid = paid;
    }

    public boolean isFullPaid() {
        return fullPaid;
    }

    public void setFullPaid(boolean paid) {
        this.fullPaid = paid;
    }

    public boolean isDepositOverdue() {
        return SystemTime.getToday().isAfter(depositDeadline) && !depositPaid;
    }

    public String getReservationId() {
        return reservationId;
    }

    public Guest getGuest() {
        return guest;
    }

    public Room getRoom() {
        return room;
    }

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }

    public void setCheckOutDate(LocalDate checkOutDate) {
        this.checkOutDate = checkOutDate;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public boolean overlaps(LocalDate newCheckIn, LocalDate newCheckOut) {
        return newCheckIn.isBefore(this.checkOutDate) &&
                newCheckOut.isAfter(this.checkInDate);
    }

    public void setTotalPrice() {
        totalPrice = calculateTotalPrice(room, checkInDate, checkOutDate, hasGymPass);
    }

    public static double calculateTotalPrice(Room room, LocalDate checkInDate, LocalDate checkOutDate, boolean hasGymPass) {
        return PRICING_SERVICE.calculateTotalPrice(room, checkInDate, checkOutDate, hasGymPass);
    }

    public static boolean isGymAmenity(Amenity amenity) {
        return PRICING_SERVICE.isGymAmenity(amenity);
    }

    public double getTotalPrice() {
        return this.totalPrice;
    }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return "ID: " + getReservationId()
                + " | Guest: " + getGuest().getUsername()
                + " | Room: " + getRoom().getRoomNumber()
                + " | " + getCheckInDate().format(formatter) + " : " + getCheckOutDate().format(formatter)
                + " | Status: " + getStatus();
    }
}
