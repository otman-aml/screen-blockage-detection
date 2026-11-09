package com.ocp.screenblockage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class OcpScreenBlockageApplication {

    public static void main(String[] args) {
        SpringApplication.run(OcpScreenBlockageApplication.class, args);
    }
}