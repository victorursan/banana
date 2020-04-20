package com.victor.banana.models.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.*;

import java.util.UUID;

@Builder
@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class TicketResp {
    private UUID ticketId;
    private String message;
    private String state;
}
