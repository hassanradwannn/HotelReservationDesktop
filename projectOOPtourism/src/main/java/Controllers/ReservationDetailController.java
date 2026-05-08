package Controllers;


import Utils.AppContext;
import Models.*;
import Repositories.*;
import Services.*;
import Utils.*;
import Utils.exceptions.*;
import Repositories.database.DatabaseConnection;
import Repositories.DatabaseInitializer.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

public class ReservationDetailController implements DashboardContentController {
    @FXML private Label titleLabel;
    @FXML private Label reservationIdLabel;
    @FXML private Label guestLabel;
    @FXML private Label roomLabel;
    @FXML private Label datesLabel;
    @FXML private Label statusLabel;
    @FXML private Label totalLabel;
    @FXML private Label paidLabel;
    @FXML private VBox refundBox;
    @FXML private Label refundLabel;
    @FXML private VBox depositDueBox;
    @FXML private Label depositDueLabel;
    @FXML private Label outstandingLabel;
    @FXML private Label messageLabel;
    @FXML private VBox receptionistActions;
    @FXML private VBox guestActions;
    @FXML private VBox extendPane;
    @FXML private VBox extensionPreviewPane;
    @FXML private DatePicker newCheckOutPicker;
    @FXML private Label extensionNightsLabel;
    @FXML private Label extensionFeeLabel;
    @FXML private Label extensionNewTotalLabel;
    @FXML private Button extendButton;
    @FXML private Button payDepositButton;
    @FXML private Button requestCheckOutButton;
    @FXML private Button cancelButton;
    @FXML private Button invoiceButton;
    @FXML private Button checkInButton;
    @FXML private Button checkOutButton;
    @FXML private ComboBox<PaymentMethod> depositPaymentCombo;
    @FXML private ComboBox<PaymentMethod> paymentCombo;
    @FXML private CheckBox extendStayCheckBox;

    private AppContext mainApp;
    private Reservation reservation;
    private String sourceTitle;
    private boolean openExtendStay;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void initData(AppContext mainApp, Object data) {
        this.mainApp = mainApp;
        if (data instanceof Object[] args) {
            this.reservation = (Reservation) args[0];
            this.sourceTitle = args.length > 1 ? (String) args[1] : "";
            this.openExtendStay = args.length > 2 && Boolean.TRUE.equals(args[2]);
        } else {
            this.reservation = (Reservation) data;
            this.sourceTitle = "";
            this.openExtendStay = false;
        }

        setupDatePicker();
        depositPaymentCombo.setItems(FXCollections.observableArrayList(PaymentMethod.values()));
        paymentCombo.setItems(FXCollections.observableArrayList(PaymentMethod.values()));
        newCheckOutPicker.valueProperty().addListener((obs, oldVal, newVal) -> updateExtensionSummary());
        render();
        mainApp.setCurrentViewRefresher(this::refreshCurrentReservation);
    }

    private void setupDatePicker() {
        newCheckOutPicker.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                return date == null ? "" : formatter.format(date);
            }

