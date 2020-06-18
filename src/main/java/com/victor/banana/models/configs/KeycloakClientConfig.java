package com.victor.banana.models.configs;

import lombok.*;
import org.jetbrains.annotations.NotNull;

@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class KeycloakClientConfig {
    @NotNull
    private String serverUrl;
    @NotNull
    private String realm;
    @NotNull
    private String username;
    @NotNull
    private String password;
    @NotNull
    private String clientId;
    @NotNull
    private String clientSecret;
}
