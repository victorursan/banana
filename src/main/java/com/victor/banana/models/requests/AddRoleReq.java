package com.victor.banana.models.requests;

import lombok.*;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
public class AddRoleReq {
    private String type;
}
