package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ChatMutedType;
import com.github.auties00.cobalt.wire.wam.type.ChatType;
import com.github.auties00.cobalt.wire.wam.type.GaStatus;
import com.github.auties00.cobalt.wire.wam.type.OppositeVisibleIdentificationType;
import com.github.auties00.cobalt.wire.wam.type.TypeOfGroupEnum;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebThreadInteractionDataCoreConsumerWamEvent")
@WamEvent(id = 6466)
public interface ThreadInteractionDataCoreConsumerEvent extends WamEventSpec {
    @WamProperty(index = 138, type = WamType.INTEGER)
    OptionalLong afterReadDuration();

    @WamProperty(index = 139, type = WamType.INTEGER)
    OptionalLong afterReadMessagesExpired();

    @WamProperty(index = 144, type = WamType.INTEGER)
    OptionalLong afterReadMessagesReceived();

    @WamProperty(index = 140, type = WamType.INTEGER)
    OptionalLong afterReadMessagesSent();

    @WamProperty(index = 141, type = WamType.INTEGER)
    OptionalLong afterReadMessagesUnreadExpired();

    @WamProperty(index = 142, type = WamType.BOOLEAN)
    Optional<Boolean> afterReadTurnedOff();

    @WamProperty(index = 143, type = WamType.BOOLEAN)
    Optional<Boolean> afterReadTurnedOn();

    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong audioMessagesReceived();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong audioMessagesSent();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong broadcastMsgsReceived();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong broadcastMsgsSent();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong chatEphemeralityDuration();

    @WamProperty(index = 7, type = WamType.ENUM)
    Optional<ChatMutedType> chatMuted();

    @WamProperty(index = 8, type = WamType.INTEGER)
    OptionalLong chatOverflowClicks();

    @WamProperty(index = 9, type = WamType.ENUM)
    Optional<ChatType> chatTypeInd();

    @WamProperty(index = 10, type = WamType.INTEGER)
    OptionalLong commentsReceived();

    @WamProperty(index = 13, type = WamType.INTEGER)
    OptionalLong documentMessagesReceived();

    @WamProperty(index = 14, type = WamType.INTEGER)
    OptionalLong documentMessagesSent();

    @WamProperty(index = 15, type = WamType.INTEGER)
    OptionalLong editedMsgsSent();

    @WamProperty(index = 110, type = WamType.INTEGER)
    OptionalLong ephemeralMessagesExpired();

    @WamProperty(index = 16, type = WamType.INTEGER)
    OptionalLong ephemeralMessagesReceived();

    @WamProperty(index = 17, type = WamType.INTEGER)
    OptionalLong ephemeralMessagesSent();

    @WamProperty(index = 18, type = WamType.INTEGER)
    OptionalLong ephemeralMessagesUnreadExpired();

    @WamProperty(index = 21, type = WamType.INTEGER)
    OptionalLong eventCreationMessagesReceived();

    @WamProperty(index = 22, type = WamType.INTEGER)
    OptionalLong eventCreationMessagesSent();

    @WamProperty(index = 23, type = WamType.INTEGER)
    OptionalLong eventResponseMessagesReceived();

    @WamProperty(index = 24, type = WamType.INTEGER)
    OptionalLong eventResponseMessagesSent();

    @WamProperty(index = 25, type = WamType.INTEGER)
    OptionalLong forwardAudioMessagesReceived();

    @WamProperty(index = 26, type = WamType.INTEGER)
    OptionalLong forwardAudioMessagesSent();

    @WamProperty(index = 27, type = WamType.INTEGER)
    OptionalLong forwardDocumentMessagesReceived();

    @WamProperty(index = 28, type = WamType.INTEGER)
    OptionalLong forwardDocumentMessagesSent();

    @WamProperty(index = 29, type = WamType.INTEGER)
    OptionalLong forwardGifMessagesReceived();

    @WamProperty(index = 30, type = WamType.INTEGER)
    OptionalLong forwardGifMessagesSent();

