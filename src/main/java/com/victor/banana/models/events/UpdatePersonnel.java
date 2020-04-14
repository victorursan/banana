package com.victor.banana.models.events;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
@DataObject(generateConverter = true)
public class UpdatePersonnel {
    private String firstName;
    private String lastName;
    private String locationId;
    private String roleId;

    public UpdatePersonnel(JsonObject jsonObject) {
        UpdatePersonnelConverter.fromJson(jsonObject, this);
    }

    public JsonObject toJson() {
        final var json = new JsonObject();
        UpdatePersonnelConverter.toJson(this, json);
        return json;
    }

    public static UpdatePersonnel fromJson(JsonObject jsonObject) {
        return new UpdatePersonnel(jsonObject);
    }
}
