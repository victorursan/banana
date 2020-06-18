package com.victor.banana.models.responses;


import lombok.*;
import org.jetbrains.annotations.NotNull;import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
public class FloorResp {
    @NotNull
    private UUID id;
    @NotNull
    private UUID buildingId;
    @NotNull
    private String name;
    @NotNull
    private Boolean active;
}
