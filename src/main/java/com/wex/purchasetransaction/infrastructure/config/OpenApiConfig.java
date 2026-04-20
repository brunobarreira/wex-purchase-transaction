package com.wex.purchasetransaction.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("WEX Purchase Transaction API")
                        .description("API for storing and retrieving purchase transactions "
                                + "with currency conversion via US Treasury exchange rates.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("WEX Inc.")
                                .url("https://www.wexinc.com")));
    }
}
