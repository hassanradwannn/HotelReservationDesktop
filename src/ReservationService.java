import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import exceptions.InvalidPaymentException;

public abstract class ReservationService {

    private static ArrayList<Room> rooms = Database.getRooms();
    private static ArrayList<Reservation> reservations = Database.getReservations();

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
            if (reservation.getRoom().getRoomNumber() == room.getRoomNumber()
                    && reservation.getStatus() != ReservationStatus.CANCELLED
                    && reservation.overlaps(checkIn, checkOut)) {
                return false;
            }
        }
        return true;
    }

    public static Reservation createReservation(Guest guest, Room room,
                                         LocalDate checkIn, LocalDate checkOut, boolean addGym) {

        if (!isDateRangeValid(checkIn, checkOut)) {
            throw new IllegalArgumentException("Invalid reservation dates.");
        }

        LocalDate today = SystemTime.getToday();
        LocalDate tomorrow = today.plusDays(1);
        if (checkIn.isBefore(tomorrow) || checkIn.isEqual(today)) {
            throw new IllegalArgumentException("Check-in must be at least tomorrow. Deposit payment deadline is 48 hours before check-in.");
        }

        if (!isRoomAvailable(room, checkIn, checkOut)) {
            throw new IllegalArgumentException("Room is not available for the selected dates.");
        }

        String reservationId = "RES-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Reservation reservation = new Reservation(
                reservationId,
                guest,
                room,
                checkIn,
                checkOut,
                ReservationStatus.PENDING,
                addGym);

        reservation.setTotalPrice();
        reservations.add(reservation);
        return reservation;
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

        // Pay remaining amount before check-in if not already paid
        if (!reservation.isFullPaid()) {
            double remaining = reservation.getRemainingBalance();
            if (guest.getBalance() < remaining) {
                cancelReservation(reservation.getReservationId());
                throw new IllegalArgumentException("Insufficient balance, you need $" + (remaining - guest.getBalance()) + " more to check in.");
            }
            guest.setBalance(guest.getBalance() - remaining);
            reservation.setFullPaid(true);
        }

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

        // Pay add-ons at checkout (e.g., gym pass)
        double addOns = reservation.getAddOnsTotal();
        if (addOns > 0) {
            Guest guest = reservation.getGuest();
            if (guest.getBalance() < addOns) {
                throw new IllegalArgumentException("Insufficient balance for add-ons. Need $" + addOns + ", have $" + guest.getBalance());
            }
            
            try {
                Invoice invoice = new Invoice(addOns, paymentMethod);
                if (invoice.processPayment()) {
                    guest.setBalance(guest.getBalance() - addOns);
                }
            } catch (InvalidPaymentException e) {
                throw new IllegalArgumentException("Add-ons payment failed: " + e.getMessage());
            }
        }

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
                if (!reservation.canManage() && !reservation.canOnlyExtend()) {
                    throw new IllegalArgumentException("Cannot extend this reservation.");
                }
                
                if (reservation.getStatus() == ReservationStatus.CANCELLED) {
                    throw new IllegalArgumentException("Cannot extend a cancelled reservation.");
                }
                
                // New check-out must be after current check-out
                if (!newCheckOut.isAfter(reservation.getCheckOutDate())) {
                    throw new IllegalArgumentException("New check-out date must be after the current check-out date.");
                }
                
                // Set new check-out date
                reservation.setCheckOutDate(newCheckOut);
                
                // Recalculate price
                reservation.update();
                
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
            if (r instanceof Manageable && (r.canManage() || r.canOnlyExtend())) {
                extendable.add((Manageable) r);
            }
        }
        return extendable;
    }
}