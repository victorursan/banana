package com.victor.banana.services.impl;

import com.victor.banana.models.events.TelegramChannel;
import com.victor.banana.models.events.TelegramLoginData;
import com.victor.banana.models.events.TokenUser;
import com.victor.banana.models.events.UserProfile;
import com.victor.banana.models.events.locations.*;
import com.victor.banana.models.events.messages.ChatMessage;
import com.victor.banana.models.events.messages.CreateChannelMessage;
import com.victor.banana.models.events.messages.RecvPersonnelMessage;
import com.victor.banana.models.events.personnel.Personnel;
import com.victor.banana.models.events.personnel.PersonnelFilter;
import com.victor.banana.models.events.personnel.UpdatePersonnel;
import com.victor.banana.services.DatabaseService;
import com.victor.banana.services.KeycloakClientService;
import com.victor.banana.services.PersonnelService;
import com.victor.banana.utils.CallbackUtils;
import com.victor.banana.utils.Constants;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.serviceproxy.ServiceException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.victor.banana.utils.CreateMappers.*;
import static com.victor.banana.utils.ExceptionUtils.FAILED_NO_ELEMENT_FOUND;
import static io.vertx.core.Future.*;

public class PersonnelServiceImpl implements PersonnelService {
    private final static Logger log = LoggerFactory.getLogger(PersonnelServiceImpl.class);

    private final DatabaseService databaseService;
    private final KeycloakClientService keycloakClientService;

    public PersonnelServiceImpl(DatabaseService databaseService, KeycloakClientService keycloakClientService) {
        this.databaseService = databaseService;
        this.keycloakClientService = keycloakClientService;
    }


    @Override
    public final void createCompany(CreateCompany createCompany, Handler<AsyncResult<Company>> result) {
        Future.<Company>future(c -> databaseService.addCompany(createCompanyToCompany().apply(createCompany), c))
                .onComplete(result);
    }

    @Override
    public final void createBuildingFloors(CreateBuildingFloors createBuilding, Handler<AsyncResult<BuildingFloors>> result) {
        Future.<BuildingFloors>future(c -> databaseService.addBuildingFloors(createBuildingFloorsToBuildingFloors().apply(createBuilding), c))
                .onComplete(result);
    }

    @Override
    public final void getBuildingsForCompany(String companyId, Handler<AsyncResult<BuildingLocations>> result) {
        Future.<BuildingLocations>future(f -> databaseService.getBuildingLocations(UUID.fromString(companyId).toString(), f))
                .onComplete(result);
    }

    @Override
    public final void getFloorLocations(String buildingId, Handler<AsyncResult<FloorLocations>> result) {
        Future.<FloorLocations>future(f -> databaseService.getFloorLocations(UUID.fromString(buildingId).toString(), f))
                .onComplete(result);
    }

    @Override
    public final void getFloors(String buildingId, Handler<AsyncResult<List<Floor>>> result) {
        Future.<List<Floor>>future(f -> databaseService.getFloors(UUID.fromString(buildingId).toString(), f))
                .onComplete(result);
    }

    @Override
    public final void getBuildings(String buildingId, Handler<AsyncResult<List<Building>>> result) {
        Future.<Building>future(f -> databaseService.getBuildingLocation(UUID.fromString(buildingId).toString(), f))
                .flatMap(companyId -> Future.<List<Building>>future(f2 -> databaseService.getBuildings(companyId.getCompanyId().toString(), f2)))
                .onComplete(result);
    }

    @Override
    public final void getUserProfile(Personnel personnel, Handler<AsyncResult<UserProfile>> result) {
        getUserProfile(personnel).onComplete(result);
    }

    private Future<UserProfile> getUserProfile(Personnel personnel) { //todo
        return personnel.getBuildingId()
                .map(buildingId ->
                        Future.<Building>future(f -> databaseService.getBuildingLocation(buildingId.toString(), f))
                                .map(buildingLocation -> createUserProfileFrom(personnel, buildingLocation))
                ).orElse(Future.succeededFuture(createUserProfileFrom(personnel)));
    }

    @Override
    public final void getOrElseCreatePersonnel(TokenUser user, Handler<AsyncResult<Personnel>> result) {
        Future.<Personnel>future((e -> databaseService.getPersonnel(user.getId().toString(), e)))
                .flatMap(p -> {
                    if (p.getRole().isEmpty() || user.getAuthority().toPersonnelRole().isBetter(p.getRole().get())) { //todo
                        final var newP = new Personnel(p.toJson());
                        newP.setRole(user.getAuthority().toPersonnelRole());
                        return future(f -> databaseService.updatePersonnel(newP, f));
                    }
                    return succeededFuture(p);
                })
                .recover(error -> {
                    if (error instanceof ServiceException) {
                        return switch (((ServiceException) error).failureCode()) {
                            case FAILED_NO_ELEMENT_FOUND -> {
                                final var personnel = createPersonnelFrom(user);
                                yield future(f -> databaseService.addPersonnel(personnel, f));
                            }
                            default -> failedFuture(error);
                        };
                    }
                    return failedFuture(error);
                })
                .onComplete(result);
    }

