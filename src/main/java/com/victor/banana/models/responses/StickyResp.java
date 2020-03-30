package com.victor.banana.models.responses;

import lombok.*;

import java.util.List;

@Builder
@AllArgsConstructor
@Data
public class StickyResp {
    private String id;
    private String message;
    @Singular
    private List<ActionStickyResp> actions;
}
