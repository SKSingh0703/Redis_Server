package Components;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

/**
 * Handles TCP networking, socket listening, and client connection management.
 * Spring automatically injects the RespSerializer bean into this component.
 */
@Component
public class TcpServer {

    private final RespSerializer respSerializer;
    private final int port = 6379;

    @Autowired
    public TcpServer(RespSerializer respSerializer) {
        this.respSerializer = respSerializer;
    }

    public void startServer() {
        ServerSocket serverSocket = null;

        try {
            // 1. Create server socket bound to port 6379 (ONCE outside the loop)
            serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);

            System.out.println("Redis server listening on port " + port + "...");

            // Main Acceptor Loop: Wait for client connections continuously
            while (true) {
                // Block and wait until a client connects
                Socket clientSocket = serverSocket.accept();

                // Concurrency: Dispatch each client connection to a separate worker thread
                new Thread(() -> handleClient(clientSocket)).start();
            }

        } catch (IOException e) {
            System.out.println("IOException in TcpServer: " + e.getMessage());
        } finally {
            try {
                if (serverSocket != null) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                System.out.println("IOException when closing server socket: " + e.getMessage());
            }
        }
    }

    /**
     * Handles reading and writing commands for a single client connection.
     */
    private void handleClient(Socket clientSocket) {
        try (clientSocket;
             InputStream inputStream = clientSocket.getInputStream();
             OutputStream outputStream = clientSocket.getOutputStream()) {

            System.out.println("Accepted connection from client: " + clientSocket.getRemoteSocketAddress());

            Scanner scanner = new Scanner(inputStream);

            // Read incoming commands continuously while connection remains open
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();

                // Command: PING
                if (line.toUpperCase().contains("PING")) {
                    String pongResponse = respSerializer.encodeSimpleString("PONG");
                    outputStream.write(pongResponse.getBytes());
                    outputStream.flush();
                } 
                // Command: ECHO <message>
                else if (line.toUpperCase().contains("ECHO")) {
                    @SuppressWarnings("unused")
                    String respHeader = scanner.nextLine(); // Header specifying length (e.g. $3)
                    String respBody = scanner.nextLine();   // Message payload (e.g. hey)

                    String bulkStringResponse = respSerializer.encodeBulkString(respBody);
                    outputStream.write(bulkStringResponse.getBytes());
                    outputStream.flush();
                }
            }

            System.out.println("Client disconnected cleanly: " + clientSocket.getRemoteSocketAddress());
            scanner.close();

        } catch (IOException e) {
            System.out.println("IOException handling client: " + e.getMessage());
        }
    }
}
