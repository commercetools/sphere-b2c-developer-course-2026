package com.lifestylehomecorp.app;

import com.lifestylehomecorp.cart.CartApplication;
import com.lifestylehomecorp.catalog.CatalogApplication;
import com.lifestylehomecorp.customer.CustomerApplication;
import com.lifestylehomecorp.discovery.DiscoveryApplication;
import com.lifestylehomecorp.project.ProjectApplication;
import com.lifestylehomecorp.training.TrainingApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * The aggregator entry point: assembles platform + every domain module into the full BFF on
 * :8081. It scans the whole {@code com.lifestylehomecorp} base package, but excludes each
 * module's standalone {@code *Application} class so their {@code @SpringBootApplication}
 * (and its auto-configuration) is not registered a second time here.
 *
 * <pre>mvn spring-boot:run -pl app        # full BFF on :8081</pre>
 */
@SpringBootApplication(scanBasePackages = "com.lifestylehomecorp")
@ComponentScan(
        basePackages = "com.lifestylehomecorp",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {ProjectApplication.class, CatalogApplication.class,
                        DiscoveryApplication.class, CartApplication.class,
                        CustomerApplication.class, TrainingApplication.class}))
public class BffApplication {

    public static void main(String[] args) {
        SpringApplication.run(BffApplication.class, args);
    }
}
