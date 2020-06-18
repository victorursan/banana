package com.victor.banana.controllers.db;

import com.victor.banana.models.events.TelegramChannel;
import com.victor.banana.models.events.messages.ChatMessage;
import com.victor.banana.models.events.messages.ChatTicketMessage;
import com.victor.banana.models.events.messages.SentDeleteMessage;
import com.victor.banana.models.events.messages.SentTicketMessage;
import com.victor.banana.models.events.tickets.Ticket;
import io.github.jklingsporn.vertx.jooq.classic.reactivepg.ReactiveClassicGenericQueryExecutor;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static com.victor.banana.controllers.db.QueryHandler.*;
import static com.victor.banana.controllers.db.RowMappers.*;
import static com.victor.banana.jooq.Tables.*;
import static io.vertx.core.Future.succeededFuture;
import static java.util.stream.Collectors.toList;


public final class QueryExecutorHandler {
    @NotNull
    private static final Logger log = LoggerFactory.getLogger(QueryExecutorHandler.class);

    private QueryExecutorHandler() {
    }

    @NotNull
    public static Function<ReactiveClassicGenericQueryExecutor, Future<Void>> healthCheckQ() {
        return execute(DSLContext::selectOne, 1, "health check");
    }

    @NotNull
    public static Function<ReactiveClassicGenericQueryExecutor, Future<TelegramChannel>> getChatQ(Long chatId) {
        return findOne(c -> c.selectFrom(TELEGRAM_CHANNEL)
                .where(TELEGRAM_CHANNEL.CHAT_ID.eq(chatId)), rowToTelegramChannel());
    }

    @NotNull
    public static Function<ReactiveClassicGenericQueryExecutor, Future<Void>> setCheckedInQ(Long chatId, Boolean checkedIn) {
        return execute(c -> c.update(TELEGRAM_CHANNEL).set(TELEGRAM_CHANNEL.CHECKED_IN, checkedIn).where(TELEGRAM_CHANNEL.CHAT_ID.eq(chatId)),
                1, "update telegram channel checked in status");
    }

    @NotNull
    public static Function<ReactiveClassicGenericQueryExecutor, Future<List<Long>>> getChatsQ(Ticket ticket) {
        return findMany(c -> c.selectDistinct(TELEGRAM_CHANNEL.CHAT_ID).from(TELEGRAM_CHANNEL)
                        .innerJoin(PERSONNEL).using(TELEGRAM_CHANNEL.PERSONNEL_ID)
                        .innerJoin(FLOOR).using(PERSONNEL.BUILDING_ID)
                        .innerJoin(STICKY_LOCATION).using(FLOOR.FLOOR_ID)
                        .innerJoin(STICKY_ACTION_ROLE).using(PERSONNEL.ROLE_ID)
                        .where(STICKY_LOCATION.LOCATION_ID.eq(ticket.getLocationId()).and(STICKY_ACTION_ROLE.ACTION_ID.eq(ticket.getActionId()))),
                rowToChatId());
    }

    @NotNull
    public static Function<ReactiveClassicGenericQueryExecutor, Future<Void>> addMessageQ(ChatMessage chatMessage) {
        return execute(c -> c.insertInto(CHAT_MESSAGE, CHAT_MESSAGE.MESSAGE_ID, CHAT_MESSAGE.CHAT_ID, CHAT_MESSAGE.MESSAGE)
                        .values(chatMessage.getMessageId(), chatMessage.getChatId(), chatMessage.getMessage()),
                1, "add message");
    }

    @NotNull
    public static Function<ReactiveClassicGenericQueryExecutor, Future<Void>> addTicketsMessageQ(List<SentTicketMessage> chatMessages) {
        if (chatMessages.isEmpty()) {
            return t -> succeededFuture();
        }
        return execute(c -> {
            final var insert = c.insertInto(CHAT_TICKET_MESSAGE, CHAT_TICKET_MESSAGE.MESSAGE_ID, CHAT_TICKET_MESSAGE.CHAT_ID, CHAT_TICKET_MESSAGE.TICKET_ID, CHAT_TICKET_MESSAGE.VISIBLE);
            chatMessages.forEach(chatMessage -> insert.values(chatMessage.getMessageId(), chatMessage.getChatId(), chatMessage.getTicketId(), true));
            return insert;
        }, chatMessages.size(), "add ticket message");
    }

    @NotNull
    public static Function<ReactiveClassicGenericQueryExecutor, Future<Void>> hideTicketsMessageQ(List<SentDeleteMessage> chatMessages) {
        return execute(c -> {
            final var primaryKey = c.newRecord(CHAT_TICKET_MESSAGE.MESSAGE_ID, CHAT_TICKET_MESSAGE.CHAT_ID);
            final var keys = chatMessages.stream().map(m -> primaryKey.values(m.getMessageId(), m.getChatId()).valuesRow()).collect(toList());
            return c.update(CHAT_TICKET_MESSAGE).set(CHAT_TICKET_MESSAGE.VISIBLE, false).where(primaryKey.fieldsRow().in(keys));
        }, chatMessages.size(), "hide ticket message");
    }

    @NotNull
    public static Function<ReactiveClassicGenericQueryExecutor, Future<List<ChatTicketMessage>>> getTicketMessageForTicketQ(UUID ticketId) {
        return findMany(c ->
                c.selectDistinct(CHAT_TICKET_MESSAGE.MESSAGE_ID, CHAT_TICKET_MESSAGE.CHAT_ID, CHAT_TICKET_MESSAGE.TICKET_ID)
                        .from(CHAT_TICKET_MESSAGE)
                        .innerJoin(TELEGRAM_CHANNEL).using(TELEGRAM_CHANNEL.CHAT_ID)
                        .where(CHAT_TICKET_MESSAGE.TICKET_ID.eq(ticketId)
                                .and(TELEGRAM_CHANNEL.CHECKED_IN.eq(true)))
                        .and(CHAT_TICKET_MESSAGE.VISIBLE.eq(true)), rowToChatTicketMessage());

    }

}
