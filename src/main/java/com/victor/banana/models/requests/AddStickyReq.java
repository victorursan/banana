package com.victor.banana.models.requests;


import lombok.*;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
public class AddStickyReq {
    private String message;
    private List<ActionStickyReq> actions;
    private List<AddLocationReq> locations;
}
