package com.dinesh.orderdelivery.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI orderDeliveryOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Event-Driven Order Delivery API")
                        .version("v1")
                        .description("Mini food-ordering backend with modular services and Kafka-driven workflows."));
    }
}

