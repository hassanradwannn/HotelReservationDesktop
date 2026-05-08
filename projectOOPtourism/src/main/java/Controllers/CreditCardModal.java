package Controllers;



import Utils.AppContext;
import Models.*;
import Repositories.*;
import Services.*;
import Utils.*;
import Utils.exceptions.*;
import Repositories.database.DatabaseConnection;
import Repositories.DatabaseInitializer.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

public class CreditCardModal {

    @FXML
    private TextField cardNumberField;
    @FXML
    private Label cardNumberErrorLabel;
    @FXML
    private ImageView cardTypeImageView;
    @FXML
    private TextField cardholderNameField;
    @FXML
    private Label cardholderNameErrorLabel;
    @FXML
    private TextField expiryDateField;
    @FXML
    private Label expiryDateErrorLabel;
    @FXML
    private TextField cvvField;
    @FXML
    private Label cvvErrorLabel;
    @FXML
    private Button confirmButton;
    @FXML
    private Button cancelButton;

    private Stage stage;
    private boolean confirmed = false;
    private boolean formattingCardNumber = false;

    private static final Pattern VISA_PATTERN = Pattern.compile("^4[0-9]{12}(?:[0-9]{3})?$");
    private static final Pattern MASTERCARD_PATTERN = Pattern.compile("^(?:5[1-5][0-9]{2}|222[1-9]|22[3-9][0-9]|2[3-6][0-9]{2}|27[01][0-9]|2720)[0-9]{12}$");
    private static final Pattern AMEX_PATTERN = Pattern.compile("^3[47][0-9]{13}$");
    private static final Pattern DISCOVER_PATTERN = Pattern.compile("^6(?:011|5[0-9]{2})[0-9]{12}$");

