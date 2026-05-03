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
    private User user;
    private int activeChatId = -1;
    private String guestUsername;
    private String receptionistUsername;

    @Override
    public void initData(Main mainApp, Object data) {
        this.user = (User) data;

        avatarLabel.setText(user.getUsername().substring(0, 1).toUpperCase());

        try {
            configureDefaultConversation();
            client = new ChatClient(
                    ChatServer.DEFAULT_HOST,
                    ChatServer.DEFAULT_PORT,
                    user.getUsername(),
                    this::appendIncomingMessage,
                    status -> chatStatus.setText(status));
            client.joinChat(guestUsername, receptionistUsername);
            chatArea.appendText("System: You joined the hotel live chat.\n");
        } catch (Exception e) {
            chatStatus.setText("Offline - start ChatServer.java first");
            chatArea.setText("Cannot connect to server.\nMake sure ChatServer.java is running first.");
        }
    }

    private void configureDefaultConversation() {
        Database.refreshUsersFromDatabase();
        if (user instanceof Guest) {
            guestUsername = user.getUsername();
            receptionistUsername = Database.getStaffMembers().stream()
                    .filter(staff -> staff instanceof Receptionist || staff instanceof Admin)
                    .map(User::getUsername)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No receptionist or admin available."));
        } else {
            receptionistUsername = user.getUsername();
            guestUsername = Database.getActiveGuests().stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No guests available."));
        }
        activeChatId = ChatDatabase.getOrCreateChat(guestUsername, receptionistUsername);
    }

    private void appendIncomingMessage(int chatId, String senderUsername, String message) {
        if (chatId == activeChatId) {
            chatArea.appendText(senderUsername + ": " + message + "\n");
            chatArea.setScrollTop(Double.MAX_VALUE);
        }
    }

    @FXML
    private void handleSend() {
        if (client != null) {
            String msg = inputField.getText().trim();
            if (!msg.isEmpty()) {
                client.send(guestUsername, receptionistUsername, user.getUsername(), msg);
                inputField.clear();
            }
        }
    }
}
