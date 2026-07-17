package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ApplicationState;
import com.github.auties00.cobalt.wire.wam.type.PsBufferUploadResult;
import com.github.auties00.cobalt.wire.wam.type.PsTokenNotReadyReason;
import com.github.auties00.cobalt.wire.wam.type.PsUploadReason;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebPsBufferUploadWamEvent")
@WamEvent(id = 2244, releaseWeight = 100)
public interface PsBufferUploadEvent extends WamEventSpec {
    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<ApplicationState> applicationState();

    @WamProperty(index = 12, type = WamType.BOOLEAN)
    Optional<Boolean> isFromWamsys();

    @WamProperty(index = 15, type = WamType.BOOLEAN)
    Optional<Boolean> isRealtime();

    @WamProperty(index = 14, type = WamType.BOOLEAN)
    Optional<Boolean> isUserSampled();

    @WamProperty(index = 13, type = WamType.INTEGER)
    OptionalLong psBufferSequenceNumber();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong psBufferUploadHttpResponseCode();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<PsBufferUploadResult> psBufferUploadResult();

    @WamProperty(index = 2, type = WamType.TIMER)
    Optional<Instant> psBufferUploadT();

    @WamProperty(index = 11, type = WamType.INTEGER)
    OptionalLong psDitheredT();

    @WamProperty(index = 10, type = WamType.BOOLEAN)
    Optional<Boolean> psForceUpload();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<PsTokenNotReadyReason> psTokenNotReadyReason();

    @WamProperty(index = 9, type = WamType.ENUM)
    Optional<PsUploadReason> psUploadReason();

    @WamProperty(index = 5, type = WamType.BOOLEAN)
    Optional<Boolean> waConnectedToChatd();
}
