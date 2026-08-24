import Config.AppConfig;
import Components.Server.TcpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("Starting Redis Server application...");

        // Default port is 6379 unless overridden by --port flag
        int port = 6379;

        // Parse CLI arguments for --port <portNumber>
        for (int i = 0; i < args.length; i++) {
            if ("--port".equals(args[i]) && i + 1 < args.length) {
                try {
                    port = Integer.parseInt(args[i + 1]);
                    i++;
                } catch (NumberFormatException e) {
                    logger.error("Invalid port number specified: {}. Falling back to default {}", args[i + 1], port);
                }
            }
        }

        // 1. Initialize the Spring IoC Container using Java-based AppConfig configuration
        // Reads Config.AppConfig and initializes bean graph across Components, Infra, and Config.
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);

        // 2. Retrieve the fully wired TcpServer bean from the Spring context
        TcpServer app = context.getBean(TcpServer.class);

        // 3. Start listening for incoming TCP client connections on configured port
        app.startServer(port);

        // Close context on shutdown
        context.close();
    }
}
