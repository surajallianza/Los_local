package com.bank.los;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@EnableJpaAuditing
@EnableScheduling
public class LosApplication {

    public static void main(String[] args) {
        loadDotEnvIfPresent();
        SpringApplication.run(LosApplication.class, args);
    }

    private static void loadDotEnvIfPresent() {
        File dotEnvFile = new File(".env");
        if (dotEnvFile.exists() && dotEnvFile.isFile()) {
            try {
                List<String> lines = Files.readAllLines(dotEnvFile.toPath());
                for (String line : lines) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) {
                        continue;
                    }
                    int eqIdx = trimmed.indexOf('=');
                    String key = trimmed.substring(0, eqIdx).trim();
                    String value = trimmed.substring(eqIdx + 1).trim();

                    if ((value.startsWith("\"") && value.endsWith("\"")) ||
                        (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }

                    if (System.getenv(key) == null && System.getProperty(key) == null) {
                        System.setProperty(key, value);
                    }
                }
            } catch (IOException e) {
                System.err.println("Notice: Could not read .env file: " + e.getMessage());
            }
        }

        synchronizeSpringProfile();
    }

    private static void synchronizeSpringProfile() {
        if (System.getProperty("spring.profiles.active") != null) {
            return;
        }

        String activeProfile = System.getProperty("SPRING_PROFILES_ACTIVE");
        if (activeProfile == null || activeProfile.isBlank()) {
            activeProfile = System.getenv("SPRING_PROFILES_ACTIVE");
        }
        if (activeProfile != null && !activeProfile.isBlank()) {
            System.setProperty("spring.profiles.active", activeProfile);
        }
    }
}