            @Override
            public LocalDate fromString(String value) {
                try {
                    return value == null || value.isBlank() ? null : LocalDate.parse(value, formatter);
                } catch (Exception ex) {
                    return null;
                }
            }
        });
    }

    private void render() {
        titleLabel.setText("Reservation " + reservation.getReservationId());
        reservationIdLabel.setText(reservation.getReservationId());
        guestLabel.setText(reservation.getGuest().getUsername());
        roomLabel.setText(reservation.getRoom().getRoomNumber() + " - " + reservation.getRoom().getRoomType().getName());
        datesLabel.setText(formatter.format(reservation.getCheckInDate()) + " : " + formatter.format(reservation.getCheckOutDate()));
        statusLabel.setText(displayStatus(reservation.getStatus()));
        totalLabel.setText("$" + mainApp.money(reservation.getTotalPrice()));
        double paid = getPaidAmount();
        double grossPaid = ReservationService.getGrossPaidAmountBeforeRefunds(reservation);
        double outstanding = reservation.getStatus() == ReservationStatus.COMPLETED
                ? 0
                : Math.max(0, reservation.getTotalPrice() - paid);
        double refund = ReservationService.getEarlyCheckOutRefundAmount(reservation);
        double displayedPaid = refund > 0.009 ? grossPaid : paid;
        paidLabel.setText("$" + mainApp.money(reservation.getStatus() == ReservationStatus.COMPLETED
                ? Math.max(displayedPaid, reservation.getTotalPrice())
                : displayedPaid));
        refundBox.setVisible(refund > 0.009);
        refundBox.setManaged(refundBox.isVisible());
        refundLabel.setText("$" + mainApp.money(refund));
        boolean pending = reservation.getStatus() == ReservationStatus.PENDING;
        depositDueBox.setVisible(pending);
        depositDueBox.setManaged(pending);
        depositDueLabel.setText(pending ? "$" + mainApp.money(ReservationService.getDepositAmount(reservation)) : "-");
        outstandingLabel.setText("$" + mainApp.money(outstanding));

        boolean receptionist = mainApp.getCurrentUser() instanceof Receptionist;
        receptionistActions.setVisible(receptionist);
        receptionistActions.setManaged(receptionist);
        invoiceButton.setVisible(receptionist);
        invoiceButton.setManaged(receptionist);

        boolean guestUser = mainApp.getCurrentUser() instanceof Guest;
        boolean completed = reservation.getStatus() == ReservationStatus.COMPLETED;
        boolean cancelled = reservation.getStatus() == ReservationStatus.CANCELLED;
        boolean checkingOut = reservation.getStatus() == ReservationStatus.CHECKING_OUT;
        boolean receptionistCannotCancel = receptionist && reservation.getStatus() == ReservationStatus.ONGOING;
        cancelButton.setDisable(completed || cancelled || checkingOut || receptionistCannotCancel);

        guestActions.setVisible(guestUser && (reservation.getStatus() == ReservationStatus.PENDING
                || reservation.getStatus() == ReservationStatus.ONGOING));
        guestActions.setManaged(guestActions.isVisible());
        boolean canPayDeposit = guestUser && reservation.getStatus() == ReservationStatus.PENDING;
        depositPaymentCombo.setVisible(canPayDeposit);
        depositPaymentCombo.setManaged(canPayDeposit);
        payDepositButton.setVisible(canPayDeposit);
        payDepositButton.setManaged(payDepositButton.isVisible());
        requestCheckOutButton.setVisible(guestUser && reservation.getStatus() == ReservationStatus.ONGOING);
        requestCheckOutButton.setManaged(requestCheckOutButton.isVisible());

        boolean ongoing = reservation.getStatus() == ReservationStatus.ONGOING;
        boolean showExtension = ongoing && (receptionist || guestUser);
        extendPane.setVisible(showExtension);
        extendPane.setManaged(showExtension);
        boolean extensionSelected = showExtension && openExtendStay;
        extendStayCheckBox.setSelected(extensionSelected);
        extensionPreviewPane.setVisible(extensionSelected);
        extensionPreviewPane.setManaged(extensionSelected);
        newCheckOutPicker.setDisable(!extensionSelected);
        extendButton.setDisable(!extensionSelected);
        newCheckOutPicker.setValue(reservation.getCheckOutDate().plusDays(1));
        newCheckOutPicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || !date.isAfter(reservation.getCheckOutDate()));
            }
        });
        updateExtensionSummary();

        checkInButton.setDisable(completed || cancelled
                || (reservation.getStatus() != ReservationStatus.CONFIRMED
                && reservation.getStatus() != ReservationStatus.CHECKING_IN)
                || !SystemTime.getToday().isEqual(reservation.getCheckInDate()));
        boolean canCheckOut = !completed && !cancelled
                && (checkingOut
                || (reservation.getStatus() == ReservationStatus.ONGOING
                && !SystemTime.getToday().isBefore(reservation.getCheckOutDate())));
        checkOutButton.setDisable(!canCheckOut);
        paymentCombo.setDisable(checkOutButton.isDisabled());
    }

    @FXML
    private void toggleExtendStay() {
        boolean selected = extendStayCheckBox.isSelected();
        newCheckOutPicker.setDisable(!selected);
        extendButton.setDisable(!selected);
        extensionPreviewPane.setVisible(selected);
        extensionPreviewPane.setManaged(selected);
    }

    private void refreshCurrentReservation() {
        Database.refreshReservationsFromDatabase();
        reservation = Database.getReservations().stream()
                .filter(r -> r.getReservationId().equals(reservation.getReservationId()))
                .findFirst()
                .orElse(reservation);
        render();
    }

    @FXML
    private void handleBack() {
        if (mainApp.getCurrentUser() instanceof Guest) {
            mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/GuestReservations.fxml", mainApp.getCurrentUser());
            return;
        }
        if ("Today's Reservations".equals(sourceTitle)) {
            mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/GenericList.fxml",
                    new Object[]{"Today's Reservations", (java.util.function.Supplier<java.util.List<?>>) Database::getTodaysReservations});
            return;
        }
        if ("Reservations To Check In".equals(sourceTitle)) {
            mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/ReceptionistReservations.fxml",
                    ReceptionistReservationsController.ViewMode.CHECKING_IN);
            return;
        }
        if ("Checking Out Reservations".equals(sourceTitle)) {
            mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/GenericList.fxml",
                    new Object[]{"Checking Out Reservations", (java.util.function.Supplier<java.util.List<?>>) Database::getCheckingOutReservations});
            return;
        }
        if ("Requested Check Outs".equals(sourceTitle)) {
            mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/ReceptionistReservations.fxml",
                    ReceptionistReservationsController.ViewMode.CHECKING_OUT);
            return;
        }
        if ("Guests Currently Residing".equals(sourceTitle)) {
            mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/ReceptionistReservations.fxml",
                    ReceptionistReservationsController.ViewMode.RESIDING);
            return;
        }
        if ("All Reservations".equals(sourceTitle)) {
            mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/ReceptionistReservations.fxml",
                    ReceptionistReservationsController.ViewMode.ALL);
            return;
        }
        String listTitle = sourceTitle == null || sourceTitle.isBlank() ? "Reservations" : sourceTitle;
        mainApp.switchDashboardContent(mainApp.getCurrentContentArea(), "/GenericList.fxml",
                new Object[]{listTitle, (java.util.function.Supplier<java.util.List<?>>) () -> {
                    Database.refreshReservationsFromDatabase();
                    return Database.getReservations();
                }});
    }

    @FXML
    private void handleCancel() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Cancel Reservation");
        confirm.setHeaderText(null);
        confirm.setContentText("Cancel reservation " + reservation.getReservationId() + "?");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                ReservationService.cancelReservation(reservation.getReservationId());
                setMessage("Reservation cancelled.");
                refreshCurrentReservation();
            } catch (Exception ex) {
                mainApp.alert("Error", ex.getMessage());
            }
        }
    }

    @FXML
    private void handleCheckIn() {
        try {
            ReservationService.checkInGuest(reservation, reservation.getGuest());
            mainApp.alert("Success", "Guest checked in.");
            refreshCurrentReservation();
        } catch (Exception ex) {
            mainApp.alert("Error", ex.getMessage());
        }
    }

    @FXML
    private void handleCheckOut() {
        try {
            PaymentMethod paymentMethod = paymentCombo.getValue();
            if (paymentMethod == null) {
                mainApp.alert("Error", "Please select a payment method.");
                return;
            }
            if (paymentMethod == PaymentMethod.CREDIT_CARD) {
                boolean paymentConfirmed = CreditCardModal.showAndWait((Stage) titleLabel.getScene().getWindow());
                if (!paymentConfirmed) {
                    return;
                }
            }
            ReservationService.checkOutGuest(reservation, paymentMethod);
            mainApp.alert("Success", "Guest checked out.");
            refreshCurrentReservation();
        } catch (Exception ex) {
            mainApp.alert("Error", ex.getMessage());
        }
    }

    @FXML
    private void handlePayDeposit() {
        try {
            if (!(mainApp.getCurrentUser() instanceof Guest currentGuest)) {
                mainApp.alert("Error", "Only guests can pay deposits from this screen.");
                return;
            }
            PaymentMethod paymentMethod = depositPaymentCombo.getValue();
            if (paymentMethod == null) {
                mainApp.alert("Error", "Please select a payment method.");
                return;
            }
            if (paymentMethod == PaymentMethod.CREDIT_CARD) {
                boolean paymentConfirmed = CreditCardModal.showAndWait((Stage) titleLabel.getScene().getWindow());
                if (!paymentConfirmed) {
                    return;
                }
            }
            ReservationService.payDeposit(reservation, currentGuest, paymentMethod);
            setMessage("Deposit paid. Reservation confirmed.");
            refreshCurrentReservation();
        } catch (Exception ex) {
            mainApp.alert("Payment Failed", ex.getMessage());
        }
    }

    @FXML
    private void handleRequestCheckOut() {
        try {
            if (!(mainApp.getCurrentUser() instanceof Guest currentGuest)) {
                mainApp.alert("Error", "Only guests can request check-out from this screen.");
                return;
            }
            ReservationService.requestCheckOut(reservation, currentGuest);
            setMessage("Check-out requested. The front desk has been notified.");
            refreshCurrentReservation();
        } catch (Exception ex) {
            mainApp.alert("Request Failed", ex.getMessage());
        }
    }

    @FXML
    private void handleExtendStay() {
        try {
            ReservationService.extendStay(reservation, newCheckOutPicker.getValue());
            setMessage("Stay extended. New outstanding balance is $" + mainApp.money(reservation.getTotalPrice() - getPaidAmount()) + ".");
            refreshCurrentReservation();
        } catch (Exception ex) {
            mainApp.alert("Error", ex.getMessage());
        }
    }

    @FXML
    private void handleViewInvoice() {
        double paid = getPaidAmount();
        double grossPaid = ReservationService.getGrossPaidAmountBeforeRefunds(reservation);
        double refund = ReservationService.getEarlyCheckOutRefundAmount(reservation);
        double displayedPaid = refund > 0.009 ? grossPaid : paid;
        double outstanding = reservation.getStatus() == ReservationStatus.COMPLETED
                ? 0
                : Math.max(0, reservation.getTotalPrice() - paid);
        mainApp.alert("Invoice",
                "Reservation: " + reservation.getReservationId()
                        + "\nGuest: " + reservation.getGuest().getUsername()
                        + "\nRoom: " + reservation.getRoom().getRoomNumber()
                        + "\nTotal charges: $" + mainApp.money(reservation.getTotalPrice())
                        + "\nPaid so far: $" + mainApp.money(reservation.getStatus() == ReservationStatus.COMPLETED
                                ? Math.max(displayedPaid, reservation.getTotalPrice())
                                : displayedPaid)
                        + (refund > 0.009 ? "\nRefunded for early check out: $" + mainApp.money(refund) : "")
                        + "\nOutstanding: $" + mainApp.money(outstanding));
    }

    private void updateExtensionSummary() {
        LocalDate newCheckOut = newCheckOutPicker.getValue();
        if (newCheckOut == null || !newCheckOut.isAfter(reservation.getCheckOutDate())) {
            extensionNightsLabel.setText("-");
            extensionFeeLabel.setText("-");
            extensionNewTotalLabel.setText("-");
            return;
        }
        long addedNights = ChronoUnit.DAYS.between(reservation.getCheckOutDate(), newCheckOut);
        double oldTotal = reservation.getTotalPrice();
        double newTotal = Reservation.calculateTotalPrice(
                reservation.getRoom(), reservation.getCheckInDate(), newCheckOut, reservation.hasGymPass());
        extensionNightsLabel.setText(String.valueOf(addedNights));
        extensionFeeLabel.setText("$" + mainApp.money(newTotal - oldTotal));
        extensionNewTotalLabel.setText("$" + mainApp.money(newTotal));
    }

    private double getPaidAmount() {
        return ReservationService.getPaidAmount(reservation);
    }

    private String displayStatus(ReservationStatus status) {
        return status.toString().replace('_', ' ');
    }

    private void setMessage(String message) {
        messageLabel.setText(message);
        messageLabel.getStyleClass().removeAll("error-message", "success-message");
        messageLabel.getStyleClass().add("success-message");
    }
}
