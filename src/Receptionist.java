
import java.time.LocalDate;
import java.util.List;

public class Receptionist extends Staff {

    public Receptionist(String username, String password, LocalDate dateOfBirth, int workingHours) {
        super(username, password, dateOfBirth, Role.RECEPTIONIST, workingHours);
    }

    @Override
    public void viewGuests(List<Guest> guests) {
        System.out.println("Receptionist viewing guests:");
        guests.forEach(g -> System.out.println(g.getUsername()));
    }

    @Override
    public void viewRooms(List<String> rooms) {
        System.out.println("Receptionist viewing rooms:");
        rooms.forEach(System.out::println);
    }

    @Override
    public void viewReservations(List<String> reservations) {
        System.out.println("Receptionist viewing reservations:");
        reservations.forEach(System.out::println);
    }

    public void checkInGuest(Guest guest, String room) {
        System.out.println("Guest " + guest.getUsername() + " checked in to room " + room);
    }

    public void checkOutGuest(Guest guest, String room) {
        System.out.println("Guest " + guest.getUsername() + " checked out from room " + room);
    }
}