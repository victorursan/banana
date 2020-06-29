package com.victor.banana.models.events.room;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.*;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static com.victor.banana.utils.SerdesUtils.deserializeIntoObject;
import static com.victor.banana.utils.SerdesUtils.serializeToJsonObject;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
@DataObject
public class Room {
    @NotNull
    private UUID id;
    @NotNull
    private String name;
    @NotNull
    private UUID floorId;
    @NotNull
    private RoomType roomType;
    @NotNull
    private Integer capacity;
    @NotNull
    private Boolean active;

    public Room(JsonObject jsonObject) {
        deserializeIntoObject(this, jsonObject);
    }

    public JsonObject toJson() {
        return serializeToJsonObject(this);
    }
}
