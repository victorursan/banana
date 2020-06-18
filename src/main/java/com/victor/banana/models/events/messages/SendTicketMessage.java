package com.victor.banana.models.events.messages;

import com.victor.banana.models.events.tickets.TicketState;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.*;
import org.jetbrains.annotations.NotNull;import java.util.Optional;
import java.util.UUID;

import static com.victor.banana.utils.SerdesUtils.deserializeIntoObject;
import static com.victor.banana.utils.SerdesUtils.serializeToJsonObject;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
@DataObject
public class SendTicketMessage {
    @NotNull
    private Long chatId;
    @NotNull
    private UUID ticketId;
    @NotNull
    private String ticketMessage;
    @Builder.Default
    private Optional<TicketState> ticketState = Optional.empty();

    public SendTicketMessage(JsonObject jsonObject) {
        deserializeIntoObject(this, jsonObject);
    }

    public JsonObject toJson() {
        return serializeToJsonObject(this);
    }
}
