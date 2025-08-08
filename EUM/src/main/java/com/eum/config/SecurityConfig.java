package com.eum.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
            .requestMatchers("/", "/login", "/oauth2/**", "/error", "/api/auth/**", "/api/members/**", "/api/ocr/**", "/rekognition/test", 
                           "/ocr.html", "/chatbot.html", "/places.html", "/integrated-test.html", "/index.html",
                           "/api/chatbot/**", "/api/places/**", "/api/tts/**", "/test-chatbot.html").permitAll()
            .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error=true")
                .failureHandler((request, response, exception) -> {
                    System.out.println("=== OAUTH2 LOGIN FAILURE ===");
                    System.out.println("Exception: " + exception.getMessage());
                    exception.printStackTrace();
                    System.out.println("=============================");
                    response.sendRedirect("/login?error=true");
                })
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
            );
        
        return http.build();
    }
} 
