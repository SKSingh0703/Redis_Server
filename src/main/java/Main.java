import Components.TcpServer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Main {
    public static void main(String[] args) {
        System.out.println("Logs from your program will appear here!");

        // 1. Initialize the Spring IoC Container using Java-based AppConfig configuration
        // The core Spring Container used for standalone Java applications (without Spring Boot overhead). 
        // It reads your @Configuration class and creates the Bean graph in memory
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);

        // 2. Retrieve the fully wired TcpServer bean from the Spring context
        //TcpServer is the entry point for the application.
        // It is responsible for listening for incoming TCP client connections
        // and dispatching client requests to the RespSerializer and CommandHandler.
        TcpServer app = context.getBean(TcpServer.class);

        // 3. Start listening for incoming TCP client connections
        app.startServer();

        // Close context on shutdown
        context.close();
    }
}
