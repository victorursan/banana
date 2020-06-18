package com.victor.banana.models.events;

import com.victor.banana.models.events.tickets.TicketState;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.*;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static com.victor.banana.utils.SerdesUtils.deserializeIntoObject;
import static com.victor.banana.utils.SerdesUtils.serializeToJsonObject;

@ToString
@EqualsAndHashCode
@AllArgsConstructor
@Builder
@NoArgsConstructor
@Getter
@DataObject
public class UpdateTicketState {
    @NotNull
    private UUID personnelId;
    @NotNull
    private TicketState newTicketState;

    public UpdateTicketState(JsonObject jsonObject) {
        deserializeIntoObject(this, jsonObject);
    }

    public JsonObject toJson() {
        return serializeToJsonObject(this);
    }

}
