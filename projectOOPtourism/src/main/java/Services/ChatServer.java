package Services;


import Controllers.*;
import Models.*;
import Repositories.*;
import Utils.*;
import Utils.exceptions.*;
import Repositories.database.DatabaseConnection;
import Repositories.DatabaseInitializer.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {
    public static final String DEFAULT_HOST = "localhost";
    public static final int DEFAULT_PORT = 5000;

    private static final Map<String, Set<ClientHandler>> clientsByUsername = new ConcurrentHashMap<>();
    private static volatile boolean embeddedServerStarted = false;

    public static void main(String[] args) {
        start(DEFAULT_PORT);
    }

    public static void startInBackgroundIfAvailable() {
        if (embeddedServerStarted) {
            return;
        }
        Thread serverThread = new Thread(() -> start(DEFAULT_PORT), "hotel-chat-server");
        serverThread.setDaemon(true);
        serverThread.start();
        embeddedServerStarted = true;
    }

    public static void start(int port) {
        ChatDatabase.ensureTablesExist();
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Chat server listening on port " + port);
            while (true) {
                Socket socket = serverSocket.accept();
                Thread clientThread = new Thread(new ClientHandler(socket), "hotel-chat-client");
                clientThread.setDaemon(true);
                clientThread.start();
            }
        } catch (BindException e) {
            System.out.println("Chat server already running on port " + port + ".");
        } catch (IOException e) {
            System.out.println("Chat server stopped: " + e.getMessage());
        }
    }

    private static void register(String username, ClientHandler handler) {
        clientsByUsername.computeIfAbsent(username, key -> ConcurrentHashMap.newKeySet()).add(handler);
    }

    private static void unregister(String username, ClientHandler handler) {
        if (username == null) {
            return;
        }
        Set<ClientHandler> handlers = clientsByUsername.get(username);
        if (handlers != null) {
            handlers.remove(handler);
            if (handlers.isEmpty()) {
                clientsByUsername.remove(username);
            }
        }
    }

    private static void broadcastToConversation(String guestUsername, String receptionistUsername, String payload) {
        sendToUser(guestUsername, payload);
        if (!guestUsername.equalsIgnoreCase(receptionistUsername)) {
            sendToUser(receptionistUsername, payload);
        }
    }

    private static void sendToUser(String username, String payload) {
        Set<ClientHandler> handlers = clientsByUsername.get(username);
        if (handlers == null) {
            return;
        }
        for (ClientHandler handler : handlers) {
            handler.send(payload);
        }
    }

    private static String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private PrintWriter out;
        private String username;

        private ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (Socket autoCloseSocket = socket;
                 BufferedReader in = new BufferedReader(new InputStreamReader(autoCloseSocket.getInputStream(), StandardCharsets.UTF_8))) {
                out = new PrintWriter(autoCloseSocket.getOutputStream(), true, StandardCharsets.UTF_8);
                handleInput(in);
            } catch (IOException e) {
                System.out.println("Chat client disconnected: " + e.getMessage());
            } finally {
                unregister(username, this);
            }
        }

        private void handleInput(BufferedReader in) throws IOException {
            String line;
            while ((line = in.readLine()) != null) {
                String[] parts = line.split("\t", 5);
                if (parts.length == 0) {
                    continue;
                }

                switch (parts[0]) {
                    case "HELLO" -> handleHello(parts);
                    case "JOIN" -> handleJoin(parts);
                    case "SEND" -> handleSend(parts);
                    case "QUIT" -> {
                        return;
                    }
                    default -> send("ERROR\tUnknown command");
                }
            }
        }

        private void handleHello(String[] parts) {
            if (parts.length < 2) {
                send("ERROR\tMissing username");
                return;
            }
            username = decode(parts[1]);
            register(username, this);
            send("READY");
        }

        private void handleJoin(String[] parts) {
            if (parts.length < 3) {
                send("ERROR\tMissing chat participants");
                return;
            }
            String guestUsername = decode(parts[1]);
            String receptionistUsername = decode(parts[2]);
            int chatId = ChatDatabase.getOrCreateChat(guestUsername, receptionistUsername);
            send("JOINED\t" + chatId + "\t" + encode(guestUsername) + "\t" + encode(receptionistUsername));

            List<ChatDatabase.ChatMessage> history = ChatDatabase.loadChatMessages(chatId);
            for (ChatDatabase.ChatMessage message : history) {
                send("MESSAGE\t" + chatId + "\t" + encode(message.getSenderUsername()) + "\t" + encode(message.getMessage()));
            }
        }

        private void handleSend(String[] parts) {
            if (parts.length < 5) {
                send("ERROR\tMissing message fields");
                return;
            }
            String guestUsername = decode(parts[1]);
            String receptionistUsername = decode(parts[2]);
            String senderUsername = decode(parts[3]);
            String message = decode(parts[4]);

            int chatId = ChatDatabase.getOrCreateChat(guestUsername, receptionistUsername);
            if (chatId == -1) {
                send("ERROR\tCould not open chat in database");
                return;
            }

            ChatDatabase.sendMessage(chatId, senderUsername, message);
            String payload = "MESSAGE\t" + chatId + "\t" + encode(senderUsername) + "\t" + encode(message);
            broadcastToConversation(guestUsername, receptionistUsername, payload);
        }

        private void send(String payload) {
            if (out != null) {
                out.println(payload);
            }
        }
    }
}
