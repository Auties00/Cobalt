package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.AddressingMode;
import com.github.auties00.cobalt.wire.wam.type.BotType;
import com.github.auties00.cobalt.wire.wam.type.E2eDeviceType;
import com.github.auties00.cobalt.wire.wam.type.EncryptionTypeCode;
import com.github.auties00.cobalt.wire.wam.type.MediaType;
import com.github.auties00.cobalt.wire.wam.type.MessageType;
import com.github.auties00.cobalt.wire.wam.type.PlaceholderAction;
import com.github.auties00.cobalt.wire.wam.type.PlaceholderChatType;
import com.github.auties00.cobalt.wire.wam.type.PlaceholderPopulationType;
import com.github.auties00.cobalt.wire.wam.type.PlaceholderReasonType;
import com.github.auties00.cobalt.wire.wam.type.PlaceholderType;
import com.github.auties00.cobalt.wire.wam.type.PlatformType;
import com.github.auties00.cobalt.wire.wam.type.SizeBucket;
import com.github.auties00.cobalt.wire.wam.type.TypeOfGroupEnum;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebPlaceholderActivityWamEvent")
@WamEvent(id = 1980)
public interface PlaceholderActivityEvent extends WamEventSpec {
    @WamProperty(index = 24, type = WamType.ENUM)
    Optional<BotType> botType();

    @WamProperty(index = 9, type = WamType.INTEGER)
    OptionalLong deviceCount();

    @WamProperty(index = 8, type = WamType.ENUM)
    Optional<SizeBucket> deviceSizeBucket();

    @WamProperty(index = 16, type = WamType.ENUM)
    Optional<E2eDeviceType> e2eSenderType();

    @WamProperty(index = 22, type = WamType.ENUM)
    Optional<EncryptionTypeCode> encryptionType();

    @WamProperty(index = 19, type = WamType.BOOLEAN)
    Optional<Boolean> isHostedChat();

    @WamProperty(index = 12, type = WamType.BOOLEAN)
    Optional<Boolean> isLid();

    @WamProperty(index = 23, type = WamType.BOOLEAN)
    Optional<Boolean> isSimpleSignal();

    @WamProperty(index = 20, type = WamType.ENUM)
    Optional<AddressingMode> localAddressingMode();

    @WamProperty(index = 15, type = WamType.BOOLEAN)
    Optional<Boolean> messageBeforeReg();

    @WamProperty(index = 7, type = WamType.BOOLEAN)
    Optional<Boolean> messageIsRevoke();

    @WamProperty(index = 14, type = WamType.STRING)
    Optional<String> messageKeyHash();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<MediaType> messageMediaType();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<MessageType> messageType();

    @WamProperty(index = 10, type = WamType.INTEGER)
    OptionalLong participantCount();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<PlaceholderAction> placeholderActionInd();

    @WamProperty(index = 11, type = WamType.ENUM)
    Optional<PlaceholderReasonType> placeholderAddReason();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<PlaceholderChatType> placeholderChatTypeInd();

    @WamProperty(index = 17, type = WamType.ENUM)
    Optional<PlaceholderPopulationType> placeholderPopulationType();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong placeholderTimePeriod();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<PlaceholderType> placeholderTypeInd();

    @WamProperty(index = 25, type = WamType.ENUM)
    Optional<PlatformType> senderPlatform();

    @WamProperty(index = 13, type = WamType.ENUM)
    Optional<TypeOfGroupEnum> typeOfGroup();
}
