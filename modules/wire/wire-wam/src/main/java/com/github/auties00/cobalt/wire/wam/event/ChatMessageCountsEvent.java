package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.AiChatOriginsType;
import com.github.auties00.cobalt.wire.wam.type.BizCatalogType;
import com.github.auties00.cobalt.wire.wam.type.BlockReason;
import com.github.auties00.cobalt.wire.wam.type.ChatMutedType;
import com.github.auties00.cobalt.wire.wam.type.ChatOriginsType;
import com.github.auties00.cobalt.wire.wam.type.ChatType;
import com.github.auties00.cobalt.wire.wam.type.DisappearingChatInitiatorType;
import com.github.auties00.cobalt.wire.wam.type.EphemeralityInitiatorType;
import com.github.auties00.cobalt.wire.wam.type.EphemeralityTriggerActionType;
import com.github.auties00.cobalt.wire.wam.type.GaStatus;
import com.github.auties00.cobalt.wire.wam.type.GroupInfoSettingType;
import com.github.auties00.cobalt.wire.wam.type.OppositeVisibleIdentificationType;
import com.github.auties00.cobalt.wire.wam.type.TypeOfGroupEnum;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebChatMessageCountsWamEvent")
@WamEvent(id = 1644)
public interface ChatMessageCountsEvent extends WamEventSpec {
    @WamProperty(index = 224, type = WamType.ENUM)
    Optional<AiChatOriginsType> aiChatOrigins();

    @WamProperty(index = 195, type = WamType.STRING)
    Optional<String> aiDiscoveryTab();

    @WamProperty(index = 149, type = WamType.INTEGER)
    OptionalLong audioMessagesReceived();

    @WamProperty(index = 150, type = WamType.INTEGER)
    OptionalLong audioMessagesSent();

    @WamProperty(index = 190, type = WamType.INTEGER)
    OptionalLong autoReplyFromIcebreakerSent();

    @WamProperty(index = 56, type = WamType.INTEGER)
    OptionalLong awayMsgsSent();

    @WamProperty(index = 60, type = WamType.ENUM)
    Optional<BizCatalogType> bizCatalogType();

    @WamProperty(index = 65, type = WamType.INTEGER)
    OptionalLong bizConversationDepth();

    @WamProperty(index = 33, type = WamType.ENUM)
    Optional<BlockReason> blockReason();

    @WamProperty(index = 206, type = WamType.INTEGER)
    OptionalLong bottomSheetAnimatedSent();

    @WamProperty(index = 207, type = WamType.INTEGER)
    OptionalLong bottomSheetEditedAnimatedSent();

    @WamProperty(index = 208, type = WamType.INTEGER)
    OptionalLong bottomSheetEditedSent();

    @WamProperty(index = 196, type = WamType.INTEGER)
    OptionalLong bottomSheetForwardMessagesSent();

    @WamProperty(index = 209, type = WamType.INTEGER)
    OptionalLong bottomSheetImagesEdited();

    @WamProperty(index = 197, type = WamType.INTEGER)
    OptionalLong bottomSheetImagesGenerated();

    @WamProperty(index = 220, type = WamType.INTEGER)
    OptionalLong bottomSheetMemuImagesGenerated();

    @WamProperty(index = 221, type = WamType.INTEGER)
    OptionalLong bottomSheetMemuInitiated();

    @WamProperty(index = 222, type = WamType.INTEGER)
    OptionalLong bottomSheetMemuMessagesSent();

    @WamProperty(index = 198, type = WamType.INTEGER)
    OptionalLong bottomSheetMessagesDownloaded();

    @WamProperty(index = 199, type = WamType.INTEGER)
    OptionalLong bottomSheetMessagesReceived();

    @WamProperty(index = 200, type = WamType.INTEGER)
    OptionalLong bottomSheetMessagesSent();

    @WamProperty(index = 201, type = WamType.INTEGER)
    OptionalLong bottomSheetPromptsInitiated();

    @WamProperty(index = 210, type = WamType.INTEGER)
    OptionalLong bottomSheetRegeneratedSent();

    @WamProperty(index = 30, type = WamType.INTEGER)
    OptionalLong broadcastMsgsReceived();

