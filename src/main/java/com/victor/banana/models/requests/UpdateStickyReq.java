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
    private Optional<String> title = Optional.empty();
    private Optional<Boolean> active = Optional.empty();
    private Optional<StickyUpdatesReq<AddActionStickyReq, ActionUpdateReq>> actions = Optional.empty();
    private Optional<StickyUpdatesReq<AddStickyLocationReq, StickyLocationUpdateReq>> locations = Optional.empty();
}
