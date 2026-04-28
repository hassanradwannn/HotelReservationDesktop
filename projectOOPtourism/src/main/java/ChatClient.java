import java.io.*;
import java.net.Socket;
import javafx.application.Platform;
import javafx.scene.control.TextArea;

public class ChatClient {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public ChatClient(String host, int port, String username) throws Exception {
        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        out.println(username);
    }

    public void send(String msg) {
        out.println(msg);
    }

    public void listen(TextArea area) {
        Thread thread = new Thread(() -> {
            try {
                String msg;
                while ((msg = in.readLine()) != null) {
                    String finalMsg = msg;
                    Platform.runLater(() -> area.appendText(finalMsg + "\n"));
                }
            } catch (Exception e) {
                Platform.runLater(() -> area.appendText("Disconnected from server.\n"));
            }
        });

        thread.setDaemon(true);
        thread.start();
    }
}