package com.victor.banana.models.requests;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Optional;
import java.util.UUID;

@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
public class ActionUpdateReq {
    private UUID id;
    private Optional<String> action = Optional.empty();
    private Optional<UUID> roleId = Optional.empty();
}
