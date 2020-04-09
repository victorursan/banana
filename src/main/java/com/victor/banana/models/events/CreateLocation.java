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
public class CreateLocation {
    private String location;
    private String parentLocation;

    public CreateLocation(JsonObject jsonObject) {
        CreateLocationConverter.fromJson(jsonObject, this);
    }

    public JsonObject toJson() {
        final var json = new JsonObject();
        CreateLocationConverter.toJson(this, json);
        return json;
    }

    public static CreateLocation fromJson(JsonObject jsonObject) {
        return new CreateLocation(jsonObject);
    }
}
