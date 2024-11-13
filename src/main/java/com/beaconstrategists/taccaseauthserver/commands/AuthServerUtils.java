package com.beaconstrategists.taccaseauthserver.commands;


import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ShellComponent
public class AuthServerUtils {

    private final RegisteredClientRepository registeredClientRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public AuthServerUtils(RegisteredClientRepository registeredClientRepository,
                           PasswordEncoder passwordEncoder,
                           JdbcTemplate jdbcTemplate) {

        this.registeredClientRepository = registeredClientRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @ShellMethod(value = "List all registered clients sorted by secret expiration date.", key = "list-clients")
    public String listClients() {
        String query = "SELECT " +
                            "id, " +
                            "client_id, " +
                            "client_id_issued_at, " +
                            "client_secret, " +
                            "client_secret_expires_at, " +
                            "client_name, " +
                            "client_authentication_methods, " +
                            "authorization_grant_types, " +
                            "redirect_uris, " +
                            "post_logout_redirect_uris, " +
                            "scopes, client_settings, " +
                            "token_settings " +
                        "FROM oauth2_registered_client";

        List<RegisteredClient> clients = jdbcTemplate.query(query, new JdbcRegisteredClientRepository.RegisteredClientRowMapper());

        List<RegisteredClient> sortedClients = clients.stream()
                .sorted((client1, client2) -> {
                    Instant expiresAt1 = client1.getClientSecretExpiresAt();
                    Instant expiresAt2 = client2.getClientSecretExpiresAt();
                    if (expiresAt1 == null && expiresAt2 == null) return 0;
                    if (expiresAt1 == null) return 1;
                    if (expiresAt2 == null) return -1;
                    return expiresAt1.compareTo(expiresAt2);
                })
                .toList();

        StringBuilder result = new StringBuilder("\nRegistered Clients (sorted by client secret expiration date):\n");
        for (RegisteredClient client : sortedClients) {
            result.append(String.format("Client ID: %s, Client Name: %s, Expires At: %s\n",
                    client.getClientId(),
                    client.getClientName(),
                    client.getClientSecretExpiresAt() != null ? client.getClientSecretExpiresAt().toString() : "Never"));
        }
        return result.toString();
    }


    @ShellMethod(key = "registerOrUpdateClient", value = "Register new client or update existing")
    public String registerOrUpdateClient(@ShellOption(value = "-i", defaultValue = "client-id", help = "Client ID") String clientId,
                                         @ShellOption(value = "-s", defaultValue = "client-secret", help = "Client Secret") String clientSecret,
                                         @ShellOption(value = "-n", defaultValue = "msft", help = "Client Name") String clientName) {

        RegisteredClient client = registerOrUpdate(clientId, clientSecret, clientName, registeredClientRepository, passwordEncoder);
        return String.format("Client: %s, registered. Expires: %s", client.getClientName(), client.getClientSecretExpiresAt());
    }

    @ShellMethod(key = "expireClient", value= "Expires an existing client")
    public String expireClient(@ShellOption(value = "-i", defaultValue = "client-app") String clientId) {
        RegisteredClient client = registeredClientRepository.findByClientId(clientId);
        if (client != null) {
            Instant expiresAt = Instant.now();
            RegisteredClient registeredClient = RegisteredClient.from(client)
                    .clientSecretExpiresAt(expiresAt)
                    .build();
            registeredClientRepository.save(registeredClient);
            return String.format("Client: %s, expired at: %s", clientId, expiresAt);
        } else {
            return String.format("Client not found: %s", clientId);
        }
    }

    public static RegisteredClient registerOrUpdate(String clientId, String clientSecret, String clientName, RegisteredClientRepository registeredClientRepository, PasswordEncoder passwordEncoder) {
        RegisteredClient existingClient = registeredClientRepository.findByClientId(clientId);
        if (existingClient == null) {
            // Register new client
            existingClient = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId(clientId)
                    .clientSecret(passwordEncoder.encode(clientSecret))
                    .clientIdIssuedAt(Instant.now())
                    .clientSecretExpiresAt(Instant.now().plus(Duration.ofDays(30)))
                    .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .clientName(clientName.toLowerCase())
                    .scope("read.cases")
                    .scope("write.cases")
                    .tokenSettings(TokenSettings.builder()
                            .accessTokenTimeToLive(Duration.ofMinutes(5))
                            .build())
                    .build();
        } else if (existingClient.getClientSecretExpiresAt() != null && existingClient.getClientSecretExpiresAt().isBefore(Instant.now())) {
            // Update existing client with new secret
            existingClient = RegisteredClient.from(existingClient)
                    .clientSecret(passwordEncoder.encode(clientSecret))
                    .clientSecretExpiresAt(Instant.now().plus(Duration.ofDays(30)))
                    .build();
        }
        registeredClientRepository.save(existingClient);
        return existingClient;
    }

}
