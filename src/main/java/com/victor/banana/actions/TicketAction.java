package com.victor.banana.actions;

import com.victor.banana.models.events.TelegramChannel;
import com.victor.banana.models.events.stickies.StickyAction;
import com.victor.banana.models.events.tickets.Ticket;
import com.victor.banana.models.events.tickets.TicketState;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static com.victor.banana.models.events.tickets.TicketState.PENDING;
import static java.util.Optional.empty;

public interface TicketAction {
    static Optional<TicketAction> computeFor(Ticket oldTicket, TicketState newState, TelegramChannel tc) {
        return switch (oldTicket.getState()) {
            case PENDING, SOLVED -> switch (newState) {
                case ACQUIRED -> Optional.of(ticketActionFor(oldTicket, Optional.of(tc.getChatId()), tc.getUsername(), newState, tc.getPersonnelId()));
                default -> empty();
            };
            case ACQUIRED -> switch (newState) {
                case PENDING, SOLVED -> Optional.of(ticketActionFor(oldTicket, Optional.of(tc.getChatId()), tc.getUsername(), newState, tc.getPersonnelId()));
                default -> empty();
            };
        };
    }

    static Ticket createTicket(StickyAction stickyAction) {
        return Ticket.builder()
                .id(UUID.randomUUID())
                .actionId(stickyAction.getActionId())
                .locationId(stickyAction.getLocationId())
                .message(String.format("%s | %s | %s", stickyAction.getFloor(), stickyAction.getLocation(), stickyAction.getActionMessage()))
                .state(PENDING)
                .build();
    }

    static TicketAction computeFor(Ticket oldTicket, TicketState newState, String username, UUID personnelId) {
        return ticketActionFor(oldTicket, empty(), username, newState, personnelId);
    }

    static TicketAction computeFor(Ticket oldTicket, Long chatId, String username) {
        final var message = messageForState(oldTicket, username);
        return ticketActionWith(oldTicket, message, Optional.of(chatId));
    }

    private static TicketAction ticketActionFor(Ticket oldTicket, Optional<Long> chatId, String username, TicketState newTicketState, UUID personnelId) {
        switch (newTicketState) {
            case PENDING -> {
                oldTicket.setOwnedBy(empty());
                oldTicket.setAcquiredAt(empty());
                oldTicket.setSolvedAt(empty());
            }
            case ACQUIRED -> {
                oldTicket.setOwnedBy(Optional.of(personnelId));
                oldTicket.setAcquiredAt(Optional.of(OffsetDateTime.now(ZoneOffset.UTC)));
                oldTicket.setSolvedAt(empty());
            }
            case SOLVED -> {
                oldTicket.setOwnedBy(Optional.of(personnelId));
                oldTicket.setAcquiredAt(Optional.of(oldTicket.getAcquiredAt().orElse(OffsetDateTime.now(ZoneOffset.UTC))));
                oldTicket.setSolvedAt(Optional.of(OffsetDateTime.now(ZoneOffset.UTC)));
            }
        }
        oldTicket.setState(newTicketState);
        final var message = messageForState(oldTicket, username);
        return ticketActionWith(oldTicket, message, chatId);
    }

    private static String messageForState(Ticket ticket, String username) {
        return switch (ticket.getState()) {
            case PENDING -> ticket.getMessage();
            case ACQUIRED -> String.format("%s | Acquired by %s", ticket.getMessage(), username);
            case SOLVED -> String.format("%s | Solved by %s", ticket.getMessage(), username);
        };
    }

    private static TicketAction ticketActionWith(Ticket ticket, String message, Optional<Long> ownerChat) {
        return new TicketAction() {

            @Override
            public String getMessage() {
                return message;
            }

            @Override
            public Ticket getTicket() {
                return ticket;
            }

            @Override
            public Optional<TicketState> getMessageStateForChat(Long chatId) {
                if (ticket.getState() == PENDING) {
                    return Optional.of(PENDING);
                }
                if (ownerChat.isPresent() && chatId.equals(ownerChat.get())) {
                    return Optional.of(ticket.getState());
                }
                return empty();
            }
        };
    }

    String getMessage();

    Ticket getTicket();

    Optional<TicketState> getMessageStateForChat(Long chatId);
}
