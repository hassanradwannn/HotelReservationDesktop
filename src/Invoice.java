import java.time.LocalDate;

public class Invoice implements Payable {
    private double totalAmount;
    private PaymentMethod paymentMethod;
    private LocalDate paymentDate;


    public Invoice(double totalAmount, PaymentMethod paymentMethod) throws InvalidPaymentException {
        setTotalAmount(totalAmount);
        this.paymentMethod = paymentMethod;
        this.paymentDate = LocalDate.now();
        Database.invoices.add(this);
    }

    public double getTotalAmount() { return totalAmount; }

    public void setTotalAmount(double totalAmount) throws InvalidPaymentException {
        if (totalAmount < 0) {
            throw new InvalidPaymentException("Invoice amount cannot be negative.");
        }
        this.totalAmount = totalAmount;
    }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public LocalDate getPaymentDate() { return paymentDate; }

    @Override
    public boolean processPayment() {
        if (this.totalAmount > 0 && this.paymentMethod != null) {
            System.out.println("Payment of $" + this.totalAmount + " processed via " + this.paymentMethod);
            return true;
        }
        return false;
    }
}