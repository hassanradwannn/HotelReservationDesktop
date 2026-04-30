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

    private Main mainApp;
    private User currentUser;
    private ComboBox<String> userSelector;
    private final int[] activeChatId = {-1};

    @Override
    public void initData(Main mainApp, Object data) {
        this.mainApp = mainApp;
        this.currentUser = (User) data;

        avatarText.setText(currentUser.getUsername().substring(0, 1).toUpperCase());
        setupUserSelector();
        loadChatHistory();

        mainApp.setCurrentViewRefresher(this::loadChatHistory);
    }

    private void setupUserSelector() {
        userSelector = new ComboBox<>();
        userSelector.getStyleClass().add("combo-box");

        if (currentUser instanceof Guest) {
            userSelector.setPromptText("Select Receptionist...");
            userSelector.setItems(FXCollections.observableArrayList(
                    Database.getStaffMembers().stream()
                            .filter(s -> s instanceof Receptionist)
                            .map(User::getUsername)
                            .toList()
            ));
            userSelector.setOnAction(e -> {
                if (userSelector.getValue() != null) {
                    activeChatId[0] = ChatDatabase.getOrCreateChat(currentUser.getUsername(), userSelector.getValue());
                    loadChatHistory();
                }
            });
        } else { // Admin or Receptionist
            userSelector.setPromptText("Select a Guest to chat...");
            userSelector.setItems(FXCollections.observableArrayList(
                    Database.getGuests().stream().map(Guest::getUsername).toList()
            ));
            userSelector.setOnAction(e -> {
                if (userSelector.getValue() != null) {
                    activeChatId[0] = ChatDatabase.getOrCreateChat(userSelector.getValue(), currentUser.getUsername());
                    loadChatHistory();
                }
            });
        }
        if (!header.getChildren().contains(userSelector)) {
            header.getChildren().add(userSelector);
        }
    }

    private void loadChatHistory() {
        if (activeChatId[0] != -1) {
            chatArea.clear();
            List<ChatDatabase.ChatMessage> msgs = ChatDatabase.loadChatMessages(activeChatId[0]);
            for (ChatDatabase.ChatMessage m : msgs) {
                chatArea.appendText(m.getSenderUsername() + ": " + m.getMessage() + "\n");
            }
            chatArea.setScrollTop(Double.MAX_VALUE);
            chatStatus.setText("Connected to " + userSelector.getValue());
        } else {
            chatArea.setText("Please select a user from the dropdown above to view chat history...\n");
            chatStatus.setText("Select a chat");
        }
    }

    @FXML
    private void handleSend() {
        String msg = inputField.getText().trim();
        if (!msg.isEmpty()) {
            if (activeChatId[0] == -1) {
                mainApp.alert("Error", "Please select a chat first.");
                return;
            }
            ChatDatabase.sendMessage(activeChatId[0], currentUser.getUsername(), msg);
            inputField.clear();
            loadChatHistory();
        }
    }

    @FXML
    public void initialize() {
        inputField.setOnAction(e -> handleSend());
    }
}