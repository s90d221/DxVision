package com.example.dxvision.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI dxVisionOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("DxVision API")
                        .version("v1")
                        .description("Clinical image-based diagnosis training platform"));
    }
}
