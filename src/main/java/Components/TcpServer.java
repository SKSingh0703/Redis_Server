package Components;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

/**
 * Handles TCP networking, socket listening, connection management, and dispatching
 * client requests to the RespSerializer and CommandHandler.
 * 
 * Spring automatically injects RespSerializer and CommandHandler beans into this component.
 */
@Component
public class TcpServer {

    private final RespSerializer respSerializer;
    private final CommandHandler commandHandler;
    private final int port = 6379;

    @Autowired
    public TcpServer(RespSerializer respSerializer, CommandHandler commandHandler) {
        this.respSerializer = respSerializer;
        this.commandHandler = commandHandler;
    }

    public void startServer() {
        ServerSocket serverSocket = null;

        try {
            // Bind server socket to port 6379 ONCE
            serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);

            System.out.println("Redis server listening on port " + port + "...");

            // Main Acceptor Loop: Accept client connections continuously
            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();

                // Concurrency: Process each client connection on a separate worker thread
                new Thread(() -> handleClient(clientSocket)).start();
            }

        } catch (IOException e) {
            System.out.println("IOException in TcpServer: " + e.getMessage());
        } finally {
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                System.out.println("IOException when closing server socket: " + e.getMessage());
            }
        }
    }

    /**
     * Handles lifecycle, reading RESP commands, and dispatching responses for a connected client.
     */
    private void handleClient(Socket clientSocket) {
        try (Client client = new Client(clientSocket)) {
            System.out.println("Accepted connection from client: " + client.getRemoteAddress());

            // Continuously process incoming RESP framed commands while client is connected
            while (client.isConnected()) {
                List<String> commandParts = respSerializer.deserialize(client.getInputStream());

                // Null or empty list indicates client disconnected (EOF) or sent empty frame
                if (commandParts == null || commandParts.isEmpty()) {
                    break;
                }

                // Delegate command evaluation to CommandHandler component
                String response = commandHandler.handleCommand(commandParts);

                // Send RESP wire response back to client
                if (response != null) {
                    client.sendResponse(response);
                }
            }

            System.out.println("Client disconnected cleanly: " + client.getRemoteAddress());

        } catch (IOException e) {
            System.out.println("IOException handling client: " + e.getMessage());
        }
    }
}