    @WamProperty(index = 29, type = WamType.INTEGER)
    OptionalLong broadcastMsgsSent();

    @WamProperty(index = 27, type = WamType.INTEGER)
    OptionalLong callOffersReceived();

    @WamProperty(index = 26, type = WamType.INTEGER)
    OptionalLong callOffersSent();

    @WamProperty(index = 70, type = WamType.INTEGER)
    OptionalLong callsResultBusy();

    @WamProperty(index = 71, type = WamType.INTEGER)
    OptionalLong callsResultCancelled();

    @WamProperty(index = 72, type = WamType.INTEGER)
    OptionalLong callsResultConnected();

    @WamProperty(index = 78, type = WamType.INTEGER)
    OptionalLong callsResultError();

    @WamProperty(index = 73, type = WamType.INTEGER)
    OptionalLong callsResultMissed();

    @WamProperty(index = 74, type = WamType.INTEGER)
    OptionalLong callsResultRejected();

    @WamProperty(index = 88, type = WamType.BOOLEAN)
    Optional<Boolean> canEditDmSettings();

    @WamProperty(index = 86, type = WamType.INTEGER)
    OptionalLong cartViews();

    @WamProperty(index = 15, type = WamType.INTEGER)
    OptionalLong chatEphemeralityDuration();

    @WamProperty(index = 8, type = WamType.ENUM)
    Optional<ChatMutedType> chatMuted();

    @WamProperty(index = 179, type = WamType.ENUM)
    Optional<ChatOriginsType> chatOrigins();

    @WamProperty(index = 79, type = WamType.INTEGER)
    OptionalLong chatOverflowClicks();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<ChatType> chatTypeInd();

    @WamProperty(index = 44, type = WamType.INTEGER)
    OptionalLong collectionInquiriesSent();

    @WamProperty(index = 174, type = WamType.INTEGER)
    OptionalLong commandSheetShow();

    @WamProperty(index = 126, type = WamType.INTEGER)
    OptionalLong commentsReceived();

    @WamProperty(index = 41, type = WamType.INTEGER)
    OptionalLong commerceMsgsReceived();

    @WamProperty(index = 40, type = WamType.INTEGER)
    OptionalLong commerceMsgsSent();

    @WamProperty(index = 219, type = WamType.STRING)
    Optional<String> deviceLanguage();

    @WamProperty(index = 59, type = WamType.ENUM)
    Optional<DisappearingChatInitiatorType> disappearingChatInitiator();

    @WamProperty(index = 151, type = WamType.INTEGER)
    OptionalLong documentMessagesReceived();

    @WamProperty(index = 152, type = WamType.INTEGER)
    OptionalLong documentMessagesSent();

    @WamProperty(index = 100, type = WamType.INTEGER)
    OptionalLong editedMsgsSent();

    @WamProperty(index = 47, type = WamType.STRING)
    Optional<String> entryPointConversionApp();

    @WamProperty(index = 46, type = WamType.STRING)
    Optional<String> entryPointConversionSource();

    @WamProperty(index = 248, type = WamType.INTEGER)
    OptionalLong ephemeralMessagesExpired();

    @WamProperty(index = 14, type = WamType.INTEGER)
    OptionalLong ephemeralMessagesReceived();

    @WamProperty(index = 13, type = WamType.INTEGER)
    OptionalLong ephemeralMessagesSent();

    @WamProperty(index = 69, type = WamType.INTEGER)
    OptionalLong ephemeralMessagesUnreadExpired();

    @WamProperty(index = 109, type = WamType.ENUM)
    Optional<EphemeralityInitiatorType> ephemeralityInitiator();

    @WamProperty(index = 110, type = WamType.ENUM)
    Optional<EphemeralityTriggerActionType> ephemeralityTriggerAction();

    @WamProperty(index = 142, type = WamType.INTEGER)
    OptionalLong eventCreationMessagesReceived();

    @WamProperty(index = 143, type = WamType.INTEGER)
    OptionalLong eventCreationMessagesSent();

    @WamProperty(index = 144, type = WamType.INTEGER)
    OptionalLong eventResponseMessagesReceived();

    @WamProperty(index = 145, type = WamType.INTEGER)
    OptionalLong eventResponseMessagesSent();

