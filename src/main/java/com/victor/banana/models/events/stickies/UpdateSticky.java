package com.victor.banana.models.events.stickies;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.*;


import java.util.Optional;

import static com.victor.banana.utils.SerdesUtils.deserializeIntoObject;
import static com.victor.banana.utils.SerdesUtils.serializeToJsonObject;

@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
@Builder
@DataObject
public class UpdateSticky {
    private String message;
    private UpdateStickyCreateAction actions;
    private UpdateStickyCreateLocation locations;

    public UpdateSticky(JsonObject jsonObject) {
        deserializeIntoObject(this, jsonObject);
    }

    public JsonObject toJson() {
        return serializeToJsonObject(this);
    }

    public Optional<String> getMessage() {
        return Optional.ofNullable(message);
    }

    public Optional<UpdateStickyCreateAction> getActions() {
        return Optional.ofNullable(actions);
    }

    public Optional<UpdateStickyCreateLocation> getLocations() {
        return Optional.ofNullable(locations);
    }
}
