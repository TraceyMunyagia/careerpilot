package com.careerpilot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * @SpringBootApplication = @Configuration + @EnableAutoConfiguration + @ComponentScan.
 * Spring scans this package and every sub-package for controllers, services and so on.
 */
@SpringBootApplication
@ConfigurationPropertiesScan   // finds @ConfigurationProperties records like AppProperties
public class CareerPilotApplication {

    public static void main(String[] args) {
        SpringApplication.run(CareerPilotApplication.class, args);
    }
}