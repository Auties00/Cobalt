package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.NoticeType;
import com.github.auties00.cobalt.wire.wam.type.UserNoticeErrorEventType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebUserNoticeErrorWamEvent")
@WamEvent(id = 2474)
public interface UserNoticeErrorEvent extends WamEventSpec {
    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<NoticeType> noticeType();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong userNoticeContentVersion();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<UserNoticeErrorEventType> userNoticeErrorEvent();

    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong userNoticeId();
}
