package com.victor.banana.models.requests;


import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
public class AddStickyReq {
    private String message;
    private List<ActionStickyReq> actions;
    private List<AddStickyLocationReq> locations;
}
