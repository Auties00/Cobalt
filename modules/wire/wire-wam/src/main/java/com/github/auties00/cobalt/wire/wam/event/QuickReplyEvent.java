package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.LastMessageDirection;
import com.github.auties00.cobalt.wire.wam.type.QuickReplyAction;
import com.github.auties00.cobalt.wire.wam.type.QuickReplyEntryPoint;
import com.github.auties00.cobalt.wire.wam.type.QuickReplyOrigin;
import com.github.auties00.cobalt.wire.wam.type.QuickReplyTranscodeResult;
import com.github.auties00.cobalt.wire.wam.type.QuickReplyType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebQuickReplyWamEvent")
@WamEvent(id = 1468)
public interface QuickReplyEvent extends WamEventSpec {
    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong attachmentGifCount();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong attachmentImageCount();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong attachmentVideoCount();

    @WamProperty(index = 10, type = WamType.BOOLEAN)
    Optional<Boolean> isSmartDefault();

    @WamProperty(index = 12, type = WamType.STRING)
    Optional<String> labelThreadId();

    @WamProperty(index = 16, type = WamType.ENUM)
    Optional<LastMessageDirection> lastMessageDirection();

    @WamProperty(index = 20, type = WamType.STRING)
    Optional<String> listIds();

    @WamProperty(index = 17, type = WamType.INTEGER)
    OptionalLong messageDepth();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<QuickReplyAction> quickReplyAction();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong quickReplyCount();

    @WamProperty(index = 11, type = WamType.ENUM)
    Optional<QuickReplyEntryPoint> quickReplyEntryPoint();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong quickReplyKeywordCount();

    @WamProperty(index = 4, type = WamType.BOOLEAN)
    Optional<Boolean> quickReplyKeywordMatched();

    @WamProperty(index = 9, type = WamType.ENUM)
    Optional<QuickReplyOrigin> quickReplyOrigin();

    @WamProperty(index = 8, type = WamType.ENUM)
    Optional<QuickReplyTranscodeResult> quickReplyTranscodeResult();

    @WamProperty(index = 19, type = WamType.ENUM)
    Optional<QuickReplyType> quickReplyType();

    @WamProperty(index = 14, type = WamType.STRING)
    Optional<String> threadCreationDate();

    @WamProperty(index = 13, type = WamType.STRING)
    Optional<String> threadEntryPoint();

    @WamProperty(index = 15, type = WamType.STRING)
    Optional<String> threadIdHmac();
}
