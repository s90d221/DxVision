package com.example.dxvision.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // ✅ Vite 개발 서버 허용
        config.setAllowedOrigins(List.of("http://localhost:5173"));

        // ✅ 허용할 HTTP 메서드
        config.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        // ✅ 모든 헤더 허용 (Authorization 포함)
        config.setAllowedHeaders(List.of("*"));

        // (선택) 프론트에서 보고 싶은 헤더
        config.setExposedHeaders(List.of("Authorization"));

        // ✅ JWT 쿠키 안 써도 true 해도 됨
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
