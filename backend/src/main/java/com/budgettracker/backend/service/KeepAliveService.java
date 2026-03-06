package com.budgettracker.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class KeepAliveService {

    // Set SELF_URL to the backend's own Render URL in production
    // e.g. https://budget-tracker-backend-n5jp.onrender.com
    // Locally this is blank so the ping is skipped
    @Value("${SELF_URL:}")
    private String selfUrl;

    private final RestTemplate restTemplate;

    public KeepAliveService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    // Ping every 14 minutes — just under Render's 15-minute sleep threshold
    @Scheduled(fixedDelay = 14 * 60 * 1000)
    public void keepAlive() {
        if (selfUrl == null || selfUrl.isBlank()) {
            return;
        }
        try {
            String url = selfUrl + "/actuator/health";
            restTemplate.getForObject(url, String.class);
            log.debug("Keep-alive ping sent to {}", url);
        } catch (Exception e) {
            log.warn("Keep-alive ping failed: {}", e.getMessage());
        }
    }
}
