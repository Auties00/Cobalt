package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.AddressingMode;
import com.github.auties00.cobalt.wire.wam.type.AgentEngagementEnumType;
import com.github.auties00.cobalt.wire.wam.type.BotType;
import com.github.auties00.cobalt.wire.wam.type.E2eCiphertextType;
import com.github.auties00.cobalt.wire.wam.type.E2eDestination;
import com.github.auties00.cobalt.wire.wam.type.E2eDeviceType;
import com.github.auties00.cobalt.wire.wam.type.E2eFailureReason;
import com.github.auties00.cobalt.wire.wam.type.EncryptionTypeCode;
import com.github.auties00.cobalt.wire.wam.type.InvisibleMessageCategoryType;
import com.github.auties00.cobalt.wire.wam.type.MediaType;
import com.github.auties00.cobalt.wire.wam.type.MessageDropReasonType;
import com.github.auties00.cobalt.wire.wam.type.PlatformType;
import com.github.auties00.cobalt.wire.wam.type.RevokeType;
import com.github.auties00.cobalt.wire.wam.type.SessionScopeType;
import com.github.auties00.cobalt.wire.wam.type.TypeOfGroupEnum;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebIncomingMessageDropWamEvent")
@WamEvent(id = 3724)
public interface IncomingMessageDropEvent extends WamEventSpec {
    @WamProperty(index = 12, type = WamType.ENUM)
    Optional<AgentEngagementEnumType> agentEngagementType();

    @WamProperty(index = 14, type = WamType.ENUM)
    Optional<BotType> botType();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<E2eCiphertextType> e2eCiphertextType();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<E2eDestination> e2eDestination();

    @WamProperty(index = 9, type = WamType.ENUM)
    Optional<E2eFailureReason> e2eFailureReason();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<E2eDeviceType> e2eSenderType();

    @WamProperty(index = 18, type = WamType.ENUM)
    Optional<EncryptionTypeCode> encryptionType();

    @WamProperty(index = 13, type = WamType.ENUM)
    Optional<InvisibleMessageCategoryType> invisibleMessageCategory();

    @WamProperty(index = 15, type = WamType.BOOLEAN)
    Optional<Boolean> isLid();

    @WamProperty(index = 20, type = WamType.BOOLEAN)
    Optional<Boolean> isPq();

    @WamProperty(index = 19, type = WamType.BOOLEAN)
    Optional<Boolean> isSimpleSignal();

    @WamProperty(index = 16, type = WamType.ENUM)
    Optional<AddressingMode> localAddressingMode();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<MessageDropReasonType> messageDropReason();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<MediaType> messageMediaType();

    @WamProperty(index = 6, type = WamType.BOOLEAN)
    Optional<Boolean> offline();

    @WamProperty(index = 11, type = WamType.INTEGER)
    OptionalLong offlineCount();

    @WamProperty(index = 22, type = WamType.BOOLEAN)
    Optional<Boolean> processingDeferred();

    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong retryCount();

    @WamProperty(index = 8, type = WamType.ENUM)
    Optional<RevokeType> revokeType();

    @WamProperty(index = 23, type = WamType.ENUM)
    Optional<PlatformType> senderPlatform();

    @WamProperty(index = 21, type = WamType.ENUM)
    Optional<SessionScopeType> sessionScope();

    @WamProperty(index = 10, type = WamType.ENUM)
    Optional<TypeOfGroupEnum> typeOfGroup();
}
