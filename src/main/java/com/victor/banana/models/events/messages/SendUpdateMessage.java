package com.victor.banana.models.events.messages;

import com.victor.banana.models.events.tickets.TicketState;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.*;

import java.util.Optional;

import static com.victor.banana.utils.SerdesUtils.deserializeIntoObject;
import static com.victor.banana.utils.SerdesUtils.serializeToJsonObject;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
@DataObject
public class SendUpdateMessage {
    private Long chatId;
    private Long messageId;
    private String text;
    private Optional<TicketState> state;

    public SendUpdateMessage(JsonObject jsonObject) {
        deserializeIntoObject(this, jsonObject);
    }

    public JsonObject toJson() {
        return serializeToJsonObject(this);
    }
}
