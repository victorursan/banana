package com.victor.banana.models.responses;


import lombok.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
public class BuildingFloorsResp {
    @NotNull
    private BuildingResp building;
    @Builder.Default
    private List<FloorResp> floors = List.of();
}
