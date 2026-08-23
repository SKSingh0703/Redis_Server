import Components.TcpServer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Main {
    public static void main(String[] args) {
        System.out.println("Logs from your program will appear here!");

        // 1. Initialize the Spring IoC Container using Java-based AppConfig configuration
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);

        // 2. Retrieve the fully wired TcpServer bean from the Spring context
        TcpServer app = context.getBean(TcpServer.class);

        // 3. Start listening for incoming TCP client connections
        app.startServer();

        // Close context on shutdown
        context.close();
    }
}
