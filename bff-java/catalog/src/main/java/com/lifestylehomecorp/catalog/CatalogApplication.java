package com.lifestylehomecorp.catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Standalone entry point for the {@code catalog} module. Runs this domain module alone:
 *
 * <pre>mvn spring-boot:run -pl catalog -Dspring-boot.run.arguments="--server.port=8083"</pre>
 *
 * Scans its own package plus {@code platform} for the shared beans and error advice.
 */
@SpringBootApplication(scanBasePackages = {
        "com.lifestylehomecorp.catalog",
        "com.lifestylehomecorp.platform"
})
public class CatalogApplication {

    public static void main(String[] args) {
        SpringApplication.run(CatalogApplication.class, args);
    }
}
