package com.victor.banana.models.responses;


import lombok.*;
import org.jetbrains.annotations.NotNull;import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
public class BuildingLocationsResp {
    @NotNull
    private CompanyResp company;
    @Builder.Default
    private List<BuildingResp> buildings = List.of();
    @Builder.Default
    private List<FloorResp> floors = List.of();
}
