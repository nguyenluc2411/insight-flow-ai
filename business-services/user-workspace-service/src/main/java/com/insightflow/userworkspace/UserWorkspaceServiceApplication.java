package com.insightflow.userworkspace;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class UserWorkspaceServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserWorkspaceServiceApplication.class, args);
    }
}