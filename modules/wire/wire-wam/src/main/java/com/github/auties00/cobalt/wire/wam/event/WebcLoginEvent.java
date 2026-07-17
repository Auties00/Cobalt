package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebWebcLoginWamEvent")
@WamEvent(id = 1664)
public interface WebcLoginEvent extends WamEventSpec {
    @WamProperty(index = 14, type = WamType.STRING)
    Optional<String> webcBrowserNetworkType();

    @WamProperty(index = 15, type = WamType.INTEGER)
    OptionalLong webcBrowserStorageQuotaBytes();

    @WamProperty(index = 16, type = WamType.INTEGER)
    OptionalLong webcBrowserStorageQuotaUsedBytes();

    @WamProperty(index = 3, type = WamType.TIMER)
    Optional<Instant> webcLoginT();

    @WamProperty(index = 17, type = WamType.BOOLEAN)
    Optional<Boolean> webcPersistentLoginEnabled();

    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong webcQrCodes();

    @WamProperty(index = 2, type = WamType.TIMER)
    Optional<Instant> webcQrLoadT();

    @WamProperty(index = 8, type = WamType.INTEGER)
    OptionalLong webcSyncChatCount();

    @WamProperty(index = 10, type = WamType.INTEGER)
    OptionalLong webcSyncChatSize();

    @WamProperty(index = 9, type = WamType.TIMER)
    Optional<Instant> webcSyncChatT();

    @WamProperty(index = 11, type = WamType.INTEGER)
    OptionalLong webcSyncContactCount();

    @WamProperty(index = 13, type = WamType.INTEGER)
    OptionalLong webcSyncContactSize();

    @WamProperty(index = 12, type = WamType.TIMER)
    Optional<Instant> webcSyncContactT();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong webcSyncMessageCount();

    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong webcSyncMessageSize();

    @WamProperty(index = 6, type = WamType.TIMER)
    Optional<Instant> webcSyncMessageT();

    @WamProperty(index = 4, type = WamType.TIMER)
    Optional<Instant> webcSyncT();
}
