package com.victor.banana.models.events.messages;

import com.victor.banana.models.events.tickets.TicketState;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.*;
import org.jetbrains.annotations.NotNull;import java.util.Optional;

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
    @NotNull
    private Long chatId;
    @NotNull
    private Long messageId;
    @NotNull
    private String text;
    @Builder.Default
    private Optional<TicketState> state = Optional.empty();

    public SendUpdateMessage(JsonObject jsonObject) {
        deserializeIntoObject(this, jsonObject);
    }

    public JsonObject toJson() {
        return serializeToJsonObject(this);
    }
}
