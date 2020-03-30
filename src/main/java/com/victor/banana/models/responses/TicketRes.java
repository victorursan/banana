package com.victor.banana.models.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@AllArgsConstructor
public class TicketRes {
    private String ticketId;
    private String message;
    private String state;
}
