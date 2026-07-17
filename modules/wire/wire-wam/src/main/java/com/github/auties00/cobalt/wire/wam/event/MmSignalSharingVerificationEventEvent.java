package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamChannel;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ConsentSource;
import com.github.auties00.cobalt.wire.wam.type.MmDirectionFrom;
import com.github.auties00.cobalt.wire.wam.type.OnePdSignalNotSharedReason;
import com.github.auties00.cobalt.wire.wam.type.SignalCanceledReason;
import com.github.auties00.cobalt.wire.wam.type.SignalMessageState;
import com.github.auties00.cobalt.wire.wam.type.SignalMessageType;
import com.github.auties00.cobalt.wire.wam.type.SignalOrigin;
import com.github.auties00.cobalt.wire.wam.type.SignalSharingStatus;
import com.github.auties00.cobalt.wire.wam.type.SignalSurface;
import com.github.auties00.cobalt.wire.wam.type.SignalType;
import com.github.auties00.cobalt.wire.wam.type.SpSignalNotSharedReason;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebMmSignalSharingVerificationEventWamEvent")
@WamEvent(id = 6554, channel = WamChannel.PRIVATE, privateStatsId = 113760892)
public interface MmSignalSharingVerificationEventEvent extends WamEventSpec {
    @WamProperty(index = 28, type = WamType.BOOLEAN)
    Optional<Boolean> accountLinked();

    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> businessLidOrJid();

    @WamProperty(index = 3, type = WamType.STRING)
    Optional<String> collectionWindowId();

    @WamProperty(index = 29, type = WamType.ENUM)
    Optional<ConsentSource> consentSource();

    @WamProperty(index = 18, type = WamType.INTEGER)
    OptionalLong deltaTimeReceived();

    @WamProperty(index = 17, type = WamType.STRING)
    Optional<String> entSourceSubplatform();

    @WamProperty(index = 16, type = WamType.BOOLEAN)
    Optional<Boolean> isCompanionDevice();

    @WamProperty(index = 22, type = WamType.BOOLEAN)
    Optional<Boolean> isIabRestore();

    @WamProperty(index = 24, type = WamType.BOOLEAN)
    Optional<Boolean> isLatestConversionToken();

    @WamProperty(index = 20, type = WamType.BOOLEAN)
    Optional<Boolean> isNetworkAvailable();

    @WamProperty(index = 21, type = WamType.BOOLEAN)
    Optional<Boolean> isShimmingSignal();

    @WamProperty(index = 4, type = WamType.BOOLEAN)
    Optional<Boolean> isUserDisclosed();

    @WamProperty(index = 25, type = WamType.INTEGER)
    OptionalLong mmConversationDepth();

    @WamProperty(index = 26, type = WamType.INTEGER)
    OptionalLong mmConversationRepeat();

    @WamProperty(index = 27, type = WamType.ENUM)
    Optional<MmDirectionFrom> mmDirectionFrom();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<OnePdSignalNotSharedReason> onePdSignalNotSharedReason();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<SignalCanceledReason> signalCanceledReason();

    @WamProperty(index = 7, type = WamType.ENUM)
    Optional<SignalMessageState> signalMessageState();

    @WamProperty(index = 8, type = WamType.ENUM)
    Optional<SignalMessageType> signalMessageType();

    @WamProperty(index = 9, type = WamType.ENUM)
    Optional<SignalOrigin> signalOrigin();

    @WamProperty(index = 10, type = WamType.ENUM)
    Optional<SignalSharingStatus> signalSharingStatus();

    @WamProperty(index = 11, type = WamType.ENUM)
    Optional<SignalSurface> signalSurface();

    @WamProperty(index = 12, type = WamType.ENUM)
    Optional<SignalType> signalType();

    @WamProperty(index = 23, type = WamType.STRING)
    Optional<String> signalTypeOrigin();

    @WamProperty(index = 13, type = WamType.ENUM)
    Optional<SpSignalNotSharedReason> spSignalNotSharedReason();

    @WamProperty(index = 14, type = WamType.STRING)
    Optional<String> templateId();
}
