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
public class ScanStickyResp {
    @NotNull
    private UUID id;
    @NotNull
    private UUID locationId;
    @NotNull
    private String message;
    @Builder.Default
    private List<ActionStickyResp> actions = List.of();
}
