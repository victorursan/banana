package com.victor.banana.services.impl;

import com.victor.banana.actions.TicketAction;
import com.victor.banana.models.events.*;
import com.victor.banana.models.events.locations.*;
import com.victor.banana.models.events.messages.*;
import com.victor.banana.models.events.personnel.Personnel;
import com.victor.banana.models.events.personnel.PersonnelFilter;
import com.victor.banana.models.events.personnel.UpdatePersonnel;
import com.victor.banana.models.events.stickies.*;
import com.victor.banana.models.events.tickets.Ticket;
import com.victor.banana.models.events.tickets.TicketFilter;
import com.victor.banana.models.events.tickets.TicketState;
import com.victor.banana.services.TicketingService;
import com.victor.banana.services.DatabaseService;
import com.victor.banana.services.KeycloakClientService;
import com.victor.banana.services.TelegramBotService;
import com.victor.banana.utils.CallbackUtils;
import com.victor.banana.utils.Constants.PersonnelRole;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.serviceproxy.ServiceException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

import static com.victor.banana.models.events.tickets.TicketState.PENDING;
import static com.victor.banana.utils.CallbackUtils.mergeFutures;
import static com.victor.banana.utils.CreateMappers.*;
import static com.victor.banana.utils.ExceptionUtils.FAILED_NO_ELEMENT_FOUND;
import static com.victor.banana.utils.MappersHelper.*;
import static io.vertx.core.Future.*;
import static java.util.stream.Collectors.toList;

public class TicketingServiceImpl implements TicketingService {
    private final static Logger log = LoggerFactory.getLogger(TicketingServiceImpl.class);

    private final TelegramBotService botService;
    private final DatabaseService databaseService;

    public TicketingServiceImpl(TelegramBotService botService, DatabaseService databaseService) {
        this.botService = botService;
        this.databaseService = databaseService;
    }

    @Override
    public final void createSticky(CreateSticky createSticky, Handler<AsyncResult<Sticky>> result) {
        Future.<Sticky>future(c -> databaseService.addSticky(createStickyToSticky().apply(createSticky), c))
                .onComplete(result);
    }

    @Override
    public final void getScanSticky(String stickyLocation, Handler<AsyncResult<ScanSticky>> result) {
        Future.<ScanSticky>future(f -> databaseService.getScanSticky(stickyLocation, f))
                .onComplete(result);
    }

    @Override
    public final void getTicket(String ticketId, Handler<AsyncResult<Ticket>> result) {
        Future.<Ticket>future(f -> databaseService.getTicket(ticketId, f))
                .onComplete(result);
    }

    @Override
    public final void getTickets(TicketFilter filter, Handler<AsyncResult<List<Ticket>>> result) {
        Future.<List<Ticket>>future(f -> databaseService.getTickets(filter, f))
                .onComplete(result);
    }

    @Override
    public final void requestPersonnelTicketsInState(Long chatId, TicketState state) {
        Future.<List<Ticket>>future(f -> databaseService.getTicketsInStateForChat(chatId, state, f))
                .flatMap(tickets -> {
                    if (tickets.isEmpty()) {
                        return succeededFuture(List.of());
                    }
                    return Future.<TelegramChannel>future(tc -> databaseService.getChat(chatId, tc))
                            .map(tc -> mapTs((Ticket ticket) -> {
                                final var ticketAction = TicketAction.computeFor(ticket, chatId, tc.getUsername());
                                return createTicketFrom(ticket.getId(), ticketAction.getMessage(), ticketAction.getMessageStateForChat(chatId), chatId);
                            }).apply(tickets))
                            .flatMap(sendMessages -> Future.<List<SentTicketMessage>>future(f -> botService.sendMessages(sendMessages, f)))
                            .onSuccess(rcvTickets -> Future.<List<SentTicketMessage>>future(f -> databaseService.addTicketsMessage(rcvTickets, f))) //todo
                            .onFailure(t -> log.error("Something went wrong while saving the ticket messages", t));
                });
    }

    @Override
    public final void checkIn(Long chatId) {
        Future.<Void>future(f -> databaseService.setCheckedIn(chatId, true, f))
                .onSuccess(ignore -> requestPersonnelTicketsInState(chatId, PENDING))
                .onFailure(t -> log.error("Something went wrong while checking in", t.getCause()));
    }