    @WamProperty(index = 45, type = WamType.INTEGER)
    OptionalLong fbCtaInquiriesSent();

    @WamProperty(index = 25, type = WamType.INTEGER)
    OptionalLong firstResponseTime();

    @WamProperty(index = 234, type = WamType.INTEGER)
    OptionalLong fmxNotMvBottomSheetDismissedCount();

    @WamProperty(index = 235, type = WamType.INTEGER)
    OptionalLong fmxNotMvBottomSheetGetMvButtonClicks();

    @WamProperty(index = 236, type = WamType.INTEGER)
    OptionalLong fmxNotMvBottomSheetGetMvButtonImpressions();

    @WamProperty(index = 237, type = WamType.INTEGER)
    OptionalLong fmxNotMvBottomSheetImpressions();

    @WamProperty(index = 238, type = WamType.INTEGER)
    OptionalLong fmxNotMvBottomSheetLearnMoreButtonClicks();

    @WamProperty(index = 229, type = WamType.INTEGER)
    OptionalLong fmxNotMvClicks();

    @WamProperty(index = 153, type = WamType.INTEGER)
    OptionalLong forwardAudioMessagesReceived();

    @WamProperty(index = 154, type = WamType.INTEGER)
    OptionalLong forwardAudioMessagesSent();

    @WamProperty(index = 155, type = WamType.INTEGER)
    OptionalLong forwardDocumentMessagesReceived();

    @WamProperty(index = 156, type = WamType.INTEGER)
    OptionalLong forwardDocumentMessagesSent();

    @WamProperty(index = 157, type = WamType.INTEGER)
    OptionalLong forwardGifMessagesReceived();

    @WamProperty(index = 158, type = WamType.INTEGER)
    OptionalLong forwardGifMessagesSent();

    @WamProperty(index = 89, type = WamType.INTEGER)
    OptionalLong forwardMessagesReceived();

    @WamProperty(index = 90, type = WamType.INTEGER)
    OptionalLong forwardMessagesSent();

    @WamProperty(index = 128, type = WamType.INTEGER)
    OptionalLong forwardPhotoMessagesReceived();

    @WamProperty(index = 129, type = WamType.INTEGER)
    OptionalLong forwardPhotoMessagesSent();

    @WamProperty(index = 159, type = WamType.INTEGER)
    OptionalLong forwardPtvMessagesReceived();

    @WamProperty(index = 160, type = WamType.INTEGER)
    OptionalLong forwardPtvMessagesSent();

    @WamProperty(index = 161, type = WamType.INTEGER)
    OptionalLong forwardStatusReplyMessagesReceived();

    @WamProperty(index = 162, type = WamType.INTEGER)
    OptionalLong forwardStatusReplyMessagesSent();

    @WamProperty(index = 163, type = WamType.INTEGER)
    OptionalLong forwardStickerMessagesReceived();

    @WamProperty(index = 164, type = WamType.INTEGER)
    OptionalLong forwardStickerMessagesSent();

    @WamProperty(index = 130, type = WamType.INTEGER)
    OptionalLong forwardTextMessagesReceived();

    @WamProperty(index = 131, type = WamType.INTEGER)
    OptionalLong forwardTextMessagesSent();

    @WamProperty(index = 132, type = WamType.INTEGER)
    OptionalLong forwardUrlMessagesReceived();

    @WamProperty(index = 133, type = WamType.INTEGER)
    OptionalLong forwardUrlMessagesSent();

    @WamProperty(index = 165, type = WamType.INTEGER)
    OptionalLong forwardVideoMessagesReceived();

    @WamProperty(index = 166, type = WamType.INTEGER)
    OptionalLong forwardVideoMessagesSent();

    @WamProperty(index = 22, type = WamType.ENUM)
    Optional<GaStatus> gaStatus();

    @WamProperty(index = 167, type = WamType.INTEGER)
    OptionalLong gifMessagesReceived();

    @WamProperty(index = 168, type = WamType.INTEGER)
    OptionalLong gifMessagesSent();

    @WamProperty(index = 214, type = WamType.INTEGER)
    OptionalLong googleSearchClick();

    @WamProperty(index = 215, type = WamType.INTEGER)
    OptionalLong googleSearchShow();