    @WamProperty(index = 31, type = WamType.INTEGER)
    OptionalLong forwardMessagesReceived();

    @WamProperty(index = 32, type = WamType.INTEGER)
    OptionalLong forwardMessagesSent();

    @WamProperty(index = 33, type = WamType.INTEGER)
    OptionalLong forwardPhotoMessagesReceived();

    @WamProperty(index = 34, type = WamType.INTEGER)
    OptionalLong forwardPhotoMessagesSent();

    @WamProperty(index = 134, type = WamType.INTEGER)
    OptionalLong forwardPtvMessagesReceived();

    @WamProperty(index = 135, type = WamType.INTEGER)
    OptionalLong forwardPtvMessagesSent();

    @WamProperty(index = 35, type = WamType.INTEGER)
    OptionalLong forwardStickerMessagesReceived();

    @WamProperty(index = 36, type = WamType.INTEGER)
    OptionalLong forwardStickerMessagesSent();

    @WamProperty(index = 37, type = WamType.INTEGER)
    OptionalLong forwardTextMessagesReceived();

    @WamProperty(index = 38, type = WamType.INTEGER)
    OptionalLong forwardTextMessagesSent();

    @WamProperty(index = 39, type = WamType.INTEGER)
    OptionalLong forwardUrlMessagesReceived();

    @WamProperty(index = 40, type = WamType.INTEGER)
    OptionalLong forwardUrlMessagesSent();

    @WamProperty(index = 41, type = WamType.INTEGER)
    OptionalLong forwardVideoMessagesReceived();

    @WamProperty(index = 42, type = WamType.INTEGER)
    OptionalLong forwardVideoMessagesSent();

    @WamProperty(index = 43, type = WamType.ENUM)
    Optional<GaStatus> gaStatus();

    @WamProperty(index = 44, type = WamType.INTEGER)
    OptionalLong gifMessagesReceived();

    @WamProperty(index = 45, type = WamType.INTEGER)
    OptionalLong gifMessagesSent();

    @WamProperty(index = 47, type = WamType.INTEGER)
    OptionalLong groupMembershipReplies();

    @WamProperty(index = 48, type = WamType.INTEGER)
    OptionalLong groupPrivateReplies();

    @WamProperty(index = 49, type = WamType.INTEGER)
    OptionalLong groupSize();

    @WamProperty(index = 113, type = WamType.INTEGER)
    OptionalLong groupStatusLikesOthersToOthers();

    @WamProperty(index = 114, type = WamType.INTEGER)
    OptionalLong groupStatusLikesOthersToOwn();

    @WamProperty(index = 115, type = WamType.INTEGER)
    OptionalLong groupStatusLikesOwnToOthers();

    @WamProperty(index = 116, type = WamType.INTEGER)
    OptionalLong groupStatusLikesOwnToOwn();

    @WamProperty(index = 117, type = WamType.INTEGER)
    OptionalLong groupStatusRepliesOthersToOthers();

    @WamProperty(index = 118, type = WamType.INTEGER)
    OptionalLong groupStatusRepliesOthersToOwn();

    @WamProperty(index = 119, type = WamType.INTEGER)
    OptionalLong groupStatusRepliesOwnToOthers();

    @WamProperty(index = 120, type = WamType.INTEGER)
    OptionalLong groupStatusRepliesOwnToOwn();

    @WamProperty(index = 145, type = WamType.BOOLEAN)
    Optional<Boolean> hasReplied1On1();

    @WamProperty(index = 50, type = WamType.BOOLEAN)
    Optional<Boolean> hasUsername();

    @WamProperty(index = 51, type = WamType.BOOLEAN)
    Optional<Boolean> hasUsernamePin();

    @WamProperty(index = 52, type = WamType.BOOLEAN)
    Optional<Boolean> isAContact();

    @WamProperty(index = 53, type = WamType.BOOLEAN)
    Optional<Boolean> isAContactAtThreadCreation();

