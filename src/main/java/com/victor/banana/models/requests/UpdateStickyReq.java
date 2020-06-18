package com.victor.banana.models.requests;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Optional;

@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
public class UpdateStickyReq {
    private Optional<String> message = Optional.empty();
    private Optional<Boolean> active = Optional.empty();
    private Optional<StickyUpdatesReq<ActionStickyReq, ActionUpdateReq>> actions = Optional.empty();
    private Optional<StickyUpdatesReq<AddStickyLocationReq, StickyLocationUpdateReq>> locations = Optional.empty();
}
