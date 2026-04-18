import java.time.LocalDate;

public interface Payable {
    /**
     * Process the payment for this payable entity
     * @return true if payment was successful, false otherwise
     */
    boolean processPayment();
    
    /**
     * Get the total amount to be paid
     * @return the total payment amount
     */
    double getTotalAmount();
    
    /**
     * Check if the payment has been completed
     * @return true if fully paid, false otherwise
     */
    boolean isPaid();
    
    /**
     * Get the payment due date (if applicable)
     * @return the due date for payment, or null if not applicable
     */
    default LocalDate getDueDate() {
        return null;
    }
    
    /**
     * Check if payment is overdue
     * @return true if payment is past due date and not paid
     */
    default boolean isOverdue() {
        LocalDate dueDate = getDueDate();
        if (dueDate == null) return false;
        return LocalDate.now().isAfter(dueDate) && !isPaid();
    }
}
