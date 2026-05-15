package Models;


import Controllers.*;
import Repositories.*;
import Services.*;
import Utils.*;
import Utils.exceptions.*;
import Repositories.database.DatabaseConnection;
import Repositories.DatabaseInitializer.*;
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
