import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import java.time.format.DateTimeFormatter;

public class AdvanceTimeController implements DashboardContentController {
    @FXML private Label todayLabel;
    @FXML private TextField daysField;
    
    private Main mainApp;
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void initData(Main mainApp, Object data) {
        this.mainApp = mainApp;
        updateTodayLabel();
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

            updateTodayLabel();
            mainApp.alert("Success", "System date advanced.");
        } catch (Exception ex) {
            mainApp.alert("Error", "Enter a valid positive number.");
        }
    }

    @FXML
    private void handleResetTime() {
        SystemTime.resetToRealToday();
        ReservationService.cancelOverdueReservations();
        updateTodayLabel();
        mainApp.alert("Success", "System date reset to actual real-world date.");
    }

    private void updateTodayLabel() {
        todayLabel.setText("Current date: " + SystemTime.getToday().format(DISPLAY_DATE));
    }
}
