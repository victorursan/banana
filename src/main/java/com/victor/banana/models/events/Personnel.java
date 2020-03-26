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
public class Personnel {
    private String id;
    private String firstName;
    private String lastName;

    public Personnel(JsonObject jsonObject) {
        PersonnelConverter.fromJson(jsonObject, this);
    }

    public JsonObject toJson() {
        final var json = new JsonObject();
        PersonnelConverter.toJson(this, json);
        return json;
    }

    public static Personnel fromJson(JsonObject jsonObject) {
        return new Personnel(jsonObject);
    }
}
