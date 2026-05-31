package com.insightflow.bff;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class DashboardBffApplication {

    public static void main(String[] args) {
        SpringApplication.run(DashboardBffApplication.class, args);
    }
}
