package Controllers;


import Utils.AppContext;
import Models.*;
import Repositories.*;
import Services.*;
import Utils.*;
import Utils.exceptions.*;
import Repositories.database.DatabaseConnection;
import Repositories.DatabaseInitializer.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class ChatController implements DashboardContentController {

    @FXML private HBox header;
    @FXML private StackPane avatar;
    @FXML private Label avatarText;
    @FXML private Label chatName;
    @FXML private Label chatStatus;
    @FXML private TextArea chatArea;
    @FXML private TextField inputField;
    @FXML private Button sendButton;

    private ComboBox<String> guestDropdown;

    private AppContext mainApp;
    private User currentUser;
    private final int[] activeChatId = {-1};
    private ChatClient chatClient;
    private String activeGuestUsername;
    private String activeReceptionistUsername;
    private boolean chatServerConnected;
    private final Set<Integer> renderedMessageIds = new HashSet<>();
    private Timeline liveSyncTimeline;

    @Override
    public void initData(AppContext mainApp, Object data) {
        this.mainApp = mainApp;
        this.currentUser = (User) data;

        try {
            avatarText.setText(currentUser.getUsername().substring(0, 1).toUpperCase());

            ChatDatabase.ensureTablesExist();
            connectToChatServer();

            guestDropdown = new ComboBox<>();
            guestDropdown.getStyleClass().add("combo-box");
            populateDropdown();

            guestDropdown.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    loadChatHistory(newVal);
                }
            });

            header.getChildren().add(guestDropdown);
            startLiveSync();

            if (!guestDropdown.getItems().isEmpty()) {
                guestDropdown.getSelectionModel().selectFirst();
            } else {
                chatArea.setText("No users available to chat with.\n");
                chatStatus.setText("No users found");
            }

            chatArea.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (oldScene != null && newScene == null) {
                    stopChatResources();
                }
            });

            mainApp.setCurrentViewRefresher(this::refreshChatView);
        } catch (Exception e) {
            showChatDisconnected();
        }
    }

    private void connectToChatServer() {
        try {
            chatClient = new ChatClient(
                    ChatServer.DEFAULT_HOST,
                    ChatServer.DEFAULT_PORT,
                    currentUser.getUsername(),
                    this::appendIncomingMessage,
                    status -> chatStatus.setText(status));
            chatServerConnected = true;
            sendButton.setDisable(false);
            inputField.setDisable(false);
        } catch (Exception e) {
            showChatDisconnected();
        }
    }

    private void showChatDisconnected() {
        chatClient = null;
        chatServerConnected = false;
        activeChatId[0] = -1;
        chatStatus.setText("Chat is not connected");
        chatArea.setText("Chat is not connected to localhost.\nStart the chat server, then reopen chat.");
        sendButton.setDisable(true);
        inputField.setDisable(true);
    }

    private void populateDropdown() {
        List<String> items;
        if (currentUser instanceof Guest) {
            guestDropdown.setPromptText("Select Staff...");
            // Guest chat depends on staff rows that may not be loaded in this view yet.
            if (Database.getStaffMembers().isEmpty()) {
                Database.refreshUsersFromDatabase();
            }
            items = Database.getStaffMembers().stream()
                    .filter(s -> s instanceof Receptionist || s instanceof Admin)
                    .map(User::getUsername)
                    .toList();
        } else {
            guestDropdown.setPromptText("Select Guest...");
            items = Database.getActiveGuests();
        }
        guestDropdown.setItems(FXCollections.observableArrayList(items));
    }

    private void refreshChatView() {
        if (!chatServerConnected) {
            connectToChatServer();
            if (!chatServerConnected) {
                syncActiveChatFromDatabase();
            }
            return;
        }

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
        if (!chatServerConnected) {
            showChatDisconnected();
            return;
        }

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
        renderedMessageIds.clear();
        if (chatClient != null) {
            chatClient.joinChat(guestUser, receptionistUser);
        } else {
            List<ChatDatabase.ChatMessage> msgs = ChatDatabase.loadChatMessages(chatId);
            for (ChatDatabase.ChatMessage m : msgs) {
                appendMessageIfNew(m.getId(), m.getSenderUsername(), m.getMessage());
            }
        }
        syncActiveChatFromDatabase();
        chatArea.setScrollTop(Double.MAX_VALUE);
        chatStatus.setText("Chatting with " + otherUsername);
        if (chatName != null) chatName.setText(otherUsername);
    }

    private void appendIncomingMessage(int chatId, int messageId, String senderUsername, String message) {
        if (chatId != activeChatId[0]) {
            return;
        }
        appendMessageIfNew(messageId, senderUsername, message);
    }

    private void appendMessageIfNew(int messageId, String senderUsername, String message) {
        if (messageId > 0 && !renderedMessageIds.add(messageId)) {
            return;
        }
        chatArea.appendText(senderUsername + ": " + message + "\n");
        chatArea.setScrollTop(Double.MAX_VALUE);
    }

    private void startLiveSync() {
        if (liveSyncTimeline != null) {
            liveSyncTimeline.stop();
        }
        liveSyncTimeline = new Timeline(new KeyFrame(Duration.millis(700), event -> syncActiveChatFromDatabase()));
        liveSyncTimeline.setCycleCount(Timeline.INDEFINITE);
        liveSyncTimeline.play();
    }

    private void syncActiveChatFromDatabase() {
        if (activeChatId[0] == -1 || chatServerConnected) {
            return;
        }

        int chatId = activeChatId[0];
        Thread thread = new Thread(() -> {
            List<ChatDatabase.ChatMessage> messages = ChatDatabase.loadChatMessages(chatId);
            Platform.runLater(() -> {
                if (activeChatId[0] != chatId) {
                    return;
                }
                for (ChatDatabase.ChatMessage message : messages) {
                    appendMessageIfNew(message.getId(), message.getSenderUsername(), message.getMessage());
                }
            });
        }, "chat-db-sync");
        thread.setDaemon(true);
        thread.start();
    }

    private void stopChatResources() {
        if (liveSyncTimeline != null) {
            liveSyncTimeline.stop();
            liveSyncTimeline = null;
        }
        if (chatClient != null) {
            chatClient.close();
            chatClient = null;
        }
    }

    @Override
    public void onRemoved() {
        stopChatResources();
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
            showChatDisconnected();
            mainApp.alert("Error", "Chat is not connected.");
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
