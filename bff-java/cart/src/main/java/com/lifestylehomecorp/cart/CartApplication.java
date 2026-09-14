package com.lifestylehomecorp.cart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Standalone entry point for the {@code cart} module. Runs this domain module alone:
 *
 * <pre>mvn spring-boot:run -pl cart -Dspring-boot.run.arguments="--server.port=8085"</pre>
 *
 * Scans its own package plus {@code platform} for the shared beans and error advice.
 */
@SpringBootApplication(scanBasePackages = {
        "com.lifestylehomecorp.cart",
        "com.lifestylehomecorp.platform"
})
public class CartApplication {

    public static void main(String[] args) {
        SpringApplication.run(CartApplication.class, args);
    }
}
