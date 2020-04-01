package com.victor.banana.models.events.stickies;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;

@Builder
@AllArgsConstructor
@Data
@DataObject(generateConverter = true)
public class StickyLocation {
    private String id;
    private String locationId;
    private String message;
    @Singular
    private List<Action> actions;


    public StickyLocation(JsonObject jsonObject) {
        StickyLocationConverter.fromJson(jsonObject, this);
    }

    public JsonObject toJson() {
        final var json = new JsonObject();
        StickyLocationConverter.toJson(this, json);
        return json;
    }

    public static StickyLocation fromJson(JsonObject jsonObject) {
        return new StickyLocation(jsonObject);
    }

}
