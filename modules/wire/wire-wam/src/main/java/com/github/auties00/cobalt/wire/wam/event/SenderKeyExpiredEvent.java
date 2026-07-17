package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ExpiryReason;
import com.github.auties00.cobalt.wire.wam.type.MessageChatType;
import com.github.auties00.cobalt.wire.wam.type.SizeBucket;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebSenderKeyExpiredWamEvent")
@WamEvent(id = 3130)
public interface SenderKeyExpiredEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<MessageChatType> chatType();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<SizeBucket> deviceSizeBucket();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<ExpiryReason> expiryReason();
}
