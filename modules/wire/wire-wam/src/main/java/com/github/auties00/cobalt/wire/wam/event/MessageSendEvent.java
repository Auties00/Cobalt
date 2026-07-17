package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.AddressingMode;
import com.github.auties00.cobalt.wire.wam.type.AgentEngagementEnumType;
import com.github.auties00.cobalt.wire.wam.type.BotType;
import com.github.auties00.cobalt.wire.wam.type.ChatOriginsType;
import com.github.auties00.cobalt.wire.wam.type.DisappearingChatInitiatorType;
import com.github.auties00.cobalt.wire.wam.type.E2eCiphertextType;
import com.github.auties00.cobalt.wire.wam.type.E2eFailureReason;
import com.github.auties00.cobalt.wire.wam.type.EditType;
import com.github.auties00.cobalt.wire.wam.type.EncryptionTypeCode;
import com.github.auties00.cobalt.wire.wam.type.EphemeralityInitiatorType;
import com.github.auties00.cobalt.wire.wam.type.EphemeralityTriggerActionType;
import com.github.auties00.cobalt.wire.wam.type.InvisibleMessageCategoryType;
import com.github.auties00.cobalt.wire.wam.type.LogoutReasonType;
import com.github.auties00.cobalt.wire.wam.type.MediaType;
import com.github.auties00.cobalt.wire.wam.type.MediaUploadResultType;
import com.github.auties00.cobalt.wire.wam.type.MessageDistributionEnumType;
import com.github.auties00.cobalt.wire.wam.type.MessageSendResultType;
import com.github.auties00.cobalt.wire.wam.type.MessageSendSource;
import com.github.auties00.cobalt.wire.wam.type.MessageType;
import com.github.auties00.cobalt.wire.wam.type.OppositeVisibleIdentificationType;
import com.github.auties00.cobalt.wire.wam.type.PairedMediaType;
import com.github.auties00.cobalt.wire.wam.type.PrivateAiFeatureName;
import com.github.auties00.cobalt.wire.wam.type.ReachabilityStatus;
import com.github.auties00.cobalt.wire.wam.type.RevokeType;
import com.github.auties00.cobalt.wire.wam.type.SessionScopeType;
import com.github.auties00.cobalt.wire.wam.type.SizeBucket;
import com.github.auties00.cobalt.wire.wam.type.StickerMakerSourceType;
import com.github.auties00.cobalt.wire.wam.type.TypeOfGroupEnum;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebMessageSendWamEvent")
@WamEvent(id = 854)
public interface MessageSendEvent extends WamEventSpec {
    @WamProperty(index = 87, type = WamType.INTEGER)
    OptionalLong afterReadDuration();

    @WamProperty(index = 49, type = WamType.ENUM)
    Optional<AgentEngagementEnumType> agentEngagementType();

    @WamProperty(index = 72, type = WamType.STRING)
    Optional<String> appContext();

    @WamProperty(index = 73, type = WamType.INTEGER)
    OptionalLong appContextBitfield();

    @WamProperty(index = 55, type = WamType.ENUM)
    Optional<BotType> botType();

    @WamProperty(index = 58, type = WamType.ENUM)
    Optional<ChatOriginsType> chatOrigins();

    @WamProperty(index = 67, type = WamType.STRING)
    Optional<String> chatSessionId();

    @WamProperty(index = 31, type = WamType.INTEGER)
    OptionalLong deviceCount();

    @WamProperty(index = 25, type = WamType.ENUM)
    Optional<SizeBucket> deviceSizeBucket();

    @WamProperty(index = 30, type = WamType.ENUM)
    Optional<DisappearingChatInitiatorType> disappearingChatInitiator();

    @WamProperty(index = 23, type = WamType.BOOLEAN)
    Optional<Boolean> e2eBackfill();

    @WamProperty(index = 10, type = WamType.ENUM)
    Optional<E2eCiphertextType> e2eCiphertextType();

    @WamProperty(index = 9, type = WamType.INTEGER)
    OptionalLong e2eCiphertextVersion();

    @WamProperty(index = 46, type = WamType.ENUM)
    Optional<E2eFailureReason> e2eFailureReason();

    @WamProperty(index = 43, type = WamType.INTEGER)
    OptionalLong editDuration();

    @WamProperty(index = 44, type = WamType.ENUM)
    Optional<EditType> editType();