    @Override
    public final void checkOut(Long chatId) {
        Future.<Void>future(f -> databaseService.setCheckedIn(chatId, false, f))
                .flatMap(ignore -> Future.<List<Ticket>>future(f -> databaseService.getTicketsInStateForChat(chatId, TicketState.ACQUIRED, f))
                        .flatMap(tickets -> Future.<TelegramChannel>future(channel -> databaseService.getChat(chatId, channel))
                                .flatMap(tc -> flatMapTsF((Ticket ticket) -> transitionTicket(ticket, PENDING, tc)).apply(tickets)))
                ).onFailure(t -> log.error(t.getMessage(), t.getCause()));
    }


    @Override
    public final void actionSelected(Personnel personnel, ActionSelected actionSelected, Handler<AsyncResult<Ticket>> result) {
        Future.<Ticket>future(f -> databaseService.getActiveTicketForActionSelected(actionSelected, f))
                .recover(error -> {
                    if (error instanceof ServiceException) {
                        return switch (((ServiceException) error).failureCode()) {
                            case FAILED_NO_ELEMENT_FOUND -> createTicketFor(personnel, actionSelected);
                            default -> failedFuture(error);
                        };
                    }
                    return failedFuture(error);
                })
                .onComplete(result);
    }

    private Future<Ticket> createTicketFor(Personnel personnel, ActionSelected actionSelected) {
        log.debug(String.format("no ticket found for actionSelected: %s", actionSelected));
        return Future.<StickyAction>future(t -> databaseService.getStickyAction(actionSelected, t))
                .flatMap(stickyAction -> {
                    final var ticket = TicketAction.createTicket(stickyAction); //todo this should return CreateTicket because of createAt
                    final var ticketF = Future.<Ticket>future(t -> databaseService.addTicket(ticket, t))
                            .onSuccess(t -> addTicketCreatedByNotification(personnel.getId(), t.getId()));
                    final var affectedChatsF = chatsForTicket(ticket);
                    mergeFutures(affectedChatsF, ticketF)
                            .onComplete(e -> {
                                if (e.succeeded()) {
                                    if (!affectedChatsF.result().isEmpty()) {
                                        sendTicketMessage(affectedChatsF.result(), ticket);
                                    } else {
                                        log.error(String.format("No personnel was found for this ticket: %s", ticket.toJson().toString()));
                                    }
                                } else {
                                    log.error("something went wrong", e.cause());
                                }
                            });
                    return ticketF;
                });
    }

    private void addTicketCreatedByNotification(UUID personnelId, UUID ticketId) {
        databaseService.addTicketNotification(createTicketNotification(personnelId, ticketId), t -> {
            if (t.failed()) {
                log.error("Failed to add ticket notification", t.cause());
            }
        });
    }

    private Future<Void> sendTicketMessage(List<Long> chats, Ticket ticket) {
        final var sendMessages = mapTs((Long chatId) -> createTicketFrom(ticket.getId(), ticket.getMessage(), Optional.of(PENDING), chatId)).apply(chats);
        return Future.<List<SentTicketMessage>>future(f -> botService.sendMessages(sendMessages, f))
                .flatMap(rcvTickets -> Future.<List<SentTicketMessage>>future(f -> databaseService.addTicketsMessage(rcvTickets, f)))
                .mapEmpty();
    }

    private Future<List<SentDeleteMessage>> sendDeleteMessage(List<SendDeleteMessage> messages) {
        return Future.<List<SentDeleteMessage>>future(f -> botService.deleteMessages(messages, f))
                .onSuccess(rcvTickets -> Future.<List<SentDeleteMessage>>future(f -> databaseService.hideTicketsMessage(rcvTickets, f))
                        .onFailure(t -> log.error("Something went wrong while saving deleted ticket status", t)))
                .onFailure(t -> log.error("Something went wrong while deleting the ticket messages", t));
    }

    @Override
    public final void receivedMessageUpdate(RecvUpdateMessage updateMessage) {
        Future.<Ticket>future(ct -> databaseService.getTicketForMessage(updateMessage.getChatId(), updateMessage.getMessageId(), ct))
                .flatMap(tick -> Future.<TelegramChannel>future(channel -> databaseService.getChat(updateMessage.getChatId(), channel))
                        .flatMap(tc -> transitionTicket(tick, updateMessage.getState(), tc)))
                .onFailure(t -> log.error(t.getMessage(), t));
    }

    @Override
    public final void updateTicketState(String ticketId, UpdateTicketState updateTicketState, Handler<AsyncResult<Ticket>> result) {
        Future.<Ticket>future(f -> databaseService.getTicket(ticketId, f))
                .flatMap(ticket -> {
                    final var ticketAction = TicketAction.computeFor(ticket, updateTicketState.getNewTicketState(), "Community Team", updateTicketState.getPersonnelId()); //todo figure out username
                    processTicketAction(ticketAction).onFailure(t -> log.error(t.getMessage(), t));
                    return updateTicket(ticketAction.getTicket());
                })
                .onComplete(result);
    }