    @WamProperty(index = 54, type = WamType.BOOLEAN)
    Optional<Boolean> isAGroup();

    @WamProperty(index = 55, type = WamType.BOOLEAN)
    Optional<Boolean> isArchived();

    @WamProperty(index = 57, type = WamType.BOOLEAN)
    Optional<Boolean> isDeleted();

    @WamProperty(index = 112, type = WamType.BOOLEAN)
    Optional<Boolean> isGuestThread();

    @WamProperty(index = 58, type = WamType.BOOLEAN)
    Optional<Boolean> isInviteCreatedThread();

    @WamProperty(index = 121, type = WamType.BOOLEAN)
    Optional<Boolean> isManagedAccount();

    @WamProperty(index = 136, type = WamType.BOOLEAN)
    Optional<Boolean> isMessageYourself();

    @WamProperty(index = 59, type = WamType.BOOLEAN)
    Optional<Boolean> isMetaAiAssistant();

    @WamProperty(index = 122, type = WamType.BOOLEAN)
    Optional<Boolean> isNewManagedAccountEmIgnored();

    @WamProperty(index = 60, type = WamType.BOOLEAN)
    Optional<Boolean> isPinned();

    @WamProperty(index = 61, type = WamType.BOOLEAN)
    Optional<Boolean> isPnhEnabledChat();

    @WamProperty(index = 123, type = WamType.BOOLEAN)
    Optional<Boolean> isUsernameThread();

    @WamProperty(index = 137, type = WamType.BOOLEAN)
    Optional<Boolean> isUsernameThreadAtCreation();

    @WamProperty(index = 124, type = WamType.BOOLEAN)
    Optional<Boolean> limitSharingOption();

    @WamProperty(index = 62, type = WamType.INTEGER)
    OptionalLong markedReadCnt();

    @WamProperty(index = 63, type = WamType.INTEGER)
    OptionalLong markedReadMessageCnt();

    @WamProperty(index = 131, type = WamType.STRING)
    Optional<String> matchedMessagesMarkedAsReadWithDeltaTime();

    @WamProperty(index = 132, type = WamType.STRING)
    Optional<String> matchedMessagesReadWithDeltaTime();

    @WamProperty(index = 64, type = WamType.INTEGER)
    OptionalLong messagesRead();

    @WamProperty(index = 65, type = WamType.INTEGER)
    OptionalLong messagesReceived();

    @WamProperty(index = 133, type = WamType.INTEGER)
    OptionalLong messagesReceivedWithEnabledReadReceipt();

    @WamProperty(index = 66, type = WamType.INTEGER)
    OptionalLong messagesSent();

    @WamProperty(index = 68, type = WamType.INTEGER)
    OptionalLong messagesUnread();

    @WamProperty(index = 146, type = WamType.BOOLEAN)
    Optional<Boolean> oppositePartyHasProfilePhoto();

    @WamProperty(index = 125, type = WamType.BOOLEAN)
    Optional<Boolean> oppositePartyLimitSharingOption();

    @WamProperty(index = 69, type = WamType.ENUM)
    Optional<OppositeVisibleIdentificationType> oppositeVisibleIdentification();

    @WamProperty(index = 70, type = WamType.INTEGER)
    OptionalLong photoMessagesReceived();

    @WamProperty(index = 71, type = WamType.INTEGER)
    OptionalLong photoMessagesSent();

    @WamProperty(index = 72, type = WamType.INTEGER)
    OptionalLong pollCreationMessagesReceived();

    @WamProperty(index = 73, type = WamType.INTEGER)
    OptionalLong pollCreationMessagesSent();

    @WamProperty(index = 74, type = WamType.INTEGER)
    OptionalLong pollUpdateMessagesReceived();

    @WamProperty(index = 75, type = WamType.INTEGER)
    OptionalLong pollUpdateMessagesSent();

    @WamProperty(index = 76, type = WamType.INTEGER)
    OptionalLong profileReplies();

    @WamProperty(index = 77, type = WamType.INTEGER)
    OptionalLong profileViews();

