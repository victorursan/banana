package com.victor.banana.models.events.tickets;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.*;

import java.time.OffsetDateTime;
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
    private OffsetDateTime createdAt;
    private Optional<OffsetDateTime> acquiredAt;
    private Optional<OffsetDateTime> solvedAt;
    private Optional<UUID> ownedBy;

    public Ticket(JsonObject jsonObject) {
        deserializeIntoObject(this, jsonObject);
    }

    public JsonObject toJson() {
        return serializeToJsonObject(this);
    }
}
