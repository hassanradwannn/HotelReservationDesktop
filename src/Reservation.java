import java.time.LocalDate;

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
    private double paidAmount;  // Track actual amount paid (for calculating outstanding after extension)
    private LocalDate originalCheckOutDate;  // Track original check-out date before extension
    private final double lateFeePercentage = 0.05; // 5% late fee
    private boolean lateFeeApplied = false;
    private boolean isSameDayBooking; // Flag for same-day bookings

    public Reservation(String reservationId, Guest guest, Room room,
                       LocalDate checkInDate, LocalDate checkOutDate,
                       ReservationStatus status, boolean hasGymPass) {
        this(reservationId, guest, room, checkInDate, checkOutDate, status, hasGymPass, false);
    }
    
    public Reservation(String reservationId, Guest guest, Room room,
                       LocalDate checkInDate, LocalDate checkOutDate,
                       ReservationStatus status, boolean hasGymPass, boolean isSameDayBooking) {
        this.reservationId = reservationId;
        this.guest = guest;
        this.room = room;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.status = status;
        
        // Check if room already has gym (penthouse rooms include gym by default)
        boolean roomHasGym = room.getAmenities().stream()
            .anyMatch(a -> a.getName().equalsIgnoreCase("Gym"));
        // Gym is included if room has it OR if guest explicitly requested it
        this.hasGymPass = roomHasGym || hasGymPass;
        
        // Deposit due 48 hours before check-in
        this.depositDeadline = checkInDate.minusDays(2);
        // Full payment due at check-in
        this.fullPaymentDeadline = checkInDate;
        
        this.depositPaid = false;
        this.fullPaid = false;
        this.hasRestaurant = false;
        this.paidAmount = 0; // Initialize paid amount
        // Don't set originalCheckOutDate here - only when actually extending
        this.isSameDayBooking = isSameDayBooking; // Initialize same-day booking flag
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
    
    public double getPaidAmount() {
        return paidAmount;
    }
    
    public void setPaidAmount(double amount) {
        this.paidAmount = amount;
    }
    
    /**
     * Get the actual outstanding amount (total - amount actually paid)
     * This is different from isFullPaid which may be stale after stay extension
     * @return amount still owed
     */
    public double getActualOutstanding() {
        return totalPrice - paidAmount;
    }
    
    public LocalDate getOriginalCheckOutDate() {
        return originalCheckOutDate;
    }
    
    public void setOriginalCheckOutDate(LocalDate originalCheckOutDate) {
        this.originalCheckOutDate = originalCheckOutDate;
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
        // Update deadlines when check-in changes (2 days before for deposit)
        this.depositDeadline = checkInDate.minusDays(2);
        this.fullPaymentDeadline = checkInDate;
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }

    public void setCheckOutDate(LocalDate checkOutDate) {
        // Only track original check-out date when actually extending an existing reservation
        // Don't set it during initial reservation creation
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
     * Includes 5% late fee if payment is overdue
     * @return remaining balance before check-in
     */
    public double getRemainingBalance() {
        if (isDepositOverdue() && !lateFeeApplied) {
            applyLateFee();
        }
        return totalPrice - getFirstNightPrice();
    }
    
    /**
     * Apply the late fee to the reservation
     */
    public void applyLateFee() {
        if (!lateFeeApplied) {
            totalPrice += totalPrice * lateFeePercentage;
            lateFeeApplied = true;
        }
    }
    
    public boolean isLateFeeApplied() {
        return lateFeeApplied;
    }
    
    public double getLateFeeAmount() {
        return totalPrice * lateFeePercentage;
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
     * @return true if only extension is allowed (within 48 hours before check-in, not paid)
     */
    public boolean canOnlyExtend() {
        return isWithin48Hours() && !depositPaid && !fullPaid;
    }
    
    /**
     * Check if reservation can be extended (more flexible than canManage)
     * Allows extension for: ongoing reservations, or within 48hrs with deposit paid
     * @return true if extension is allowed
     */
    public boolean canExtend() {
        // Ongoing reservations can always be extended
        if (status == ReservationStatus.ONGOING) {
            return true;
        }
        // Confirmed reservations can be extended if not cancelled
        if (status == ReservationStatus.CONFIRMED && !isWithin48Hours()) {
            return true;
        }
        // Within 48 hours but deposit paid - can still extend
        if (status == ReservationStatus.CONFIRMED && isWithin48Hours() && depositPaid) {
            return true;
        }
        return false;
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
        // Check if update is allowed - but allow ONGOING reservations to update (for stay extension)
        if (status != ReservationStatus.ONGOING) {
            if (!canManage() && !canOnlyExtend()) {
                return false;
            }
        }
        
        if (status == ReservationStatus.CANCELLED) {
            return false;
        }
        
        // Recalculate price
        setTotalPrice();
        return true;
    }

    @Override
    public String getStatusString() {
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
