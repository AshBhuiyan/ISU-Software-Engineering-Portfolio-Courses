package onetoone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import onetoone.config.GameConfig;

@SpringBootApplication
@EnableJpaRepositories
@EnableConfigurationProperties(GameConfig.class)
public class Main {

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    // No hardcoded users - all users must be created through registration
    // Users can register via POST /users endpoint
}
