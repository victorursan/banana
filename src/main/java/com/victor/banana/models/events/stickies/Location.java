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
public class Location {
    private String id;
    private String text;

    public Location(JsonObject jsonObject) {
        LocationConverter.fromJson(jsonObject, this);
    }

    public JsonObject toJson() {
        final var json = new JsonObject();
        LocationConverter.toJson(this, json);
        return json;
    }

    public static Location fromJson(JsonObject jsonObject) {
        return new Location(jsonObject);
    }
}
