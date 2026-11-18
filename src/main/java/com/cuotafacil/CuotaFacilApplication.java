package com.cuotafacil;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CuotaFacilApplication {

    public static void main(String[] args) {
        SpringApplication.run(CuotaFacilApplication.class, args);
    }
}
