package com.victor.banana.models.events.locations;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;
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
public class CreateBuildingFloors {
    @NotNull
    private CreateBuilding building;
    @Builder.Default
    private List<CreateFloor> floors = List.of();

    public CreateBuildingFloors(JsonObject jsonObject) {
        deserializeIntoObject(this, jsonObject);
    }

    public JsonObject toJson() {
        return serializeToJsonObject(this);
    }
}
