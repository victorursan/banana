package com.victor.banana.models.events.personnel;

import com.victor.banana.utils.Constants.PersonnelRole;
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
public class Personnel {
    @NotNull
    private UUID id;
    @Builder.Default
    private Optional<PersonnelRole> role = Optional.empty();
    @Builder.Default
    private Optional<UUID> buildingId = Optional.empty();
    @Builder.Default
    private Optional<String> firstName = Optional.empty();
    @Builder.Default
    private Optional<String> lastName = Optional.empty();
    @Builder.Default
    private Optional<String> email = Optional.empty();
    @Builder.Default
    private Optional<String> telegramUsername = Optional.empty();
    @Builder.Default
    private Optional<Long> chatId = Optional.empty();

    public Personnel(JsonObject jsonObject) {
        deserializeIntoObject(this, jsonObject);
    }

    public JsonObject toJson() {
        return serializeToJsonObject(this);
    }

    public void setRole(PersonnelRole role) {
        this.role = Optional.of(role);
    }
}