    @WamProperty(index = 57, type = WamType.INTEGER)
    OptionalLong greetingMsgsSent();

    @WamProperty(index = 75, type = WamType.BOOLEAN)
    Optional<Boolean> groupContainsBiz();

    @WamProperty(index = 87, type = WamType.ENUM)
    Optional<GroupInfoSettingType> groupInfoSetting();

    @WamProperty(index = 225, type = WamType.INTEGER)
    OptionalLong groupLimitSharingOnCnt();

    @WamProperty(index = 51, type = WamType.INTEGER)
    OptionalLong groupMembershipReplies();

    @WamProperty(index = 52, type = WamType.INTEGER)
    OptionalLong groupPrivateReplies();

    @WamProperty(index = 19, type = WamType.INTEGER)
    OptionalLong groupSize();

    @WamProperty(index = 249, type = WamType.INTEGER)
    OptionalLong groupStatusLikesOthersToOthers();

    @WamProperty(index = 250, type = WamType.INTEGER)
    OptionalLong groupStatusLikesOthersToOwn();

    @WamProperty(index = 251, type = WamType.INTEGER)
    OptionalLong groupStatusLikesOwnToOthers();

    @WamProperty(index = 252, type = WamType.INTEGER)
    OptionalLong groupStatusLikesOwnToOwn();

    @WamProperty(index = 253, type = WamType.INTEGER)
    OptionalLong groupStatusRepliesOthersToOthers();

    @WamProperty(index = 254, type = WamType.INTEGER)
    OptionalLong groupStatusRepliesOthersToOwn();

    @WamProperty(index = 255, type = WamType.INTEGER)
    OptionalLong groupStatusRepliesOwnToOthers();

    @WamProperty(index = 256, type = WamType.INTEGER)
    OptionalLong groupStatusRepliesOwnToOwn();

    @WamProperty(index = 216, type = WamType.BOOLEAN)
    Optional<Boolean> hasReplied1On1();

    @WamProperty(index = 180, type = WamType.BOOLEAN)
    Optional<Boolean> hasUsername();

    @WamProperty(index = 228, type = WamType.BOOLEAN)
    Optional<Boolean> hasUsernamePin();

    @WamProperty(index = 175, type = WamType.INTEGER)
    OptionalLong imagineCommandClick();

    @WamProperty(index = 202, type = WamType.INTEGER)
    OptionalLong imagineMeMessagesSent();

    @WamProperty(index = 203, type = WamType.INTEGER)
    OptionalLong imagineMePromptsInitiatedCount();

    @WamProperty(index = 146, type = WamType.INTEGER)
    OptionalLong imagineMentionClick();

    @WamProperty(index = 176, type = WamType.INTEGER)
    OptionalLong imagineMentionShow();

    @WamProperty(index = 6, type = WamType.BOOLEAN)
    Optional<Boolean> isAContact();

    @WamProperty(index = 213, type = WamType.BOOLEAN)
    Optional<Boolean> isAContactAtThreadCreation();

    @WamProperty(index = 5, type = WamType.BOOLEAN)
    Optional<Boolean> isAGroup();

    @WamProperty(index = 10, type = WamType.BOOLEAN)
    Optional<Boolean> isArchived();

    @WamProperty(index = 108, type = WamType.BOOLEAN)
    Optional<Boolean> isAutoMuted();

    @WamProperty(index = 230, type = WamType.BOOLEAN)
    Optional<Boolean> isBizMvFrictionEligible();

    @WamProperty(index = 32, type = WamType.BOOLEAN)
    Optional<Boolean> isBlocked();

    @WamProperty(index = 36, type = WamType.BOOLEAN)
    Optional<Boolean> isCartAddClicked();

    @WamProperty(index = 35, type = WamType.BOOLEAN)
    Optional<Boolean> isCommerceViewed();

    @WamProperty(index = 37, type = WamType.BOOLEAN)
    Optional<Boolean> isCtaOnPdpClicked();

    @WamProperty(index = 185, type = WamType.BOOLEAN)
    Optional<Boolean> isDeleted();

    @WamProperty(index = 257, type = WamType.BOOLEAN)
    Optional<Boolean> isGuestThread();

    @WamProperty(index = 106, type = WamType.BOOLEAN)
    Optional<Boolean> isInviteCreatedThread();

