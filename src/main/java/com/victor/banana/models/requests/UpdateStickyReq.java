package com.victor.banana.models.requests;

import lombok.*;
import lombok.NoArgsConstructor;

import java.util.Optional;

@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
public class UpdateStickyReq {
    private Optional<String> message = Optional.empty();
    private Optional<StickyUpdatesReq<ActionStickyReq>> actions = Optional.empty();
    private Optional<StickyUpdatesReq<LocationStickyReq>> locations = Optional.empty();
}
