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
    private Optional<Boolean> active = Optional.empty();
    private Optional<StickyUpdatesReq<ActionStickyReq, ActionUpdateReq>> actions = Optional.empty();
    private Optional<StickyUpdatesReq<LocationStickyReq, LocationUpdateReq>> locations = Optional.empty();
}
