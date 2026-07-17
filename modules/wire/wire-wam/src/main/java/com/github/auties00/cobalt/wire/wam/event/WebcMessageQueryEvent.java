package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.WebcChatType;
import com.github.auties00.cobalt.wire.wam.type.WebcMessageQueryDirection;
import com.github.auties00.cobalt.wire.wam.type.WebcQueryTriggerType;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebWebcMessageQueryWamEvent")
@WamEvent(id = 1876, releaseWeight = 5)
public interface WebcMessageQueryEvent extends WamEventSpec {
    @WamProperty(index = 14, type = WamType.INTEGER)
    OptionalLong webcAudioMessageCount();

    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> webcBrowserNetworkType();

    @WamProperty(index = 20, type = WamType.INTEGER)
    OptionalLong webcBrowserStorageQuotaBytes();

    @WamProperty(index = 21, type = WamType.INTEGER)
    OptionalLong webcBrowserStorageQuotaUsedBytes();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong webcChatPosition();

    @WamProperty(index = 13, type = WamType.ENUM)
    Optional<WebcChatType> webcChatType();

    @WamProperty(index = 16, type = WamType.INTEGER)
    OptionalLong webcDocumentMessageCount();

    @WamProperty(index = 11, type = WamType.INTEGER)
    OptionalLong webcEarliestMessageIndex();

    @WamProperty(index = 12, type = WamType.TIMER)
    Optional<Instant> webcEarliestMessageT();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong webcMessageCount();

    @WamProperty(index = 19, type = WamType.ENUM)
    Optional<WebcQueryTriggerType> webcMessageQueryTrigger();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<WebcMessageQueryDirection> webcMessageQueryType();

    @WamProperty(index = 18, type = WamType.INTEGER)
    OptionalLong webcOtherMessageCount();

    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong webcPhotoMessageCount();

    @WamProperty(index = 15, type = WamType.INTEGER)
    OptionalLong webcPttMessageCount();

    @WamProperty(index = 9, type = WamType.TIMER)
    Optional<Instant> webcQueryT();

    @WamProperty(index = 10, type = WamType.INTEGER)
    OptionalLong webcResponseBytes();

    @WamProperty(index = 17, type = WamType.INTEGER)
    OptionalLong webcStickerMessageCount();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong webcTextMessageCount();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong webcVideoMessageCount();
}
