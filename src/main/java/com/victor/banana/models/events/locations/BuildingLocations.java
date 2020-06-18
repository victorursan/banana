package com.victor.banana.models.events.locations;

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
public class  BuildingLocations {
    @NotNull
    private Company company;
    @Builder.Default
    private List<Building> buildings = List.of();
    @Builder.Default
    private List<Floor> floors = List.of();

    public BuildingLocations(JsonObject jsonObject) {
        deserializeIntoObject(this, jsonObject);
    }

    public JsonObject toJson() {
        return serializeToJsonObject(this);
    }
}
