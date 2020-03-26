package com.victor.banana.services.impl;

import com.victor.banana.models.events.ChatTicketMessage;
import com.victor.banana.models.events.StickyAction;
import com.victor.banana.models.events.TelegramChannel;
import com.victor.banana.models.events.Ticket;
import com.victor.banana.services.CartchufiService;
import com.victor.banana.services.DatabaseService;
import com.victor.banana.services.TelegramBotService;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class CartchufiServiceImpl implements CartchufiService {
    private final TelegramBotService botService;
    private final DatabaseService databaseService;

    public CartchufiServiceImpl(TelegramBotService botService, DatabaseService databaseService) {
        this.botService = botService;
        this.databaseService = databaseService;
    }

    @Override
    public final void stickyActionScanned(StickyAction stickyAction, Handler<AsyncResult<Ticket>> result) {
        final var ticket = Ticket.builder()
                .id(UUID.randomUUID().toString())
                .actionId(stickyAction.getActionId())
                .message(String.format("%s | %s", stickyAction.getStickyMessage(), stickyAction.getActionMessage()))
                .build();
        Future.<Boolean>future(t -> databaseService.addTicket(ticket, t))
                .flatMap((Boolean t) -> {
                    if (t) {
                        return Future.<Void>future(f -> botService.sendTicket(ticket, f));
                    }
                    return Future.failedFuture("Ticket not created");
                })
                .map(ticket)
                .setHandler(result);
    }

    @Override
    public final void messageAcquired(Long chatId, Long messageId, Handler<AsyncResult<List<ChatTicketMessage>>> result) {
        Future.<TelegramChannel>future(t -> databaseService.getChat(chatId, t))
                .flatMap(t -> Future.<Ticket>future(t2 -> databaseService.getTicketForMessage(chatId, messageId, t2))
                        .map(ticket -> { //todo (solved_by)
                            ticket.setAcquiredBy(t.getPersonnelId());
                            return ticket;
                        }))
                .flatMap(ticket ->
                    Future.<Boolean>future(t -> databaseService.updateTicket(ticket, t)).flatMap(didUpdate -> {
                                if (didUpdate) {
                                    return Future.<List<ChatTicketMessage>>future(i -> databaseService.getTicketMessageForTicket(ticket.getId(), i)).map(l -> l.stream().filter(c -> !c.getChatId().equals(chatId)).collect(Collectors.toList()));
                                }
                                return Future.failedFuture("Ticket was not updated");
                    })
                ).onComplete(result);
    }


}
