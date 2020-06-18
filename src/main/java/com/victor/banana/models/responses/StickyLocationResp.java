package com.victor.banana.models.responses;


import lombok.*;
import org.jetbrains.annotations.NotNull;import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
public class StickyLocationResp {
    @NotNull
    private UUID id;
    @NotNull
    private UUID floorId;
    @NotNull
    private UUID stickyId;
    @NotNull
    private String name;
    @NotNull
    private Boolean active;
}
