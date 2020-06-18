package com.victor.banana.models.events.messages;

import com.victor.banana.models.events.tickets.TicketState;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.*;
import org.jetbrains.annotations.NotNull;import static com.victor.banana.utils.SerdesUtils.deserializeIntoObject;
import static com.victor.banana.utils.SerdesUtils.serializeToJsonObject;

@Builder
@AllArgsConstructor
@EqualsAndHashCode
@NoArgsConstructor
@Getter
@ToString
@DataObject
public class RecvUpdateMessage {
    @NotNull
    private Long chatId;
    @NotNull
    private Long messageId;
    @NotNull
    private TicketState state;

    public RecvUpdateMessage(JsonObject jsonObject) {
        deserializeIntoObject(this, jsonObject);
    }

    public JsonObject toJson() {
        return serializeToJsonObject(this);
    }
}
