package com.victor.banana.models.requests;

import lombok.*;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
public class StickyUpdatesReq<Add, UPDATE> {
    private List<Add> add = List.of();
    private List<UPDATE> update = List.of();
    private List<UUID> activate = List.of();
    private List<UUID> remove = List.of();
}
