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

        javafx.application.Platform.runLater(() -> {
            if (daysField.getScene() != null) {
                for (javafx.scene.Node node : daysField.getScene().getRoot().lookupAll(".label")) {
                    if (node instanceof Label label && label.getText() != null && label.getText().toLowerCase().contains("time")) {
                        String currentStyle = label.getStyle() == null ? "" : label.getStyle();
                        label.setStyle(currentStyle + "; -fx-text-fill: #2B2421;");
                    }
                }
                String todayStyle = todayLabel.getStyle() == null ? "" : todayLabel.getStyle();
                todayLabel.setStyle(todayStyle + "; -fx-text-fill: #2B2421;");
            }
        });
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
}