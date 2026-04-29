import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class LiveChatController implements DashboardContentController {
    @FXML private Label chatStatus;
    @FXML private TextArea chatArea;
    @FXML private TextField inputField;
    @FXML private Label avatarLabel;
    
    private ChatClient client;
    private Main mainApp;

    @Override
    public void initData(Main mainApp, Object data) {
        this.mainApp = mainApp;
        User user = (User) data;
        
        avatarLabel.setText(user.getUsername().substring(0, 1).toUpperCase());
        
        try {
            client = new ChatClient("localhost", 5000, user.getUsername());
            client.listen(chatArea);
            chatArea.appendText("System: You joined the hotel live chat.\n");
        } catch (Exception e) {
            chatStatus.setText("Offline — start ChatServer.java first");
            chatArea.setText("Cannot connect to server.\nMake sure ChatServer.java is running first.");
        }
    }

    @FXML
    private void handleSend() {
        if (client != null) {
            String msg = inputField.getText().trim();
            if (!msg.isEmpty()) {
                client.send(msg);
                inputField.clear();
            }
        }
    }
}