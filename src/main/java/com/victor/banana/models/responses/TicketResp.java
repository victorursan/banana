package com.victor.banana.models.responses;

import lombok.*;
import org.jetbrains.annotations.NotNull;import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Builder
@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class TicketResp {
    @NotNull
    private UUID ticketId;
    @NotNull
    private String message;
    @NotNull
    private String state;
    @NotNull
    private OffsetDateTime createdAt;
    @Builder.Default
    private Optional<OffsetDateTime> acquiredAt = Optional.empty();
    @Builder.Default
    private Optional<OffsetDateTime> solvedAt = Optional.empty();
    @Builder.Default
    private Optional<UUID> ownedBy = Optional.empty();
}
