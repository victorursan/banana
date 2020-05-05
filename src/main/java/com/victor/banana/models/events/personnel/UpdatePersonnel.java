package com.victor.banana.models.events.personnel;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.*;

import java.util.Optional;
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
public class UpdatePersonnel {
    private String firstName;
    private String lastName;
    private UUID locationId;
    private UUID roleId;

    public UpdatePersonnel(JsonObject jsonObject) {
        deserializeIntoObject(this, jsonObject);
    }

    public JsonObject toJson() {
        return serializeToJsonObject(this);
    }

    public Optional<String> getFirstName() {
        return Optional.ofNullable(firstName);
    }

    public Optional<String> getLastName() {
        return Optional.ofNullable(lastName);
    }

    public Optional<UUID> getLocationId() {
        return Optional.ofNullable(locationId);
    }

    public Optional<UUID> getRoleId() {
        return Optional.ofNullable(roleId);
    }
}
