package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.BizCatalogType;
import com.github.auties00.cobalt.wire.wam.type.ChatOriginsType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebThreadInteractionDataBizWamEvent")
@WamEvent(id = 6464)
public interface ThreadInteractionDataBizEvent extends WamEventSpec {
    @WamProperty(index = 51, type = WamType.INTEGER)
    OptionalLong autoReplyFromIcebreakerSent();

    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong awayMsgsSent();

    @WamProperty(index = 52, type = WamType.INTEGER)
    OptionalLong bizAiSuggestedRepliesSeen();

    @WamProperty(index = 53, type = WamType.INTEGER)
    OptionalLong bizAiSuggestedRepliesSentWithEdits();

    @WamProperty(index = 54, type = WamType.INTEGER)
    OptionalLong bizAiSuggestedRepliesSentWithoutEdits();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<BizCatalogType> bizCatalogType();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong bizConversationDepth();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong cartViews();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<ChatOriginsType> chatOrigins();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong collectionInquiriesSent();

    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong commerceMsgsReceived();

    @WamProperty(index = 8, type = WamType.INTEGER)
    OptionalLong commerceMsgsSent();

    @WamProperty(index = 9, type = WamType.STRING)
    Optional<String> entryPointConversionApp();

    @WamProperty(index = 10, type = WamType.STRING)
    Optional<String> entryPointConversionSource();

    @WamProperty(index = 11, type = WamType.INTEGER)
    OptionalLong firstResponseTime();

    @WamProperty(index = 41, type = WamType.INTEGER)
    OptionalLong fmxNotMvBottomSheetDismissedCount();

    @WamProperty(index = 42, type = WamType.INTEGER)
    OptionalLong fmxNotMvBottomSheetGetMvButtonClicks();

    @WamProperty(index = 43, type = WamType.INTEGER)
    OptionalLong fmxNotMvBottomSheetGetMvButtonImpressions();

    @WamProperty(index = 44, type = WamType.INTEGER)
    OptionalLong fmxNotMvBottomSheetImpressions();

    @WamProperty(index = 45, type = WamType.INTEGER)
    OptionalLong fmxNotMvBottomSheetLearnMoreButtonClicks();

    @WamProperty(index = 46, type = WamType.INTEGER)
    OptionalLong fmxNotMvClicks();

    @WamProperty(index = 13, type = WamType.BOOLEAN)
    Optional<Boolean> groupContainsBiz();

    @WamProperty(index = 47, type = WamType.BOOLEAN)
    Optional<Boolean> isBizMvFrictionEligible();

    @WamProperty(index = 14, type = WamType.BOOLEAN)
    Optional<Boolean> isCommerceViewed();

    @WamProperty(index = 15, type = WamType.BOOLEAN)
    Optional<Boolean> isCtaOnPdpClicked();

    @WamProperty(index = 16, type = WamType.BOOLEAN)
    Optional<Boolean> isLabelled();

    @WamProperty(index = 36, type = WamType.BOOLEAN)
    Optional<Boolean> isOppositePartyInitiated();

    @WamProperty(index = 17, type = WamType.BOOLEAN)
    Optional<Boolean> isUser1pBizBotChat();

    @WamProperty(index = 18, type = WamType.BOOLEAN)
    Optional<Boolean> isUser3pBotChat();

    @WamProperty(index = 19, type = WamType.BOOLEAN)
    Optional<Boolean> isUserAgent();

    @WamProperty(index = 20, type = WamType.BOOLEAN)
    Optional<Boolean> isUserCreatedAgent();

    @WamProperty(index = 21, type = WamType.INTEGER)
    OptionalLong labelledMsgs();

    @WamProperty(index = 37, type = WamType.INTEGER)
    OptionalLong locationsSent();

    @WamProperty(index = 49, type = WamType.STRING)
    Optional<String> matchedMessagesMarkedAsReadWithDeltaTime();

    @WamProperty(index = 50, type = WamType.STRING)
    Optional<String> matchedMessagesReadWithDeltaTime();

    @WamProperty(index = 48, type = WamType.INTEGER)
    OptionalLong notMvImpressions();

    @WamProperty(index = 40, type = WamType.BOOLEAN)
    Optional<Boolean> oppositePartyHasBadge();

    @WamProperty(index = 22, type = WamType.BOOLEAN)
    Optional<Boolean> oppositePartyHasBusinessIntent();

    @WamProperty(index = 23, type = WamType.INTEGER)
    OptionalLong ordersSent();

    @WamProperty(index = 24, type = WamType.INTEGER)
    OptionalLong pdpInquiriesSent();

    @WamProperty(index = 25, type = WamType.INTEGER)
    OptionalLong pdpViews();

    @WamProperty(index = 26, type = WamType.INTEGER)
    OptionalLong quickRepliesSent();

    @WamProperty(index = 27, type = WamType.INTEGER)
    OptionalLong smbMarketingMessagesReactionsSent();

    @WamProperty(index = 28, type = WamType.INTEGER)
    OptionalLong smbMarketingMessagesRepliesSent();

    @WamProperty(index = 29, type = WamType.INTEGER)
    OptionalLong smbMarketingMessagesSpamReports();

    @WamProperty(index = 30, type = WamType.INTEGER)
    OptionalLong smbMarketingMsgsReceived();

    @WamProperty(index = 31, type = WamType.INTEGER)
    OptionalLong smbMarketingMsgsSent();

    @WamProperty(index = 39, type = WamType.STRING)
    Optional<String> threadCreationDate();

    @WamProperty(index = 32, type = WamType.STRING)
    Optional<String> threadDs();

    @WamProperty(index = 33, type = WamType.STRING)
    Optional<String> threadId();

    @WamProperty(index = 38, type = WamType.STRING)
    Optional<String> threadIdByLid();

    @WamProperty(index = 35, type = WamType.BOOLEAN)
    Optional<Boolean> userHasBusinessIntent();
}
