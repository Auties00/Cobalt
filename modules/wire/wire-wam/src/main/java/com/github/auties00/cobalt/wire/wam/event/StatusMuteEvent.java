package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.MuteAction;
import com.github.auties00.cobalt.wire.wam.type.MuteOrigin;
import com.github.auties00.cobalt.wire.wam.type.StatusCategory;
import com.github.auties00.cobalt.wire.wam.type.StatusPosterContactType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebStatusMuteWamEvent")
@WamEvent(id = 2978)
public interface StatusMuteEvent extends WamEventSpec {
    @WamProperty(index = 15, type = WamType.STRING)
    Optional<String> cid();

    @WamProperty(index = 9, type = WamType.BOOLEAN)
    Optional<Boolean> isPosterBiz();

    @WamProperty(index = 10, type = WamType.BOOLEAN)
    Optional<Boolean> isPosterInAddressBook();

    @WamProperty(index = 8, type = WamType.ENUM)
    Optional<MuteAction> muteAction();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<MuteOrigin> muteOrigin();

    @WamProperty(index = 7, type = WamType.STRING)
    Optional<String> psaCampaignId();

    @WamProperty(index = 4, type = WamType.STRING)
    Optional<String> psaCampaignIds();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong psaCampaignItemIndex();

    @WamProperty(index = 13, type = WamType.ENUM)
    Optional<StatusCategory> statusCategory();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong statusItemIndex();

    @WamProperty(index = 14, type = WamType.ENUM)
    Optional<StatusPosterContactType> statusPosterContactType();

    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong statusSessionId();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong statusViewerSessionId();

    @WamProperty(index = 11, type = WamType.STRING)
    Optional<String> unifiedSessionId();

    @WamProperty(index = 12, type = WamType.INTEGER)
    OptionalLong updatesTabSessionId();
}
