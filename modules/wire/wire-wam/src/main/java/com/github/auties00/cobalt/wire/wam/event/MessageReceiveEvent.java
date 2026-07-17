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
import com.github.auties00.cobalt.wire.wam.type.E2eDeviceType;
import com.github.auties00.cobalt.wire.wam.type.EditType;
import com.github.auties00.cobalt.wire.wam.type.EncryptionTypeCode;
import com.github.auties00.cobalt.wire.wam.type.EphemeralityInitiatorType;
import com.github.auties00.cobalt.wire.wam.type.EphemeralityTriggerActionType;
import com.github.auties00.cobalt.wire.wam.type.InvisibleMessageCategoryType;
import com.github.auties00.cobalt.wire.wam.type.MediaType;
import com.github.auties00.cobalt.wire.wam.type.MessageType;
import com.github.auties00.cobalt.wire.wam.type.OppositeVisibleIdentificationType;
import com.github.auties00.cobalt.wire.wam.type.PairedMediaType;
import com.github.auties00.cobalt.wire.wam.type.PlatformType;
import com.github.auties00.cobalt.wire.wam.type.PrivateAiFeatureName;
import com.github.auties00.cobalt.wire.wam.type.RevokeType;
import com.github.auties00.cobalt.wire.wam.type.SessionScopeType;
import com.github.auties00.cobalt.wire.wam.type.SizeBucket;
import com.github.auties00.cobalt.wire.wam.type.StickerMakerSourceType;
import com.github.auties00.cobalt.wire.wam.type.TypeOfGroupEnum;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebMessageReceiveWamEvent")
@WamEvent(id = 450, releaseWeight = 5)
public interface MessageReceiveEvent extends WamEventSpec {
    @WamProperty(index = 61, type = WamType.INTEGER)
    OptionalLong afterReadDuration();

    @WamProperty(index = 28, type = WamType.ENUM)
    Optional<AgentEngagementEnumType> agentEngagementType();

    @WamProperty(index = 43, type = WamType.STRING)
    Optional<String> appContext();

    @WamProperty(index = 44, type = WamType.INTEGER)
    OptionalLong appContextBitfield();

    @WamProperty(index = 37, type = WamType.ENUM)
    Optional<BotType> botType();

    @WamProperty(index = 38, type = WamType.ENUM)
    Optional<ChatOriginsType> chatOrigins();

    @WamProperty(index = 16, type = WamType.INTEGER)
    OptionalLong deviceCount();

    @WamProperty(index = 10, type = WamType.ENUM)
    Optional<SizeBucket> deviceSizeBucket();

    @WamProperty(index = 14, type = WamType.ENUM)
    Optional<DisappearingChatInitiatorType> disappearingChatInitiator();

    @WamProperty(index = 57, type = WamType.ENUM)
    Optional<E2eDeviceType> e2eSenderType();

    @WamProperty(index = 25, type = WamType.ENUM)
    Optional<EditType> editType();

    @WamProperty(index = 54, type = WamType.ENUM)
    Optional<EncryptionTypeCode> encryptionType();

    @WamProperty(index = 13, type = WamType.INTEGER)
    OptionalLong ephemeralityDuration();

    @WamProperty(index = 26, type = WamType.ENUM)
    Optional<EphemeralityInitiatorType> ephemeralityInitiator();

    @WamProperty(index = 27, type = WamType.ENUM)
    Optional<EphemeralityTriggerActionType> ephemeralityTriggerAction();

    @WamProperty(index = 39, type = WamType.BOOLEAN)
    Optional<Boolean> hasUsername();

    @WamProperty(index = 45, type = WamType.BOOLEAN)
    Optional<Boolean> hasUsernamePin();

    @WamProperty(index = 41, type = WamType.ENUM)
    Optional<InvisibleMessageCategoryType> invisibleMessageCategory();

    @WamProperty(index = 36, type = WamType.BOOLEAN)
    Optional<Boolean> isAComment();

    @WamProperty(index = 19, type = WamType.BOOLEAN)
    Optional<Boolean> isAReply();

    @WamProperty(index = 62, type = WamType.BOOLEAN)
    Optional<Boolean> isAfterRead();

    @WamProperty(index = 18, type = WamType.BOOLEAN)
    Optional<Boolean> isForwardedForward();

    @WamProperty(index = 24, type = WamType.BOOLEAN)
    Optional<Boolean> isLid();

    @WamProperty(index = 55, type = WamType.BOOLEAN)
    Optional<Boolean> isPq();