    @WamProperty(index = 54, type = WamType.BOOLEAN)
    Optional<Boolean> isLabelled();

    @WamProperty(index = 260, type = WamType.BOOLEAN)
    Optional<Boolean> isManagedAccount();

    @WamProperty(index = 91, type = WamType.BOOLEAN)
    Optional<Boolean> isMessageYourself();

    @WamProperty(index = 211, type = WamType.BOOLEAN)
    Optional<Boolean> isMetaAiAssistant();

    @WamProperty(index = 261, type = WamType.BOOLEAN)
    Optional<Boolean> isNewManagedAccountEmIgnored();

    @WamProperty(index = 62, type = WamType.BOOLEAN)
    Optional<Boolean> isOppositePartyInitiated();

    @WamProperty(index = 9, type = WamType.BOOLEAN)
    Optional<Boolean> isPinned();

    @WamProperty(index = 92, type = WamType.BOOLEAN)
    Optional<Boolean> isPnhEnabledChat();

    @WamProperty(index = 186, type = WamType.BOOLEAN)
    Optional<Boolean> isReported();

    @WamProperty(index = 141, type = WamType.BOOLEAN)
    Optional<Boolean> isUser1pBizBotChat();

    @WamProperty(index = 140, type = WamType.BOOLEAN)
    Optional<Boolean> isUser3pBotChat();

    @WamProperty(index = 111, type = WamType.BOOLEAN)
    Optional<Boolean> isUserAgent();

    @WamProperty(index = 123, type = WamType.BOOLEAN)
    Optional<Boolean> isUserCreatedAgent();

    @WamProperty(index = 258, type = WamType.BOOLEAN)
    Optional<Boolean> isUsernameThread();

    @WamProperty(index = 259, type = WamType.BOOLEAN)
    Optional<Boolean> isUsernameThreadAtCreation();

    @WamProperty(index = 81, type = WamType.BOOLEAN)
    Optional<Boolean> isWaPayRegistered();

    @WamProperty(index = 55, type = WamType.INTEGER)
    OptionalLong labelledMsgs();

    @WamProperty(index = 226, type = WamType.BOOLEAN)
    Optional<Boolean> limitSharingOption();

    @WamProperty(index = 96, type = WamType.INTEGER)
    OptionalLong locationsSent();

    @WamProperty(index = 193, type = WamType.INTEGER)
    OptionalLong markedReadCnt();

    @WamProperty(index = 194, type = WamType.INTEGER)
    OptionalLong markedReadMessageCnt();

    @WamProperty(index = 246, type = WamType.STRING)
    Optional<String> matchedMessagesMarkedAsReadWithDeltaTime();

    @WamProperty(index = 247, type = WamType.STRING)
    Optional<String> matchedMessagesReadWithDeltaTime();

    @WamProperty(index = 127, type = WamType.INTEGER)
    OptionalLong messagesRead();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong messagesReceived();

    @WamProperty(index = 245, type = WamType.INTEGER)
    OptionalLong messagesReceivedWithEnabledReadReceipt();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong messagesSent();

    @WamProperty(index = 12, type = WamType.INTEGER)
    OptionalLong messagesStarred();

    @WamProperty(index = 11, type = WamType.INTEGER)
    OptionalLong messagesUnread();

    @WamProperty(index = 147, type = WamType.INTEGER)
    OptionalLong metaAiMentionClick();

    @WamProperty(index = 148, type = WamType.INTEGER)
    OptionalLong metaAiMentionShow();

    @WamProperty(index = 68, type = WamType.BOOLEAN)
    Optional<Boolean> newThread();

    @WamProperty(index = 231, type = WamType.INTEGER)
    OptionalLong notMvImpressions();

    @WamProperty(index = 82, type = WamType.INTEGER)
    OptionalLong odReceived();

    @WamProperty(index = 240, type = WamType.BOOLEAN)
    Optional<Boolean> oppositePartyHasBadge();

    @WamProperty(index = 204, type = WamType.BOOLEAN)
    Optional<Boolean> oppositePartyHasBusinessIntent();

    @WamProperty(index = 217, type = WamType.BOOLEAN)
    Optional<Boolean> oppositePartyHasProfilePhoto();

