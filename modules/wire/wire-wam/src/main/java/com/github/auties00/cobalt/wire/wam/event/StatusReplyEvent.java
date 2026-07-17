package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.MessageSendResultType;
import com.github.auties00.cobalt.wire.wam.type.QuickReplySource;
import com.github.auties00.cobalt.wire.wam.type.ReplyEntryMethod;
import com.github.auties00.cobalt.wire.wam.type.ReplyExitMethod;
import com.github.auties00.cobalt.wire.wam.type.StatusCategory;
import com.github.auties00.cobalt.wire.wam.type.StatusContentType;
import com.github.auties00.cobalt.wire.wam.type.StatusPosterContactType;
import com.github.auties00.cobalt.wire.wam.type.StatusReplyMessageType;
import com.github.auties00.cobalt.wire.wam.type.StatusReplyResult;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebStatusReplyWamEvent")
@WamEvent(id = 1180)
public interface StatusReplyEvent extends WamEventSpec {
    @WamProperty(index = 7, type = WamType.BOOLEAN)
    Optional<Boolean> isMentioned();

    @WamProperty(index = 4, type = WamType.BOOLEAN)
    Optional<Boolean> isPosterBiz();

    @WamProperty(index = 6, type = WamType.BOOLEAN)
    Optional<Boolean> isPosterInAddressBook();

    @WamProperty(index = 16, type = WamType.BOOLEAN)
    Optional<Boolean> isRecentQuickReplyUsed();

    @WamProperty(index = 9, type = WamType.BOOLEAN)
    Optional<Boolean> isReplyBarBelowCanvas();

    @WamProperty(index = 10, type = WamType.BOOLEAN)
    Optional<Boolean> isReplyBarOverMedia();

    @WamProperty(index = 19, type = WamType.BOOLEAN)
    Optional<Boolean> isSubscribed();

    @WamProperty(index = 11, type = WamType.INTEGER)
    OptionalLong mediaHeight();

    @WamProperty(index = 12, type = WamType.INTEGER)
    OptionalLong mediaWidth();

    @WamProperty(index = 20, type = WamType.ENUM)
    Optional<MessageSendResultType> messageSendResult();

    @WamProperty(index = 15, type = WamType.BOOLEAN)
    Optional<Boolean> postContainedPrompt();

    @WamProperty(index = 26, type = WamType.ENUM)
    Optional<QuickReplySource> quickReplySource();

    @WamProperty(index = 13, type = WamType.ENUM)
    Optional<ReplyEntryMethod> replyEntryMethod();

    @WamProperty(index = 14, type = WamType.ENUM)
    Optional<ReplyExitMethod> replyExitMethod();

    @WamProperty(index = 17, type = WamType.ENUM)
    Optional<StatusCategory> statusCategory();

    @WamProperty(index = 8, type = WamType.ENUM)
    Optional<StatusContentType> statusContentType();

    @WamProperty(index = 23, type = WamType.STRING)
    Optional<String> statusId();

    @WamProperty(index = 25, type = WamType.ENUM)
    Optional<StatusPosterContactType> statusPosterContactType();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<StatusReplyMessageType> statusReplyMessageType();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<StatusReplyResult> statusReplyResult();

    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong statusSessionId();

    @WamProperty(index = 24, type = WamType.INTEGER)
    OptionalLong statusViewerSessionId();

    @WamProperty(index = 21, type = WamType.STRING)
    Optional<String> unifiedSessionId();

    @WamProperty(index = 22, type = WamType.INTEGER)
    OptionalLong updatesTabSessionId();
}
