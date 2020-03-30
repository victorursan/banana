package com.victor.banana.services.impl;

import com.victor.banana.jooq.enums.State;
import com.victor.banana.models.events.*;
import com.victor.banana.models.events.messages.SentTicketMessage;
import com.victor.banana.models.events.tickets.Ticket;
import com.victor.banana.models.events.tickets.TicketState;
import com.victor.banana.services.DatabaseService;
import io.github.jklingsporn.vertx.jooq.classic.reactivepg.ReactiveClassicGenericQueryExecutor;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import org.jooq.DSLContext;
import org.jooq.impl.DefaultConfiguration;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.victor.banana.jooq.Tables.*;
import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static org.jooq.SQLDialect.POSTGRES;

public class DatabaseServiceImpl implements DatabaseService {
    private final ReactiveClassicGenericQueryExecutor queryExecutor;

    public DatabaseServiceImpl(PgPool client) {
        final var configuration = new DefaultConfiguration();
        configuration.setSQLDialect(POSTGRES);
        queryExecutor = new ReactiveClassicGenericQueryExecutor(configuration, client);
    }

    @Override
    public final void healthCheck(Handler<AsyncResult<Void>> result) {
        queryExecutor.findOneRow(DSLContext::selectOne).<Void>mapEmpty().setHandler(result);
    }

    @Override
    public final void addChat(TelegramChannel chat, Handler<AsyncResult<Boolean>> result) {
        queryExecutor.execute(c -> c.insertInto(TELEGRAM_CHANNEL, TELEGRAM_CHANNEL.CHAT_ID, TELEGRAM_CHANNEL.PERSONNEL_ID, TELEGRAM_CHANNEL.USERNAME)
                .values(chat.getChatId(), UUID.fromString(chat.getPersonnelId()), chat.getUsername())
                .onConflictDoNothing()
        ).map(i -> i == 1 || i == 0).setHandler(result);
    }

    @Override
    public final void addPersonnel(Personnel personnel, Handler<AsyncResult<Boolean>> result) {
        queryExecutor.execute(c -> c.insertInto(PERSONNEL, PERSONNEL.PERSONNEL_ID, PERSONNEL.FIRST_NAME, PERSONNEL.LAST_NAME)
                .values(UUID.fromString(personnel.getId()), personnel.getFirstName(), personnel.getLastName())
                .onConflictDoNothing()
        ).map(i -> i == 1 || i == 0).setHandler(result);
    }

    @Override
    public final void getChat(Long chatId, Handler<AsyncResult<TelegramChannel>> result) {
        queryExecutor.findOneRow(c -> c.selectFrom(TELEGRAM_CHANNEL)
                .where(TELEGRAM_CHANNEL.CHAT_ID.eq(chatId)))
                .flatMap(r -> {
                    if (r != null) {
                        final var telegramChannel = TelegramChannel.builder()
                                .chatId(r.getLong(TELEGRAM_CHANNEL.CHAT_ID.getName()))
                                .personnelId(r.getUUID(TELEGRAM_CHANNEL.PERSONNEL_ID.getName()).toString())
                                .username(r.getString(TELEGRAM_CHANNEL.USERNAME.getName()))
                                .build();
                        return succeededFuture(telegramChannel);
                    }
                    return failedFuture("No Row found");
                }).setHandler(result);
    }


    @Override
    public final void getChats(Handler<AsyncResult<List<Long>>> result) {
        queryExecutor.findManyRow(c -> c.select(TELEGRAM_CHANNEL.CHAT_ID).from(TELEGRAM_CHANNEL))
                .flatMap(rows -> {
                    if (rows != null) {
                        final var ids = rows.stream()
                                .map(r -> r.getLong(TELEGRAM_CHANNEL.CHAT_ID.getName()))
                                .collect(Collectors.toList());
                        return succeededFuture(ids);
                    }
                    return failedFuture("No Rows found");
                }).setHandler(result);
    }


