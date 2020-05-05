package com.victor.banana.services.impl;

import com.victor.banana.actions.TicketAction;
import com.victor.banana.models.events.ActionSelected;
import com.victor.banana.models.events.TelegramChannel;
import com.victor.banana.models.events.UpdateTicketState;
import com.victor.banana.models.events.locations.CreateLocation;
import com.victor.banana.models.events.locations.Location;
import com.victor.banana.models.events.messages.*;
import com.victor.banana.models.events.personnel.Personnel;
import com.victor.banana.models.events.personnel.PersonnelFilter;
import com.victor.banana.models.events.personnel.UpdatePersonnel;
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
import java.util.function.Function;

import static com.victor.banana.models.events.tickets.TicketState.PENDING;
import static com.victor.banana.utils.Constants.DBConstants.NO_LOCATION;
import static com.victor.banana.utils.Constants.DBConstants.NO_ROLE;
import static io.vertx.core.Future.*;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;

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
                .id(UUID.randomUUID())
                .message(createSticky.getMessage())
                .actions(createActionsToActions(createSticky.getActions()))
                .locations(createLocationsToLocations(createSticky.getLocations()))
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

    private <F, T> List<T> mapElements(List<F> elements, Function<F, T> mapper) {
        return elements.stream().map(mapper).collect(toList());
    }

    private List<Action> createActionsToActions(List<CreateAction> createActions) {
        return mapElements(createActions, this::createActionToAction);
    }

    private List<Location> createLocationsToLocations(List<CreateLocation> createLocations) {
        return mapElements(createLocations, this::createLocationToLocation);
    }

    private Location createLocationToLocation(CreateLocation location) {
        return Location.builder()
                .id(UUID.randomUUID())
                .parentLocation(location.getParentLocation())
                .text(location.getLocation())
                .build();
    }

    private Action createActionToAction(CreateAction createAction) {
        return Action.builder()
                .id(UUID.randomUUID())
                .roleId(createAction.getRoleId())
                .message(createAction.getMessage())
                .build();
    }

    @Override
    public final void createLocation(CreateLocation createLocation, Handler<AsyncResult<Location>> result) {
        final var location = createLocationToLocation(createLocation);
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
                .id(UUID.randomUUID())
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
        future(databaseService::getTickets)
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

    private SendTicketMessage fromTicket(UUID ticketId, String message, Optional<TicketState> state, Long chatId) {
        return SendTicketMessage.builder()
                .chatId(chatId)
                .ticketId(ticketId)
                .ticketMessage(message)
                .ticketState(state)
                .build();
    }

    @Override
    public final void checkIn(Long chatId) {
        Future.<Boolean>future(f -> databaseService.setCheckedIn(chatId, true, f))
                .onSuccess(b -> {
                    if (b) {
                        requestPersonnelTicketsInState(chatId, PENDING);
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
                                            final var sentTickets = tickets.stream().map(ticket -> transitionTicket(ticket, PENDING, tc)).collect(toList());
                                            return CallbackUtils.mergeFutures(sentTickets).map(l -> l.stream().flatMap(List::stream).collect(toList()));
                                        }));
                    } else {
                        return failedFuture("Failed to check out :(");
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
                                        .id(UUID.randomUUID())
                                        .actionId(stickyAction.getActionId())
                                        .locationId(stickyAction.getLocationId())
                                        .message(String.format("%s | %s | %s | %s", stickyAction.getParentLocation(), stickyAction.getLocation(), stickyAction.getStickyMessage(), stickyAction.getActionMessage()))
                                        .state(PENDING)
                                        .build();
                                final var affectedChatsF = chatsForTicket(ticket);
                                final var ticketF = Future.<Boolean>future(t -> databaseService.addTicket(ticket, t));
                                CompositeFuture.join(affectedChatsF, ticketF)
                                        .onSuccess(s -> {
                                            if (!affectedChatsF.result().isEmpty()) {
                                                final var sendMessages = affectedChatsF.result().stream().map(chatId ->
                                                        fromTicket(ticket.getId(), ticket.getMessage(), Optional.of(PENDING), chatId))
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
                                .flatMap(tc -> transitionTicket(tick, updateMessage.getState(), tc)))
                .onFailure(t -> log.error(t.getMessage(), t));
    }

    @Override
    public final void updateTicketState(String ticketId, UpdateTicketState updateTicketState, Handler<AsyncResult<Ticket>> result) {
        Future.<Ticket>future(f -> databaseService.getTicket(ticketId, f))
                .flatMap(ticket -> {
                    final var ticketAction = TicketAction.computeFor(ticket, updateTicketState.getNewTicketState(), "Community Team", updateTicketState.getPersonnelId()); //todo figure out username
                    processTicketAction(ticketAction).onFailure(t -> log.error(t.getMessage(), t));

                    return Future.<Boolean>future(f -> databaseService.updateTicket(ticketAction.getTicket(), f))
                            .flatMap(s -> {
                                if (s) {
                                    return succeededFuture(ticket);
                                }
                                return failedFuture("Failed to update ticket");
                            });
                })
                .onComplete(result);

    }

    @Override
    public final void updatePersonnel(String personnelId, UpdatePersonnel updatePersonnel, Handler<AsyncResult<Personnel>> result) {
        Future.<Personnel>future(f -> databaseService.getPersonnel(personnelId, f))
                .flatMap(p -> {
                    final var pers = Personnel.builder()
                            .id(UUID.fromString(personnelId))
                            .firstName(updatePersonnel.getFirstName().orElse(p.getFirstName()))
                            .lastName(updatePersonnel.getLastName().orElse(p.getLastName()))
                            .roleId(updatePersonnel.getRoleId().orElse(p.getRoleId()))
                            .locationId(updatePersonnel.getLocationId().orElse(p.getLocationId()))
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

    @Override
    public final void findPersonnel(PersonnelFilter filter, Handler<AsyncResult<List<Personnel>>> result) {
        Future.<List<Personnel>>future(f -> databaseService.findPersonnelWithUsername(filter, f))
                .onComplete(result);
    }

    private Future<List<SentUpdateMessage>> transitionTicket(Ticket ticket, TicketState newTicketState, TelegramChannel tc) {
        final var ticketActionOpt = TicketAction.computeFor(ticket, newTicketState, tc);
        return ticketActionOpt.map(this::processTicketAction)
                .orElse(failedFuture(String.format("Invalid Transition for [%s] + [%s] + [%s]", ticket, newTicketState, tc))); //todo
    }

    private Future<List<SentUpdateMessage>> processTicketAction(TicketAction ticketAction) {
        return Future.<Boolean>future(t -> databaseService.updateTicket(ticketAction.getTicket(), t))
                .flatMap(didUpdate -> {
                    if (didUpdate) {
                        return Future.<List<ChatTicketMessage>>future(i -> databaseService.getTicketMessageForTicket(ticketAction.getTicket().getId().toString(), i))
                                .map(l -> l.stream()
                                        .map(c -> SendUpdateMessage.builder()
                                                .chatId(c.getChatId())
                                                .messageId(c.getMessageId())
                                                .state(ticketAction.getMessageStateForChat(c.getChatId()))
                                                .text(ticketAction.getMessage())
                                                .build())
                                        .collect(toList()));
                    }
                    return failedFuture("Could not update ticket");
                }).flatMap(updates -> Future.future(f -> botService.updateMessages(updates, f)));
    }

    @Override
    public final void receivedPersonnelMessage(RecvPersonnelMessage recvPersonnelMessage) {
        final var message = ChatMessage.builder()
                .chatId(recvPersonnelMessage.getChatId())
                .messageId(recvPersonnelMessage.getMessageId())
                .message(recvPersonnelMessage.getMessage())
                .build();

        Future.<TelegramChannel>future((e -> databaseService.getChat(recvPersonnelMessage.getChatId(), e)))
                .map(i -> true)
                .recover(t -> {
                    final var personnel = Personnel.builder()
                            .id(UUID.randomUUID())
                            .firstName(recvPersonnelMessage.getFirstName())
                            .lastName(recvPersonnelMessage.getLastName())
                            .locationId(NO_LOCATION)
                            .roleId(NO_ROLE)
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
    public final void updateSticky(String stickyId, UpdateSticky update, Handler<AsyncResult<Sticky>> result) {
        Future.<Sticky>future(f -> databaseService.getSticky(stickyId, f))
                .flatMap(s -> {
                    final var stickyStatusS = update.getActive()
                            .map(status ->
                                    Future.<Boolean>future(f -> databaseService.setStickyStatus(StickyStatus.builder()
                                            .id(UUID.fromString(stickyId))
                                            .status(status)
                                            .build(), f)))
                            .stream();

                    final var stickyNameS = update.getMessage()
                            .map(message -> Future.<Boolean>future(f -> databaseService.updateStickyMessage(StickyMessage.builder()
                                    .id(UUID.fromString(stickyId))
                                    .message(message)
                                    .build(), f)))
                            .stream();

                    final var stickyActionsS = update.getActions()
                            .map(usa -> Future.<Boolean>future(f ->
                                    databaseService.updateStickyActions(stickyId,
                                            UpdateStickyAction.builder()
                                                    .add(createActionsToActions(usa.getAdd()))
                                                    .update(updateActions(usa.getUpdate(), s.getActions()))
                                                    .activate(usa.getActivate())
                                                    .remove(usa.getRemove())
                                                    .build(),
                                            f)))
                            .stream();

                    final var stickyLocationsS = update.getLocations()
                            .map(usl -> Future.<Boolean>future(f ->
                                    databaseService.updateStickyLocation(stickyId,
                                            UpdateStickyLocation.builder()
                                                    .add(createLocationsToLocations(usl.getAdd()))
                                                    .update(updateLocation(usl.getUpdate(), s.getLocations()))
                                                    .activate(usl.getActivate())
                                                    .remove(usl.getRemove())
                                                    .build(),
                                            f)))
                            .stream();

                    final var futures = concat(stickyStatusS, concat(stickyNameS, concat(stickyActionsS, stickyLocationsS))).collect(toList());
                    if (futures.isEmpty()) {
                        return succeededFuture(s);
                    }
                    return CallbackUtils.mergeFutures(futures)
                            .map(r -> r.stream().reduce(Boolean::logicalOr).orElse(true))
                            .flatMap(ignore -> future(f -> databaseService.getSticky(stickyId, f)));
                })
                .onComplete(result);
    }

    private List<Location> updateLocation(List<StickyLocationUpdate> stickyLocationUpdates, List<Location> locations) {
        return stickyLocationUpdates.stream().flatMap(au ->
                locations.stream().filter(a -> a.getId().equals(au.getId())).limit(1).map(a ->
                        Location.builder()
                                .id(a.getId())
                                .text(au.getLocation().orElse(a.getText()))
                                .parentLocation(au.getParentLocation().orElse(a.getParentLocation()))
                                .build())
        ).collect(toList());
    }

    private List<Action> updateActions(List<ActionUpdate> actionUpdates, List<Action> actions) {
        return actionUpdates.stream().flatMap(au ->
                actions.stream().filter(a -> a.getId().equals(au.getId())).limit(1).map(a ->
                        Action.builder()
                                .id(a.getId())
                                .roleId(au.getRoleId().orElse(a.getRoleId()))
                                .message(au.getAction().orElse(a.getMessage()))
                                .build())
        ).collect(toList());
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
