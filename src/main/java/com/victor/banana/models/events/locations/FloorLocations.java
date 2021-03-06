package com.victor.banana.models.events.locations;

import com.victor.banana.models.events.desk.Desk;
import com.victor.banana.models.events.room.Room;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.*;
import org.jetbrains.annotations.NotNull;import java.util.List;

import static com.victor.banana.utils.SerdesUtils.deserializeIntoObject;
import static com.victor.banana.utils.SerdesUtils.serializeToJsonObject;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
@DataObject
public class FloorLocations {
    @NotNull
    private Building building;
    @Builder.Default
    private List<Floor> floors = List.of();
    @Builder.Default
    private List<StickyLocation> stickyLocations = List.of();
    @Builder.Default
    private List<Desk> desks = List.of();
    @Builder.Default
    private List<Room> rooms = List.of();

    public FloorLocations(JsonObject jsonObject) {
        deserializeIntoObject(this, jsonObject);
    }

    public JsonObject toJson() {
        return serializeToJsonObject(this);
    }
}
