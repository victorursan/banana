package com.victor.banana.models.responses;


import com.victor.banana.models.events.room.RoomType;
import lombok.*;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
public class RoomResp {
    @NotNull
    private UUID id;
    @NotNull
    private String name;
    @NotNull
    private UUID floorId;
    @NotNull
    private String roomType;
    @NotNull
    private Integer capacity;
    @NotNull
    private Boolean active;
}
