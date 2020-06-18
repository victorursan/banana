package com.victor.banana.models.events.tickets;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

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
    @NotNull
    private UUID id;
    @NotNull
    private UUID actionId;
    @NotNull
    private UUID locationId;
    @NotNull
    private String message;
    @NotNull
    private TicketState state;
    @NotNull
    private OffsetDateTime createdAt;
    @Builder.Default
    private Optional<OffsetDateTime> acquiredAt = Optional.empty();
    @Builder.Default
    private Optional<OffsetDateTime> solvedAt = Optional.empty();
    @Builder.Default
    private Optional<UUID> ownedBy = Optional.empty();

    public Ticket(JsonObject jsonObject) {
        deserializeIntoObject(this, jsonObject);
    }

    public JsonObject toJson() {
        return serializeToJsonObject(this);
    }
}
