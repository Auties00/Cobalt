package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ChatsFolderType;
import com.github.auties00.cobalt.wire.wam.type.ContactType;
import com.github.auties00.cobalt.wire.wam.type.MessageBodyTypeEnum;
import com.github.auties00.cobalt.wire.wam.type.QbmFlag;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebPaidMessageVpvImpressionWamEvent")
@WamEvent(id = 7652)
public interface PaidMessageVpvImpressionEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong bodyUrlCountInt();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong bodyUrlUniqueCountInt();

    @WamProperty(index = 3, type = WamType.STRING)
    Optional<String> buttonValueJsonArray();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<ChatsFolderType> chatsFolderType();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<ContactType> contactType();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong ctaUrlUniqueCountInt();

    @WamProperty(index = 7, type = WamType.STRING)
    Optional<String> decisionId();

    @WamProperty(index = 8, type = WamType.INTEGER)
    OptionalLong deltaTime();

    @WamProperty(index = 9, type = WamType.INTEGER)
    OptionalLong deltaTimeReceived();

    @WamProperty(index = 10, type = WamType.STRING)
    Optional<String> hsmTagStr();

    @WamProperty(index = 11, type = WamType.BOOLEAN)
    Optional<Boolean> isBizIntent();

    @WamProperty(index = 12, type = WamType.BOOLEAN)
    Optional<Boolean> isBroadcastMessage();

    @WamProperty(index = 13, type = WamType.BOOLEAN)
    Optional<Boolean> isInsubContact();

    @WamProperty(index = 14, type = WamType.BOOLEAN)
    Optional<Boolean> isMuted();

    @WamProperty(index = 25, type = WamType.ENUM)
    Optional<MessageBodyTypeEnum> messageBodyType();

    @WamProperty(index = 27, type = WamType.INTEGER)
    OptionalLong messageBubbleHeightPx();

    @WamProperty(index = 28, type = WamType.INTEGER)
    OptionalLong messageBubbleWidthPx();

    @WamProperty(index = 15, type = WamType.STRING)
    Optional<String> messageFieldJsonArray();

    @WamProperty(index = 16, type = WamType.STRING)
    Optional<String> messageIdHmac();

    @WamProperty(index = 26, type = WamType.INTEGER)
    OptionalLong mmCarouselCardIndex();

    @WamProperty(index = 17, type = WamType.ENUM)
    Optional<QbmFlag> qbmFlag();

    @WamProperty(index = 18, type = WamType.BOOLEAN)
    Optional<Boolean> readReceiptsEnabled();

    @WamProperty(index = 19, type = WamType.STRING)
    Optional<String> submessageFieldJsonArray();

    @WamProperty(index = 20, type = WamType.STRING)
    Optional<String> threadIdHmac();

    @WamProperty(index = 21, type = WamType.STRING)
    Optional<String> unifiedSessionId();

    @WamProperty(index = 22, type = WamType.INTEGER)
    OptionalLong urlUniqueCountInt();

    @WamProperty(index = 23, type = WamType.INTEGER)
    OptionalLong vpvDwellTimeMs();

    @WamProperty(index = 24, type = WamType.STRING)
    Optional<String> vpvJsonObject();
}
