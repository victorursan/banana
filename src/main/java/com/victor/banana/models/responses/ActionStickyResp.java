package com.victor.banana.models.responses;

import lombok.*;
import org.jetbrains.annotations.NotNull;import java.util.List;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
public class ActionStickyResp {
    @NotNull
    private UUID id;
    @Builder.Default
    private List<UUID> roles = List.of();
    @NotNull
    private String message;
    @NotNull
    private String state;
}
