package com.victor.banana.models.responses;

import lombok.*;

import java.util.Optional;
import java.util.UUID;

@Builder
@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class UserProfileResp {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    @Builder.Default
    private Optional<String> telegramUsername = Optional.empty();
    private RoleResp role;
    private LocationResp location;
}
