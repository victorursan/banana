package com.victor.banana.models.events.stickies;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@AllArgsConstructor
@Data
@DataObject(generateConverter = true)
public class CreateAction {
    private String message;
    private String roleId;

    public CreateAction(JsonObject jsonObject) {
        CreateActionConverter.fromJson(jsonObject, this);
    }

    public JsonObject toJson() {
        final var json = new JsonObject();
        CreateActionConverter.toJson(this, json);
        return json;
    }

    public static CreateAction fromJson(JsonObject jsonObject) {
        return new CreateAction(jsonObject);
    }
}
