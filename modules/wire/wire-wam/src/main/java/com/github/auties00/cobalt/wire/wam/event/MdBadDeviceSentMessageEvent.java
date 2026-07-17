package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.DeviceType;
import com.github.auties00.cobalt.wire.wam.type.DsmError;
import com.github.auties00.cobalt.wire.wam.type.EditType;
import com.github.auties00.cobalt.wire.wam.type.EncryptionTypeCode;
import com.github.auties00.cobalt.wire.wam.type.MediaType;
import com.github.auties00.cobalt.wire.wam.type.MessageType;
import com.github.auties00.cobalt.wire.wam.type.PlatformType;
import com.github.auties00.cobalt.wire.wam.type.RevokeType;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebMdBadDeviceSentMessageWamEvent")
@WamEvent(id = 2176)
public interface MdBadDeviceSentMessageEvent extends WamEventSpec {
    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<DsmError> dsmError();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<EditType> editType();

    @WamProperty(index = 9, type = WamType.ENUM)
    Optional<EncryptionTypeCode> encryptionType();

    @WamProperty(index = 4, type = WamType.BOOLEAN)
    Optional<Boolean> isLid();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<MediaType> mediaType();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<MessageType> messageType();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<DeviceType> peerType();

    @WamProperty(index = 7, type = WamType.ENUM)
    Optional<RevokeType> revokeType();

    @WamProperty(index = 10, type = WamType.ENUM)
    Optional<PlatformType> senderPlatform();
}
