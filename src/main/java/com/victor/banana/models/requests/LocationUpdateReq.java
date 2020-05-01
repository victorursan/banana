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
public class LocationUpdateReq {
    private UUID id;
    private Optional<UUID> parentLocation = Optional.empty();
    private Optional<String> location = Optional.empty();
}
