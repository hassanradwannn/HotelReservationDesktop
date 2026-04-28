import java.time.LocalDate;

public class Reservation {
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
    private boolean isSameDay;

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
        setSameDay();
    }

    public final double getDepositAmount() {
        if (isSameDay) {
            return 0.0;
        }
        return this.totalPrice * 0.25;
    }

    public double getRemainingAmount() {
        return this.totalPrice - getDepositAmount();
    }

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
        double price = room.getPricePerNight() * checkInDate.until(checkOutDate).getDays();
        for (Amenity a : room.getAmenities()) {
            price += a.getPrice();
        }
        totalPrice = price;
    }

    public double getTotalPrice() {
        return this.totalPrice;
    }

    private void setSameDay() {
        if(this.checkInDate != null && this.checkInDate.isEqual(SystemTime.getToday())) {
            isSameDay = true;
        }
    }

    @Override
    public String toString() {
        return "ID: " + getReservationId()
                + " | Guest: " + getGuest().getUsername()
                + " | Room: " + getRoom().getRoomNumber()
                + " | " + getCheckInDate() + " : " + getCheckOutDate()
                + " | Status: " + getStatus();
    }
}