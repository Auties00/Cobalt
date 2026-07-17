package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ContactType;
import com.github.auties00.cobalt.wire.wam.type.MessageActionEntryPoint;
import com.github.auties00.cobalt.wire.wam.type.MessageLevelAction;
import com.github.auties00.cobalt.wire.wam.type.SignupEntryPoint;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebQbmMessageLevelActionWamEvent")
@WamEvent(id = 5976)
public interface QbmMessageLevelActionEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> bizTrustTier();

    @WamProperty(index = 23, type = WamType.INTEGER)
    OptionalLong bodyUrlCountInt();

    @WamProperty(index = 24, type = WamType.INTEGER)
    OptionalLong bodyUrlUniqueCountInt();

    @WamProperty(index = 16, type = WamType.STRING)
    Optional<String> buttonValueJsonArray();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<ContactType> contactType();

    @WamProperty(index = 25, type = WamType.INTEGER)
    OptionalLong ctaUrlUniqueCountInt();

    @WamProperty(index = 13, type = WamType.STRING)
    Optional<String> decisionId();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong deltaTimeReceived();

    @WamProperty(index = 12, type = WamType.STRING)
    Optional<String> entSourceSubplatform();

    @WamProperty(index = 29, type = WamType.ENUM)
    Optional<SignupEntryPoint> iasEntryPoint();

    @WamProperty(index = 30, type = WamType.STRING)
    Optional<String> iasOptinDs();

    @WamProperty(index = 10, type = WamType.BOOLEAN)
    Optional<Boolean> isBizIntent();

    @WamProperty(index = 11, type = WamType.BOOLEAN)
    Optional<Boolean> isBroadcastMessage();

    @WamProperty(index = 27, type = WamType.BOOLEAN)
    Optional<Boolean> isCoex();

    @WamProperty(index = 31, type = WamType.BOOLEAN)
    Optional<Boolean> isIasSubscriber();

    @WamProperty(index = 4, type = WamType.BOOLEAN)
    Optional<Boolean> isInsubContact();

    @WamProperty(index = 15, type = WamType.BOOLEAN)
    Optional<Boolean> isOba();

    @WamProperty(index = 28, type = WamType.BOOLEAN)
    Optional<Boolean> isThroughDecisionService();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<MessageActionEntryPoint> messageActionEntryPoint();

    @WamProperty(index = 17, type = WamType.STRING)
    Optional<String> messageFieldJsonArray();

    @WamProperty(index = 6, type = WamType.BOOLEAN)
    Optional<Boolean> messageHasUrl();

    @WamProperty(index = 7, type = WamType.STRING)
    Optional<String> messageIdHmac();

    @WamProperty(index = 8, type = WamType.ENUM)
    Optional<MessageLevelAction> messageLevelAction();

    @WamProperty(index = 18, type = WamType.STRING)
    Optional<String> submessageFieldJsonArray();

    @WamProperty(index = 9, type = WamType.STRING)
    Optional<String> threadIdHmac();

    @WamProperty(index = 14, type = WamType.STRING)
    Optional<String> threadLidHmac();

    @WamProperty(index = 26, type = WamType.INTEGER)
    OptionalLong urlUniqueCountInt();
}
