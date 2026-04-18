import java.time.LocalDate;
import java.util.ArrayList;

public class Reservation implements Payable, Manageable {
    private String reservationId;
    private Guest guest;
    private Room room;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private ReservationStatus status;
    private boolean hasGymPass;
    private boolean hasRestaurant;
    private double totalPrice;
    private LocalDate depositDeadline;      // 48 hours before check-in
    private LocalDate fullPaymentDeadline;  // At check-in
    private boolean depositPaid;
    private boolean fullPaid;

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
        
        // Deposit due 48 hours before check-in
        this.depositDeadline = checkInDate.minusHours(48);
        // Full payment due at check-in
        this.fullPaymentDeadline = checkInDate;
        
        this.depositPaid = false;
        this.fullPaid = false;
        this.hasRestaurant = false;
    }
    
    public boolean hasRestaurant() {
        return hasRestaurant;
    }
    
    public void setHasRestaurant(boolean hasRestaurant) {
        this.hasRestaurant = hasRestaurant;
    }

    // ==================== GYM PASS & PAYMENT METHODS ====================
    
    public boolean hasGymPass() {
        return hasGymPass;
    }
    
    public void setHasGymPass(boolean hasGymPass) {
        this.hasGymPass = hasGymPass;
    }

    public LocalDate getDepositDeadline() {
        return depositDeadline;
    }
    
    public LocalDate getFullPaymentDeadline() {
        return fullPaymentDeadline;
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
    
    public boolean isFullPaymentOverdue() {
        return SystemTime.getToday().isAfter(fullPaymentDeadline) && !fullPaid;
    }

    // ==================== GETTERS & SETTERS ====================
    
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

    public void setCheckInDate(LocalDate checkInDate) {
        this.checkInDate = checkInDate;
        // Update deadlines when check-in changes
        this.depositDeadline = checkInDate.minusHours(48);
        this.fullPaymentDeadline = checkInDate;
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

    // ==================== UTILITY METHODS ====================
    
    public boolean overlaps(LocalDate newCheckIn, LocalDate newCheckOut) {
        return newCheckIn.isBefore(this.checkOutDate) &&
                newCheckOut.isAfter(this.checkInDate);
    }

    public void setTotalPrice() {
        double price = room.getPricePerNight() * checkInDate.until(checkOutDate).getDays();
        for (Amenity a : room.getAmenities()) {
            price += a.getPrice();
        }
        // Add gym pass if selected
        if (hasGymPass) {
            for (Amenity a : Database.getAmenities()) {
                if (a.getName().equalsIgnoreCase("Gym")) {
                    price += a.getPrice();
                    break;
                }
            }
        }
        totalPrice = price;
    }

    public double getTotalPrice() {
        return this.totalPrice;
    }

    /**
     * Get the first night's cost (deposit amount)
     * @return the price of one night's stay
     */
    public double getFirstNightPrice() {
        return room.getPricePerNight();
    }

    /**
     * Get the remaining amount to be paid before check-in (total - first night)
     * @return remaining balance before check-in
     */
    public double getRemainingBalance() {
        return totalPrice - getFirstNightPrice();
    }
    
    /**
     * Get the amount due at checkout (add-ons like restaurant)
     * @return add-ons total (restaurant)
     */
    public double getAddOnsTotal() {
        double addOns = 0;
        // Gym is paid upfront, but check if added later
        if (hasGymPass) {
            for (Amenity a : Database.getAmenities()) {
                if (a.getName().equalsIgnoreCase("Gym")) {
                    addOns += a.getPrice();
                    break;
                }
            }
        }
        // Restaurant is paid at checkout
        if (hasRestaurant) {
            for (Amenity a : Database.getAmenities()) {
                if (a.getName().equalsIgnoreCase("Restaurant")) {
                    addOns += a.getPrice();
                    break;
                }
            }
        }
        return addOns;
    }

    /**
     * Check if the reservation is within 48 hours of check-in date
     * @return true if within 48 hours
     */
    public boolean isWithin48Hours() {
        LocalDate today = SystemTime.getToday();
        return !today.isBefore(checkInDate.minusDays(2)) && today.isBefore(checkInDate);
    }

    /**
     * Check if reservation can be managed (cancelled/modified)
     * - Cannot manage if deposit or full payment has been made
     * - Cannot manage within 48 hours of check-in (except to extend stay)
     * @return true if manageable
     */
    public boolean canManage() {
        // Cannot manage if deposit or full payment has been made
        if (depositPaid || fullPaid) {
            return false;
        }
        // Cannot manage within 48 hours of check-in date
        if (isWithin48Hours()) {
            return false;
        }
        return true;
    }

    /**
     * Check if only stay extension is allowed
     * @return true if only extension is allowed (within 48 hours)
     */
    public boolean canOnlyExtend() {
        return isWithin48Hours() && !depositPaid && !fullPaid;
    }

    // ==================== PAYABLE INTERFACE IMPLEMENTATION ====================
    
    @Override
    public double getTotalAmount() {
        return totalPrice;
    }

    @Override
    public boolean isPaid() {
        return fullPaid;
    }

    @Override
    public LocalDate getDueDate() {
        // Return the earliest due date (deposit deadline)
        return depositDeadline;
    }

    @Override
    public boolean processPayment() {
        // Full payment processing at check-out for add-ons only
        if (status == ReservationStatus.ONGOING && !fullPaid) {
            double addOns = getAddOnsTotal();
            if (addOns > 0 && guest.getBalance() >= addOns) {
                guest.setBalance(guest.getBalance() - addOns);
                fullPaid = true;
                status = ReservationStatus.COMPLETED;
                room.setAvailable(true);
                return true;
            } else if (addOns == 0) {
                // No add-ons, just complete
                fullPaid = true;
                status = ReservationStatus.COMPLETED;
                room.setAvailable(true);
                return true;
            }
        }
        return false;
    }
    
    /**
     * Process full payment (minus deposit already paid) before check-in
     * @return true if payment successful
     */
    public boolean processFullPayment() {
        if (fullPaid) {
            return false;
        }
        
        double remaining = getRemainingBalance();
        if (guest.getBalance() >= remaining) {
            guest.setBalance(guest.getBalance() - remaining);
            fullPaid = true;
            return true;
        }
        return false;
    }
    
    /**
     * Process only the add-ons payment at checkout
     * @return true if payment successful
     */
    public boolean processAddOnsPayment() {
        if (status != ReservationStatus.ONGOING) {
            return false;
        }
        
        double addOns = getAddOnsTotal();
        if (addOns > 0 && guest.getBalance() >= addOns) {
            guest.setBalance(guest.getBalance() - addOns);
            return true;
        } else if (addOns == 0) {
            // No add-ons to pay
            return true;
        }
        return false;
    }

    // ==================== MANAGEABLE INTERFACE IMPLEMENTATION ====================
    
    @Override
    public String getId() {
        return reservationId;
    }

    @Override
    public boolean cancel() {
        // Check if cancellation is allowed
        if (!canManage()) {
            return false;
        }
        
        if (status == ReservationStatus.CANCELLED) {
            return false;
        }
        
        // If deposit was paid, refund the guest (first night)
        if (depositPaid) {
            double deposit = getFirstNightPrice();
            guest.setBalance(guest.getBalance() + deposit);
        }
        
        status = ReservationStatus.CANCELLED;
        return true;
    }

    @Override
    public boolean update() {
        // Check if update is allowed
        if (!canManage() && !canOnlyExtend()) {
            return false;
        }
        
        if (status == ReservationStatus.CANCELLED) {
            return false;
        }
        
        // Recalculate price
        setTotalPrice();
        return true;
    }

    @Override
    public String getStatus() {
        return status.toString();
    }

    @Override
    public boolean isActive() {
        return status != ReservationStatus.CANCELLED && 
               status != ReservationStatus.COMPLETED;
    }

    @Override
    public String toString() {
        return "ID: " + getReservationId()
                + " | Guest: " + getGuest().getUsername()
                + " | Room: " + getRoom().getRoomNumber()
                + " | " + getCheckInDate() + " → " + getCheckOutDate()
                + " | Status: " + getStatus();
    }
}
