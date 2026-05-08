import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.function.Consumer;

import javafx.application.Platform;

public class ChatClient implements Closeable {
    public interface MessageListener {
        void onMessage(int chatId, String senderUsername, String message);
    }

    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private final MessageListener messageListener;
    private final Consumer<String> statusListener;
    private volatile boolean running = true;
    private static final int CONNECT_TIMEOUT_MS = 1200;

    public ChatClient(
            String host,
            int port,
            String username,
            MessageListener messageListener,
            Consumer<String> statusListener) throws IOException {
        this.socket = new Socket();
        this.socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
        this.messageListener = messageListener;
        this.statusListener = statusListener;

        out.println("HELLO\t" + encode(username));
        listen();
    }

    public void joinChat(String guestUsername, String receptionistUsername) {
        out.println("JOIN\t" + encode(guestUsername) + "\t" + encode(receptionistUsername));
    }

    public void send(String guestUsername, String receptionistUsername, String senderUsername, String message) {
        out.println("SEND\t"
                + encode(guestUsername) + "\t"
                + encode(receptionistUsername) + "\t"
                + encode(senderUsername) + "\t"
                + encode(message));
    }

    private void listen() {
        Thread thread = new Thread(() -> {
            try {
                String line;
                while (running && (line = in.readLine()) != null) {
                    handleServerLine(line);
                }
            } catch (IOException e) {
                if (running) {
                    notifyStatus("Disconnected from chat server.");
                }
            }
        }, "hotel-chat-client-listener");

        thread.setDaemon(true);
        thread.start();
    }

    private void handleServerLine(String line) {
        String[] parts = line.split("\t", 4);
        if (parts.length == 0) {
            return;
        }

        switch (parts[0]) {
            case "READY" -> notifyStatus("Connected to reception desk");
            case "JOINED" -> notifyStatus("Live chat connected");
            case "MESSAGE" -> handleMessage(parts);
            case "ERROR" -> notifyStatus(parts.length > 1 ? parts[1] : "Chat server error");
            default -> notifyStatus("Unexpected chat server response");
        }
    }

    private void handleMessage(String[] parts) {
        if (parts.length < 4) {
            return;
        }
        int chatId;
        try {
            chatId = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return;
        }
        String senderUsername = decode(parts[2]);
        String message = decode(parts[3]);
        Platform.runLater(() -> messageListener.onMessage(chatId, senderUsername, message));
    }

    private void notifyStatus(String status) {
        if (statusListener != null) {
            Platform.runLater(() -> statusListener.accept(status));
        }
    }

    private static String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }

    @Override
    public void close() {
        running = false;
        out.println("QUIT");
        try {
            socket.close();
        } catch (IOException e) {
            System.out.println("Could not close chat client: " + e.getMessage());
        }
    }
}
