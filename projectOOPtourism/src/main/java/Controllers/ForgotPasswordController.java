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

import javafx.fxml.FXML;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

public class ForgotPasswordController {
    @FXML private TextField usernameField;
    @FXML private DatePicker dobPicker;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label messageLabel;

    private AppContext mainApp;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public void setMainApp(AppContext mainApp) {
        this.mainApp = mainApp;
    }

    @FXML
    public void initialize() {
        dobPicker.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                return date == null ? "" : dtf.format(date);
            }

            @Override
            public LocalDate fromString(String text) {
                if (text == null || text.trim().isEmpty()) {
                    return null;
                }
                return LocalDate.parse(text.trim(), dtf);
            }
        });

        dobPicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isAfter(SystemTime.getToday()));
            }
        });
    }

    @FXML
    private void handleResetPassword() {
        String username = usernameField.getText().trim();
        LocalDate dob = dobPicker.getValue();
        String newPass = newPasswordField.getText().trim();
        String confPass = confirmPasswordField.getText().trim();

        if (username.isEmpty() || dob == null || newPass.isEmpty() || confPass.isEmpty()) {
            messageLabel.setText("Please fill all fields.");
            return;
        }

        if (!newPass.equals(confPass)) {
            messageLabel.setText("Passwords do not match.");
            return;
        }

        try {
            Authentication.validatePasswordStrength(newPass);

            User user = UserDatabase.findUser(username);
            if (user == null || !user.getDateOfBirth().isEqual(dob)) {
                messageLabel.setText("Invalid Username or Date of Birth.");
                return;
            }

            DatabaseSaver.updateUserPassword(username, newPass);

            mainApp.alert("Success", "Password reset successfully. You can now log in.");
            mainApp.showLoginScreen();

        } catch (Exception ex) {
            messageLabel.setText(ex.getMessage() != null ? ex.getMessage() : "Failed to reset password.");
        }
    }

    @FXML
    private void handleBack() {
        mainApp.showLoginScreen();
    }
}
