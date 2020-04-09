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
public class Role {
    private String id;
    private String type;

    public Role(JsonObject jsonObject) {
        RoleConverter.fromJson(jsonObject, this);
    }

    public JsonObject toJson() {
        final var json = new JsonObject();
        RoleConverter.toJson(this, json);
        return json;
    }

    public static Role fromJson(JsonObject jsonObject) {
        return new Role(jsonObject);
    }
}