    @Override
    public final void addMessage(ChatMessage chatMessage, Handler<AsyncResult<Boolean>> result) {
        queryExecutor.execute(c -> c.insertInto(CHAT_MESSAGE, CHAT_MESSAGE.MESSAGE_ID, CHAT_MESSAGE.CHAT_ID, CHAT_MESSAGE.MESSAGE)
                .values(chatMessage.getMessageId(), chatMessage.getChatId(), chatMessage.getMessage()))
                .map(i -> i == 1)
                .setHandler(result);
    }

    @Override
    public final void addTicketsMessage(List<SentTicketMessage> chatMessages, Handler<AsyncResult<Boolean>> result) {
        queryExecutor.execute(c -> {
             final var insert = c.insertInto(CHAT_TICKET_MESSAGE, CHAT_TICKET_MESSAGE.MESSAGE_ID, CHAT_TICKET_MESSAGE.CHAT_ID, CHAT_TICKET_MESSAGE.TICKET_ID);
             chatMessages.forEach(chatMessage ->
                    insert.values(chatMessage.getMessageId(), chatMessage.getChatId(), UUID.fromString(chatMessage.getTicketId())));
            return insert.onConflictDoNothing();
        })
                .map(i -> i == chatMessages.size())
                .onComplete(result);
    }

    @Override
    public final void getTicketMessageForTicket(String ticketId, Handler<AsyncResult<List<ChatTicketMessage>>> result) {
        queryExecutor.findManyRow(c -> c.select(CHAT_TICKET_MESSAGE.MESSAGE_ID, CHAT_TICKET_MESSAGE.CHAT_ID).from(CHAT_TICKET_MESSAGE)
                .where(CHAT_TICKET_MESSAGE.TICKET_ID.eq(UUID.fromString(ticketId))))
                .map(rows -> rows.stream()
                        .map(row ->
                                ChatTicketMessage.builder()
                                        .messageId(row.getLong(CHAT_TICKET_MESSAGE.MESSAGE_ID.getName()))
                                        .chatId(row.getLong(CHAT_TICKET_MESSAGE.CHAT_ID.getName()))
                                        .ticketId(ticketId)
                        .build()).collect(Collectors.toList()))
                .setHandler(result);
    }

    @Override
    public final void getTicketForMessage(Long chatId, Long messageId, Handler<AsyncResult<Ticket>> result) {
        queryExecutor.findOneRow(c ->
                c.select(TICKET.TICKET_ID, TICKET.ACTION_ID, TICKET.AQUIRED_BY, TICKET.SOLVED_BY, TICKET.MESSAGE, TICKET.STATE)
                 .from(CHAT_TICKET_MESSAGE)
                        .innerJoin(TICKET)
                        .using(TICKET.TICKET_ID)
                 .where(CHAT_TICKET_MESSAGE.CHAT_ID.eq(chatId).and(CHAT_TICKET_MESSAGE.MESSAGE_ID.eq(messageId))))
                .flatMap(this::rowToTicket)
                .onComplete(result);

    }

    @Override
    public final void getSticky(String stickyId, Handler<AsyncResult<Sticky>> result) { //todo
        queryExecutor.findOneRow(c -> c.selectFrom(STICKY)
                .where(STICKY.STICKY_ID.eq(UUID.fromString(stickyId))))
                .flatMap(r -> {
                    if (r != null) {
                        final var sticky = Sticky.builder()
                                .id(r.getUUID(STICKY.STICKY_ID.getName()).toString())
                                .message(r.getString(STICKY.MESSAGE.getName()))
                                .build();
                        return succeededFuture(sticky);
                    }
                    return failedFuture("No Row found");
                })
                .setHandler(result);
    }

    @Override
    public final void getStickyAction(String actionId, Handler<AsyncResult<StickyAction>> result) {
        queryExecutor.findOneRow(c -> c.select(STICKY.MESSAGE.as("sticky_message"), STICKY_ACTION.MESSAGE.as("action_message"))
                .from(STICKY_ACTION).innerJoin(STICKY).using(STICKY_ACTION.STICKY_ID)
                .where(STICKY_ACTION.ACTION_ID.eq(UUID.fromString(actionId))))
                .flatMap(r -> {
                    if (r != null) {
                        final var stickyAction = StickyAction.builder()
                                .actionId(actionId)
                                .actionMessage(r.getString("action_message"))
                                .stickyMessage(r.getString("sticky_message"))
                                .build();
                        return succeededFuture(stickyAction);
                    }
                    return failedFuture("No Row found");
                })
                .setHandler(result);
    }

