package com.tft.coach.orchestrator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.tft.coach")
public class TftOrchestratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(TftOrchestratorApplication.class, args);
    }
}
