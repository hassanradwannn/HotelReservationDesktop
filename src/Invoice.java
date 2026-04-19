import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import exceptions.*;

public class Invoice implements Payable {
    private Reservation reservation;
    private double totalAmount;
    private PaymentMethod paymentMethod;
    private LocalDate paymentDate;
    private boolean paid;
    private String invoiceType;

    public Invoice(Reservation reservation, double totalAmount, PaymentMethod paymentMethod, String invoiceType) throws InvalidPaymentException {
        if (totalAmount < 0) {
            throw new InvalidPaymentException("Invoice amount cannot be negative.");
        }
        this.reservation = reservation;
        this.totalAmount = totalAmount;
        this.paymentMethod = paymentMethod;
        this.invoiceType = invoiceType;
        this.paymentDate = SystemTime.getToday();
        this.paid = false;
        Database.getInvoices().add(this);
    }

    public Reservation getReservation() { return reservation; }
    public void setReservation(Reservation reservation) { this.reservation = reservation; }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) throws InvalidPaymentException {
        if (totalAmount < 0) {
            throw new InvalidPaymentException("Invoice amount cannot be negative.");
        }
        this.totalAmount = totalAmount;
    }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public LocalDate getPaymentDate() { return paymentDate; }

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    public String getInvoiceType() { return invoiceType; }
    public void setInvoiceType(String invoiceType) { this.invoiceType = invoiceType; }

    @Override
    public boolean processPayment() {
        if (this.totalAmount > 0 && this.paymentMethod != null) {
            System.out.println("Payment of $" + this.totalAmount + " processed via " + this.paymentMethod);
            this.paid = true;
            return true;
        } else if (this.totalAmount == 0) {
            this.paid = true;
            return true;
        }
        return false;
    }

    public void printReceipt() {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║                    HOTEL RECEIPT                         ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println("  " + (invoiceType != null ? invoiceType : "PAYMENT RECEIPT"));
        System.out.println("──────────────────────────────────────────────────────────");
        System.out.println("  Reservation ID: " + reservation.getReservationId());
        System.out.println("  Issue Date:     " + paymentDate.format(dateFormatter));
        System.out.println("──────────────────────────────────────────────────────────");
        System.out.println("  Guest Name:     " + reservation.getGuest().getUsername());
        System.out.println("  Room Number:    " + reservation.getRoom().getRoomNumber());
        System.out.println("  Room Type:      " + reservation.getRoom().getRoomType().getName());
        System.out.println("──────────────────────────────────────────────────────────");
        System.out.println("  Check-in Date:  " + reservation.getCheckInDate().format(dateFormatter));
        System.out.println("  Check-out Date: " + reservation.getCheckOutDate().format(dateFormatter));
        System.out.println("──────────────────────────────────────────────────────────");
        System.out.println("  INCLUDED AMENITIES & SERVICES:");

        String amenities = buildAmenitiesString(reservation);
        if (amenities != null && !amenities.isEmpty()) {
            String[] items = amenities.split(", ");
            for (String item : items) {
                System.out.println("    • " + item);
            }
        } else {
            System.out.println("    • Standard Room Amenities");
        }
        System.out.println("──────────────────────────────────────────────────────────");
        System.out.println("  SUBTOTAL:       $" + String.format("%.2f", reservation.getTotalPrice()));
        System.out.println("  Amount Paid:    $" + String.format("%.2f", reservation.getPaidAmount()));

        double outstandingAmount = reservation.getActualOutstanding();
        if (outstandingAmount > 0) {
            System.out.println("  Outstanding:    $" + String.format("%.2f", outstandingAmount));
        } else {
            System.out.println("  Outstanding:    $0.00 (FULLY PAID)");
        }

        boolean wasExtended = reservation.getOriginalCheckOutDate() != null
                && !reservation.getOriginalCheckOutDate().equals(reservation.getCheckOutDate());
        if (wasExtended) {
            System.out.println("  Note:          Stay was EXTENDED");
        }
        System.out.println("══════════════════════════════════════════════════════════");
        System.out.println("           Thank you for staying with us!                 ");
        System.out.println();
    }

    private String buildAmenitiesString(Reservation res) {
        StringBuilder sb = new StringBuilder();

        sb.append(res.getRoom().getRoomType().getName()).append(" Room ($").append(res.getRoom().getRoomType().getPricePerNight()).append("/night)");

        for (Amenity a : res.getRoom().getAmenities()) {
            sb.append(", ").append(a.getName()).append(" ($").append(a.getPrice()).append(")");
        }

        boolean roomHasGym = res.getRoom().getAmenities().stream()
                .anyMatch(a -> a.getName().equalsIgnoreCase("Gym"));

        if (res.hasGymPass() && !roomHasGym) {
            for (Amenity a : Database.getAmenities()) {
                if (a.getName().equalsIgnoreCase("Gym")) {
                    sb.append(", Gym Pass ($").append(a.getPrice()).append(")");
                    break;
                }
            }
        }

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