    @WamProperty(index = 227, type = WamType.BOOLEAN)
    Optional<Boolean> oppositePartyLimitSharingOption();

    @WamProperty(index = 181, type = WamType.ENUM)
    Optional<OppositeVisibleIdentificationType> oppositeVisibleIdentification();

    @WamProperty(index = 38, type = WamType.INTEGER)
    OptionalLong ordersSent();

    @WamProperty(index = 83, type = WamType.INTEGER)
    OptionalLong p2mOdNnpTransactionsSent();

    @WamProperty(index = 84, type = WamType.INTEGER)
    OptionalLong p2mOdNpTransactionsSent();

    @WamProperty(index = 39, type = WamType.INTEGER)
    OptionalLong paymentsSent();

    @WamProperty(index = 42, type = WamType.INTEGER)
    OptionalLong pdpInquiriesSent();

    @WamProperty(index = 61, type = WamType.INTEGER)
    OptionalLong pdpViews();

    @WamProperty(index = 134, type = WamType.INTEGER)
    OptionalLong photoMessagesReceived();

    @WamProperty(index = 135, type = WamType.INTEGER)
    OptionalLong photoMessagesSent();

    @WamProperty(index = 115, type = WamType.INTEGER)
    OptionalLong pollCreationMessagesReceived();

    @WamProperty(index = 116, type = WamType.INTEGER)
    OptionalLong pollCreationMessagesSent();

    @WamProperty(index = 117, type = WamType.INTEGER)
    OptionalLong pollUpdateMessagesReceived();

    @WamProperty(index = 118, type = WamType.INTEGER)
    OptionalLong pollUpdateMessagesSent();

    @WamProperty(index = 64, type = WamType.INTEGER)
    OptionalLong profileReplies();

    @WamProperty(index = 63, type = WamType.INTEGER)
    OptionalLong profileViews();

    @WamProperty(index = 119, type = WamType.INTEGER)
    OptionalLong pttMessagesReceived();

    @WamProperty(index = 120, type = WamType.INTEGER)
    OptionalLong pttMessagesSent();

    @WamProperty(index = 121, type = WamType.INTEGER)
    OptionalLong ptvMessagesReceived();

    @WamProperty(index = 122, type = WamType.INTEGER)
    OptionalLong ptvMessagesSent();

    @WamProperty(index = 58, type = WamType.INTEGER)
    OptionalLong quickRepliesSent();

    @WamProperty(index = 97, type = WamType.INTEGER)
    OptionalLong reactionsReceived();

    @WamProperty(index = 98, type = WamType.INTEGER)
    OptionalLong reactionsSent();

    @WamProperty(index = 21, type = WamType.INTEGER)
    OptionalLong receiverDefaultDisappearingDuration();

    @WamProperty(index = 80, type = WamType.INTEGER)
    OptionalLong repliesSent();

    @WamProperty(index = 93, type = WamType.BOOLEAN)
    Optional<Boolean> requestedPhoneNumber();

    @WamProperty(index = 94, type = WamType.BOOLEAN)
    Optional<Boolean> seenMaskedPhoneNumber();

    @WamProperty(index = 20, type = WamType.INTEGER)
    OptionalLong senderDefaultDisappearingDuration();

    @WamProperty(index = 99, type = WamType.BOOLEAN)
    Optional<Boolean> sharedPhoneNumber();

    @WamProperty(index = 218, type = WamType.BOOLEAN)
    Optional<Boolean> sharesCommonGroup();

    @WamProperty(index = 101, type = WamType.INTEGER)
    OptionalLong smbMarketingMessagesReactionsSent();

    @WamProperty(index = 102, type = WamType.INTEGER)
    OptionalLong smbMarketingMessagesRepliesSent();

    @WamProperty(index = 103, type = WamType.INTEGER)
    OptionalLong smbMarketingMessagesSpamReports();

    @WamProperty(index = 104, type = WamType.INTEGER)
    OptionalLong smbMarketingMsgsReceived();

    @WamProperty(index = 105, type = WamType.INTEGER)
    OptionalLong smbMarketingMsgsSent();

    @WamProperty(index = 31, type = WamType.INTEGER)
    OptionalLong spamReports();

    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong startTime();

