package com.victor.banana.controllers.db;

import com.victor.banana.models.events.TelegramChannel;
import com.victor.banana.models.events.personnel.Personnel;
import com.victor.banana.models.events.personnel.PersonnelFilter;
import com.victor.banana.utils.Constants;
import io.github.jklingsporn.vertx.jooq.classic.reactivepg.ReactiveClassicGenericQueryExecutor;
import io.vertx.core.Future;
import org.jetbrains.annotations.NotNull;
import org.jooq.*;
import org.jooq.impl.DSL;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static com.victor.banana.controllers.db.QueryHandler.*;
import static com.victor.banana.controllers.db.RowMappers.rowToPersonnel;
import static com.victor.banana.jooq.Tables.PERSONNEL;
import static com.victor.banana.jooq.Tables.TELEGRAM_CHANNEL;
import static com.victor.banana.utils.Constants.PersonnelRole.ADMIN;

public final class PersonnelQueryHandler {
    @NotNull
    private static final Condition isNotAdmin = PERSONNEL.ROLE_ID.notEqual(ADMIN.getUuid());

    private PersonnelQueryHandler() {
    }

    @NotNull
    public static Function<ReactiveClassicGenericQueryExecutor, Future<Void>> addChatQ(TelegramChannel chat) {
        return execute(c -> c.insertInto(TELEGRAM_CHANNEL, TELEGRAM_CHANNEL.CHAT_ID, TELEGRAM_CHANNEL.PERSONNEL_ID, TELEGRAM_CHANNEL.USERNAME)
                        .values(chat.getChatId(), chat.getPersonnelId(), chat.getUsername()),
                1, "add telegram channel");
    }

    @NotNull
    public static Function<ReactiveClassicGenericQueryExecutor, Future<Void>> addPersonnelQ(Personnel personnel) {
        return execute(c -> c.insertInto(PERSONNEL, PERSONNEL.PERSONNEL_ID, PERSONNEL.FIRST_NAME, PERSONNEL.LAST_NAME, PERSONNEL.EMAIL, PERSONNEL.BUILDING_ID, PERSONNEL.ROLE_ID)
                        .values(personnel.getId(),
                                personnel.getFirstName().orElse(null),
                                personnel.getLastName().orElse(null),
                                personnel.getEmail().orElse(null),
                                personnel.getBuildingId().orElse(null),
                                personnel.getRole().map(Constants.PersonnelRole::getUuid).orElse(null)),
                1, "add personnel");
    }

    @NotNull
    public static Function<ReactiveClassicGenericQueryExecutor, Future<Void>> deactivatePersonnelQ(UUID personnelId) {
        return execute(c -> c.update(PERSONNEL)
                        .setNull(PERSONNEL.FIRST_NAME)
                        .setNull(PERSONNEL.LAST_NAME)
                        .setNull(PERSONNEL.EMAIL)
                        .setNull(PERSONNEL.BUILDING_ID)
                        .setNull(PERSONNEL.ROLE_ID)
                        .set(PERSONNEL.ACTIVE, false)
                        .where(PERSONNEL.PERSONNEL_ID.eq(personnelId).and(isNotAdmin)),
                1, "deactivate personnel");
    }


    @NotNull
    public static Function<ReactiveClassicGenericQueryExecutor, Future<Void>> updatePersonnelQ(Personnel personnel) {
        return execute(c -> c.update(PERSONNEL)
                        .set(PERSONNEL.FIRST_NAME, personnel.getFirstName().orElse(null))
                        .set(PERSONNEL.LAST_NAME, personnel.getLastName().orElse(null))
                        .set(PERSONNEL.EMAIL, personnel.getEmail().orElse(null))
                        .set(PERSONNEL.BUILDING_ID, personnel.getBuildingId().orElse(null))
                        .set(PERSONNEL.ROLE_ID, personnel.getRole().map(Constants.PersonnelRole::getUuid).orElse(null))
                        .where(PERSONNEL.PERSONNEL_ID.eq(personnel.getId()).and(isNotAdmin)),
                1, "update personnel");
    }

    @NotNull
    public static Function<ReactiveClassicGenericQueryExecutor, Future<Personnel>> getPersonnelQ(UUID personnelId) {
        return findOne(selectWhere(selectPersonnel(), PERSONNEL.PERSONNEL_ID.eq(personnelId)), rowToPersonnel());
    }

    @NotNull
    public static Function<ReactiveClassicGenericQueryExecutor, Future<List<Personnel>>> findPersonnelWithFilterQ(PersonnelFilter filter) {
        final var operating = PERSONNEL.BUILDING_ID.isNotNull().and(PERSONNEL.ROLE_ID.isNotNull());
        final var isOperating = isNotAdmin.and(filter.getOperating() ? operating : DSL.not(operating)).and(PERSONNEL.ACTIVE.eq(true));
        return t -> filter.getUsername()
                .map(username ->
                        findOne(selectWhere(selectPersonnel(), isOperating.and(TELEGRAM_CHANNEL.USERNAME.equalIgnoreCase(username))), rowToPersonnel())
                                .apply(t)
                                .map(List::of))
                .orElseGet(() -> findMany(selectWhere(selectPersonnel(), isOperating), rowToPersonnel()).apply(t));
    }

    @NotNull
    private static Function<DSLContext, SelectJoinStep<Record8<UUID, UUID, UUID, String, String, String, Long, String>>> selectPersonnel() {
        return c -> c.selectDistinct(PERSONNEL.PERSONNEL_ID, PERSONNEL.BUILDING_ID, PERSONNEL.ROLE_ID, PERSONNEL.FIRST_NAME, PERSONNEL.LAST_NAME, PERSONNEL.EMAIL, TELEGRAM_CHANNEL.CHAT_ID, TELEGRAM_CHANNEL.USERNAME)
                .from(PERSONNEL)
                .leftJoin(TELEGRAM_CHANNEL).using(PERSONNEL.PERSONNEL_ID);//todo, there might be multiple channels for same personnel
    }


}
