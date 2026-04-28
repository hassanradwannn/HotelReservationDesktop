import DatabaseInitializer.DatabaseConnection;
import java.sql.*;
import java.util.Scanner;

public class ChatDatabase {

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

    public static void sendMessage(int chatId, String senderUsername, String message) {
        String sql = """
            INSERT INTO chat_messages (chat_id, sender_username, message)
            VALUES (?, ?, ?)
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, chatId);
            stmt.setString(2, senderUsername);
            stmt.setString(3, message);
            stmt.executeUpdate();

        } catch (Exception e) {
            System.out.println("Message save failed: " + e.getMessage());
        }
    }

    public static void viewMessages(int chatId) {
        String sql = """
            SELECT sender_username, message, sent_at
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