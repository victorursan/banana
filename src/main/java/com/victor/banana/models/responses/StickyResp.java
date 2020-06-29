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
public class StickyResp {
    @NotNull
    private UUID id;
    @NotNull
    private String title;
    @NotNull
    private Boolean active;
    @Builder.Default
    private List<ActionStickyResp> actions = List.of();
    @Builder.Default
    private List<StickyLocationResp> locations = List.of();
}
