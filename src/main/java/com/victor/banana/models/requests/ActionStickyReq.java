package com.victor.banana.models.requests;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
public class ActionStickyReq {
    private String action;
    private List<UUID> roles;
}