    @Override
    public final void receivedPersonnelMessage(RecvPersonnelMessage recvPersonnelMessage) {
        final ChatMessage message = createChatMessage(recvPersonnelMessage);
        Future.<TelegramChannel>future(f -> createChannel(recvPersonnelMessage.getChannel(), f))
                .flatMap(telegramChannel -> Future.<ChatMessage>future(c -> databaseService.addMessage(message, c)))
                .onFailure(t -> log.error("An error occurred: ", t))
                .onSuccess(m -> log.debug("Added message: " + m.toString()));
    }

    private Future<Personnel> createPersonnel(Optional<String> firstName, Optional<String> lastName) {
        final Personnel personnel = createPersonnelFrom(firstName, lastName);
        return future(e -> databaseService.addPersonnel(personnel, e));
    }

    private Future<TelegramChannel> createTelegramChannel(UUID personnelId, Long chatId, String username) {
        final TelegramChannel chat = createTelegramChannelFrom(personnelId, chatId, username);
        return future(e -> databaseService.addChat(chat, e));
    }

    @Override
    public final void createChannel(CreateChannelMessage createChannel, Handler<AsyncResult<TelegramChannel>> result) {
        Future.<TelegramChannel>future((e -> databaseService.getChat(createChannel.getChatId(), e)))
                .recover(error -> {
                    if (error instanceof ServiceException) {
                        return switch (((ServiceException) error).failureCode()) {
                            case FAILED_NO_ELEMENT_FOUND -> createPersonnel(createChannel.getFirstName(), createChannel.getLastName())
                                    .flatMap(personnel -> createTelegramChannel(personnel.getId(), createChannel.getChatId(), createChannel.getUsername()));
                            default -> failedFuture(error);
                        };
                    }
                    return failedFuture(error);
                }).onComplete(result);
    }

    @Override
    public final void getPersonnel(String personnelId, Handler<AsyncResult<Personnel>> result) {
        getPersonnel(personnelId).onComplete(result);
    }

    private Future<Personnel> getPersonnel(String personnelId) {
        return future(f -> databaseService.getPersonnel(personnelId, f));
    }

    @Override
    public final void findPersonnel(PersonnelFilter filter, Handler<AsyncResult<List<Personnel>>> result) {
        Future.<List<Personnel>>future(f -> databaseService.findPersonnelWithFilter(filter, f))
                .onComplete(result);
    }

    @Override
    public final void updatePersonnel(String personnelId, UpdatePersonnel updatePersonnel, Handler<AsyncResult<Personnel>> result) {
        Future.<Personnel>future(f -> databaseService.getPersonnel(personnelId, f))
                .flatMap(p -> {
                    final var newRoleOpt = updatePersonnel.getRoleId().flatMap(Constants.PersonnelRole::from);
                    final var userId = UUID.fromString(personnelId);

                    final var keycloakUpdate = newRoleOpt.map(role -> {
                        final var keyU = createKeyUserRoleUpdate(userId, role);
                        return Future.<Void>future(t -> keycloakClientService.userRoleUpdate(keyU, t));
                    }).orElse(Future.succeededFuture());
                    final var pers = createPersonnelFrom(updatePersonnel, p, userId);
                    final var personnelUpdate = Future.<Personnel>future(ft -> databaseService.updatePersonnel(pers, ft));
                    return CallbackUtils.mergeFutures(personnelUpdate, keycloakUpdate)
                            .map(i -> personnelUpdate.result());
                }).onComplete(result);
    }

    @Override
    public final void deletePersonnel(String personnelId, Handler<AsyncResult<Void>> result) {
        final var keycloakDelete = Future.<Void>future(f -> keycloakClientService.deleteUser(createKeyUserDelete(personnelId), f));
        final var dbDelete = Future.<Void>future(f -> databaseService.deactivatePersonnel(personnelId, f));
        CallbackUtils.mergeFutures(keycloakDelete, dbDelete)
                .<Void>mapEmpty()
                .onComplete(result);
    }

    @Override
    public final void addTelegramToUserProfile(TelegramLoginData telegramLoginData, Handler<AsyncResult<UserProfile>> result) {
        final var personnelId = telegramLoginData.getPersonnel().getId();
        createTelegramChannel(personnelId, telegramLoginData.getChatId(), telegramLoginData.getUsername())
                .flatMap(telegramChannel -> getPersonnel(telegramChannel.getPersonnelId().toString()))
                .flatMap(this::getUserProfile)
                .onComplete(result);
    }
}
