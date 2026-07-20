package com.lifestylehomecorp.project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Standalone entry point for the {@code project} module. Runs this single domain module on
 * its own port so it can be demoed in isolation:
 *
 * <pre>mvn spring-boot:run -pl project -Dspring-boot.run.arguments="--server.port=8082"</pre>
 *
 * It scans its own package plus {@code platform} so the shared ProjectApiRoot bean and error
 * advice are wired even when run alone.
 */
@SpringBootApplication(scanBasePackages = {
        "com.lifestylehomecorp.project",
        "com.lifestylehomecorp.platform"
})
public class ProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProjectApplication.class, args);
    }
}
