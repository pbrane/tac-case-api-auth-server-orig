package com.beaconstrategists.taccaseauthserver.config;

import com.beaconstrategists.taccaseauthserver.commands.AuthServerUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import javax.sql.DataSource;

@Configuration
public class RegisteredClientRepositoryConfig {

    @Value("${CLIENT_ID:client-app}")
    private String clientId;

    @Value("${CLIENT_SECRET:secret}")
    private String clientSecret;

    @Value("${CLIENT_NAME:client-app}")
    private String clientName;

    private final PasswordEncoder passwordEncoder;

    // Constructor injection for PasswordEncoder
    public RegisteredClientRepositoryConfig(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository(DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        JdbcRegisteredClientRepository registeredClientRepository = new JdbcRegisteredClientRepository(jdbcTemplate);

        // Register or update a client with clientId and clientSecret from environment variables
        AuthServerUtils.registerOrUpdate(clientId, clientSecret, clientName, registeredClientRepository, passwordEncoder);

        return registeredClientRepository;
    }

}