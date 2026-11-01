package rocks.leug.turmadamonicarank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class TurmadamonicarankApplication {

    public static void main(String[] args) {
        SpringApplication.run(TurmadamonicarankApplication.class, args);
    }
}
