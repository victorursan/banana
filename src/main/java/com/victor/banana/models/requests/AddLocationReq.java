package com.victor.banana.models.requests;

import lombok.*;
import lombok.NoArgsConstructor;

import java.util.UUID;

@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
public class AddLocationReq {
    private UUID parentLocation;
    private String location;
}
