package com.victor.banana.models.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.Optional;
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
    private OffsetDateTime createdAt;
    @Builder.Default
    private Optional<OffsetDateTime> acquiredAt = Optional.empty();
    @Builder.Default
    private Optional<OffsetDateTime> solvedAt = Optional.empty();
    @Builder.Default
    private Optional<UUID> ownedBy = Optional.empty();
}
