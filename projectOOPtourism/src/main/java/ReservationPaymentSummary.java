public class ReservationPaymentSummary {
    private final double grossPaidAmountBeforeRefunds;
    private final double earlyCheckOutRefundAmount;

    public ReservationPaymentSummary(double grossPaidAmountBeforeRefunds, double earlyCheckOutRefundAmount) {
        this.grossPaidAmountBeforeRefunds = grossPaidAmountBeforeRefunds;
        this.earlyCheckOutRefundAmount = earlyCheckOutRefundAmount;
    }

    public double getGrossPaidAmountBeforeRefunds() {
        return grossPaidAmountBeforeRefunds;
    }

    public double getEarlyCheckOutRefundAmount() {
        return earlyCheckOutRefundAmount;
    }
}
