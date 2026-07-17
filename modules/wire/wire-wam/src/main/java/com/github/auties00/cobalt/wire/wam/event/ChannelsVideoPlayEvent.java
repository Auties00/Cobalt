package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.VideoPlayOrigin;
import com.github.auties00.cobalt.wire.wam.type.VideoPlayResult;
import com.github.auties00.cobalt.wire.wam.type.VideoPlayType;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebChannelsVideoPlayWamEvent")
@WamEvent(id = 6556)
public interface ChannelsVideoPlayEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong autoPlayT();

    @WamProperty(index = 2, type = WamType.STRING)
    Optional<String> cid();

    @WamProperty(index = 13, type = WamType.INTEGER)
    OptionalLong finishCount();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong height();

    @WamProperty(index = 4, type = WamType.STRING)
    Optional<String> postId();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong videoDuration();

    @WamProperty(index = 6, type = WamType.TIMER)
    Optional<Instant> videoInitialBufferingT();

    @WamProperty(index = 7, type = WamType.ENUM)
    Optional<VideoPlayOrigin> videoPlayOrigin();

    @WamProperty(index = 8, type = WamType.ENUM)
    Optional<VideoPlayResult> videoPlayResult();

    @WamProperty(index = 9, type = WamType.INTEGER)
    OptionalLong videoPlayT();

    @WamProperty(index = 10, type = WamType.ENUM)
    Optional<VideoPlayType> videoPlayType();

    @WamProperty(index = 11, type = WamType.FLOAT)
    OptionalDouble videoSize();

    @WamProperty(index = 14, type = WamType.STRING)
    Optional<String> watchingModule();

    @WamProperty(index = 12, type = WamType.INTEGER)
    OptionalLong width();
}
