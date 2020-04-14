package com.victor.banana.services.impl;

import com.victor.banana.actions.TicketAction;
import com.victor.banana.models.events.ActionSelected;
import com.victor.banana.models.events.Personnel;
import com.victor.banana.models.events.TelegramChannel;
import com.victor.banana.models.events.UpdatePersonnel;
import com.victor.banana.models.events.locations.CreateLocation;
import com.victor.banana.models.events.locations.Location;
import com.victor.banana.models.events.messages.*;
import com.victor.banana.models.events.roles.CreateRole;
import com.victor.banana.models.events.roles.Role;
import com.victor.banana.models.events.stickies.*;
import com.victor.banana.models.events.tickets.Ticket;
import com.victor.banana.models.events.tickets.TicketState;
import com.victor.banana.services.CartchufiService;
import com.victor.banana.services.DatabaseService;
import com.victor.banana.services.TelegramBotService;
import com.victor.banana.utils.CallbackUtils;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static io.vertx.core.Future.*;
import static java.util.stream.Collectors.toList;

public class CartchufiServiceImpl implements CartchufiService {
    private final static Logger log = LoggerFactory.getLogger(CartchufiServiceImpl.class);
    private final TelegramBotService botService;
    private final DatabaseService databaseService;

    public CartchufiServiceImpl(TelegramBotService botService, DatabaseService databaseService) {
        this.botService = botService;
        this.databaseService = databaseService;
    }

    @Override
    public final void createSticky(CreateSticky createSticky, Handler<AsyncResult<Sticky>> result) {
        final var sticky = Sticky.builder()
                .id(UUID.randomUUID().toString())
                .message(createSticky.getMessage())
                .actions(createSticky.getActions().stream()
                        .map(action ->
                                Action.builder()
                                        .id(UUID.randomUUID().toString())
                                        .roleId(action.getRoleId())
                                        .message(action.getMessage())
                                        .build()
                        ).collect(toList()))
                .locations(createSticky.getLocations().stream()
                        .map(location ->
                                Location.builder()
                                        .id(UUID.randomUUID().toString())
                                        .parentLocation(location.getParentLocation())
                                        .text(location.getLocation())
                                        .build()
                        ).collect(toList()))
                .build();
        Future.<Boolean>future(c ->
                databaseService.addSticky(sticky, c)
        ).flatMap(e -> {
            if (e) {
                return succeededFuture(sticky);
            }
            return failedFuture("something went wrong");
        }).onComplete(result);
    }

    @Override
    public final void createLocation(CreateLocation createLocation, Handler<AsyncResult<Location>> result) {
        final var location = Location.builder()
                .id(UUID.randomUUID().toString())
                .parentLocation(createLocation.getParentLocation())
                .text(createLocation.getLocation())
                .build();
        Future.<Boolean>future(f -> databaseService.addLocation(location, f))
                .flatMap(b -> {
                    if (b) {
                        return succeededFuture(location);
                    }
                    return failedFuture("something went wrong");
                }).onComplete(result);
    }

    @Override
    public final void createRole(CreateRole createRole, Handler<AsyncResult<Role>> result) {
        final var role = Role.builder()
                .id(UUID.randomUUID().toString())
                .type(createRole.getType())
                .build();
        Future.<Boolean>future(f -> databaseService.addRole(role, f))
                .flatMap(b -> {
                    if (b) {
                        return succeededFuture(role);
                    }
                    return failedFuture("something went wrong");
                }).onComplete(result);
    }

    @Override
    public final void getStickyLocation(String stickyLocation, Handler<AsyncResult<StickyLocation>> result) {
        Future.<StickyLocation>future(f -> databaseService.getStickyLocation(stickyLocation, f))
                .onComplete(result);
    }

    @Override
    public final void getLocations(Handler<AsyncResult<List<Location>>> result) {
        future(databaseService::getLocations).onComplete(result);
    }

    @Override
    public final void getRoles(Handler<AsyncResult<List<Role>>> result) {
        future(databaseService::getRoles).onComplete(result);
    }

    @Override
    public final void getTicket(String ticketId, Handler<AsyncResult<Ticket>> result) {
        Future.<Ticket>future(f -> databaseService.getTicket(ticketId, f))
                .onComplete(result);
    }

    @Override
    public final void getTickets(Handler<AsyncResult<List<Ticket>>> result) {
        Future.future(databaseService::getTickets)
                .onComplete(result);
    }

    @Override
    public final void requestPersonnelTicketsInState(Long chatId, TicketState state) {
        final Future<List<Ticket>> ticketsF = switch (state) {
            case ACQUIRED -> future(f -> databaseService.getTicketsInStateForChat(chatId, state, f));
            case PENDING -> future(f -> databaseService.ticketsViableForChat(chatId, state, f));
            default -> failedFuture("state not supported for this acction");
        };
        ticketsF.flatMap((List<Ticket> tickets) -> {
            if (tickets.isEmpty()) {
                return succeededFuture(List.of());
            }
            return Future.<TelegramChannel>future(tc -> databaseService.getChat(chatId, tc))
                    .map(tc -> tickets.stream().map(ticket -> {
                        final var ticketAction = TicketAction.computeFor(ticket, chatId, tc.getUsername());
                        return fromTicket(ticket.getId(), ticketAction.getMessage(), ticketAction.getMessageStateForChat(chatId), chatId);
                    }).collect(toList()))
                    .flatMap(sendMessages -> Future.<List<SentTicketMessage>>future(f -> botService.sendMessages(sendMessages, f)))
                    .onSuccess(rcvTickets -> Future.<Boolean>future(f -> databaseService.addTicketsMessage(rcvTickets, f)))
                    .onFailure(t -> log.error("Something went wrong while saving the ticket messages", t));
        });
    }

