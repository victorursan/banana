package com.victor.banana.models.responses;

import lombok.*;
import org.jetbrains.annotations.NotNull;import java.util.Optional;
import java.util.UUID;

@Builder
@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class UserProfileResp {
    @NotNull
    private UUID id;
    @Builder.Default
    private Optional<String> firstName = Optional.empty();
    @Builder.Default
    private Optional<String> lastName = Optional.empty();
    @Builder.Default
    private Optional<String> email = Optional.empty();
    @Builder.Default
    private Optional<String> telegramUsername = Optional.empty();
    @Builder.Default
    private Optional<RoleResp> role = Optional.empty();
    @Builder.Default
    private Optional<BuildingResp> building = Optional.empty();
}
