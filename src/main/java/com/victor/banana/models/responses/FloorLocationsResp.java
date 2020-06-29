package com.victor.banana.models.responses;


import lombok.*;
import org.jetbrains.annotations.NotNull;import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
public class FloorLocationsResp {
    @NotNull
    private BuildingResp building;
    @Builder.Default
    private List<FloorResp> floors = List.of();
    @Builder.Default
    private List<StickyLocationResp> stickyLocations = List.of();
    @Builder.Default
    private List<DeskResp> desks = List.of();
    @Builder.Default
    private List<RoomResp> rooms = List.of();
}
