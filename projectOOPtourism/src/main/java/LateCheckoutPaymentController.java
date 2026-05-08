import exceptions.InvalidPaymentException;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;

public class LateCheckoutPaymentController implements DashboardContentController {

    @FXML private Label reservationIdLabel;
    @FXML private Label feeAmountLabel;
    @FXML private Label balanceLabel;
    @FXML private ComboBox<PaymentMethod> paymentCombo;
    @FXML private Label errorLabel;

    private Main mainApp;
    private Guest guest;
    private Reservation reservation;

    @Override
    public void initData(Main mainApp, Object data) {
        // This path is used by the payment wall in Main.loadLateCheckoutPaymentWall()
        // which calls initData(main, guest, reservation) directly — see overload below
    }

    public void initData(Main mainApp, Guest guest, Reservation reservation) {
        this.mainApp = mainApp;
        this.guest = guest;
        this.reservation = reservation;

        reservationIdLabel.setText("Reservation: " + reservation.getReservationId());
        feeAmountLabel.setText("Late Checkout Fee: $" + mainApp.money(reservation.getLateFee()));
        balanceLabel.setText("Your Balance: $" + mainApp.money(guest.getBalance()));

        paymentCombo.setItems(FXCollections.observableArrayList(PaymentMethod.values()));
    }

    @FXML
    private void handlePay() {
        PaymentMethod method = paymentCombo.getValue();
        if (method == null) {
            mainApp.alert("Error", "Please select a payment method.");
            return;
        }

        double fee = reservation.getLateFee();

        // Refresh balance from DB before deducting
        User fresh = UserDatabase.findUser(guest.getUsername());
        if (fresh instanceof Guest dbGuest) {
            guest.setBalance(dbGuest.getBalance());
        }

        if (guest.getBalance() < fee) {
            mainApp.alert("Insufficient Balance",
                    String.format("You need $%s but your balance is $%s.",
                            mainApp.money(fee), mainApp.money(guest.getBalance())));
            return;
        }

        guest.setBalance(guest.getBalance() - fee);
        DatabaseSaver.updateUserBalance(guest.getUsername(), guest.getBalance());



        try {
            new Invoice(fee, method);
        } catch (InvalidPaymentException e) {
            mainApp.alert("Payment Error", e.getMessage());
            return;
        }

        mainApp.alert("Success", "Late checkout fee paid. Welcome back!");
        mainApp.switchDashboardContent(
                mainApp.getCurrentContentArea(), "/GuestHome.fxml", guest);
    }
}