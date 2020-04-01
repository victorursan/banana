package com.victor.banana.models.events;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@AllArgsConstructor
@Data
@DataObject(generateConverter = true)
public class ActionSelected {
    private String actionId;
    private String locationId;

    public ActionSelected(JsonObject jsonObject) {
        ActionSelectedConverter.fromJson(jsonObject, this);
    }

    public JsonObject toJson() {
        final var json = new JsonObject();
        ActionSelectedConverter.toJson(this, json);
        return json;
    }

    public static ActionSelected fromJson(JsonObject jsonObject) {
        return new ActionSelected(jsonObject);
    }
}