    @WamProperty(index = 191, type = WamType.INTEGER)
    OptionalLong statusReactionsReceived();

    @WamProperty(index = 192, type = WamType.INTEGER)
    OptionalLong statusReactionsSent();

    @WamProperty(index = 50, type = WamType.INTEGER)
    OptionalLong statusReplies();

    @WamProperty(index = 169, type = WamType.INTEGER)
    OptionalLong statusReplyMessagesReceived();

    @WamProperty(index = 49, type = WamType.INTEGER)
    OptionalLong statusViews();

    @WamProperty(index = 170, type = WamType.INTEGER)
    OptionalLong stickerMessagesReceived();

    @WamProperty(index = 171, type = WamType.INTEGER)
    OptionalLong stickerMessagesSent();

    @WamProperty(index = 43, type = WamType.INTEGER)
    OptionalLong storefrontInquiriesSent();

    @WamProperty(index = 177, type = WamType.INTEGER)
    OptionalLong suggestionPromptsClick();

    @WamProperty(index = 178, type = WamType.INTEGER)
    OptionalLong suggestionPromptsShow();

    @WamProperty(index = 136, type = WamType.INTEGER)
    OptionalLong textMessagesReceived();

    @WamProperty(index = 137, type = WamType.INTEGER)
    OptionalLong textMessagesSent();

    @WamProperty(index = 124, type = WamType.INTEGER)
    OptionalLong textMessagesToUserCreatedAgentCnt();

    @WamProperty(index = 189, type = WamType.STRING)
    Optional<String> threadCreationDate();

    @WamProperty(index = 66, type = WamType.STRING)
    Optional<String> threadDs();

    @WamProperty(index = 67, type = WamType.STRING)
    Optional<String> threadId();

    @WamProperty(index = 223, type = WamType.STRING)
    Optional<String> threadIdMonthly();

    @WamProperty(index = 264, type = WamType.INTEGER)
    OptionalLong tombstoneAiFutureproofedMessagesReceived();

    @WamProperty(index = 262, type = WamType.INTEGER)
    OptionalLong tombstoneEphemeralMessagesReceived();

    @WamProperty(index = 263, type = WamType.INTEGER)
    OptionalLong tombstoneViewOnceMessagesReceived();

    @WamProperty(index = 28, type = WamType.INTEGER)
    OptionalLong totalCallDuration();

    @WamProperty(index = 112, type = WamType.INTEGER)
    OptionalLong totalMessageEditsFromAgentCnt();

    @WamProperty(index = 113, type = WamType.INTEGER)
    OptionalLong totalMessageFromAgentCnt();

    @WamProperty(index = 114, type = WamType.INTEGER)
    OptionalLong totalMessageToAgentCnt();

    @WamProperty(index = 125, type = WamType.INTEGER)
    OptionalLong totalMessagesToUserCreatedAgentCnt();

    @WamProperty(index = 85, type = WamType.ENUM)
    Optional<TypeOfGroupEnum> typeOfGroup();

    @WamProperty(index = 138, type = WamType.INTEGER)
    OptionalLong urlMessagesReceived();

    @WamProperty(index = 139, type = WamType.INTEGER)
    OptionalLong urlMessagesSent();

    @WamProperty(index = 205, type = WamType.BOOLEAN)
    Optional<Boolean> userHasBusinessIntent();

    @WamProperty(index = 107, type = WamType.BOOLEAN)
    Optional<Boolean> viaContactlessChats();

    @WamProperty(index = 76, type = WamType.INTEGER)
    OptionalLong videoCallsOffered();

    @WamProperty(index = 172, type = WamType.INTEGER)
    OptionalLong videoMessagesReceived();

    @WamProperty(index = 173, type = WamType.INTEGER)
    OptionalLong videoMessagesSent();

    @WamProperty(index = 18, type = WamType.INTEGER)
    OptionalLong viewOnceMessagesOpened();

    @WamProperty(index = 17, type = WamType.INTEGER)
    OptionalLong viewOnceMessagesReceived();

    @WamProperty(index = 16, type = WamType.INTEGER)
    OptionalLong viewOnceMessagesSent();

    @WamProperty(index = 77, type = WamType.INTEGER)
    OptionalLong voiceCallsOffered();
}
