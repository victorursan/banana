package com.victor.banana.models.requests;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
public class UpdateTicketReq {
    private String newState;
}
