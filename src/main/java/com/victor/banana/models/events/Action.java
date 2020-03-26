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
public class Action {
    private String id;
    private String message;

    public Action(JsonObject jsonObject) {
        ActionConverter.fromJson(jsonObject, this);
    }

    public JsonObject toJson() {
        final var json = new JsonObject();
        ActionConverter.toJson(this, json);
        return json;
    }

    public static Action fromJson(JsonObject jsonObject) {
        return new Action(jsonObject);
    }
}
