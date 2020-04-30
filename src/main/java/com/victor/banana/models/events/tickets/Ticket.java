package com.victor.banana.models.events.tickets;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.*;

import java.util.Optional;
import java.util.UUID;

import static com.victor.banana.utils.SerdesUtils.deserializeIntoObject;
import static com.victor.banana.utils.SerdesUtils.serializeToJsonObject;


@Builder
@AllArgsConstructor
@Data
@DataObject
public class Ticket {
    private UUID id;
    private UUID actionId;
    private UUID locationId;
    private String message;
    private TicketState state;
    private Optional<UUID> acquiredBy;
    private Optional<UUID> solvedBy;

    public Ticket(JsonObject jsonObject) {
        deserializeIntoObject(this, jsonObject);
    }

    public JsonObject toJson() {
        return serializeToJsonObject(this);
    }
}
