package com.lifestylehomecorp.training;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Standalone entry point for the {@code training} module:
 *
 * <pre>mvn spring-boot:run -pl training -Dspring-boot.run.arguments="--server.port=8084"</pre>
 */
@SpringBootApplication(scanBasePackages = {
        "com.lifestylehomecorp.training",
        "com.lifestylehomecorp.platform"
})
public class TrainingApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrainingApplication.class, args);
    }
}
