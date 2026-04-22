import exceptions.InvalidPaymentException;

public interface Payable {
    boolean processPayment() throws InvalidPaymentException;
}
