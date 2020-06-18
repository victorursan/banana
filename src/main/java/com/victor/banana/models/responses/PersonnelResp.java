package com.victor.banana.models.responses;

import lombok.*;
import org.jetbrains.annotations.NotNull;import java.util.Optional;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
public class PersonnelResp {
    @NotNull
    private UUID id;
    @Builder.Default
    private Optional<String> firstName = Optional.empty();
    @Builder.Default
    private Optional<String> lastName = Optional.empty();
    @Builder.Default
    private Optional<String> email = Optional.empty();
    @Builder.Default
    private Optional<String> username = Optional.empty();
    @Builder.Default
    private Optional<UUID> buildingId = Optional.empty();
    @Builder.Default
    private Optional<UUID> roleId = Optional.empty();
}