    @WamProperty(index = 9, type = WamType.BOOLEAN)
    Optional<Boolean> isViewOnce();

    @WamProperty(index = 33, type = WamType.ENUM)
    Optional<AddressingMode> localAddressingMode();

    @WamProperty(index = 34, type = WamType.ENUM)
    Optional<AddressingMode> messageAddressingMode();

    @WamProperty(index = 4, type = WamType.BOOLEAN)
    Optional<Boolean> messageIsInternational();

    @WamProperty(index = 23, type = WamType.BOOLEAN)
    Optional<Boolean> messageIsInvisible();

    @WamProperty(index = 5, type = WamType.BOOLEAN)
    Optional<Boolean> messageIsOffline();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<MediaType> messageMediaType();

    @WamProperty(index = 15, type = WamType.TIMER)
    Optional<Instant> messageQueueTime();

    @WamProperty(index = 6, type = WamType.TIMER)
    Optional<Instant> messageReceiveT0();

    @WamProperty(index = 7, type = WamType.TIMER)
    Optional<Instant> messageReceiveT1();

    @WamProperty(index = 49, type = WamType.TIMER)
    Optional<Instant> messageReceiveT2();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<MessageType> messageType();

    @WamProperty(index = 8, type = WamType.BOOLEAN)
    Optional<Boolean> mutedGroupMessage();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong numOfWebUrlsInTextMessage();

    @WamProperty(index = 30, type = WamType.INTEGER)
    OptionalLong offlineCount();

    @WamProperty(index = 65, type = WamType.BOOLEAN)
    Optional<Boolean> oppositeHasUsername();

    @WamProperty(index = 40, type = WamType.ENUM)
    Optional<OppositeVisibleIdentificationType> oppositeVisibleIdentification();

    @WamProperty(index = 22, type = WamType.INTEGER)
    OptionalLong paddingBytesSize();

    @WamProperty(index = 42, type = WamType.ENUM)
    Optional<PairedMediaType> pairedMediaType();

    @WamProperty(index = 17, type = WamType.INTEGER)
    OptionalLong participantCount();

    @WamProperty(index = 53, type = WamType.ENUM)
    Optional<PrivateAiFeatureName> privateAiFeatureName();

    @WamProperty(index = 47, type = WamType.BOOLEAN)
    Optional<Boolean> processingDeferred();

    @WamProperty(index = 58, type = WamType.STRING)
    Optional<String> receivedContactCardType();

    @WamProperty(index = 59, type = WamType.STRING)
    Optional<String> receivedContactMetadataTypes();

    @WamProperty(index = 50, type = WamType.INTEGER)
    OptionalLong receivedPhoneNumberContactSize();

    @WamProperty(index = 51, type = WamType.INTEGER)
    OptionalLong receivedPhoneNumberWithUsernameContactSize();

    @WamProperty(index = 52, type = WamType.INTEGER)
    OptionalLong receivedUsernameContactSize();

    @WamProperty(index = 12, type = WamType.INTEGER)
    OptionalLong receiverDefaultDisappearingDuration();

    @WamProperty(index = 20, type = WamType.ENUM)
    Optional<RevokeType> revokeType();

    @WamProperty(index = 11, type = WamType.INTEGER)
    OptionalLong senderDefaultDisappearingDuration();

    @WamProperty(index = 64, type = WamType.ENUM)
    Optional<PlatformType> senderPlatform();

    @WamProperty(index = 35, type = WamType.ENUM)
    Optional<AddressingMode> serverAddressingMode();

    @WamProperty(index = 63, type = WamType.ENUM)
    Optional<SessionScopeType> sessionScope();

    @WamProperty(index = 48, type = WamType.INTEGER)
    OptionalLong stanzaProcessCount();

    @WamProperty(index = 29, type = WamType.BOOLEAN)
    Optional<Boolean> stickerIsAi();

    @WamProperty(index = 31, type = WamType.BOOLEAN)
    Optional<Boolean> stickerIsFromStickerMaker();

    @WamProperty(index = 60, type = WamType.BOOLEAN)
    Optional<Boolean> stickerIsPremium();

    @WamProperty(index = 32, type = WamType.ENUM)
    Optional<StickerMakerSourceType> stickerMakerSourceType();

    @WamProperty(index = 56, type = WamType.INTEGER)
    OptionalLong traceIdInt();

    @WamProperty(index = 21, type = WamType.ENUM)
    Optional<TypeOfGroupEnum> typeOfGroup();
}
