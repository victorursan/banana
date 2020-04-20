package com.victor.banana.actions;

import com.victor.banana.models.events.TelegramChannel;
import com.victor.banana.models.events.messages.TicketMessageState;
import com.victor.banana.models.events.tickets.Ticket;
import com.victor.banana.models.events.tickets.TicketState;

import java.util.Optional;

import static java.util.Optional.empty;

public interface TicketAction {
    static Optional<TicketAction> computeFor(Ticket oldTicket, TicketMessageState messageState, TelegramChannel tc) {
        return switch (oldTicket.getState()) {
            case PENDING -> switch (messageState) {
                case ACQUIRED -> {
                    oldTicket.setState(TicketState.ACQUIRED);
                    oldTicket.setAcquiredBy(tc.getPersonnelId());
                    final var message = messageForState(oldTicket, tc.getUsername());
                    yield Optional.of(ticketActionWith(oldTicket, messageState, message, tc.getChatId()));
                }
                default -> empty();
            };
            case ACQUIRED -> switch (messageState) {
                case UN_ACQUIRED -> {
                    oldTicket.setState(TicketState.PENDING);
                    oldTicket.setAcquiredBy(null);
                    final var message = messageForState(oldTicket, tc.getUsername());
                    yield Optional.of(ticketActionWith(oldTicket, messageState, message, tc.getChatId()));
                }
                case SOLVED -> {
                    oldTicket.setState(TicketState.SOLVED);
                    oldTicket.setSolvedBy(tc.getPersonnelId());
                    final var message = messageForState(oldTicket, tc.getUsername());
                    yield Optional.of(ticketActionWith(oldTicket, messageState, message, tc.getChatId()));
                }
                default -> empty();
            };
            case SOLVED -> switch (messageState) {
                case UN_SOLVE -> {
                    oldTicket.setState(TicketState.ACQUIRED);
                    oldTicket.setSolvedBy(null);
                    final var message = messageForState(oldTicket, tc.getUsername());
                    yield Optional.of(ticketActionWith(oldTicket, messageState, message, tc.getChatId()));
                }
                default -> empty();
            };
        };
    }

    static TicketAction computeFor(Ticket oldTicket, Long chatId, String username) {
        final var message = messageForState(oldTicket, username);
        return switch (oldTicket.getState()) {
            case PENDING -> ticketActionWith(oldTicket, TicketMessageState.UN_ACQUIRED, message, chatId);
            case ACQUIRED -> ticketActionWith(oldTicket, TicketMessageState.ACQUIRED, message, chatId);
            case SOLVED -> ticketActionWith(oldTicket, TicketMessageState.SOLVED, message, chatId);
        };
    }

    private static String messageForState(Ticket ticket, String username) {
        return switch (ticket.getState()) {
            case PENDING -> ticket.getMessage();
            case ACQUIRED -> String.format("%s | Acquired by @%s", ticket.getMessage(), username);
            case SOLVED -> String.format("%s | Solved by @%s", ticket.getMessage(), username);
        };
    }

    private static TicketAction ticketActionWith(Ticket ticket, TicketMessageState ticketMessageState, String message, Long ownerChat) {
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
            public TicketMessageState getMessageStateForChat(Long chatId) {
                if (chatId.equals(ownerChat)) {
                    return ticketMessageState;
                }
                return TicketMessageState.NO_ACTION;
            }
        };
    }

    String getMessage();

    Ticket getTicket();

    TicketMessageState getMessageStateForChat(Long chatId);
}
