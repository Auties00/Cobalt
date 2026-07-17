package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.AddressingMode;
import com.github.auties00.cobalt.wire.wam.type.EncryptionTypeCode;
import com.github.auties00.cobalt.wire.wam.type.InvisibleMessageCategoryType;
import com.github.auties00.cobalt.wire.wam.type.MessageChatType;
import com.github.auties00.cobalt.wire.wam.type.TypeOfGroupEnum;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebMdDeviceSyncAckWamEvent")
@WamEvent(id = 2180)
public interface MdDeviceSyncAckEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<MessageChatType> chatType();

    @WamProperty(index = 8, type = WamType.ENUM)
    Optional<EncryptionTypeCode> encryptionType();

    @WamProperty(index = 7, type = WamType.ENUM)
    Optional<InvisibleMessageCategoryType> invisibleMessageCategory();

    @WamProperty(index = 3, type = WamType.BOOLEAN)
    Optional<Boolean> isLid();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<AddressingMode> localAddressingMode();

    @WamProperty(index = 2, type = WamType.BOOLEAN)
    Optional<Boolean> revoke();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<AddressingMode> serverAddressingMode();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<TypeOfGroupEnum> typeOfGroup();
}
