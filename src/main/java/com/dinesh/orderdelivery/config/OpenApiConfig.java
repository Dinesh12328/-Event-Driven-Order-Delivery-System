package com.dinesh.orderdelivery.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI orderDeliveryOpenApi() {
        String bearerAuth = "bearerAuth";
        return new OpenAPI()
                .components(new Components().addSecuritySchemes(bearerAuth, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(bearerAuth))
                .info(new Info()
                        .title("Event-Driven Order Delivery API")
                        .version("v1")
                        .description("Mini food-ordering backend with modular services and Kafka-driven workflows."));
    }
}
