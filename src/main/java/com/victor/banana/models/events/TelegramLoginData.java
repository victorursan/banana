package com.victor.banana.models.events;

import com.victor.banana.models.events.personnel.Personnel;
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
public class TelegramLoginData {
    @NotNull
    private Personnel personnel;
    @NotNull
    private Long chatId;
    @NotNull
    private String username;
    @Builder.Default
    private Optional<String> firstName = Optional.empty();
    @Builder.Default
    private Optional<String> lastName = Optional.empty();

    public TelegramLoginData(JsonObject jsonObject) {
        deserializeIntoObject(this, jsonObject);
    }

    public JsonObject toJson() {
        return serializeToJsonObject(this);
    }
}