    private SendTicketMessage fromTicket(String ticketId, String message, TicketMessageState state, Long chatId) {
        return SendTicketMessage.builder()
                .chatId(chatId)
                .ticketId(ticketId)
                .ticketMessage(message)
                .ticketMessageState(state)
                .build();
    }

    @Override
    public final void checkIn(Long chatId) {
        Future.<Boolean>future(f -> databaseService.setCheckedIn(chatId, true, f))
                .onSuccess(b -> {
                    if (b) {
                        requestPersonnelTicketsInState(chatId, TicketState.PENDING);
                    } else {
                        log.error("Failed to check in :(");
                    }

                }).onFailure(t -> log.error("", t.getCause()));
    }

    @Override
    public final void checkOut(Long chatId) {
        Future.<Boolean>future(f -> databaseService.setCheckedIn(chatId, false, f))
                .flatMap(b -> {
                    if (b) {
                        return Future.<List<Ticket>>future(f -> databaseService.getTicketsInStateForChat(chatId, TicketState.ACQUIRED, f))
                                .flatMap(tickets -> Future.<TelegramChannel>future(channel -> databaseService.getChat(chatId, channel))
                                        .flatMap((TelegramChannel tc) -> {
                                            final var sentTickets = tickets.stream().map(ticket -> transitionTicket(TicketMessageState.UN_ACQUIRED, ticket, tc)).collect(toList());
                                            return CallbackUtils.mergeFutures(sentTickets).map(l -> l.stream().flatMap(List::stream).collect(toList()));
                                        }));
                    } else {
                        return failedFuture("Failed to check in :(");
                    }
                }).onFailure(t -> log.error(t.getMessage(), t.getCause()));
    }


    @Override
    public final void actionSelected(ActionSelected actionSelected, Handler<AsyncResult<Ticket>> result) {
        Future.<Ticket>future(f -> databaseService.getActiveTicketForActionSelected(actionSelected, f))
                .recover(noTicket -> {
                    log.debug(String.format("no ticket found for actionSelected: %s", actionSelected));
                    return Future.<StickyAction>future(t -> databaseService.getStickyAction(actionSelected, t))
                            .flatMap(stickyAction -> {
                                final var ticket = Ticket.builder()
                                        .id(UUID.randomUUID().toString())
                                        .actionId(stickyAction.getActionId())
                                        .locationId(stickyAction.getLocationId())
                                        .message(String.format("%s | %s | %s", stickyAction.getStickyMessage(), stickyAction.getActionMessage(), stickyAction.getLocation()))
                                        .state(TicketState.PENDING)
                                        .build();
                                final var affectedChatsF = chatsForTicket(ticket);
                                final var ticketF = Future.<Boolean>future(t -> databaseService.addTicket(ticket, t));
                                CompositeFuture.join(affectedChatsF, ticketF)
                                        .onSuccess(s -> {
                                            if (!affectedChatsF.result().isEmpty()) {
                                                final var sendMessages = affectedChatsF.result().stream().map(chatId ->
                                                        fromTicket(ticket.getId(), ticket.getMessage(), TicketMessageState.UN_ACQUIRED, chatId))
                                                        .collect(toList());
                                                Future.<List<SentTicketMessage>>future(f -> botService.sendMessages(sendMessages, f))
                                                        .flatMap(rcvTickets -> Future.<Boolean>future(f -> databaseService.addTicketsMessage(rcvTickets, f)))
                                                        .onFailure(t -> log.error("Something went wrong while saving the ticket messages", t));
                                            } else {
                                                log.error(String.format("No personnel was found for this ticket: %s", ticket.toJson().toString()));
                                            }
                                        })
                                        .onFailure(t -> log.error(t.getMessage(), t));
                                return ticketF.map(ticket);
                            });
                })
                .onComplete(result);
    }

    @Override
    public final void receivedMessageUpdate(RecvUpdateMessage updateMessage) {
        Future.<Ticket>future(ct -> databaseService.getTicketForMessage(updateMessage.getChatId(), updateMessage.getMessageId(), ct))
                .flatMap(tick ->
                        Future.<TelegramChannel>future(channel -> databaseService.getChat(updateMessage.getChatId(), channel))
                                .flatMap(tc -> transitionTicket(updateMessage.getState(), tick, tc)))
                .onFailure(t -> log.error(t.getMessage(), t));
    }

