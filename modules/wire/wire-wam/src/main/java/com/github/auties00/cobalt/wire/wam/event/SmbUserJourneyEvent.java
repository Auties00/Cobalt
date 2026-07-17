package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ChatFilterActionTypes;
import com.github.auties00.cobalt.wire.wam.type.EntryPoint;
import com.github.auties00.cobalt.wire.wam.type.OppositePlatformEnum;
import com.github.auties00.cobalt.wire.wam.type.SmbFeatureNameEnum;
import com.github.auties00.cobalt.wire.wam.type.SmbUserActionTypeEnum;
import com.github.auties00.cobalt.wire.wam.type.SurfaceType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebSmbUserJourneyWamEvent")
@WamEvent(id = 5462)
public interface SmbUserJourneyEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<ChatFilterActionTypes> actionType();

    @WamProperty(index = 2, type = WamType.BOOLEAN)
    Optional<Boolean> contactIsSaved();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<EntryPoint> entryPoint();

    @WamProperty(index = 18, type = WamType.STRING)
    Optional<String> entryPointDetails();

    @WamProperty(index = 4, type = WamType.STRING)
    Optional<String> extraAttributes();

    @WamProperty(index = 19, type = WamType.BOOLEAN)
    Optional<Boolean> hasCatalog();

    @WamProperty(index = 20, type = WamType.BOOLEAN)
    Optional<Boolean> isCoexAccount();

    @WamProperty(index = 21, type = WamType.BOOLEAN)
    Optional<Boolean> isMvSubscriber();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<OppositePlatformEnum> oppositePlatform();

    @WamProperty(index = 14, type = WamType.ENUM)
    Optional<SurfaceType> prevSurface();

    @WamProperty(index = 12, type = WamType.INTEGER)
    OptionalLong recipientSize();

    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong seqId();

    @WamProperty(index = 8, type = WamType.ENUM)
    Optional<SmbFeatureNameEnum> smbFeatureName();

    @WamProperty(index = 13, type = WamType.ENUM)
    Optional<SmbUserActionTypeEnum> smbUserActionType();

    @WamProperty(index = 9, type = WamType.STRING)
    Optional<String> smbUserSessionId();

    @WamProperty(index = 10, type = WamType.ENUM)
    Optional<SurfaceType> surface();

    @WamProperty(index = 11, type = WamType.STRING)
    Optional<String> userActionTarget();
}
