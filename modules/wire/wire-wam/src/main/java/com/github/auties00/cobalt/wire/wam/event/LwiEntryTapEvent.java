package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.LwiEntryPoint;
import com.github.auties00.cobalt.wire.wam.type.LwiSubEntryPoint;
import com.github.auties00.cobalt.wire.wam.type.StatusTypeMedia;
import com.github.auties00.cobalt.wire.wam.type.WebFlowType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebLwiEntryTapWamEvent")
@WamEvent(id = 2770)
public interface LwiEntryTapEvent extends WamEventSpec {
    @WamProperty(index = 14, type = WamType.INTEGER)
    OptionalLong activeItemsCount();

    @WamProperty(index = 15, type = WamType.INTEGER)
    OptionalLong archivedItemsCount();

    @WamProperty(index = 4, type = WamType.STRING)
    Optional<String> businessToolsSessionId();

    @WamProperty(index = 2, type = WamType.STRING)
    Optional<String> catalogSessionId();

    @WamProperty(index = 11, type = WamType.INTEGER)
    OptionalLong itemsCount();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<LwiEntryPoint> lwiEntryPoint();

    @WamProperty(index = 17, type = WamType.STRING)
    Optional<String> lwiExtras();

    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> lwiFlowId();

    @WamProperty(index = 7, type = WamType.ENUM)
    Optional<LwiSubEntryPoint> lwiSubEntryPoint();

    @WamProperty(index = 16, type = WamType.STRING)
    Optional<String> notificationLogId();

    @WamProperty(index = 13, type = WamType.STRING)
    Optional<String> previousLwiFlowId();

    @WamProperty(index = 18, type = WamType.STRING)
    Optional<String> statusId();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong statusSessionId();

    @WamProperty(index = 12, type = WamType.ENUM)
    Optional<StatusTypeMedia> statusTypeMedia();

    @WamProperty(index = 5, type = WamType.BOOLEAN)
    Optional<Boolean> userHasLinkedFbPage();

    @WamProperty(index = 10, type = WamType.STRING)
    Optional<String> waCampaignId();

    @WamProperty(index = 19, type = WamType.ENUM)
    Optional<WebFlowType> webFlowType();
}
