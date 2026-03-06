package com.budgettracker.backend.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory rate limiter for the /api/demo/reset endpoint.
 * Allows at most MAX_REQUESTS per IP within WINDOW_SECONDS.
 *
 * For multi-instance deployments, replace with a Redis-backed solution.
 */
@Slf4j
@Component
public class DemoRateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS = 5;
    private static final long WINDOW_SECONDS = 60;
    private static final String DEMO_RESET_PATH = "/api/demo/reset";

    private record Window(long windowStart, int count) {}

    private final Map<String, Window> ipWindows = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (!DEMO_RESET_PATH.equals(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = getClientIp(request);
        long now = Instant.now().getEpochSecond();

        Window window = ipWindows.compute(ip, (key, existing) -> {
            if (existing == null || now - existing.windowStart() >= WINDOW_SECONDS) {
                return new Window(now, 1);
            }
            return new Window(existing.windowStart(), existing.count() + 1);
        });

        if (window.count() > MAX_REQUESTS) {
            log.warn("Demo rate limit exceeded for IP: {}", ip);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Demo reset limit exceeded. Try again shortly.\"}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
