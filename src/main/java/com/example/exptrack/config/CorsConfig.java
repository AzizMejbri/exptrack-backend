package com.example.exptrack.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration
        .setAllowedOrigins(Arrays.asList(
            "http://localhost:4200",
            "https://localhost:4200",
            "https://frontend:4200",
            "https://exptrackz-omega.vercel.app",
            "https://exptrackz-spiders-projects-8bcdef85.vercel.app",
            "https://exptrackz-4ad2eql9w-spiders-projects-8bcdef85.vercel.app",
            "https://exptrackz-guyunkown185-3310-spiders-projects-8bcdef85.vercel.app",
            "https://exptrackz-myaxwirvd-spiders-projects-8bcdef85.vercel.app"));
    configuration.setAllowedHeaders(Arrays.asList("*"));
    configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
    configuration.setAllowedMethods(Arrays.asList("OPTIONS", "PATCH", "POST", "GET", "PUT", "DELETE"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
