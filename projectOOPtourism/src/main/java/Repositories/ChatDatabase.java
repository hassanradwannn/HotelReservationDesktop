package Repositories;


import Controllers.*;
import Models.*;
import Services.*;
import Utils.*;
import Utils.exceptions.*;
import Repositories.database.DatabaseConnection;
import Repositories.DatabaseInitializer.*;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class ChatDatabase {

    public static class ChatMessage {
        private final int id;
        private final String senderUsername;
        private final String message;
        private final Timestamp sentAt;

        public ChatMessage(int id, String senderUsername, String message, Timestamp sentAt) {
            this.id = id;
            this.senderUsername = senderUsername;
            this.message = message;
            this.sentAt = sentAt;
        }

        public int getId() { return id; }
        public String getSenderUsername() { return senderUsername; }
        public String getMessage() { return message; }
        public Timestamp getSentAt() { return sentAt; }

        @Override
        public String toString() {
            return senderUsername + ": " + message + " [" + sentAt + "]";
        }
    }

    public static class ChatInfo {
        private final int chatId;
        private final String guestUsername;
        private final String receptionistUsername;
        private final Timestamp createdAt;

        public ChatInfo(int chatId, String guestUsername, String receptionistUsername, Timestamp createdAt) {
            this.chatId = chatId;
            this.guestUsername = guestUsername;
            this.receptionistUsername = receptionistUsername;
            this.createdAt = createdAt;
        }

        public int getChatId() { return chatId; }
        public String getGuestUsername() { return guestUsername; }
        public String getReceptionistUsername() { return receptionistUsername; }
        public Timestamp getCreatedAt() { return createdAt; }
    }

    // Chat is optional, so the tables are created lazily when the chat screen opens.
    public static void ensureTablesExist() {
        String chatsTable = """
            CREATE TABLE IF NOT EXISTS chats (
                id INT AUTO_INCREMENT PRIMARY KEY,
                guest_username VARCHAR(100) NOT NULL,
                receptionist_username VARCHAR(100) NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE KEY unique_chat (guest_username, receptionist_username)
            )
        """;
        String messagesTable = """
            CREATE TABLE IF NOT EXISTS chat_messages (
                id INT AUTO_INCREMENT PRIMARY KEY,
                chat_id INT NOT NULL,
                sender_username VARCHAR(100) NOT NULL,
                message TEXT NOT NULL,
                sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (chat_id) REFERENCES chats(id) ON DELETE CASCADE
            )
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(chatsTable);
            stmt.execute(messagesTable);
            ensureMessageIdColumn(conn, stmt);
        } catch (Exception e) {
            System.out.println("Could not ensure chat tables: " + e.getMessage());
        }
    }

    private static void ensureMessageIdColumn(Connection conn, Statement stmt) {
        try {
            DatabaseMetaData metadata = conn.getMetaData();
            try (ResultSet columns = metadata.getColumns(null, null, "chat_messages", "id")) {
                if (columns.next()) {
                    return;
                }
            }

            stmt.execute("ALTER TABLE chat_messages ADD COLUMN id INT AUTO_INCREMENT PRIMARY KEY FIRST");
        } catch (Exception e) {
            System.out.println("Could not upgrade chat messages table: " + e.getMessage());
        }
    }

    public static int getOrCreateChat(String guestUsername, String receptionistUsername) {
        String findSql = """
            SELECT id FROM chats 
            WHERE guest_username = ? AND receptionist_username = ?
        """;

        String insertSql = """
            INSERT INTO chats (guest_username, receptionist_username)
            VALUES (?, ?)
        """;

        try (Connection conn = DatabaseConnection.getConnection()) {

            PreparedStatement findStmt = conn.prepareStatement(findSql);
            findStmt.setString(1, guestUsername);
            findStmt.setString(2, receptionistUsername);

            ResultSet rs = findStmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("id");
            }

            PreparedStatement insertStmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
            insertStmt.setString(1, guestUsername);
            insertStmt.setString(2, receptionistUsername);
            insertStmt.executeUpdate();

            ResultSet keys = insertStmt.getGeneratedKeys();

            if (keys.next()) {
                return keys.getInt(1);
            }

        } catch (Exception e) {
            System.out.println("Chat creation failed: " + e.getMessage());
        }

        return -1;
    }

    public static int sendMessage(int chatId, String senderUsername, String message) {
        String sql = """
            INSERT INTO chat_messages (chat_id, sender_username, message)
            VALUES (?, ?, ?)
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, chatId);
            stmt.setString(2, senderUsername);
            stmt.setString(3, message);
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        } catch (Exception e) {
            System.out.println("Message save failed: " + e.getMessage());
        }

        return -1;
    }

    public static void viewMessages(int chatId) {
        String sql = """
            SELECT id, sender_username, message, sent_at
            FROM chat_messages
            WHERE chat_id = ?
            ORDER BY sent_at ASC
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, chatId);
            ResultSet rs = stmt.executeQuery();

            System.out.println("\n========== CHAT HISTORY ==========");

            boolean hasMessages = false;

            while (rs.next()) {
                hasMessages = true;
                System.out.println(
                        rs.getString("sender_username")
                                + ": "
                                + rs.getString("message")
                                + " [" + rs.getTimestamp("sent_at") + "]"
                );
            }

            if (!hasMessages) {
                System.out.println("No messages yet.");
            }

            System.out.println("==================================");

        } catch (Exception e) {
            System.out.println("Could not load messages: " + e.getMessage());
        }
    }

    public static List<ChatMessage> loadChatMessages(int chatId) {
        List<ChatMessage> messages = new ArrayList<>();
        String sql = """
            SELECT id, sender_username, message, sent_at
            FROM chat_messages
            WHERE chat_id = ?
            ORDER BY id ASC, sent_at ASC
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, chatId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                messages.add(new ChatMessage(
                        rs.getInt("id"),
                        rs.getString("sender_username"),
                        rs.getString("message"),
                        rs.getTimestamp("sent_at")
                ));
            }

        } catch (Exception e) {
            System.out.println("Could not load messages: " + e.getMessage());
        }

        return messages;
    }

    public static List<ChatMessage> loadChatMessagesAfter(int chatId, int lastMessageId) {
        List<ChatMessage> messages = new ArrayList<>();
        String sql = """
            SELECT id, sender_username, message, sent_at
            FROM chat_messages
            WHERE chat_id = ? AND id > ?
            ORDER BY id ASC, sent_at ASC
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, chatId);
            stmt.setInt(2, lastMessageId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                messages.add(new ChatMessage(
                        rs.getInt("id"),
                        rs.getString("sender_username"),
                        rs.getString("message"),
                        rs.getTimestamp("sent_at")
                ));
            }

        } catch (Exception e) {
            System.out.println("Could not load new messages: " + e.getMessage());
        }

        return messages;
    }

    public static List<ChatInfo> getUserChats(String username) {
        List<ChatInfo> chats = new ArrayList<>();
        String sql = """
            SELECT id, guest_username, receptionist_username, created_at
            FROM chats
            WHERE guest_username = ? OR receptionist_username = ?
            ORDER BY created_at DESC
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, username);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                chats.add(new ChatInfo(
                        rs.getInt("id"),
                        rs.getString("guest_username"),
                        rs.getString("receptionist_username"),
                        rs.getTimestamp("created_at")
                ));
            }

        } catch (Exception e) {
            System.out.println("Could not load user chats: " + e.getMessage());
        }

        return chats;
    }

    public static ChatInfo getChatBetweenUsers(String guestUsername, String receptionistUsername) {
        String sql = """
            SELECT id, guest_username, receptionist_username, created_at
            FROM chats
            WHERE guest_username = ? AND receptionist_username = ?
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, guestUsername);
            stmt.setString(2, receptionistUsername);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new ChatInfo(
                        rs.getInt("id"),
                        rs.getString("guest_username"),
                        rs.getString("receptionist_username"),
                        rs.getTimestamp("created_at")
                );
            }

        } catch (Exception e) {
            System.out.println("Could not find chat: " + e.getMessage());
        }

        return null;
    }

    public static void guestChatMenu(Scanner input, String guestUsername) {
        String receptionistUsername = "Manar";

        int chatId = getOrCreateChat(guestUsername, receptionistUsername);

        if (chatId == -1) {
            System.out.println("Could not open chat.");
            return;
        }

        int choice;

        do {
            System.out.println("\n===== Private Chat With Receptionist =====");
            System.out.println("1. View chat history");
            System.out.println("2. Send message");
            System.out.println("0. Back");
            System.out.print("Choose: ");

            choice = input.nextInt();
            input.nextLine();

            switch (choice) {
                case 1:
                    viewMessages(chatId);
                    break;

                case 2:
                    System.out.print("Enter message: ");
                    String msg = input.nextLine();

                    if (!msg.trim().isEmpty()) {
                        sendMessage(chatId, guestUsername, msg);
                        System.out.println("Message sent.");
                    } else {
                        System.out.println("Message cannot be empty.");
                    }
                    break;

                case 0:
                    break;

                default:
                    System.out.println("Invalid choice.");
            }

        } while (choice != 0);
    }

    public static void receptionistChatMenu(Scanner input, String receptionistUsername) {
        int choice;

        do {
            System.out.println("\n===== Receptionist Chat Inbox =====");
            System.out.println("1. View all guest chats");
            System.out.println("0. Back");
            System.out.print("Choose: ");

            choice = input.nextInt();
            input.nextLine();

            if (choice == 1) {
                showReceptionistChats(input, receptionistUsername);
            }

        } while (choice != 0);
    }

    private static void showReceptionistChats(Scanner input, String receptionistUsername) {
        String sql = """
            SELECT id, guest_username, created_at
            FROM chats
            WHERE receptionist_username = ?
            ORDER BY created_at DESC
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, receptionistUsername);
            ResultSet rs = stmt.executeQuery();

            System.out.println("\n========== GUEST CHATS ==========");

            boolean hasChats = false;

            while (rs.next()) {
                hasChats = true;
                System.out.println(
                        "Chat ID: " + rs.getInt("id")
                                + " | Guest: " + rs.getString("guest_username")
                                + " | Created: " + rs.getTimestamp("created_at")
                );
            }

            if (!hasChats) {
                System.out.println("No guest chats yet.");
                return;
            }

            System.out.println("=================================");
            System.out.print("Enter chat ID to open, or 0 to back: ");

            int chatId = input.nextInt();
            input.nextLine();

            if (chatId == 0) return;

            viewMessages(chatId);

            System.out.print("Reply? yes/no: ");
            String answer = input.nextLine();

            if (answer.equalsIgnoreCase("yes")) {
                System.out.print("Enter reply: ");
                String reply = input.nextLine();

                if (!reply.trim().isEmpty()) {
                    sendMessage(chatId, receptionistUsername, reply);
                    System.out.println("Reply sent.");
                }
            }

        } catch (Exception e) {
            System.out.println("Could not load receptionist chats: " + e.getMessage());
        }
    }
}