    private Future<Void> transitionTicket(Ticket ticket, TicketState newTicketState, TelegramChannel tc) {
        final var ticketActionOpt = TicketAction.computeFor(ticket, newTicketState, tc);
        return ticketActionOpt.map(this::processTicketAction)
                .orElseGet(() -> {
                    log.error(String.format("Invalid Transition for [%s] + [%s] + [%s]", ticket, newTicketState, tc));
                    return processTicketAction(TicketAction.computeFor(ticket, tc.getChatId(), tc.getUsername()));
                });
    }

    private Future<Void> processTicketAction(TicketAction ticketAction) {
        return updateTicket(ticketAction.getTicket())
                .flatMap(ticket -> {
                    final var ticketMessages = Future.<List<ChatTicketMessage>>future(i -> databaseService.getTicketMessageForTicket(ticket.getId().toString(), i));
                    return switch (ticketAction.getTicket().getState()) {
                        case PENDING -> ticketMessages.flatMap(mapTsF(chatTicketMessageToSendDeleteMessage()))
                                .onSuccess(deleteMsg -> sendDeleteMessage(deleteMsg)
                                        .onSuccess(sentMsg -> {
                                            final var crossMsg = sentMsg.stream()
                                                    .filter(Predicate.not(SentDeleteMessage::getWasDeleted))
                                                    .map(sdm -> createSendUpdateMessage(ticketAction, sdm)).collect(toList());
                                            Future.<List<SentUpdateMessage>>future(f -> botService.updateMessages(crossMsg, f))
                                                    .onFailure(t -> log.error("Failed to cross message", t));
                                        }))
                                .flatMap(ignore -> chatsForTicket(ticketAction.getTicket()))
                                .flatMap(chats -> sendTicketMessage(chats, ticketAction.getTicket()));
                        case ACQUIRED, SOLVED -> ticketMessages.flatMap(mapTsF(chatTicketMessageToSendUpdateMessage(ticketAction)))
                                .flatMap(updates -> Future.<List<SentUpdateMessage>>future(f -> botService.updateMessages(updates, f)))
                                .mapEmpty();
                    };
                });
    }

    private Future<Ticket> updateTicket(Ticket ticket) {
        return Future.future(t -> databaseService.updateTicket(ticket, t));
    }

    @Override
    public final void updateSticky(String stickyIdS, UpdateSticky update, Handler<AsyncResult<Sticky>> result) {
        getSticky(stickyIdS).flatMap(s -> {
            final var stickyTitleF = updateStickyTitle(s, update.getActive(), update.getTitle());
            final var stickyActionsF = updateStickyActions(update, s);
            final var stickyLocationsF = updateStickyLocations(update, s);
            return mergeFutures(stickyTitleF, stickyActionsF, stickyLocationsF)
                    .flatMap(ignore -> getSticky(s.getId().toString()));
        })
                .onComplete(result);
    }

    private Future<Void> updateStickyTitle(Sticky sticky, Optional<Boolean> status, Optional<String> title) {
        if (status.isPresent() || title.isPresent()) {
            return Future.<StickyTitle>future(f ->
                    databaseService.updateStickyTitle(createStickyTitle(sticky, status, title), f))
                    .mapEmpty();
        }
        return Future.succeededFuture();
    }

    private Future<Void> updateStickyLocations(UpdateSticky update, Sticky s) {
        return update.getLocations()
                .map(usl ->
                        Future.<Void>future(f ->
                                databaseService.updateStickyLocation(createUpdateStickyLocation(s, usl), f)))
                .orElse(succeededFuture());
    }


    private Future<Void> updateStickyActions(UpdateSticky update, Sticky s) {
        return update.getActions()
                .map(usa ->
                        Future.<Void>future(f ->
                                databaseService.updateStickyActions(createUpdateStickyAction(s, usa), f)))
                .orElse(succeededFuture());
    }

    @Override
    public final void getStickies(Handler<AsyncResult<List<Sticky>>> result) {
        future(databaseService::getStickies)
                .onComplete(result);
    }

    @Override
    public final void getSticky(String stickyId, Handler<AsyncResult<Sticky>> result) {
        getSticky(stickyId).onComplete(result);
    }

    private Future<Sticky> getSticky(String stickyId) {
        return future(f -> databaseService.getSticky(stickyId, f));
    }

    private Future<List<Long>> chatsForTicket(Ticket t) {
        return future(f -> databaseService.getChats(t, f));
    }

}
