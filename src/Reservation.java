import java.time.LocalDate;

enum Status {
    PENDING,
    CONFIRMED,
    CANCELLED,
    COMPLETED,
}

public class Reservation {

    private Guest guest;
    private Room room;
    private LocalDate checkinDate;
    private LocalDate checkoutDate;
    private Status status;

    public Reservation(Guest guest, Room room, LocalDate checkinDate, LocalDate checkoutDate) {
        this.guest = guest;
        this.room = room;
        this.checkinDate = checkinDate;
        this.checkoutDate = checkoutDate;
        this.status = Status.PENDING;
    }

    public Guest getGuest() {
        return guest;
    }

    public void setGuest(Guest guest) {
        this.guest = guest;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public LocalDate getCheckinDate() {
        return checkinDate;
    }

    public void setCheckinDate(LocalDate checkinDate) {
        this.checkinDate = checkinDate;
    }

    public LocalDate getCheckoutDate() {
        return checkoutDate;
    }

    public void setCheckoutDate(LocalDate checkoutDate) {
        this.checkoutDate = checkoutDate;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }    

}