    // Returns true only when the payment form passes validation and the user confirms it.
    public static boolean showAndWait(Stage owner) {
        try {
            FXMLLoader loader = new FXMLLoader(CreditCardModal.class.getResource("/CreditCardModal.fxml"));
            AnchorPane root = loader.load();
            CreditCardModal controller = loader.getController();

            Stage stage = new Stage();
            stage.initOwner(owner);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setScene(new Scene(root));

            controller.setStage(stage);
            controller.setupListeners();

            stage.showAndWait();
            return controller.confirmed;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    // FXMLLoader needs a public no-argument constructor for fx:controller.
    public CreditCardModal() {
    }

    private void setStage(Stage stage) {
        this.stage = stage;
    }

    private void setupListeners() {
        cardNumberField.textProperty().addListener((obs, oldVal, newVal) -> {
            formatCardNumber();
            validateCardNumber();
            updateCardTypeIndicator();
        });
        cardholderNameField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) validateCardholderName();
        });
        expiryDateField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) validateExpiryDate();
        });
        cvvField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) validateCvv();
        });
    }

    private void formatCardNumber() {
        if (formattingCardNumber) {
            return;
        }

        String original = cardNumberField.getText();
        int digitsBeforeCaret = countDigits(original, cardNumberField.getCaretPosition());
        String unformatted = original.replaceAll("[^\\d]", "");
        if (unformatted.length() > 16) {
            unformatted = unformatted.substring(0, 16);
            digitsBeforeCaret = Math.min(digitsBeforeCaret, 16);
        }

        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < unformatted.length(); i += 4) {
            if (i > 0) {
                formatted.append(" ");
            }
            formatted.append(unformatted.substring(i, Math.min(i + 4, unformatted.length())));
        }

        String formattedText = formatted.toString();
        if (!original.equals(formattedText)) {
            formattingCardNumber = true;
            cardNumberField.setText(formattedText);
            cardNumberField.positionCaret(caretPositionForDigitCount(formattedText, digitsBeforeCaret));
            formattingCardNumber = false;
        }
    }

    private int countDigits(String text, int endExclusive) {
        int count = 0;
        int end = Math.min(endExclusive, text == null ? 0 : text.length());
        for (int i = 0; i < end; i++) {
            if (Character.isDigit(text.charAt(i))) {
                count++;
            }
        }
        return count;
    }

    private int caretPositionForDigitCount(String text, int digitCount) {
        if (digitCount <= 0) {
            return 0;
        }

        int seen = 0;
        for (int i = 0; i < text.length(); i++) {
            if (Character.isDigit(text.charAt(i))) {
                seen++;
                if (seen == digitCount) {
                    return i + 1;
                }
            }
        }
        return text.length();
    }

    private void updateCardTypeIndicator() {
        String cardNumber = cardNumberField.getText().replaceAll("[^\\d]", "");
        double requestedWidth = 100;
        double requestedHeight = 60;
        if (VISA_PATTERN.matcher(cardNumber).matches()) {
             cardTypeImageView.setImage(new Image(getClass().getResourceAsStream("/icons/visa.png"), requestedWidth, requestedHeight, true, true));
        } else if (MASTERCARD_PATTERN.matcher(cardNumber).matches()) {
             cardTypeImageView.setImage(new Image(getClass().getResourceAsStream("/icons/mastercard.png"), requestedWidth, requestedHeight, true, true));
        } else if (AMEX_PATTERN.matcher(cardNumber).matches()) {
             cardTypeImageView.setImage(new Image(getClass().getResourceAsStream("/icons/amex.png"), requestedWidth, requestedHeight, true, true));
        } else if (DISCOVER_PATTERN.matcher(cardNumber).matches()) {
             cardTypeImageView.setImage(new Image(getClass().getResourceAsStream("/icons/discover.png"), requestedWidth, requestedHeight, true, true));
        } else {
            cardTypeImageView.setImage(null);
        }
    }

    @FXML
    private void handleConfirm() {
        boolean isValid = validateAllFields();
        if (isValid) {
            confirmed = true;
            stage.close();
        }
    }

    @FXML
    private void handleCancel() {
        stage.close();
    }

    private boolean validateAllFields() {
        boolean cardNumberValid = validateCardNumber();
        boolean cardholderNameValid = validateCardholderName();
        boolean expiryDateValid = validateExpiryDate();
        boolean cvvValid = validateCvv();
        return cardNumberValid && cardholderNameValid && expiryDateValid && cvvValid;
    }

    private boolean validateCardNumber() {
        String cardNumber = cardNumberField.getText().replaceAll("[^\\d]", "");
        if (cardNumber.length() != 16 && cardNumber.length() != 15) {
            cardNumberErrorLabel.setText("Card number must be 15 or 16 digits.");
            return false;
        }
        if (!luhnCheck(cardNumber)) {
            cardNumberErrorLabel.setText("Invalid card number!");
            return false;
        }
        cardNumberErrorLabel.setText("");
        return true;
    }

    private boolean validateCardholderName() {
        String name = cardholderNameField.getText();
        if (name == null || name.trim().isEmpty()) {
            cardholderNameErrorLabel.setText("Cardholder name cannot be empty.");
            return false;
        }
        if (!name.matches("^[a-zA-Z .'-]+$")) {
            cardholderNameErrorLabel.setText("Invalid characters in name.");
            return false;
        }
        cardholderNameErrorLabel.setText("");
        return true;
    }

    private boolean validateExpiryDate() {
        String text = expiryDateField.getText();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yy");
        try {
            YearMonth expiry = YearMonth.parse(text, formatter);
            if (expiry.isBefore(YearMonth.now())) {
                expiryDateErrorLabel.setText("Card has expired.");
                return false;
            }
            if (expiry.isAfter(YearMonth.now().plusYears(10))) {
                expiryDateErrorLabel.setText("Expiry date too far in the future.");
                return false;
            }
        } catch (DateTimeParseException e) {
            expiryDateErrorLabel.setText("Invalid format. Use MM/YY.");
            return false;
        }
        expiryDateErrorLabel.setText("");
        return true;
    }

    private boolean validateCvv() {
        String cvv = cvvField.getText();
        String cardNumber = cardNumberField.getText().replaceAll("[^\\d]", "");
        boolean isAmex = AMEX_PATTERN.matcher(cardNumber).matches();
        int expectedLength = isAmex ? 4 : 3;

        if (cvv == null || !cvv.matches("\\d{" + expectedLength + "}")) {
            cvvErrorLabel.setText("Invalid CVV (must be " + expectedLength + " digits).");
            return false;
        }
        cvvErrorLabel.setText("");
        return true;
    }

    private boolean luhnCheck(String cardNumber) {
        int sum = 0;
        boolean alternate = false;
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(cardNumber.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }
}
