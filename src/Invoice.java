import java.time.LocalDate;

public class Invoice implements Payable {
    private double totalAmount;
    private PaymentMethod paymentMethod;
    private LocalDate paymentDate;

    public Invoice(double total, PaymentMethod paymentMethod, LocalDate paymentDate) {
        this.totalAmount = total;
        this.paymentMethod = paymentMethod;
        this.paymentDate = paymentDate;
    }

    public double getTotalAmount() {
        return totalAmount;
    }
    public void setTotalAmount(double total) {
        this.totalAmount = total;
    }
    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }
    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    public LocalDate getPaymentDate() {
        return paymentDate;
    }
    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }
}
