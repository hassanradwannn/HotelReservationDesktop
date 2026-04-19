import exceptions.InvalidPaymentException;
import exceptions.RoomNotAvailableException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ReservationService {

    private static ArrayList<Room> rooms = Database.getRooms();
    private static ArrayList<Reservation> reservations = Database.getReservations();

    private ReservationService() {} // Utility class

    public static boolean isDateRangeValid(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            return false;
        }

        LocalDate today = SystemTime.getToday();

        if (checkIn.isBefore(today)) {
            return false;
        }
        if (!checkOut.isAfter(checkIn)) {
            return false;
        }

        return true;
    }
    
    public static boolean isRoomAvailable(Room room, LocalDate checkIn, LocalDate checkOut) {
        for (Reservation reservation : reservations) {
            if (reservation.getRoom().getRoomNumber().equals(room.getRoomNumber())
                    && reservation.getStatus() != ReservationStatus.CANCELLED
                    && reservation.overlaps(checkIn, checkOut)) {
                return false;
            }
        }
        return true;
    }

    public static Reservation createReservation(Guest guest, Room room,
                                         LocalDate checkIn, LocalDate checkOut, boolean addGym) throws RoomNotAvailableException {

        if (!isDateRangeValid(checkIn, checkOut)) {
            throw new IllegalArgumentException("Invalid reservation dates.");
        }

        LocalDate today = SystemTime.getToday();
        LocalDate tomorrow = today.plusDays(1);
        
        // Check if booking is for today (same-day booking)
        boolean isSameDayBooking = checkIn.isEqual(today);
        boolean isTomorrowBooking = checkIn.isEqual(tomorrow);
        
        // For same-day bookings, require full payment immediately
        // For tomorrow bookings, allow with deposit
        // For 2+ days out, require deposit with 48hr deadline
        if (checkIn.isBefore(today)) {
            throw new IllegalArgumentException("Check-in date cannot be in the past.");
        }

        if (!isRoomAvailable(room, checkIn, checkOut)) {
            throw new RoomNotAvailableException("Room is not available for the selected dates.");
        }

        String reservationId = "RES-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Reservation reservation = new Reservation(
                reservationId,
                guest,
                room,
                checkIn,
                checkOut,
                ReservationStatus.PENDING,
                addGym,
                isSameDayBooking); // Pass same-day booking flag

        reservation.setTotalPrice();
        reservations.add(reservation);
        
        // Check for any overdue deposits and cancel those reservations
        cancelOverdueReservations();
        
        return reservation;
    }
    
    /**
     * Cancel all reservations with overdue deposits (not paid 1 day before check-in)
     */
    public static void cancelOverdueReservations() {
        List<Reservation> toCancel = new ArrayList<>();
        LocalDate today = SystemTime.getToday();
        
        for (Reservation r : reservations) {
            // Calculate days until check-in
            long daysUntilCheckIn = today.until(r.getCheckInDate()).getDays();
            
            // Don't auto-cancel same-day bookings (check-in is today or already passed)
            // These require full payment at booking, but if PENDING, let them be
            if (daysUntilCheckIn <= 0) {
                continue;
            }
            
            // Cancel if deposit deadline passed and not paid
            LocalDate cancelDeadline = r.getDepositDeadline().plusDays(1); // 1 day before check-in
            if (r.getStatus() == ReservationStatus.PENDING 
                    && !r.isDepositPaid() 
                    && (today.equals(cancelDeadline) || today.isAfter(cancelDeadline))) {
                toCancel.add(r);
            }
        }
        
        for (Reservation r : toCancel) {
            r.setStatus(ReservationStatus.CANCELLED);
            System.out.println("⚠️ Reservation " + r.getReservationId() + " has been AUTOMATICALLY CANCELLED (deposit not paid 1 day before check-in)");
        }
    }
    
    /**
     * Get count of cancelled overdue reservations
     */
    public static int getCancelledOverdueCount() {
        int count = 0;
        LocalDate today = SystemTime.getToday();
        for (Reservation r : reservations) {
            LocalDate cancelDeadline = r.getDepositDeadline().plusDays(1);
            if (r.getStatus() == ReservationStatus.CANCELLED
                    && !r.isDepositPaid()
                    && (today.equals(cancelDeadline) || today.isAfter(cancelDeadline))) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Process no-shows - cancel reservations where check-in date has passed and guest never checked in
     * Refund the guest (minus 10% no-show fee)
     */
    public static void processNoShows() {
        LocalDate today = SystemTime.getToday();
        double noShowFeePercentage = 0.10; // 10% no-show fee
        
        for (Reservation r : reservations) {
            // Only process CONFIRMED reservations where check-in date has passed
            if (r.getStatus() == ReservationStatus.CONFIRMED
                    && today.isAfter(r.getCheckInDate())) {
                
                double refundAmount = 0;
                
                // If guest paid deposit, refund them minus 10% no-show fee
                if (r.isDepositPaid()) {
                    double depositAmount = r.getFirstNightPrice();
                    double noShowFee = depositAmount * noShowFeePercentage;
                    refundAmount = depositAmount - noShowFee;
                    
                    // Refund to guest account
                    r.getGuest().setBalance(r.getGuest().getBalance() + refundAmount);
                    
                    System.out.println("⚠️ NO-SHOW: Reservation " + r.getReservationId() + " cancelled.");
                    System.out.println("  Original deposit: $" + String.format("%.2f", depositAmount));
                    System.out.println("  No-show fee (10%): $" + String.format("%.2f", noShowFee));
                    System.out.println("  Refund to guest: $" + String.format("%.2f", refundAmount));
                } else {
                    System.out.println("⚠️ NO-SHOW: Reservation " + r.getReservationId() + " cancelled (no deposit paid).");
                }
                
                // Cancel the reservation
                r.setStatus(ReservationStatus.CANCELLED);
                // Make room available again
                r.getRoom().setAvailable(true);
            }
        }
    }

    public static void cancelReservation(String reservationId) {
        for (Reservation reservation : reservations) {
            if (reservation.getReservationId().equals(reservationId)) {
                // Use the Manageable interface cancel method with canManage check
                if (reservation.cancel()) {
                    return;
                } else {
                    throw new IllegalArgumentException("Cannot cancel this reservation. It may be already paid or within 48 hours of check-in.");
                }
            }
        }
        throw new IllegalArgumentException("Reservation not found.");
    }

    // Overloaded method that accepts Manageable for polymorphism
    public static void cancelReservation(Manageable manageable) {
        if (!manageable.cancel()) {
            throw new IllegalArgumentException("Cannot cancel this reservation. It may be already paid or within 48 hours of check-in.");
        }
    }

    /**
     * Get the deposit amount (first night's price)
     * @param reservation the reservation to calculate deposit for
     * @return first night's price as deposit
     */
    public static double getDepositAmount(Reservation reservation) {
        return reservation.getFirstNightPrice();
    }

    /**
     * Get the remaining amount to be paid before check-in (total - deposit)
     * @param reservation the reservation
     * @return remaining balance (total - first night)
     */
    public static double getRemainingAmount(Reservation reservation) {
        return reservation.getRemainingBalance();
    }
    
    /**
     * Get add-ons total (e.g., gym pass)
     * @param reservation the reservation
     * @return add-ons amount
     */
    public static double getAddOnsAmount(Reservation reservation) {
        return reservation.getAddOnsTotal();
    }

    /**
     * Pay deposit (first night) - due 48 hours before check-in
     */
    public static boolean payDeposit(Reservation reservation, Guest guest) {
        if (reservation.isDepositPaid()) {
            throw new IllegalArgumentException("Deposit already paid.");
        }

        double deposit = getDepositAmount(reservation);
        if (guest.getBalance() < deposit) {
            throw new IllegalArgumentException("Insufficient balance. Need $" + deposit + ", have $" + guest.getBalance());
        }

        guest.setBalance(guest.getBalance() - deposit);
        reservation.setDepositPaid(true);
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setPaidAmount(deposit);
        return true;
    }
    
    /**
     * Pay full amount before check-in (total - deposit already paid)
     */
    public static boolean payFullAmount(Reservation reservation, Guest guest) {
        if (reservation.isFullPaid()) {
            throw new IllegalArgumentException("Full amount already paid.");
        }

        double remaining = getRemainingAmount(reservation);
        if (guest.getBalance() < remaining) {
            throw new IllegalArgumentException("Insufficient balance. Need $" + remaining + ", have $" + guest.getBalance());
        }

        guest.setBalance(guest.getBalance() - remaining);
        reservation.setFullPaid(true);
        reservation.setPaidAmount(reservation.getTotalPrice());
        return true;
    }
    
    /**
     * Pay full amount including deposit (if not already paid)
     */
    public static boolean payInFull(Reservation reservation, Guest guest) {
        double totalNeeded = reservation.getTotalPrice();
        
        if (guest.getBalance() < totalNeeded) {
            throw new IllegalArgumentException("Insufficient balance. Need $" + totalNeeded + ", have $" + guest.getBalance());
        }

        guest.setBalance(guest.getBalance() - totalNeeded);
        reservation.setDepositPaid(true);
        reservation.setFullPaid(true);
        reservation.setPaidAmount(totalNeeded); // Track full payment
        reservation.setStatus(ReservationStatus.CONFIRMED);
        return true;
    }

    public static boolean checkInGuest(Reservation reservation, Guest guest) {
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new IllegalArgumentException("Reservation must be CONFIRMED to check in.");
        }

        if (!SystemTime.getToday().isEqual(reservation.getCheckInDate())) {
            throw new IllegalArgumentException("Check-in date has not arrived yet.");
        }

        // Check if already fully paid
        if (reservation.isFullPaid()) {
            // Already paid, just check in
            reservation.setStatus(ReservationStatus.ONGOING);
            reservation.getRoom().setAvailable(false);
            return true;
        }
        
        // If not fully paid, require confirmation that guest already paid
        // (receptionist would have collected payment manually beforehand)
        // For now, auto-charge remaining balance from guest's account
        double remaining = reservation.getRemainingBalance();
        if (guest.getBalance() < remaining) {
            throw new IllegalArgumentException("Insufficient balance. Guest needs $" + remaining + " but has only $" + guest.getBalance() + ". Payment required before check-in.");
        }
        
        // Process remaining payment
        guest.setBalance(guest.getBalance() - remaining);
        reservation.setFullPaid(true);
        reservation.setPaidAmount(reservation.getTotalPrice()); // Mark fully paid

        reservation.setStatus(ReservationStatus.ONGOING);
        reservation.getRoom().setAvailable(false);
        return true;
    }
    
    /**
     * Confirm check-in with manual payment verification (receptionist confirms payment received)
     */
    public static boolean checkInGuestManual(Reservation reservation) {
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new IllegalArgumentException("Reservation must be CONFIRMED to check in.");
        }

        if (!SystemTime.getToday().isEqual(reservation.getCheckInDate())) {
            throw new IllegalArgumentException("Check-in date has not arrived yet.");
        }
        
        // Mark as fully paid (receptionist confirmed payment received)
        reservation.setFullPaid(true);
        reservation.setPaidAmount(reservation.getTotalPrice()); // Track full payment
        reservation.setStatus(ReservationStatus.ONGOING);
        reservation.getRoom().setAvailable(false);
        return true;
    }

    public static boolean checkOutGuest(Reservation reservation, PaymentMethod paymentMethod) {
        if (reservation.getStatus() != ReservationStatus.ONGOING) {
            throw new IllegalArgumentException("Reservation must be ONGOING to check out.");
        }

        if (paymentMethod == null) {
            throw new IllegalArgumentException("Payment method must be provided.");
        }

        Guest guest = reservation.getGuest();
        
        // Get the total amount still owed (stay extension + add-ons)
        // This is calculated from totalPrice - paidAmount in the checkout summary
        // So we just need to pay the remaining balance once
        double totalDue = reservation.getTotalPrice() - reservation.getPaidAmount();
        
        if (totalDue > 0) {
            if (guest.getBalance() < totalDue) {
                throw new IllegalArgumentException("Insufficient balance. Need $" + totalDue + ", have $" + guest.getBalance());
            }
            
            // Process single payment for total amount due (stay extension + add-ons combined)
            try {
                Invoice invoice = new Invoice(totalDue, paymentMethod);
                if (invoice.processPayment()) {
                    guest.setBalance(guest.getBalance() - totalDue);
                    // Update paid amount
                    reservation.setPaidAmount(reservation.getPaidAmount() + totalDue);
                }
            } catch (InvalidPaymentException e) {
                throw new IllegalArgumentException("Payment failed: " + e.getMessage());
            }
        }
        
        // Mark as fully paid after all payments processed
        reservation.setFullPaid(true);
        
        reservation.setStatus(ReservationStatus.COMPLETED);
        reservation.getRoom().setAvailable(true);
        return true;
    }

    // Updated checkout that accepts Payable for polymorphism
    public static boolean checkOutGuest(Payable payable, PaymentMethod paymentMethod) {
        if (!(payable instanceof Reservation)) {
            throw new IllegalArgumentException("Can only checkout Reservation objects.");
        }
        Reservation reservation = (Reservation) payable;
        return checkOutGuest(reservation, paymentMethod);
    }

    /**
     * Modify reservation dates using Manageable interface
     * Only allowed if reservation is manageable (not paid, not within 48 hours of check-in)
     */
    public static boolean modifyReservation(String reservationId, LocalDate newCheckIn, LocalDate newCheckOut) {
        for (Reservation reservation : reservations) {
            if (reservation.getReservationId().equals(reservationId)) {
                
                // Check if only extension is allowed (within 48 hours)
                if (reservation.canOnlyExtend()) {
                    // Only allow extending stay (check-out date can move forward)
                    if (!newCheckOut.isAfter(reservation.getCheckOutDate())) {
                        throw new IllegalArgumentException("Within 48 hours of check-in, you can only extend your stay (push check-out date forward).");
                    }
                } else if (!reservation.canManage()) {
                    throw new IllegalArgumentException("Cannot modify this reservation. It may be already paid or within 48 hours of check-in.");
                }
                
                if (reservation.getStatus() == ReservationStatus.CANCELLED) {
                    throw new IllegalArgumentException("Cannot modify a cancelled reservation.");
                }
                
                // Check room availability for new dates
                if (!isRoomAvailable(reservation.getRoom(), newCheckIn, newCheckOut)) {
                    throw new IllegalArgumentException("Room is not available for the new dates.");
                }
                
                // Update dates
                reservation.setCheckInDate(newCheckIn);
                reservation.setCheckOutDate(newCheckOut);
                
                // Recalculate price using Manageable update method
                reservation.update();
                
                return true;
            }
        }
        throw new IllegalArgumentException("Reservation not found.");
    }

    // Overloaded method accepting Manageable
    public static boolean modifyReservation(Manageable manageable, LocalDate newCheckIn, LocalDate newCheckOut) {
        if (!(manageable instanceof Reservation)) {
            throw new IllegalArgumentException("Can only modify Reservation objects.");
        }
        Reservation reservation = (Reservation) manageable;
        return modifyReservation(reservation.getReservationId(), newCheckIn, newCheckOut);
    }

    /**
     * Extend stay - allows extending even within 48 hours of check-in
     */
    public static boolean extendStay(String reservationId, LocalDate newCheckOut) {
        for (Reservation reservation : reservations) {
            if (reservation.getReservationId().equals(reservationId)) {
                
                // Must be within manageable timeframe OR allow extension
                if (!reservation.canManage() && !reservation.canOnlyExtend() && !reservation.canExtend()) {
                    throw new IllegalArgumentException("Cannot extend this reservation.");
                }
                
                if (reservation.getStatus() == ReservationStatus.CANCELLED) {
                    throw new IllegalArgumentException("Cannot extend a cancelled reservation.");
                }
                
                // New check-out must be after current check-out
                if (!newCheckOut.isAfter(reservation.getCheckOutDate())) {
                    throw new IllegalArgumentException("New check-out date must be after the current check-out date.");
                }
                
                // Calculate additional cost for the extension BEFORE updating dates
                // This ensures we track the original price correctly
                double pricePerNight = reservation.getRoom().getPricePerNight();
                long additionalDays = reservation.getCheckOutDate().until(newCheckOut).getDays();
                double additionalCost = pricePerNight * additionalDays;
                
                // Store the original check-out date if not already stored
                if (reservation.getOriginalCheckOutDate() == null) {
                    reservation.setOriginalCheckOutDate(reservation.getCheckOutDate());
                }
                
                // Set new check-out date
                reservation.setCheckOutDate(newCheckOut);
                
                // Recalculate total - this adds the extension cost to existing total
                reservation.setTotalPrice();
                
                return true;
            }
        }
        throw new IllegalArgumentException("Reservation not found.");
    }

    public static List<Room> searchAvailableRooms(LocalDate checkIn, LocalDate checkOut,
            RoomType requestedType, int guests) {
        ArrayList<Room> availableRooms = new ArrayList<>();
        if (!isDateRangeValid(checkIn, checkOut)) {
            return availableRooms;
        }

        for (Room room : rooms) {
            if (room.getRoomType() != requestedType) {
                continue;
            }

            if (room.getRoomType().getCapacity() < guests) {
                continue;
            }

            if (isRoomAvailable(room, checkIn, checkOut)) {
                availableRooms.add(room);
            }
        }

        return availableRooms;
    }

    // Utility method to get all payable reservations
    public static List<Payable> getPayableReservations() {
        List<Payable> payables = new ArrayList<>();
        for (Reservation r : reservations) {
            if (r instanceof Payable) {
                payables.add((Payable) r);
            }
        }
        return payables;
    }

    // Utility method to get all manageable reservations
    public static List<Manageable> getManageableReservations() {
        List<Manageable> manageables = new ArrayList<>();
        for (Reservation r : reservations) {
            if (r instanceof Manageable && r.canManage()) {
                manageables.add((Manageable) r);
            }
        }
        return manageables;
    }

    // Utility method to get all reservations that can be extended
    public static List<Manageable> getExtendableReservations() {
        List<Manageable> extendable = new ArrayList<>();
        for (Reservation r : reservations) {
            if (r instanceof Manageable && (r.canManage() || r.canOnlyExtend() || r.canExtend())) {
                extendable.add((Manageable) r);
            }
        }
        return extendable;
    }
}