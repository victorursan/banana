package com.victor.banana.models.requests;

import lombok.*;

import java.util.Optional;
import java.util.UUID;

@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
public class StickyLocationUpdateReq {
    private UUID id;
    private Optional<UUID> floorId = Optional.empty();
    private Optional<String> name = Optional.empty();
}