    @WamProperty(index = 76, type = WamType.ENUM)
    Optional<EncryptionTypeCode> encryptionType();

    @WamProperty(index = 21, type = WamType.INTEGER)
    OptionalLong ephemeralityDuration();

    @WamProperty(index = 47, type = WamType.ENUM)
    Optional<EphemeralityInitiatorType> ephemeralityInitiator();

    @WamProperty(index = 48, type = WamType.ENUM)
    Optional<EphemeralityTriggerActionType> ephemeralityTriggerAction();

    @WamProperty(index = 40, type = WamType.INTEGER)
    OptionalLong excessPayloadKbSize();

    @WamProperty(index = 15, type = WamType.BOOLEAN)
    Optional<Boolean> fastForwardEnabled();

    @WamProperty(index = 59, type = WamType.BOOLEAN)
    Optional<Boolean> hasUsername();

    @WamProperty(index = 74, type = WamType.BOOLEAN)
    Optional<Boolean> hasUsernamePin();

    @WamProperty(index = 90, type = WamType.ENUM)
    Optional<ReachabilityStatus> initialSendAttemptReachabilityStatus();

    @WamProperty(index = 64, type = WamType.ENUM)
    Optional<InvisibleMessageCategoryType> invisibleMessageCategory();

    @WamProperty(index = 54, type = WamType.BOOLEAN)
    Optional<Boolean> isAComment();

    @WamProperty(index = 35, type = WamType.BOOLEAN)
    Optional<Boolean> isAReply();

    @WamProperty(index = 88, type = WamType.BOOLEAN)
    Optional<Boolean> isAfterRead();

    @WamProperty(index = 19, type = WamType.BOOLEAN)
    Optional<Boolean> isFromWamsys();

    @WamProperty(index = 39, type = WamType.BOOLEAN)
    Optional<Boolean> isLid();

    @WamProperty(index = 82, type = WamType.BOOLEAN)
    Optional<Boolean> isPq();

    @WamProperty(index = 69, type = WamType.BOOLEAN)
    Optional<Boolean> isPremium();

    @WamProperty(index = 94, type = WamType.BOOLEAN)
    Optional<Boolean> isScheduled();

    @WamProperty(index = 22, type = WamType.BOOLEAN)
    Optional<Boolean> isViewOnce();

    @WamProperty(index = 75, type = WamType.BOOLEAN)
    Optional<Boolean> isWhatsapiBuild();

    @WamProperty(index = 53, type = WamType.ENUM)
    Optional<AddressingMode> localAddressingMode();

    @WamProperty(index = 70, type = WamType.ENUM)
    Optional<LogoutReasonType> logoutReason();

    @WamProperty(index = 89, type = WamType.INTEGER)
    OptionalLong logoutSessionId();

    @WamProperty(index = 8, type = WamType.BOOLEAN)
    Optional<Boolean> mediaCaptionPresent();

    @WamProperty(index = 61, type = WamType.ENUM)
    Optional<MediaUploadResultType> mediaUploadError();

    @WamProperty(index = 62, type = WamType.INTEGER)
    OptionalLong mediaUploadRetryCount();

    @WamProperty(index = 41, type = WamType.ENUM)
    Optional<MessageDistributionEnumType> messageDistributionType();

    @WamProperty(index = 14, type = WamType.TIMER)
    Optional<Instant> messageForwardAgeT();

    @WamProperty(index = 5, type = WamType.BOOLEAN)
    Optional<Boolean> messageIsFanout();

    @WamProperty(index = 13, type = WamType.BOOLEAN)
    Optional<Boolean> messageIsFastForward();

    @WamProperty(index = 26, type = WamType.BOOLEAN)
    Optional<Boolean> messageIsFirstUserMessage();

    @WamProperty(index = 4, type = WamType.BOOLEAN)
    Optional<Boolean> messageIsForward();

    @WamProperty(index = 7, type = WamType.BOOLEAN)
    Optional<Boolean> messageIsInternational();

    @WamProperty(index = 29, type = WamType.BOOLEAN)
    Optional<Boolean> messageIsInvisible();

    @WamProperty(index = 24, type = WamType.BOOLEAN)
    Optional<Boolean> messageIsRevoke();

    @WamProperty(index = 57, type = WamType.STRING)
    Optional<String> messageKeyHash();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<MediaType> messageMediaType();

