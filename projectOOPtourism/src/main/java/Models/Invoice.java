package Models;


import Controllers.*;
import Repositories.*;
import Services.*;
import Utils.*;
import Utils.exceptions.*;
import Repositories.database.DatabaseConnection;
import Repositories.DatabaseInitializer.*;
import java.time.LocalDate;
import Utils.exceptions.*;

public class Invoice implements Payable {
    private double totalAmount;
    private PaymentMethod paymentMethod;
    private LocalDate paymentDate;


    public Invoice(double totalAmount, PaymentMethod paymentMethod) throws InvalidPaymentException {
        setTotalAmount(totalAmount);
        this.paymentMethod = paymentMethod;
        this.paymentDate = LocalDate.now();
        Database.getInvoices().add(this);
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
    public boolean processPayment() throws InvalidPaymentException {
        if (this.totalAmount <= 0) {
            throw new InvalidPaymentException("Invoice amount must be positive.");
        }
        if (this.paymentMethod == null) {
            throw new InvalidPaymentException("No payment method provided.");
        }
        System.out.println("Payment of $" + this.totalAmount + " processed via " + this.paymentMethod);
        return true;
    }
}
