package Components.Server;

import Components.Service.CommandHandler;
import Infra.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

/**
 * Handles TCP networking, socket listening, connection management, and dispatching
 * client requests to the RespSerializer and CommandHandler.
 * Located in package Components.Server.
 * 
 * Spring automatically injects RespSerializer and CommandHandler beans into this component.
 */
@Component
public class TcpServer {

    private static final Logger logger = LoggerFactory.getLogger(TcpServer.class);

    private final RespSerializer respSerializer;
    private final CommandHandler commandHandler;
    private static final int DEFAULT_PORT = 6379;

    @Autowired
    public TcpServer(RespSerializer respSerializer, CommandHandler commandHandler) {
        this.respSerializer = respSerializer;
        this.commandHandler = commandHandler;
    }

    /**
     * Starts the TCP server listening on the default port 6379.
     */
    public void startServer() {
        startServer(DEFAULT_PORT);
    }

    /**
     * Starts the TCP server listening on the specified port.
     * 
     * @param port the port number to bind the ServerSocket to (e.g. 6379, 6380)
     */
    public void startServer(int port) {
        ServerSocket serverSocket = null;

        try {
            // Bind server socket to the specified port
            serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);

            logger.info("Redis server listening on port {}...", port);

            // Main Acceptor Loop: Accept client connections continuously
            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();

                // Concurrency: Process each client connection on a separate worker thread
                new Thread(() -> handleClient(clientSocket)).start();
            }

        } catch (IOException e) {
            logger.error("IOException in TcpServer on port {}: {}", port, e.getMessage());
        } finally {
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                logger.error("IOException when closing server socket: {}", e.getMessage());
            }
        }
    }

    /**
     * Handles lifecycle, reading RESP commands, and dispatching responses for a connected client.
     */
    private void handleClient(Socket clientSocket) {
        try (Client client = new Client(clientSocket)) {
            logger.info("Accepted connection from client: {}", client.getRemoteAddress());

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

            logger.info("Client disconnected cleanly: {}", client.getRemoteAddress());

        } catch (IOException e) {
            logger.error("IOException handling client: {}", e.getMessage());
        }
    }
}