    @WamProperty(index = 12, type = WamType.BOOLEAN)
    Optional<Boolean> messageSendOptUploadEnabled();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<MessageSendResultType> messageSendResult();

    @WamProperty(index = 17, type = WamType.BOOLEAN)
    Optional<Boolean> messageSendResultIsTerminal();

    @WamProperty(index = 66, type = WamType.ENUM)
    Optional<MessageSendSource> messageSendSource();

    @WamProperty(index = 11, type = WamType.TIMER)
    Optional<Instant> messageSendT();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<MessageType> messageType();

    @WamProperty(index = 92, type = WamType.STRING)
    Optional<String> messageTypeStr();

    @WamProperty(index = 37, type = WamType.BOOLEAN)
    Optional<Boolean> networkWasDisconnected();

    @WamProperty(index = 93, type = WamType.BOOLEAN)
    Optional<Boolean> oppositeHasUsername();

    @WamProperty(index = 60, type = WamType.ENUM)
    Optional<OppositeVisibleIdentificationType> oppositeVisibleIdentification();

    @WamProperty(index = 42, type = WamType.FLOAT)
    OptionalDouble overallMediaSize();

    @WamProperty(index = 71, type = WamType.ENUM)
    Optional<PairedMediaType> pairedMediaType();

    @WamProperty(index = 32, type = WamType.INTEGER)
    OptionalLong participantCount();

    @WamProperty(index = 81, type = WamType.ENUM)
    Optional<PrivateAiFeatureName> privateAiFeatureName();

    @WamProperty(index = 28, type = WamType.INTEGER)
    OptionalLong receiverDefaultDisappearingDuration();

    @WamProperty(index = 16, type = WamType.INTEGER)
    OptionalLong resendCount();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong retryCount();

    @WamProperty(index = 33, type = WamType.INTEGER)
    OptionalLong revokeDuration();

    @WamProperty(index = 34, type = WamType.ENUM)
    Optional<RevokeType> revokeType();

    @WamProperty(index = 63, type = WamType.STRING)
    Optional<String> runningTasks();

    @WamProperty(index = 45, type = WamType.INTEGER)
    OptionalLong sendButtonPressT();

    @WamProperty(index = 27, type = WamType.INTEGER)
    OptionalLong senderDefaultDisappearingDuration();

    @WamProperty(index = 56, type = WamType.INTEGER)
    OptionalLong serverErrorCode();

    @WamProperty(index = 91, type = WamType.ENUM)
    Optional<SessionScopeType> sessionScope();

    @WamProperty(index = 84, type = WamType.STRING)
    Optional<String> sharedContactCardType();

    @WamProperty(index = 85, type = WamType.STRING)
    Optional<String> sharedContactMetadataTypes();

    @WamProperty(index = 77, type = WamType.INTEGER)
    OptionalLong sharedPhoneNumberContactSize();

    @WamProperty(index = 78, type = WamType.INTEGER)
    OptionalLong sharedPhoneNumberWithUsernameContactSize();

    @WamProperty(index = 79, type = WamType.INTEGER)
    OptionalLong sharedUsernameContactSize();

    @WamProperty(index = 50, type = WamType.BOOLEAN)
    Optional<Boolean> stickerIsAi();

    @WamProperty(index = 38, type = WamType.BOOLEAN)
    Optional<Boolean> stickerIsAvatar();

    @WamProperty(index = 18, type = WamType.BOOLEAN)
    Optional<Boolean> stickerIsFirstParty();

    @WamProperty(index = 51, type = WamType.BOOLEAN)
    Optional<Boolean> stickerIsFromStickerMaker();

    @WamProperty(index = 86, type = WamType.BOOLEAN)
    Optional<Boolean> stickerIsPremium();

    @WamProperty(index = 52, type = WamType.ENUM)
    Optional<StickerMakerSourceType> stickerMakerSourceType();

    @WamProperty(index = 20, type = WamType.FLOAT)
    OptionalDouble thumbSize();

    @WamProperty(index = 83, type = WamType.INTEGER)
    OptionalLong traceIdInt();

    @WamProperty(index = 36, type = WamType.ENUM)
    Optional<TypeOfGroupEnum> typeOfGroup();

    @WamProperty(index = 68, type = WamType.STRING)
    Optional<String> unifiedSessionId();

    @WamProperty(index = 65, type = WamType.STRING)
    Optional<String> userToDeviceSizeBucket();
}
