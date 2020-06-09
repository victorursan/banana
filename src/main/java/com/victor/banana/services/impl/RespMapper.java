package com.victor.banana.services.impl;

import com.victor.banana.models.events.UserProfile;
import com.victor.banana.models.events.locations.Location;
import com.victor.banana.models.events.personnel.Personnel;
import com.victor.banana.models.events.roles.Role;
import com.victor.banana.models.events.stickies.Action;
import com.victor.banana.models.events.stickies.ActionState;
import com.victor.banana.models.events.stickies.Sticky;
import com.victor.banana.models.events.stickies.StickyLocation;
import com.victor.banana.models.events.tickets.Ticket;
import com.victor.banana.models.events.tickets.TicketState;
import com.victor.banana.models.responses.*;
import org.apache.commons.lang3.StringUtils;

import java.util.function.Function;

import static com.victor.banana.utils.MappersHelper.mapTs;

public final class RespMapper {
    public static Function<Location, LocationResp> locationSerializer() {
        return l -> LocationResp.builder()
                .id(l.getId())
                .parentLocation(l.getParentLocation())
                .message(l.getText())
                .build();
    }

    public static Function<Role, RoleResp> roleSerializer() {
        return r -> RoleResp.builder()
                .id(r.getId())
                .role(r.getType())
                .build();
    }

    public static Function<Personnel, PersonnelResp> personnelSerializer() {
        return p -> PersonnelResp.builder()
                .id(p.getId())
                .firstName(p.getFirstName())
                .lastName(p.getLastName())
                .email(p.getEmail())
                .username(p.getTelegramUsername())
                .locationId(p.getLocationId())
                .roleId(p.getRole().getUuid())
                .build();
    }


    public static Function<Ticket, TicketResp> ticketSerializer() {
        return ticketSerializer(false);
    }

    public static Function<Ticket, TicketResp> ticketSerializer(boolean full) {
        return t -> {
            final var ticketRespB = TicketResp.builder()
                    .ticketId(t.getId())
                    .message(t.getMessage())
                    .state(ticketStateSerializer(t.getState()))
                    .createdAt(t.getCreatedAt());
            if (full) {
                ticketRespB.acquiredAt(t.getAcquiredAt())
                        .solvedAt(t.getSolvedAt())
                        .ownedBy(t.getOwnedBy());
            }
            return ticketRespB.build();
        };
    }

    public static Function<StickyLocation, StickyLocationResp> stickyLocationSerializer() {
        return s -> StickyLocationResp.builder()
                .id(s.getId())
                .message(s.getMessage())
                .actions(mapTs(actionStickySerializer()).apply(s.getActions()))
                .locationId(s.getLocationId())
                .build();
    }

    public static Function<Action, ActionStickyResp> actionStickySerializer() {
        return a -> ActionStickyResp.builder()
                .id(a.getId())
                .roleId(a.getRoleId())
                .message(a.getMessage())
                .state(actionStateSerializer(a.getState()))
                .build();
    }

    public static Function<Sticky, StickyResp> stickySerializer() {
        return sticky -> StickyResp.builder()
                .id(sticky.getId())
                .message(sticky.getMessage())
                .actions(mapTs(actionStickySerializer()).apply(sticky.getActions()))
                .locations(mapTs(locationSerializer()).apply(sticky.getLocations()))
                .build();
    }

    public static Function<UserProfile, UserProfileResp> userProfileSerializer() {
        return userProfile -> UserProfileResp.builder()
                .id(userProfile.getPersonnel().getId())
                .email(userProfile.getPersonnel().getEmail().orElse(StringUtils.EMPTY))
                .firstName(userProfile.getPersonnel().getFirstName().orElse(StringUtils.EMPTY))
                .lastName(userProfile.getPersonnel().getLastName().orElse(StringUtils.EMPTY))
                .role(roleSerializer().apply(userProfile.getRole()))
                .location(locationSerializer().apply(userProfile.getLocation()))
                .telegramUsername(userProfile.getPersonnel().getTelegramUsername())
                .build();
    }

    public static String ticketStateSerializer(TicketState ticketState) {
        return switch (ticketState) {
            case ACQUIRED -> "acquired";
            case SOLVED -> "solved";
            case PENDING -> "pending";
        };
    }

    public static String actionStateSerializer(ActionState actionState) {
        return switch (actionState) {
            case AVAILABLE -> "available";
            case IN_PROGRESS -> "in_progress";
        };
    }
}
