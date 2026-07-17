package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.GroupProfileActionType;
import com.github.auties00.cobalt.wire.wam.type.PreciseSizeBucket;
import com.github.auties00.cobalt.wire.wam.type.ProfilePictureType;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebGroupProfilePictureWamEvent")
@WamEvent(id = 3652)
public interface GroupProfilePictureEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> groupCreationDs();

    @WamProperty(index = 7, type = WamType.ENUM)
    Optional<GroupProfileActionType> groupProfileAction();

    @WamProperty(index = 3, type = WamType.BOOLEAN)
    Optional<Boolean> hasProfilePicture();

    @WamProperty(index = 4, type = WamType.BOOLEAN)
    Optional<Boolean> isAdmin();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<PreciseSizeBucket> preciseGroupSizeBucket();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<ProfilePictureType> profilePictureType();
}
