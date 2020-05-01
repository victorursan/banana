package com.victor.banana.models.events.stickies;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.*;

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
public class ActionUpdate {
    private UUID id;
    private String action;
    private UUID roleId;

    public ActionUpdate(JsonObject jsonObject) {
        deserializeIntoObject(this, jsonObject);
    }

    public JsonObject toJson() {
        return serializeToJsonObject(this);
    }

    public UUID getId() {
        return id;
    }

    public Optional<String> getAction() {
        return Optional.ofNullable(action);
    }

    public Optional<UUID> getRoleId() {
        return Optional.ofNullable(roleId);
    }
}