    @Override
    public final void updatePersonnel(String personnelId, UpdatePersonnel updatePersonnel, Handler<AsyncResult<Personnel>> result) {
        Future.<Personnel>future(f -> databaseService.getPersonnel(personnelId, f))
                .flatMap(p -> {
                    final var pers = Personnel.builder()
                            .id(personnelId)
                            .firstName(Optional.ofNullable(updatePersonnel.getFirstName()).orElse(p.getFirstName()))
                            .lastName(Optional.ofNullable(updatePersonnel.getLastName()).orElse(p.getLastName()))
                            .roleId(Optional.ofNullable(updatePersonnel.getRoleId()).orElse(p.getRoleId()))
                            .locationId(Optional.ofNullable(updatePersonnel.getLocationId()).orElse(p.getLocationId()))
                            .build();
                    return Future.<Boolean>future(ft -> databaseService.updatePersonnel(pers, ft))
                            .flatMap(e -> {
                                if (e) {
                                    return succeededFuture(pers);
                                }
                                return failedFuture("failed to update the personnel");
                            });
                }).onComplete(result);
    }

    @Override
    public final void getPersonnel(String personnelId, Handler<AsyncResult<Personnel>> result) {
        Future.<Personnel>future(f -> databaseService.getPersonnel(personnelId, f))
                .onComplete(result);
    }

    private Future<List<SentUpdateMessage>> transitionTicket(TicketMessageState stateTransition, Ticket tick, TelegramChannel tc) {
        final var ticketActionOpt = TicketAction.computeFor(tick, stateTransition, tc);
        if (ticketActionOpt.isPresent()) {
            final var ticketAction = ticketActionOpt.get();
            return Future.<Boolean>future(t -> databaseService.updateTicket(ticketAction.getTicket(), t))
                    .flatMap(didUpdate -> {
                        if (didUpdate) {
                            return Future.<List<ChatTicketMessage>>future(i -> databaseService.getTicketMessageForTicket(tick.getId(), i))
                                    .map(l -> l.stream()
                                            .map(c -> SendUpdateMessage.builder()
                                                    .chatId(c.getChatId())
                                                    .messageId(c.getMessageId())
                                                    .state(ticketAction.getMessageStateForChat(c.getChatId()))
                                                    .text(ticketAction.getMessage())
                                                    .build())
                                            .collect(toList()));
                        }
                        return failedFuture("Ticket was not updated");
                    }).flatMap(updates -> future(f -> botService.updateMessages(updates, f)));
        }
        return failedFuture(String.format("Invalid Transition for [%s] + [%s] + [%s]", tick, stateTransition, tc)); //todo
    }

    @Override
    public final void receivedPersonnelMessage(RecvPersonnelMessage recvPersonnelMessage) {
        final var message = ChatMessage.builder()
                .chatId(recvPersonnelMessage.getChatId())
                .messageId(recvPersonnelMessage.getMessageId())
                .message(recvPersonnelMessage.getMessage())
                .build();

        Future.<TelegramChannel>future((e -> databaseService.getChat(recvPersonnelMessage.getChatId(), e)))
                .map(true)
                .recover(t -> {
                    final var personnel = Personnel.builder()
                            .id(UUID.randomUUID().toString())
                            .firstName(recvPersonnelMessage.getFirstName())
                            .lastName(recvPersonnelMessage.getLastName())
                            .locationId("929abc9f-f34f-4a44-9928-863d9dfbe705")
                            .roleId("56841b70-d343-445f-b4a7-c0b10ea4e0f6")
                            .build();
                    final var chat = TelegramChannel.builder()
                            .chatId(recvPersonnelMessage.getChatId())
                            .personnelId(personnel.getId())
                            .username(recvPersonnelMessage.getUsername())
                            .build();

                    return Future.<Boolean>future(e -> databaseService.addPersonnel(personnel, e))
                            .flatMap(personnelExist -> {
                                if (personnelExist) {
                                    return Future.<Boolean>future(e -> databaseService.addChat(chat, e));
                                }
                                return failedFuture("didn't find row");
                            });
                })
                .flatMap(chatExists -> {
                    if (chatExists) {
                        return Future.<Boolean>future(c -> databaseService.addMessage(message, c)).map(message);
                    }
                    return failedFuture("problem adding the chat");
                })
                .onFailure(t -> log.error("An error occurred: ", t))
                .onSuccess(m -> log.info("Added message: " + m.toString()));
    }

    @Override
    public final void deleteSticky(String stickyId, Handler<AsyncResult<Boolean>> result) {
        Future.<Boolean>future(f -> databaseService.deactivateSticky(stickyId, f))
                .onComplete(result);
    }

    @Override
    public final void deleteRole(String roleId, Handler<AsyncResult<Boolean>> result) {
        Future.<Boolean>future(f -> databaseService.deactivateRole(roleId, f))
                .onComplete(result);
    }

    @Override
    public final void deleteLocation(String locationId, Handler<AsyncResult<Boolean>> result) {
        Future.<Boolean>future(f -> databaseService.deactivateLocation(locationId, f))
                .onComplete(result);
    }

    private Future<List<Long>> chatsForTicket(Ticket t) {
        return future(f -> databaseService.getChats(t, f));
    }

}
