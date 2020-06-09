package com.victor.banana.services.impl;

import com.victor.banana.actions.TicketAction;
import com.victor.banana.models.events.*;
import com.victor.banana.models.events.locations.CreateLocation;
import com.victor.banana.models.events.locations.Location;
import com.victor.banana.models.events.messages.*;
import com.victor.banana.models.events.personnel.Personnel;
import com.victor.banana.models.events.personnel.PersonnelFilter;
import com.victor.banana.models.events.personnel.UpdatePersonnel;
import com.victor.banana.models.events.roles.CreateRole;
import com.victor.banana.models.events.roles.Role;
import com.victor.banana.models.events.stickies.*;
import com.victor.banana.models.events.tickets.*;
import com.victor.banana.services.CartchufiService;
import com.victor.banana.services.DatabaseService;
import com.victor.banana.services.KeycloakClientService;
import com.victor.banana.services.TelegramBotService;
import com.victor.banana.utils.CallbackUtils;
import com.victor.banana.utils.Constants;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.text.StringEscapeUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.victor.banana.models.events.tickets.TicketState.PENDING;
import static com.victor.banana.utils.Constants.DBConstants.NO_LOCATION;
import static com.victor.banana.utils.Constants.PersonnelRole.NO_ROLE;
import static com.victor.banana.utils.MappersHelper.mapTs;
import static io.vertx.core.Future.*;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;

public class CartchufiServiceImpl implements CartchufiService {
    private final static Logger log = LoggerFactory.getLogger(CartchufiServiceImpl.class);
    private final TelegramBotService botService;
    private final DatabaseService databaseService;
    private final KeycloakClientService keycloakClientService;

    public CartchufiServiceImpl(TelegramBotService botService, DatabaseService databaseService, KeycloakClientService keycloakClientService) {
        this.botService = botService;
        this.databaseService = databaseService;
        this.keycloakClientService = keycloakClientService;
    }

    @Override
    public final void createSticky(CreateSticky createSticky, Handler<AsyncResult<Sticky>> result) {
        final var sticky = Sticky.builder()
                .id(UUID.randomUUID())
                .message(createSticky.getMessage())
                .actions(mapTs(this::createActionToAction).apply(createSticky.getActions()))
                .locations(mapTs(this::createLocationToLocation).apply(createSticky.getLocations()))
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
        Future.succeededFuture(Arrays.stream(Constants.PersonnelRole.values())
                .map(getPersonnelRoleRoleMapper())
                .collect(toList())).onComplete(result);
    }

    private Function<Constants.PersonnelRole, Role> getPersonnelRoleRoleMapper() {
        return pr -> Role.builder()
                .id(pr.getUuid())
                .type(pr.getName())
                .build();
    }

    @Override
    public final void getUserProfile(Personnel personnel, Handler<AsyncResult<UserProfile>> result) {
        getUserProfile(personnel).onComplete(result);
    }

