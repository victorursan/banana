package com.victor.banana.models.events.stickies;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.victor.banana.utils.SerdesUtils.deserializeIntoObject;
import static com.victor.banana.utils.SerdesUtils.serializeToJsonObject;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
@DataObject
public class StickyLocationUpdate {
    private UUID id;
    private UUID parentLocation;
    private String location;

    public StickyLocationUpdate(JsonObject jsonObject) {
        deserializeIntoObject(this, jsonObject);
    }

    public JsonObject toJson() {
        return serializeToJsonObject(this);
    }

    public UUID getId() {
        return id;
    }

    public Optional<UUID> getParentLocation() {
        return Optional.ofNullable(parentLocation);
    }

    public Optional<String> getLocation() {
        return Optional.ofNullable(location);
    }
}
