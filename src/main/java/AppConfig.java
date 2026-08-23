import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * AppConfig serves as the Spring Java-based configuration class.
 * @Configuration tells Spring this is a configuration source.
 * @ComponentScan tells Spring to automatically discover and register @Component classes in the "Components" package.
 */
@Configuration
@ComponentScan(basePackages = {"Components"})
public class AppConfig {
}
