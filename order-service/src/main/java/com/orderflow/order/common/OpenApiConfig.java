package com.orderflow.order.common;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI orderFlowOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("OrderFlow API")
                        .version("1.0")
                        .description("""
                                Order-to-cash backend: catalog, orders, payments, invoices, rollback (saga), audit trail.

                                **How to try it**
                                1. `POST /api/auth/register`, then `POST /api/auth/login`
                                2. Copy the `token` from the login response
                                3. Click **Authorize** (top right) and paste the token
                                4. Admin endpoints (`/api/admin/**`) need a user whose role is ADMIN

                                Every response uses the same envelope: `success`, `status`, `message`, `data`, `errors`, `timestamp`.
                                """))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