    @Override
    public final void addSticky(Sticky sticky, Handler<AsyncResult<Boolean>> result) {
        final var stickyId = UUID.fromString(sticky.getId());
        queryExecutor.transaction(t ->
                t.execute(c -> c.insertInto(STICKY, STICKY.STICKY_ID, STICKY.MESSAGE)
                        .values(stickyId, sticky.getMessage()))
                        .flatMap(i -> {
                            if (i == 1) {
                                return t.execute(c -> {
                                            final var insert = c.insertInto(STICKY_ACTION, STICKY_ACTION.ACTION_ID, STICKY_ACTION.STICKY_ID, STICKY_ACTION.MESSAGE);
                                            sticky.getActions().forEach(action -> insert.values(UUID.fromString(action.getId()), stickyId, action.getMessage()));
                                            return insert;
                                        }
                                ).map(ii -> ii == sticky.getActions().size());
                            }
                            return failedFuture("Failed to insert sticky");
                        })).setHandler(result);
    }


    @Override
    public final void getTicket(String ticketId, Handler<AsyncResult<Ticket>> result) {
        queryExecutor.findOneRow(c -> c.select(TICKET.TICKET_ID, TICKET.ACTION_ID, TICKET.AQUIRED_BY, TICKET.SOLVED_BY, TICKET.MESSAGE, TICKET.STATE).from(TICKET)
                .where(TICKET.TICKET_ID.eq(UUID.fromString(ticketId))))
                .flatMap(this::rowToTicket)
                .setHandler(result);
    }

    private Future<Ticket> rowToTicket(Row r) {
            if (r != null) {
                final var sticky = Ticket.builder()
                        .id(r.getUUID(TICKET.TICKET_ID.getName()).toString())
                        .actionId(r.getUUID(TICKET.ACTION_ID.getName()).toString())
                        .acquiredBy(r.getString(TICKET.AQUIRED_BY.getName()))
                        .solvedBy(r.getString(TICKET.SOLVED_BY.getName()))
                        .message(r.getString(TICKET.MESSAGE.getName()))
                        .state(ticketToTicketState(State.valueOf(r.getString(TICKET.STATE.getName()))))
                        .build();
                return succeededFuture(sticky);
            }
            return failedFuture("No Row found");
    }

    private State ticketStateToState(TicketState ts) {
        return switch (ts) {
            case SOLVED -> State.SOLVED;
            case ACQUIRED -> State.ACQUIRED;
            case PENDING -> State.PENDING;
        };
    }

    private TicketState ticketToTicketState(State ts) {
        return switch (ts) {
            case SOLVED -> TicketState.SOLVED;
            case ACQUIRED -> TicketState.ACQUIRED;
            case PENDING -> TicketState.PENDING;
        };
    }

    @Override
    public final void addTicket(Ticket ticket, Handler<AsyncResult<Boolean>> result) {
        queryExecutor.execute(c -> c.insertInto(TICKET, TICKET.TICKET_ID, TICKET.ACTION_ID, TICKET.MESSAGE, TICKET.STATE)
                .values(UUID.fromString(ticket.getId()), UUID.fromString(ticket.getActionId()), ticket.getMessage(), ticketStateToState(ticket.getState())))
                .map(i -> i == 1)
                .setHandler(result);
    }

    @Override
    public final void updateTicket(Ticket ticket, Handler<AsyncResult<Boolean>> result) {
        queryExecutor.execute(c ->
                c.update(TICKET)
                .set(TICKET.AQUIRED_BY, ticket.getAcquiredBy() != null ? UUID.fromString(ticket.getAcquiredBy()) : null)
                .set(TICKET.SOLVED_BY, ticket.getSolvedBy() != null ? UUID.fromString(ticket.getSolvedBy()) : null)
                .set(TICKET.STATE, ticketStateToState(ticket.getState()))
                .where(TICKET.TICKET_ID.eq(UUID.fromString(ticket.getId())))
        ).map(i -> i == 1)
         .setHandler(result);
    }


}
