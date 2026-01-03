
package com.example.exptrack.config;

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
  public OpenAPI expenseTrackerOpenAPI() {

    return new OpenAPI()
        .info(new Info()
            .title("Expense Tracker Backend API")
            .version("1.0.0")
            .description("""
                ## Authentication
                This API uses **cookie-based JWT authentication**:

                | Cookie Name      | Purpose                           |
                |-----------------|-----------------------------------|
                | `access_token`   | Short-lived JWT for authentication |
                | `refresh_token`  | Long-lived JWT to refresh access  |

                Tokens are refreshed automatically using the endpoints below.

                ## Public (No Auth Required)
                These endpoints can be called without any authentication:

                - **POST /auth/login** — Login with email & password
                - **POST /auth/signup** — Create a new user account

                ## Protected (Requires `access_token` Cookie)
                All other endpoints require the user to be authenticated:

                - **POST /auth/login/refresh** — Refresh access & refresh tokens
                - **POST /auth/login/refresh-access** — Refresh access token only
                - **POST /auth/logout** — Log out the current user
                - **GET /auth/me** — Get currently authenticated user info
                - **All `/api/users/**` endpoints** — Transactions, Expenses, Revenues, Reports, Analytics
                """))
        .components(new Components()
            .addSecuritySchemes("cookieAuth",
                new SecurityScheme()
                    .name("access_token")
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.COOKIE)
                    .description("JWT access token stored in HttpOnly cookie")));
  }
}
