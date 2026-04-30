import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;

public class AdvanceTimeController implements DashboardContentController {
    @FXML private Label todayLabel;
    @FXML private TextField daysField;
    
    private Main mainApp;

    @Override
    public void initData(Main mainApp, Object data) {
        this.mainApp = mainApp;
        todayLabel.setText("Current date: " + SystemTime.getDate());
    }

    @FXML
    private void handleAdvanceTime() {
        try {
            int value = Integer.parseInt(daysField.getText().trim());

            if (value <= 0) {
                mainApp.alert("Error", "Enter a positive number.");
                return;
            }

            SystemTime.advanceDays(value);
            ReservationService.cancelOverdueReservations();

            todayLabel.setText("Current date: " + SystemTime.getDate());
            mainApp.alert("Success", "System date advanced.");
        } catch (Exception ex) {
            mainApp.alert("Error", "Enter a valid positive number.");
        }
    }

    @FXML
    private void handleResetTime() {
        SystemTime.resetToRealToday();
        ReservationService.cancelOverdueReservations();
        todayLabel.setText("Current date: " + SystemTime.getDate());
        mainApp.alert("Success", "System date reset to actual real-world date.");
    }
}