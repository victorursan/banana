package com.victor.banana.services.impl;

import com.victor.banana.models.events.ActionSelected;
import com.victor.banana.models.events.personnel.UpdatePersonnel;
import com.victor.banana.models.events.UpdateTicketState;
import com.victor.banana.models.events.locations.CreateLocation;
import com.victor.banana.models.events.roles.CreateRole;
import com.victor.banana.models.events.stickies.*;
import com.victor.banana.models.events.tickets.TicketState;
import com.victor.banana.models.requests.*;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static com.victor.banana.models.events.tickets.TicketState.*;
import static com.victor.banana.utils.MappersHelper.mapTs;

public final class ReqMapper {

    public static Optional<TicketState> ticketStateDeserializer(String ticketState) {
        return switch (ticketState) {
            case "acquired" -> Optional.of(ACQUIRED);
            case "solved" -> Optional.of(SOLVED);
            case "pending" -> Optional.of(PENDING);
            default -> Optional.empty();
        };
    }

    public static Function<AddLocationReq, CreateLocation> createLocationDeserializer() {
        return ls -> CreateLocation.builder()
                .location(ls.getLocation())
                .parentLocation(ls.getParentLocation())
                .build();
    }

    public static Function<ActionUpdateReq, ActionUpdate> actionUpdateDeserializer() {
        return as -> {
            final var au = ActionUpdate.builder()
                    .id(as.getId());
            as.getAction().ifPresent(au::action);
            as.getRoleId().ifPresent(au::roleId);
            return au.build();
        };
    }

    public static Function<AddRoleReq, CreateRole> createRoleDeserializer() {
        return roleReq -> CreateRole.builder()
                .type(roleReq.getType())
                .build();
    }

    public static Function<ActionStickyReq, CreateAction> createActionDeserializer() {
        return as -> CreateAction.builder()
                .roleId(as.getRoleId())
                .message(as.getAction())
                .build();
    }

    public static Function<ActionSelectedReq, ActionSelected> actionSelectedDeserializer() {
        return actionSelectedReq -> ActionSelected.builder()
                .actionId(actionSelectedReq.getActionId())
                .locationId(actionSelectedReq.getLocationId())
                .build();
    }


    public static Function<AddStickyReq, CreateSticky> createStickyDeserializer() {
        return stickyReq -> CreateSticky.builder()
                .message(stickyReq.getMessage())
                .actions(mapTs(createActionDeserializer()).apply(stickyReq.getActions()))
                .locations(mapTs(createLocationDeserializer()).apply(stickyReq.getLocations()))
                .build();
    }

    public static Function<UpdatePersonnelReq, UpdatePersonnel> updatePersonnelDeserializer() {
        return updatePersonnelReq -> {
            final var updatePersB = UpdatePersonnel.builder();
            updatePersonnelReq.getNewRole().ifPresent(updatePersB::roleId);
            updatePersonnelReq.getNewLocation().ifPresent(updatePersB::locationId);
            return updatePersB.build();
        };
    }

    public static Function<UpdateStickyReq, UpdateSticky> updateStickyDeserializer() {
        return stickyReq -> {
            final var stickyUpdate = UpdateSticky.builder();
            stickyReq.getMessage().ifPresent(stickyUpdate::message);
            stickyReq.getActive().ifPresent(stickyUpdate::active);
            stickyReq.getActions().ifPresent(sa -> stickyUpdate.actions(UpdateStickyCreateAction.builder()
                    .add(mapTs(createActionDeserializer()).apply(sa.getAdd()))
                    .update(mapTs(actionUpdateDeserializer()).apply(sa.getUpdate()))
                    .activate(sa.getActivate())
                    .remove(sa.getRemove()).build()));
            stickyReq.getLocations().ifPresent(sl -> stickyUpdate.locations(UpdateStickyCreateLocation.builder()
                    .add(mapTs(createLocationDeserializer()).apply(sl.getAdd()))
                    .update(mapTs(stickyLocationUpdateDeserializer()).apply(sl.getUpdate()))
                    .activate(sl.getActivate())
                    .remove(sl.getRemove()).build()));
            return stickyUpdate.build();
        };
    }

    public static Function<UpdateTicketReq, UpdateTicketState> updateTicketStateDeserializer() {
        return updateTicketReq -> UpdateTicketState.builder()
                .newTicketState(ticketStateDeserializer(updateTicketReq.getNewState()).orElseThrow()) //todo
                //todo figure out .personnelId()
                .personnelId(UUID.fromString("cf338d20-073a-4f28-ad68-a104d02eef9d"))
                .build();
    }

    public static Function<LocationUpdateReq, StickyLocationUpdate> stickyLocationUpdateDeserializer() {
        return as -> {
            final var au = StickyLocationUpdate.builder()
                    .id(as.getId());
            as.getLocation().ifPresent(au::location);
            as.getParentLocation().ifPresent(au::parentLocation);
            return au.build();
        };
    }


}
