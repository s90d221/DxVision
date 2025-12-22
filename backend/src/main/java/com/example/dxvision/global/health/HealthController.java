package com.example.dxvision.global.health;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
public class HealthController {

    private final DataSource dataSource;

    @GetMapping
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("time", LocalDateTime.now().toString());

        // DB 체크
        try (Connection conn = dataSource.getConnection()) {
            if (conn.isValid(2)) {
                result.put("db", "UP");
            } else {
                result.put("db", "DOWN");
            }
        } catch (Exception e) {
            result.put("db", "DOWN");
            result.put("error", e.getMessage());
        }

        return result;
    }
}
