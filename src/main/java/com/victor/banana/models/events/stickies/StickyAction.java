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
public class StickyAction {
    private String actionId;
    private String locationId;
    private String stickyMessage;
    private String actionMessage;
    private String location;

    public StickyAction(JsonObject jsonObject) {
        StickyActionConverter.fromJson(jsonObject, this);
    }

    public JsonObject toJson() {
        final var json = new JsonObject();
        StickyActionConverter.toJson(this, json);
        return json;
    }

    public static StickyAction fromJson(JsonObject jsonObject) {
        return new StickyAction(jsonObject);
    }
}
