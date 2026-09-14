package com.lifestylehomecorp.customer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Standalone entry point for the {@code customer} module. Runs this domain module alone:
 *
 * <pre>mvn spring-boot:run -pl customer -Dspring-boot.run.arguments="--server.port=8084"</pre>
 *
 * Scans its own package plus {@code platform} for the shared beans, the shopper session and error advice.
 */
@SpringBootApplication(scanBasePackages = {
        "com.lifestylehomecorp.customer",
        "com.lifestylehomecorp.platform"
})
public class CustomerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomerApplication.class, args);
    }
}
