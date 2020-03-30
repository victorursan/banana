package com.victor.banana.models.events.tickets;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.*;


@Builder
@AllArgsConstructor
@Data
@DataObject(generateConverter = true)
public class Ticket {
    private String id;
    private String actionId;
    private String message;
    private TicketState state;
    private String acquiredBy;
    private String solvedBy;

    public Ticket(JsonObject jsonObject) {
        TicketConverter.fromJson(jsonObject, this);
    }

    public JsonObject toJson() {
        final var json = new JsonObject();
        TicketConverter.toJson(this, json);
        return json;
    }

    public static Ticket fromJson(JsonObject jsonObject) {
        return new Ticket(jsonObject);
    }

}
