import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Receipt {
    private String reservationId;
    private String guestName;
    private String roomNumber;
    private String roomType;
    private LocalDateTime checkInDate;
    private LocalDateTime checkOutDate;
    private double amountPaid;
    private double totalAmount;
    private double outstandingAmount;
    private String amenities;
    private String receiptType;
    private LocalDateTime issueDate;
    private boolean wasExtended;  // Flag to indicate if stay was extended
    
    public Receipt(String reservationId, String guestName, String roomNumber, String roomType,
                   LocalDateTime checkInDate, LocalDateTime checkOutDate,
                   double amountPaid, double totalAmount, String amenities, String receiptType) {
        this(reservationId, guestName, roomNumber, roomType, checkInDate, checkOutDate, 
             amountPaid, totalAmount, amenities, receiptType, false);
    }
    
    public Receipt(String reservationId, String guestName, String roomNumber, String roomType,
                   LocalDateTime checkInDate, LocalDateTime checkOutDate,
                   double amountPaid, double totalAmount, String amenities, String receiptType,
                   boolean wasExtended) {
        this.reservationId = reservationId;
        this.guestName = guestName;
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.amountPaid = amountPaid;
        this.totalAmount = totalAmount;
        this.outstandingAmount = totalAmount - amountPaid;
        this.amenities = amenities;
        this.receiptType = receiptType;
        this.issueDate = LocalDateTime.now();
        this.wasExtended = wasExtended;
    }
    
    public void print() {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║                    HOTEL RECEIPT                         ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println("  " + receiptType);
        System.out.println("──────────────────────────────────────────────────────────");
        System.out.println("  Reservation ID: " + reservationId);
        System.out.println("  Issue Date:     " + issueDate.format(dateTimeFormatter));
        System.out.println("──────────────────────────────────────────────────────────");
        System.out.println("  Guest Name:     " + guestName);
        System.out.println("  Room Number:    " + roomNumber);
        System.out.println("  Room Type:      " + roomType);
        System.out.println("──────────────────────────────────────────────────────────");
        System.out.println("  Check-in Date:  " + checkInDate.format(dateFormatter));
        System.out.println("  Check-out Date: " + checkOutDate.format(dateFormatter));
        System.out.println("──────────────────────────────────────────────────────────");
        System.out.println("  INCLUDED AMENITIES & SERVICES:");
        if (amenities != null && !amenities.isEmpty()) {
            String[] items = amenities.split(", ");
            for (String item : items) {
                System.out.println("    • " + item);
            }
        } else {
            System.out.println("    • Standard Room Amenities");
        }
        System.out.println("──────────────────────────────────────────────────────────");
        System.out.println("  SUBTOTAL:       $" + String.format("%.2f", totalAmount));
        System.out.println("  Amount Paid:    $" + String.format("%.2f", amountPaid));
        if (outstandingAmount > 0) {
            System.out.println("  Outstanding:    $" + String.format("%.2f", outstandingAmount));
        } else {
            System.out.println("  Outstanding:    $0.00 (FULLY PAID)");
        }
        
        // Show stay extension info only if stay was actually extended
        if (wasExtended) {
            System.out.println("  Note:          Stay was EXTENDED");
        }
        System.out.println("══════════════════════════════════════════════════════════");
        System.out.println("           Thank you for staying with us!                 ");
        System.out.println();
    }
    
    // Static factory methods for creating receipts from Reservation
    public static Receipt createDepositReceipt(Reservation res) {
        String amenities = buildAmenitiesString(res);
        // Check if stay was extended by checking original check-out date
        boolean wasExtended = res.getOriginalCheckOutDate() != null 
            && !res.getOriginalCheckOutDate().equals(res.getCheckOutDate());
        return new Receipt(
            res.getReservationId(),
            res.getGuest().getUsername(),
            res.getRoom().getRoomNumber(),
            res.getRoom().getRoomType().getName(),
            res.getCheckInDate().atStartOfDay(),
            res.getCheckOutDate().atStartOfDay(),
            res.getFirstNightPrice(),
            res.getTotalPrice(),
            amenities,
            "DEPOSIT PAYMENT RECEIPT",
            wasExtended
        );
    }
    
    public static Receipt createFullPaymentReceipt(Reservation res) {
        String amenities = buildAmenitiesString(res);
        // Check if stay was extended by comparing original and current check-out dates
        boolean wasExtended = res.getOriginalCheckOutDate() != null 
            && !res.getOriginalCheckOutDate().equals(res.getCheckOutDate());
        return new Receipt(
            res.getReservationId(),
            res.getGuest().getUsername(),
            res.getRoom().getRoomNumber(),
            res.getRoom().getRoomType().getName(),
            res.getCheckInDate().atStartOfDay(),
            res.getCheckOutDate().atStartOfDay(),
            res.getTotalPrice(),
            res.getTotalPrice(),
            amenities,
            "FULL PAYMENT RECEIPT",
            wasExtended
        );
    }
    
    public static Receipt createCheckoutReceipt(Reservation res, double amountPaidAtCheckout) {
        String amenities = buildAmenitiesString(res);
        // Check if stay was extended by comparing original and current check-out dates
        boolean wasExtended = res.getOriginalCheckOutDate() != null 
            && !res.getOriginalCheckOutDate().equals(res.getCheckOutDate());
        // Use actual paid amount from reservation (includes deposit + any payments)
        double totalPaid = res.getPaidAmount();
        return new Receipt(
            res.getReservationId(),
            res.getGuest().getUsername(),
            res.getRoom().getRoomNumber(),
            res.getRoom().getRoomType().getName(),
            res.getCheckInDate().atStartOfDay(),
            res.getCheckOutDate().atStartOfDay(),
            totalPaid,
            res.getTotalPrice(),
            amenities,
            "CHECKOUT RECEIPT",
            wasExtended
        );
    }
    
    private static String buildAmenitiesString(Reservation res) {
        StringBuilder sb = new StringBuilder();
        
        // Room type with price
        sb.append(res.getRoom().getRoomType().getName()).append(" Room ($").append(res.getRoom().getRoomType().getPricePerNight()).append("/night)");
        
        // Add room amenities (these are always included in the room)
        for (Amenity a : res.getRoom().getAmenities()) {
            sb.append(", ").append(a.getName());
        }
        
        // Add gym pass - only show as paid addon for non-penthouse rooms
        // Penthouse rooms include gym for free (already in room amenities)
        boolean roomHasGym = res.getRoom().getAmenities().stream()
            .anyMatch(a -> a.getName().equalsIgnoreCase("Gym"));
        
        if (res.hasGymPass() && !roomHasGym) {
            // Gym is a paid addon for this reservation (non-penthouse room)
            for (Amenity a : Database.getAmenities()) {
                if (a.getName().equalsIgnoreCase("Gym")) {
                    sb.append(", Gym Pass ($").append(a.getPrice()).append(")");
                    break;
                }
            }
        }
        // Note: If roomHasGym is true (like penthouse), Gym is already in the amenities list
        // No need to add "Gym (Included)" separately to avoid duplicates
        
        // Add restaurant if selected
        if (res.hasRestaurant()) {
            for (Amenity a : Database.getAmenities()) {
                if (a.getName().equalsIgnoreCase("Restaurant")) {
                    sb.append(", Restaurant Service ($").append(a.getPrice()).append(")");
                    break;
                }
            }
        }
        
        return sb.toString();
    }
}