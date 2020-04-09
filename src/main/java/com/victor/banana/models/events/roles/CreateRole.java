package com.victor.banana.models.events.roles;


import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@AllArgsConstructor
@Data
@DataObject(generateConverter = true)
public class CreateRole {
    private String type;

    public CreateRole(JsonObject jsonObject) {
        CreateRoleConverter.fromJson(jsonObject, this);
    }

    public JsonObject toJson() {
        final var json = new JsonObject();
        CreateRoleConverter.toJson(this, json);
        return json;
    }

    public static CreateRole fromJson(JsonObject jsonObject) {
        return new CreateRole(jsonObject);
    }
}
