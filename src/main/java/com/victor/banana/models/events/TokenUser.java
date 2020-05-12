package com.victor.banana.models.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.victor.banana.utils.SecurityUtils;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.*;

import java.util.UUID;

import static com.victor.banana.utils.SerdesUtils.deserializeIntoObject;
import static com.victor.banana.utils.SerdesUtils.serializeToJsonObject;

@Data
@NoArgsConstructor
@DataObject
@JsonIgnoreProperties(ignoreUnknown = true)
public class TokenUser {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String username;
    private SecurityUtils.Authority authority;

    @JsonSetter("sub")
    public void setId(UUID id) {
        this.id = id;
    }

    @JsonSetter("given_name")
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @JsonSetter("family_name")
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @JsonSetter("preferred_username")
    public void setUsername(String username) {
        this.username = username;
    }

    public TokenUser(JsonObject jsonObject) {
        deserializeIntoObject(this, jsonObject);
    }

    public JsonObject toJson() {
        return serializeToJsonObject(this);
    }
}