    private Future<UserProfile> getUserProfile(Personnel personnel) {
        return Future.<Location>future(f -> databaseService.getLocation(personnel.getLocationId().toString(), f))
                .map(location -> UserProfile.builder()
                        .role(getPersonnelRoleRoleMapper().apply(personnel.getRole()))
                        .location(location)
                        .personnel(personnel)
                        .build());
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
        final Future<List<Ticket>> ticketsF = switch (state) {
            case ACQUIRED -> future(f -> databaseService.getTicketsInStateForChat(chatId, state, f));
            case PENDING -> future(f -> databaseService.ticketsViableForChat(chatId, state, f));
            default -> failedFuture("state not supported for this action");
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
                    .onSuccess(rcvTickets -> Future.<Boolean>future(f -> databaseService.addTicketsMessage(rcvTickets, f))) //todo
                    .onFailure(t -> log.error("Something went wrong while saving the ticket messages", t));
        });
    }

    private SendTicketMessage fromTicket(UUID ticketId, String message, Optional<TicketState> state, Long chatId) {
        return SendTicketMessage.builder()
                .chatId(chatId)
                .ticketId(ticketId)
                .ticketMessage(StringEscapeUtils.escapeHtml4(message))
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
                                            final var sentTickets = mapTs((Ticket ticket) -> transitionTicket(ticket, PENDING, tc)).apply(tickets);
                                            return CallbackUtils.mergeFutures(sentTickets);
                                        }));
                    } else {
                        return failedFuture("Failed to check out :(");
                    }
                }).onFailure(t -> log.error(t.getMessage(), t.getCause()));
    }


    @Override
    public final void actionSelected(Personnel personnel, ActionSelected actionSelected, Handler<AsyncResult<Ticket>> result) {
        Future.<Ticket>future(f -> databaseService.getActiveTicketForActionSelected(actionSelected, f))
                .recover(noTicket -> {
                    log.debug(String.format("no ticket found for actionSelected: %s", actionSelected));
                    return Future.<StickyAction>future(t -> databaseService.getStickyAction(actionSelected, t))
                            .flatMap(stickyAction -> {
                                final var ticket = Ticket.builder()
                                        .id(UUID.randomUUID())
                                        .actionId(stickyAction.getActionId())
                                        .locationId(stickyAction.getLocationId())
                                        .message(String.format("%s | %s | %s", stickyAction.getParentLocation(), stickyAction.getLocation(), stickyAction.getActionMessage()))
                                        .state(PENDING)
                                        .build();
                                final var affectedChatsF = chatsForTicket(ticket);
                                final var ticketF = Future.<Boolean>future(t -> databaseService.addTicket(ticket, t))
                                        .onSuccess(r -> {
                                            if (r) {
                                                databaseService.addTicketNotification(TicketNotification.builder()
                                                        .ticketId(ticket.getId())
                                                        .personnelId(personnel.getId())
                                                        .type(NotificationType.CREATED_BY)
                                                        .build(), t -> {
                                                    if (t.failed()) {
                                                        log.error("Failed to add ticket notification", t.cause());
                                                    }
                                                });
                                            } else {
                                                log.error("Failed to add ticket");
                                            }
                                        });
                                CompositeFuture.join(affectedChatsF, ticketF)
                                        .onSuccess(s -> {
                                            if (ticketF.result() && !affectedChatsF.result().isEmpty()) {
                                                sendTicketMessage(affectedChatsF.result(), ticket);
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

    private Future<Boolean> sendTicketMessage(List<Long> chats, Ticket ticket) {
        final var sendMessages = mapTs((Long chatId) -> fromTicket(ticket.getId(), ticket.getMessage(), Optional.of(PENDING), chatId)).apply(chats);
        return Future.<List<SentTicketMessage>>future(f -> botService.sendMessages(sendMessages, f))
                .flatMap(rcvTickets -> Future.future(f -> databaseService.addTicketsMessage(rcvTickets, f)));
    }

    private Future<List<SentDeleteMessage>> sendDeleteMessage(List<SendDeleteMessage> messages) {
        return Future.<List<SentDeleteMessage>>future(f -> botService.deleteMessages(messages, f))
                .onSuccess(rcvTickets -> Future.<Boolean>future(f -> databaseService.hideTicketsMessage(rcvTickets, f))
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
                    final var newRoleOpt = updatePersonnel.getRoleId().flatMap(Constants.PersonnelRole::from);
                    final var userId = UUID.fromString(personnelId);

                    final var keycloakUpdate = newRoleOpt.map(role -> {
                        final var keyU = KeyUserRoleUpdate.builder()
                                .personnelId(userId)
                                .personnelRole(role)
                                .build();
                        return Future.<Void>future(t -> keycloakClientService.userRoleUpdate(keyU, t));
                    }).orElse(Future.succeededFuture());

                    final var pers = Personnel.builder()
                            .id(userId)
                            .firstName(updatePersonnel.getFirstName().or(p::getFirstName))
                            .lastName(updatePersonnel.getLastName().or(p::getLastName))
                            .email(updatePersonnel.getEmail().or(p::getEmail))
                            .role(updatePersonnel.getRoleId().flatMap(Constants.PersonnelRole::from).orElse(p.getRole()))
                            .locationId(updatePersonnel.getLocationId().orElse(p.getLocationId()))
                            .build();
                    final var databaseUpdate = Future.<Boolean>future(ft -> databaseService.updatePersonnel(pers, ft));
                    return CompositeFuture.all(databaseUpdate, keycloakUpdate)
                            .flatMap(e -> {
                                        if (e.succeeded()) {
                                            return succeededFuture(pers);
                                        }
                                        return failedFuture("failed to update the personnel");
                                    }

                            );
                }).onComplete(result);
    }

    @Override
    public final void deletePersonnel(String personnelId, Handler<AsyncResult<Void>> result) {
        final var keycloakDelete = Future.<Void>future(f -> keycloakClientService.deleteUser(KeyUserDelete.builder().personnelId(UUID.fromString(personnelId)).build(), f));
        final var dbDelete = Future.<Boolean>future(f -> databaseService.deletePersonnel(personnelId, f));
        CompositeFuture.all(keycloakDelete, dbDelete)
                .<Void>mapEmpty()
                .onComplete(result);
    }

    @Override
    public final void addTelegramToUserProfile(TelegramLoginData telegramLoginData, Handler<AsyncResult<UserProfile>> result) {
        final var personnelId = telegramLoginData.getPersonnel().getId();
        createTelegramChannel(personnelId, telegramLoginData.getChatId(), telegramLoginData.getUsername())
                .flatMap(r -> {
                    if (r) {
                        return getPersonnel(personnelId.toString()).flatMap(this::getUserProfile);
                    }
                    return Future.failedFuture("Failed to create TelegramChannel");
                }).onComplete(result);
    }


    @Override
    public final void getPersonnel(String personnelId, Handler<AsyncResult<Personnel>> result) {
        getPersonnel(personnelId).onComplete(result);
    }

    private Future<Personnel> getPersonnel(String personnelId) {
        return Future.future(f -> databaseService.getPersonnel(personnelId, f));
    }

    @Override
    public final void findPersonnel(PersonnelFilter filter, Handler<AsyncResult<List<Personnel>>> result) {
        Future.<List<Personnel>>future(f -> databaseService.findPersonnelWithFilter(filter, f))
                .onComplete(result);
    }

    private Future<Boolean> transitionTicket(Ticket ticket, TicketState newTicketState, TelegramChannel tc) {
        final var ticketActionOpt = TicketAction.computeFor(ticket, newTicketState, tc);
        return ticketActionOpt.map(this::processTicketAction)
                .orElseGet(() -> {
                    log.error(String.format("Invalid Transition for [%s] + [%s] + [%s]", ticket, newTicketState, tc));
                    return processTicketAction(TicketAction.computeFor(ticket, tc.getChatId(), tc.getUsername()));
                });
    }

    private Future<Boolean> processTicketAction(TicketAction ticketAction) {
        return Future.<Boolean>future(t -> databaseService.updateTicket(ticketAction.getTicket(), t))
                .flatMap(didUpdate -> {
                    if (didUpdate) {
                        final var ticketMessages = Future.<List<ChatTicketMessage>>future(i -> databaseService.getTicketMessageForTicket(ticketAction.getTicket().getId().toString(), i));
                        return switch (ticketAction.getTicket().getState()) {
                            case PENDING -> ticketMessages.map(mapTs(chatTicketMessageToSendDeleteMessage()))
                                    .onSuccess(deleteMsg -> sendDeleteMessage(deleteMsg)
                                            .onSuccess(sentMsg -> {
                                                final var crossMsg = sentMsg.stream()
                                                        .filter(Predicate.not(SentDeleteMessage::getWasDeleted))
                                                        .map(sdm -> SendUpdateMessage.builder()
                                                                .chatId(sdm.getChatId())
                                                                .messageId(sdm.getMessageId())
                                                                .state(Optional.empty())
                                                                .text(String.format("<s>%s</s>", StringEscapeUtils.escapeHtml4(ticketAction.getMessage())))
                                                                .build()).collect(toList());
                                                Future.<List<SentUpdateMessage>>future(f -> botService.updateMessages(crossMsg, f))
                                                        .onFailure(t -> log.error("Failed to cross message", t));
                                            }))
                                    .flatMap(ignore -> chatsForTicket(ticketAction.getTicket()))
                                    .flatMap(chats -> sendTicketMessage(chats, ticketAction.getTicket()));
                            case ACQUIRED, SOLVED -> ticketMessages.map(mapTs(chatTicketMessageToSendUpdateMessage(ticketAction)))
                                    .flatMap(updates -> Future.<List<SentUpdateMessage>>future(f -> botService.updateMessages(updates, f)))
                                    .map(ignore -> true);
                        };
                    }
                    return failedFuture("Could not update ticket");
                });
    }

    private Function<ChatTicketMessage, SendUpdateMessage> chatTicketMessageToSendUpdateMessage(TicketAction ticketAction) {
        return c -> SendUpdateMessage.builder()
                .chatId(c.getChatId())
                .messageId(c.getMessageId())
                .state(ticketAction.getMessageStateForChat(c.getChatId()))
                .text(StringEscapeUtils.escapeHtml4(ticketAction.getMessage()))
                .build();
    }

    private Function<ChatTicketMessage, SendDeleteMessage> chatTicketMessageToSendDeleteMessage() {
        return c -> SendDeleteMessage.builder()
                .chatId(c.getChatId())
                .messageId(c.getMessageId())
                .build();
    }

    @Override
    public final void getOrElseCreatePersonnel(TokenUser user, Handler<AsyncResult<Personnel>> result) {
        Future.<Personnel>future((e -> databaseService.getPersonnel(user.getId().toString(), e)))
                .flatMap(p -> {
                    if (user.getAuthority().toPersonnelRole().isBetter(p.getRole())) {
                        final var newP = new Personnel(p.toJson());
                        newP.setRole(user.getAuthority().toPersonnelRole());
                        return Future.<Boolean>future(f -> databaseService.updatePersonnel(newP, f))
                                .flatMap(s -> {
                                    if (s) {
                                        return Future.succeededFuture(newP);
                                    }
                                    log.error(String.format("Couldn't update personnel's [%s] role to role [%s]", newP.getId(), newP.getRole()));
                                    return Future.succeededFuture(p);
                                });
                    }
                    return succeededFuture(p);
                })
                .recover(t -> {
                    final var personnel = Personnel.builder()
                            .id(user.getId())
                            .firstName(Optional.ofNullable(user.getFirstName()))
                            .lastName(Optional.ofNullable(user.getLastName()))
                            .email(Optional.ofNullable(user.getEmail()))
                            .locationId(NO_LOCATION)
                            .role(user.getAuthority().toPersonnelRole())
                            .build();
                    return Future.<Boolean>future(f -> databaseService.addPersonnel(personnel, f))
                            .flatMap(s -> {
                                if (s) {
                                    return Future.succeededFuture(personnel);
                                }
                                return Future.failedFuture(t);
                            });
                }).onComplete(result);
    }

    @Override
    public final void receivedPersonnelMessage(RecvPersonnelMessage recvPersonnelMessage) {
        final var message = ChatMessage.builder()
                .chatId(recvPersonnelMessage.getChannel().getChatId())
                .messageId(recvPersonnelMessage.getMessageId())
                .message(recvPersonnelMessage.getMessage())
                .build();

        Future.<Boolean>future(f -> createChannel(recvPersonnelMessage.getChannel(), f))
                .flatMap(chatExists -> {
                    if (chatExists) {
                        return Future.<Boolean>future(c -> databaseService.addMessage(message, c)).map(message);
                    }
                    return failedFuture("problem adding the chat");
                })
                .onFailure(t -> log.error("An error occurred: ", t))
                .onSuccess(m -> log.info("Added message: " + m.toString()));
    }

    private Future<Boolean> createPersonnel(UUID personnelId, Optional<String> firstName, Optional<String> lastName) {
        final var personnel = Personnel.builder()
                .id(personnelId)
                .firstName(firstName)
                .lastName(lastName)
                .email(Optional.empty())
                .locationId(NO_LOCATION)
                .role(NO_ROLE)
                .build();
        return Future.future(e -> databaseService.addPersonnel(personnel, e));
    }

    private Future<Boolean> createTelegramChannel(UUID personnelId, Long chatId, String username) {
        final var chat = TelegramChannel.builder()
                .chatId(chatId)
                .personnelId(personnelId)
                .username(username)
                .build();
        return Future.future(e -> databaseService.addChat(chat, e));
    }

    @Override
    public void createChannel(CreateChannelMessage createChannel, Handler<AsyncResult<Boolean>> result) {
        Future.<TelegramChannel>future((e -> databaseService.getChat(createChannel.getChatId(), e)))
                .map(i -> true)
                .recover(t -> {
                    final var personnelId = UUID.randomUUID();
                    return createPersonnel(personnelId, createChannel.getFirstName(), createChannel.getLastName())
                            .flatMap(personnelExist -> {
                                if (personnelExist) {
                                    return createTelegramChannel(personnelId, createChannel.getChatId(), createChannel.getUsername());
                                }
                                return failedFuture("didn't find row");
                            });
                }).onComplete(result);
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
                                                    .add(mapTs(this::createActionToAction).apply(usa.getAdd()))
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
                                                    .add(mapTs(this::createLocationToLocation).apply(usl.getAdd()))
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

    @Override
    public final void getStickies(Handler<AsyncResult<List<Sticky>>> result) {
        Future.future(databaseService::getStickies)
                .onComplete(result);
    }

    @Override
    public final void getSticky(String stickyId, Handler<AsyncResult<Sticky>> result) {
        Future.<Sticky>future(f -> databaseService.getSticky(stickyId, f))
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
