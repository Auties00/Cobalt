package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.AgentEngagementEnumType;
import com.github.auties00.cobalt.wire.wam.type.BotType;
import com.github.auties00.cobalt.wire.wam.type.ClientGroupSizeBucket;
import com.github.auties00.cobalt.wire.wam.type.ClientMessageSendStage;
import com.github.auties00.cobalt.wire.wam.type.EditType;
import com.github.auties00.cobalt.wire.wam.type.InvisibleMessageCategoryType;
import com.github.auties00.cobalt.wire.wam.type.MediaType;
import com.github.auties00.cobalt.wire.wam.type.MessageSendResultType;
import com.github.auties00.cobalt.wire.wam.type.MessageType;
import com.github.auties00.cobalt.wire.wam.type.SizeBucket;
import com.github.auties00.cobalt.wire.wam.type.TypeOfGroupEnum;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebAndroidMessageSendPerfWamEvent")
@WamEvent(id = 1994, releaseWeight = 2000)
public interface AndroidMessageSendPerfEvent extends WamEventSpec {
    @WamProperty(index = 47, type = WamType.ENUM)
    Optional<AgentEngagementEnumType> agentEngagementType();

    @WamProperty(index = 16, type = WamType.BOOLEAN)
    Optional<Boolean> appRestart();

    @WamProperty(index = 48, type = WamType.ENUM)
    Optional<BotType> botType();

    @WamProperty(index = 32, type = WamType.INTEGER)
    OptionalLong bucketedSenderKeyDistributionCountPercentage();

    @WamProperty(index = 35, type = WamType.INTEGER)
    OptionalLong bucketedSenderKeyDistributionHashTime();

    @WamProperty(index = 36, type = WamType.INTEGER)
    OptionalLong deviceCount();

    @WamProperty(index = 26, type = WamType.ENUM)
    Optional<SizeBucket> deviceSizeBucket();

    @WamProperty(index = 11, type = WamType.TIMER)
    Optional<Instant> durationAbs();

    @WamProperty(index = 12, type = WamType.TIMER)
    Optional<Instant> durationRelative();

    @WamProperty(index = 1, type = WamType.TIMER)
    Optional<Instant> durationT();

    @WamProperty(index = 42, type = WamType.ENUM)
    Optional<EditType> editType();

    @WamProperty(index = 15, type = WamType.BOOLEAN)
    Optional<Boolean> fetchPrekeys();

    @WamProperty(index = 21, type = WamType.INTEGER)
    OptionalLong fetchPrekeysPercentage();

    @WamProperty(index = 17, type = WamType.ENUM)
    Optional<ClientGroupSizeBucket> groupSizeBucket();

    @WamProperty(index = 45, type = WamType.ENUM)
    Optional<InvisibleMessageCategoryType> invisibleMessageCategory();

    @WamProperty(index = 33, type = WamType.BOOLEAN)
    Optional<Boolean> isDirectedMessage();

    @WamProperty(index = 27, type = WamType.BOOLEAN)
    Optional<Boolean> isE2eBackfill();

    @WamProperty(index = 41, type = WamType.BOOLEAN)
    Optional<Boolean> isLid();

    @WamProperty(index = 9, type = WamType.BOOLEAN)
    Optional<Boolean> isMessageFanout();

    @WamProperty(index = 8, type = WamType.BOOLEAN)
    Optional<Boolean> isMessageForward();

    @WamProperty(index = 49, type = WamType.BOOLEAN)
    Optional<Boolean> isPq();

    @WamProperty(index = 24, type = WamType.BOOLEAN)
    Optional<Boolean> isRevokeMessage();

    @WamProperty(index = 29, type = WamType.BOOLEAN)
    Optional<Boolean> isViewOnce();

    @WamProperty(index = 18, type = WamType.INTEGER)
    OptionalLong jobsInQueue();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<MediaType> mediaType();

    @WamProperty(index = 30, type = WamType.BOOLEAN)
    Optional<Boolean> messageIsFirstUserMessage();

    @WamProperty(index = 31, type = WamType.BOOLEAN)
    Optional<Boolean> messageIsInvisible();

    @WamProperty(index = 50, type = WamType.ENUM)
    Optional<MessageSendResultType> messageSendResult();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<MessageType> messageType();

    @WamProperty(index = 14, type = WamType.BOOLEAN)
    Optional<Boolean> networkWasDisconnected();

    @WamProperty(index = 37, type = WamType.INTEGER)
    OptionalLong participantCount();

    @WamProperty(index = 34, type = WamType.INTEGER)
    OptionalLong phoneCores();

    @WamProperty(index = 28, type = WamType.BOOLEAN)
    Optional<Boolean> prekeysEligibleForPrallelProcessing();

    @WamProperty(index = 39, type = WamType.INTEGER)
    OptionalLong receiverDeviceCount();

    @WamProperty(index = 44, type = WamType.STRING)
    Optional<String> runningTasks();

    @WamProperty(index = 13, type = WamType.INTEGER)
    OptionalLong sendCount();

    @WamProperty(index = 10, type = WamType.INTEGER)
    OptionalLong sendRetryCount();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<ClientMessageSendStage> sendStage();

    @WamProperty(index = 40, type = WamType.INTEGER)
    OptionalLong senderDeviceCount();

    @WamProperty(index = 23, type = WamType.INTEGER)
    OptionalLong senderKeyDistributionCountPercentage();

    @WamProperty(index = 25, type = WamType.BOOLEAN)
    Optional<Boolean> sessionsMissingWhenComposing();

    @WamProperty(index = 20, type = WamType.ENUM)
    Optional<ClientGroupSizeBucket> targetDeviceGroupSizeBucket();

    @WamProperty(index = 19, type = WamType.INTEGER)
    OptionalLong threadsInExecution();

    @WamProperty(index = 38, type = WamType.ENUM)
    Optional<TypeOfGroupEnum> typeOfGroup();

    @WamProperty(index = 46, type = WamType.STRING)
    Optional<String> userToDeviceSizeBucket();
}
