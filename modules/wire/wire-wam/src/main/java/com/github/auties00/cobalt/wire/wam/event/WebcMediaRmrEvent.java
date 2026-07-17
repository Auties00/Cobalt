package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.MediaType;
import com.github.auties00.cobalt.wire.wam.type.WebcChatType;
import com.github.auties00.cobalt.wire.wam.type.WebcRmrReasonCode;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebWebcMediaRmrWamEvent")
@WamEvent(id = 1906)
public interface WebcMediaRmrEvent extends WamEventSpec {
    @WamProperty(index = 10, type = WamType.ENUM)
    Optional<MediaType> messageMediaType();

    @WamProperty(index = 2, type = WamType.STRING)
    Optional<String> webcBrowserNetworkType();

    @WamProperty(index = 11, type = WamType.INTEGER)
    OptionalLong webcBrowserStorageQuotaBytes();

    @WamProperty(index = 12, type = WamType.INTEGER)
    OptionalLong webcBrowserStorageQuotaUsedBytes();

    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong webcChatPosition();

    @WamProperty(index = 9, type = WamType.ENUM)
    Optional<WebcChatType> webcChatType();

    @WamProperty(index = 8, type = WamType.BOOLEAN)
    Optional<Boolean> webcMediaRmrError();

    @WamProperty(index = 6, type = WamType.TIMER)
    Optional<Instant> webcMediaRmrT();

    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong webcMediaSize();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong webcMessageIndex();

    @WamProperty(index = 5, type = WamType.TIMER)
    Optional<Instant> webcMessageT();

    @WamProperty(index = 14, type = WamType.ENUM)
    Optional<WebcRmrReasonCode> webcRmrReason();

    @WamProperty(index = 13, type = WamType.INTEGER)
    OptionalLong webcRmrStatusCode();
}
