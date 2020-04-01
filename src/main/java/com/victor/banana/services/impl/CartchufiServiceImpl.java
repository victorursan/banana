package com.victor.banana.services.impl;

import com.victor.banana.actions.TicketAction;
import com.victor.banana.models.events.*;
import com.victor.banana.models.events.messages.*;
import com.victor.banana.models.events.stickies.*;
import com.victor.banana.models.events.tickets.Ticket;
import com.victor.banana.models.events.tickets.TicketState;
import com.victor.banana.services.CartchufiService;
import com.victor.banana.services.DatabaseService;
import com.victor.banana.services.TelegramBotService;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

import static io.vertx.core.Future.failedFuture;
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
                .actions(createSticky.getActions().stream().map(message ->
                        Action.builder()
                                .id(UUID.randomUUID().toString())
                                .message(message)
                                .build()
                ).collect(toList()))
                .locations(createSticky.getLocations().stream().map(text ->
                        Location.builder()
                                .id(UUID.randomUUID().toString())
                                .text(text)
                                .build()
                ).collect(toList()))
                .build();
        Future.<Boolean>future(c ->
                databaseService.addSticky(sticky, c)
        ).flatMap(e -> {
            if (e) {
                return Future.succeededFuture(sticky);
            } else {
                return Future.failedFuture("something went wrong");
            }
        }).onComplete(result);
    }

    @Override
    public final void getStickyLocation(String stickyLocation, Handler<AsyncResult<StickyLocation>> result) {
        Future.<StickyLocation>future(f -> databaseService.getStickyLocation(UUID.fromString(stickyLocation).toString(), f))
                .onComplete(result);
    }

    @Override
    public final void getTicket(String ticketId, Handler<AsyncResult<Ticket>> result) {
        Future.<Ticket>future(f -> databaseService.getTicket(UUID.fromString(ticketId).toString(), f))
                .onComplete(result);
    }

    @Override
    public final void actionSelected(ActionSelected actionSelected, Handler<AsyncResult<Ticket>> result) {
        Future.<StickyAction>future(t -> databaseService.getStickyAction(actionSelected, t))
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
                                            SendTicketMessage.builder()
                                                    .chatId(chatId)
                                                    .ticketId(ticket.getId())
                                                    .ticketMessage(ticket.getMessage())
                                                    .build())
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
                })
                .onComplete(result);
    }

    @Override
    public final void receivedMessageUpdate(RecvUpdateMessage updateMessage) {
        Future.<Ticket>future(ct -> databaseService.getTicketForMessage(updateMessage.getChatId(), updateMessage.getMessageId(), ct))
                .flatMap(tick ->
                        Future.<TelegramChannel>future(channel -> databaseService.getChat(updateMessage.getChatId(), channel))
                                .flatMap((TelegramChannel tc) -> {
                                    final var ticketActionOpt = TicketAction.computeFor(tick, updateMessage.getState(), tc);
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
                                                    return Future.failedFuture("Ticket was not updated");
                                                }).flatMap(updates -> Future.<List<SentUpdateMessage>>future(f -> botService.updateMessages(updates, f)));
                                    }
                                    return Future.failedFuture(String.format("Invalid Transition for [%s] + [%s] + [%s]", tick, updateMessage.getState(), tc)); //todo
                                }))
                .onFailure(t -> log.error(t.getMessage(), t));
    }

    @Override
    public final void receivedPersonnelMessage(RecvPersonnelMessage recvPersonnelMessage) {
        final var message = ChatMessage.builder()
                .chatId(recvPersonnelMessage.getChatId())
                .messageId(recvPersonnelMessage.getMessageId())
                .message(recvPersonnelMessage.getMessage())
                .build();
        final var personnel = Personnel.builder()
                .id(UUID.randomUUID().toString())
                .firstName(recvPersonnelMessage.getFirstName())
                .lastName(recvPersonnelMessage.getLastName())
                .build();
        final var chat = TelegramChannel.builder()
                .chatId(recvPersonnelMessage.getChatId())
                .personnelId(personnel.getId())
                .username(recvPersonnelMessage.getUsername())
                .build();
        Future.<Boolean>future(e -> databaseService.addPersonnel(personnel, e)) //todo doesn't make any sense
                .flatMap(personnelExist -> {
                    if (personnelExist) {
                        return Future.<Boolean>future(e -> databaseService.addChat(chat, e));
                    }
                    return failedFuture("didn't find row");
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

    private Future<List<Long>> chatsForTicket(Ticket t) {
        return Future.future(databaseService::getChats);
    }

}
