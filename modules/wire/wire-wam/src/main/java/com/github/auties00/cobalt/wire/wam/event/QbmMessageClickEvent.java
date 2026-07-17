package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ChatsFolderType;
import com.github.auties00.cobalt.wire.wam.type.ContactType;
import com.github.auties00.cobalt.wire.wam.type.QbmFlag;
import com.github.auties00.cobalt.wire.wam.type.QbmMessageClickButtonClickedType;
import com.github.auties00.cobalt.wire.wam.type.SignupEntryPoint;
import com.github.auties00.cobalt.wire.wam.type.ThumbnailType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebQbmMessageClickWamEvent")
@WamEvent(id = 5178)
public interface QbmMessageClickEvent extends WamEventSpec {
    @WamProperty(index = 10, type = WamType.STRING)
    Optional<String> bizTrustTier();

    @WamProperty(index = 27, type = WamType.INTEGER)
    OptionalLong bodyUrlCountInt();

    @WamProperty(index = 28, type = WamType.INTEGER)
    OptionalLong bodyUrlUniqueCountInt();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<QbmMessageClickButtonClickedType> buttonClickedType();

    @WamProperty(index = 20, type = WamType.STRING)
    Optional<String> buttonValueJsonArray();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<ChatsFolderType> chatsFolderType();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<ContactType> contactType();

    @WamProperty(index = 29, type = WamType.INTEGER)
    OptionalLong ctaUrlUniqueCountInt();

    @WamProperty(index = 17, type = WamType.STRING)
    Optional<String> decisionId();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong deltaTime();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong deltaTimeReceived();

    @WamProperty(index = 16, type = WamType.STRING)
    Optional<String> entSourceSubplatform();

    @WamProperty(index = 6, type = WamType.STRING)
    Optional<String> hsmTagStr();

    @WamProperty(index = 33, type = WamType.ENUM)
    Optional<SignupEntryPoint> iasEntryPoint();

    @WamProperty(index = 34, type = WamType.STRING)
    Optional<String> iasOptinDs();

    @WamProperty(index = 13, type = WamType.BOOLEAN)
    Optional<Boolean> isBizIntent();

    @WamProperty(index = 14, type = WamType.BOOLEAN)
    Optional<Boolean> isBroadcastMessage();

    @WamProperty(index = 31, type = WamType.BOOLEAN)
    Optional<Boolean> isCoex();

    @WamProperty(index = 35, type = WamType.BOOLEAN)
    Optional<Boolean> isIasSubscriber();

    @WamProperty(index = 15, type = WamType.BOOLEAN)
    Optional<Boolean> isInsubContact();

    @WamProperty(index = 19, type = WamType.BOOLEAN)
    Optional<Boolean> isOba();

    @WamProperty(index = 32, type = WamType.BOOLEAN)
    Optional<Boolean> isThroughDecisionService();

    @WamProperty(index = 21, type = WamType.STRING)
    Optional<String> messageFieldJsonArray();

    @WamProperty(index = 11, type = WamType.STRING)
    Optional<String> messageIdHmac();

    @WamProperty(index = 7, type = WamType.STRING)
    Optional<String> messageTypeStr();

    @WamProperty(index = 8, type = WamType.ENUM)
    Optional<QbmFlag> qbmFlag();

    @WamProperty(index = 22, type = WamType.STRING)
    Optional<String> submessageFieldJsonArray();

    @WamProperty(index = 9, type = WamType.STRING)
    Optional<String> threadIdHmac();

    @WamProperty(index = 18, type = WamType.STRING)
    Optional<String> threadLidHmac();

    @WamProperty(index = 12, type = WamType.ENUM)
    Optional<ThumbnailType> thumbnailType();

    @WamProperty(index = 30, type = WamType.INTEGER)
    OptionalLong urlUniqueCountInt();
}
