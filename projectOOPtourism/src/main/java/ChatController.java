import java.util.List;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

public class ChatController implements DashboardContentController {

    @FXML private HBox header;
    @FXML private StackPane avatar;
    @FXML private Label avatarText;
    @FXML private Label chatName;
    @FXML private Label chatStatus;
    @FXML private TextArea chatArea;
    @FXML private TextField inputField;
    @FXML private Button sendButton;

    // NOT @FXML — created programmatically to avoid a double-instance conflict.
    private ComboBox<String> guestDropdown;

    private Main mainApp;
    private User currentUser;
    private final int[] activeChatId = {-1};
    private ChatClient chatClient;
    private String activeGuestUsername;
    private String activeReceptionistUsername;

    @Override
    public void initData(Main mainApp, Object data) {
        this.mainApp = mainApp;
        this.currentUser = (User) data;

        avatarText.setText(currentUser.getUsername().substring(0, 1).toUpperCase());

        // Ensure chat tables exist before any DB calls
        ChatDatabase.ensureTablesExist();
        connectToChatServer();

        // Create single ComboBox, populate, then wire listener
        guestDropdown = new ComboBox<>();
        guestDropdown.getStyleClass().add("combo-box");
        populateDropdown();

        // Listener attached AFTER population to avoid premature fires
        guestDropdown.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadChatHistory(newVal);
            }
        });

        header.getChildren().add(guestDropdown);

        if (!guestDropdown.getItems().isEmpty()) {
            guestDropdown.getSelectionModel().selectFirst();
        } else {
            chatArea.setText("No users available to chat with.\n");
            chatStatus.setText("No users found");
        }

        mainApp.setCurrentViewRefresher(this::refreshChatView);
    }

    private void connectToChatServer() {
        try {
            chatClient = new ChatClient(
                    ChatServer.DEFAULT_HOST,
                    ChatServer.DEFAULT_PORT,
                    currentUser.getUsername(),
                    this::appendIncomingMessage,
                    status -> chatStatus.setText(status));
        } catch (Exception e) {
            chatClient = null;
            chatStatus.setText("Offline - chat server unavailable");
            chatArea.setText("Cannot connect to the chat server.\nStart ChatServer.java or open another app instance to host it.");
        }
    }

    private void populateDropdown() {
        List<String> items;
        if (currentUser instanceof Guest) {
            guestDropdown.setPromptText("Select Staff...");
            // Ensure staff are loaded
            if (Database.getStaffMembers().isEmpty()) {
                Database.refreshUsersFromDatabase();
            }
            // Guests can chat with both Receptionists and Admins
            items = Database.getStaffMembers().stream()
                    .filter(s -> s instanceof Receptionist || s instanceof Admin)
                    .map(User::getUsername)
                    .toList();
        } else {
            // Staff (Receptionist or Admin) see all guests
            guestDropdown.setPromptText("Select Guest...");
            items = Database.getActiveGuests();
        }
        guestDropdown.setItems(FXCollections.observableArrayList(items));
    }

    private void refreshChatView() {
        String selected = guestDropdown.getValue();

        List<String> freshItems;
        if (currentUser instanceof Guest) {
            Database.refreshUsersFromDatabase();
            freshItems = Database.getStaffMembers().stream()
                    .filter(s -> s instanceof Receptionist || s instanceof Admin)
                    .map(User::getUsername)
                    .toList();
        } else {
            freshItems = Database.getActiveGuests();
        }

        if (!guestDropdown.getItems().equals(freshItems)) {
            guestDropdown.setItems(FXCollections.observableArrayList(freshItems));
        }

        if (selected != null) {
            if (!guestDropdown.getItems().contains(selected)) {
                guestDropdown.getItems().add(selected);
            }
            guestDropdown.setValue(selected);
        } else {
            loadChatHistory(null);
        }
    }

    public void loadChatHistory(String otherUsername) {
        if (otherUsername == null) {
            activeChatId[0] = -1;
            chatArea.setText("Please select a user from the dropdown above to view chat history...\n");
            chatStatus.setText("Select a chat");
            return;
        }

        String guestUser;
        String receptionistUser;

        if (currentUser instanceof Guest) {
            guestUser = currentUser.getUsername();
            receptionistUser = otherUsername;
        } else {
            // Staff talking to a guest — guest is the other person
            guestUser = otherUsername;
            receptionistUser = currentUser.getUsername();
        }

        int chatId = ChatDatabase.getOrCreateChat(guestUser, receptionistUser);
        activeChatId[0] = chatId;
        activeGuestUsername = guestUser;
        activeReceptionistUsername = receptionistUser;

        if (chatId == -1) {
            chatArea.setText("Could not open chat. Check database connection.\n");
            chatStatus.setText("Connection error");
            return;
        }

        chatArea.clear();
        if (chatClient != null) {
            chatClient.joinChat(guestUser, receptionistUser);
        } else {
            List<ChatDatabase.ChatMessage> msgs = ChatDatabase.loadChatMessages(chatId);
            for (ChatDatabase.ChatMessage m : msgs) {
                chatArea.appendText(m.getSenderUsername() + ": " + m.getMessage() + "\n");
            }
        }
        chatArea.setScrollTop(Double.MAX_VALUE);
        chatStatus.setText("Chatting with " + otherUsername);
        if (chatName != null) chatName.setText(otherUsername);
    }

    private void appendIncomingMessage(int chatId, String senderUsername, String message) {
        if (chatId != activeChatId[0]) {
            return;
        }
        chatArea.appendText(senderUsername + ": " + message + "\n");
        chatArea.setScrollTop(Double.MAX_VALUE);
    }

    @FXML
    private void handleSend() {
        String msg = inputField.getText().trim();
        if (msg.isEmpty()) return;
        if (activeChatId[0] == -1) {
            mainApp.alert("Error", "Please select a chat first.");
            return;
        }
        if (chatClient == null) {
            mainApp.alert("Error", "Chat server is not connected.");
            return;
        }
        chatClient.send(activeGuestUsername, activeReceptionistUsername, currentUser.getUsername(), msg);
        inputField.clear();
    }

    @FXML
    public void initialize() {
        if (inputField != null) {
            inputField.setOnAction(e -> handleSend());
        }
    }
}