    @WamProperty(index = 78, type = WamType.INTEGER)
    OptionalLong pttMessagesReceived();

    @WamProperty(index = 79, type = WamType.INTEGER)
    OptionalLong pttMessagesSent();

    @WamProperty(index = 80, type = WamType.INTEGER)
    OptionalLong ptvMessagesReceived();

    @WamProperty(index = 81, type = WamType.INTEGER)
    OptionalLong ptvMessagesSent();

    @WamProperty(index = 82, type = WamType.INTEGER)
    OptionalLong reactionsReceived();

    @WamProperty(index = 83, type = WamType.INTEGER)
    OptionalLong reactionsSent();

    @WamProperty(index = 85, type = WamType.INTEGER)
    OptionalLong repliesSent();

    @WamProperty(index = 86, type = WamType.BOOLEAN)
    Optional<Boolean> requestedPhoneNumber();

    @WamProperty(index = 87, type = WamType.BOOLEAN)
    Optional<Boolean> seenMaskedPhoneNumber();

    @WamProperty(index = 89, type = WamType.BOOLEAN)
    Optional<Boolean> sharedPhoneNumber();

    @WamProperty(index = 107, type = WamType.BOOLEAN)
    Optional<Boolean> sharesCommonGroup();

    @WamProperty(index = 90, type = WamType.INTEGER)
    OptionalLong statusReactionsReceived();

    @WamProperty(index = 126, type = WamType.INTEGER)
    OptionalLong statusReactionsSent();

    @WamProperty(index = 91, type = WamType.INTEGER)
    OptionalLong statusReplies();

    @WamProperty(index = 127, type = WamType.INTEGER)
    OptionalLong statusReplyMessagesReceived();

    @WamProperty(index = 92, type = WamType.INTEGER)
    OptionalLong statusViews();

    @WamProperty(index = 93, type = WamType.INTEGER)
    OptionalLong stickerMessagesReceived();

    @WamProperty(index = 94, type = WamType.INTEGER)
    OptionalLong stickerMessagesSent();

    @WamProperty(index = 95, type = WamType.INTEGER)
    OptionalLong textMessagesReceived();

    @WamProperty(index = 96, type = WamType.INTEGER)
    OptionalLong textMessagesSent();

    @WamProperty(index = 111, type = WamType.STRING)
    Optional<String> threadCreationDate();

    @WamProperty(index = 97, type = WamType.STRING)
    Optional<String> threadDs();

    @WamProperty(index = 98, type = WamType.STRING)
    Optional<String> threadId();

    @WamProperty(index = 109, type = WamType.STRING)
    Optional<String> threadIdByLid();

    @WamProperty(index = 130, type = WamType.INTEGER)
    OptionalLong tombstoneAiFutureproofedMessagesReceived();

    @WamProperty(index = 128, type = WamType.INTEGER)
    OptionalLong tombstoneEphemeralMessagesReceived();

    @WamProperty(index = 129, type = WamType.INTEGER)
    OptionalLong tombstoneViewOnceMessagesReceived();

    @WamProperty(index = 108, type = WamType.ENUM)
    Optional<TypeOfGroupEnum> typeOfGroup();

    @WamProperty(index = 99, type = WamType.INTEGER)
    OptionalLong urlMessagesReceived();

    @WamProperty(index = 100, type = WamType.INTEGER)
    OptionalLong urlMessagesSent();

    @WamProperty(index = 102, type = WamType.INTEGER)
    OptionalLong videoMessagesReceived();

    @WamProperty(index = 103, type = WamType.INTEGER)
    OptionalLong videoMessagesSent();

    @WamProperty(index = 104, type = WamType.INTEGER)
    OptionalLong viewOnceMessagesOpened();

    @WamProperty(index = 105, type = WamType.INTEGER)
    OptionalLong viewOnceMessagesReceived();

    @WamProperty(index = 106, type = WamType.INTEGER)
    OptionalLong viewOnceMessagesSent();
}
