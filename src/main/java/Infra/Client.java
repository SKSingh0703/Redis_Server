package Infra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Client encapsulates a single connected TCP client socket and its streaming I/O lifecycle.
 * Located in package Infra to prepare for storing node replication metadata and client session lists.
 */
public class Client implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(Client.class);

    private final Socket socket;
    private final InputStream inputStream;
    private final OutputStream outputStream;
    private final String remoteAddress;

    public Client(Socket socket) throws IOException {
        this.socket = socket;
        this.inputStream = socket.getInputStream();
        this.outputStream = socket.getOutputStream();
        this.remoteAddress = socket.getRemoteSocketAddress() != null 
                ? socket.getRemoteSocketAddress().toString() 
                : "Unknown Remote";
    }

    public Socket getSocket() {
        return socket;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    /**
     * Checks if the underlying socket is connected and open.
     */
    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    /**
     * Sends a String (encoded as UTF-8 bytes) down the client's output stream and flushes.
     */
    public void sendResponse(String response) throws IOException {
        if (response != null && isConnected()) {
            outputStream.write(response.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        }
    }

    /**
     * Sends raw byte arrays down the client's output stream and flushes.
     */
    public void sendBytes(byte[] bytes) throws IOException {
        if (bytes != null && isConnected()) {
            outputStream.write(bytes);
            outputStream.flush();
        }
    }

    /**
     * Safely closes the underlying socket and streams.
     */
    @Override
    public void close() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            logger.error("Error closing client connection [{}]: {}", remoteAddress, e.getMessage());
        }
    }
}
