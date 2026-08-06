package com.lifestylehomecorp.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Standalone entry point for the {@code product-discovery} module. Runs this domain module alone:
 *
 * <pre>mvn spring-boot:run -pl product-discovery -Dspring-boot.run.arguments="--server.port=8084"</pre>
 *
 * Scans its own package plus {@code platform} for the shared beans and error advice.
 */
@SpringBootApplication(scanBasePackages = {
        "com.lifestylehomecorp.discovery",
        "com.lifestylehomecorp.platform"
})
public class DiscoveryApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiscoveryApplication.class, args);
    }
}
