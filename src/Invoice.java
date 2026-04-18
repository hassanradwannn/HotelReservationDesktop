import java.time.LocalDate;
import exceptions.*;

public class Invoice implements Payable {
    private double totalAmount;
    private PaymentMethod paymentMethod;
    private LocalDate paymentDate;
    private boolean paid;


    public Invoice(double totalAmount, PaymentMethod paymentMethod) throws InvalidPaymentException {
        setTotalAmount(totalAmount);
        this.paymentMethod = paymentMethod;
        this.paymentDate = LocalDate.now();
        this.paid = false;
        Database.getInvoices().add(this);
    }

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

    @Override
    public boolean processPayment() {
        if (this.totalAmount > 0 && this.paymentMethod != null) {
            System.out.println("Payment of $" + this.totalAmount + " processed via " + this.paymentMethod);
            this.paid = true;
            return true;
        }
        return false;
    }
}