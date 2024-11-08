package com.beaconstrategists.taccaseauthserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
public class AuthorizationServerSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Apply the default OAuth2 Authorization Server security configuration
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

        // Customize the form login page
        http.formLogin(form -> form.loginPage("/login").permitAll());
        return http.build();
    }
}

