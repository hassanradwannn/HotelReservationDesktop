import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class PricingService {

    public double calculateTotalPrice(Room room, LocalDate checkInDate, LocalDate checkOutDate, boolean hasGymPass) {
        long nights = Math.max(1, ChronoUnit.DAYS.between(checkInDate, checkOutDate));
        double price = room.getPricePerNight() * nights;
        for (Amenity amenity : room.getAmenities()) {
            if (hasGymPass && isGymAmenity(amenity)) {
                continue;
            }
            price += amenity.getPrice();
        }
        if (hasGymPass) {
            price += Reservation.GYM_PASS_PRICE;
        }
        return price;
    }

    public double calculateDeposit(double totalPrice) {
        return totalPrice * 0.25;
    }

    public double calculateRemaining(double totalPrice) {
        return totalPrice - calculateDeposit(totalPrice);
    }

    public double calculateLateFee(double totalPrice) { return totalPrice += (1/10 * totalPrice);}

    public boolean isGymAmenity(Amenity amenity) {
        return amenity != null && CatalogService.isGymAmenityName(amenity.getName());
    }
}
