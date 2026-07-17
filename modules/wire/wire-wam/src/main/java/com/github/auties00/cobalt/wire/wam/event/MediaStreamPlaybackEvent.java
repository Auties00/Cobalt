package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.MediaType;
import com.github.auties00.cobalt.wire.wam.type.PlaybackOriginType;
import com.github.auties00.cobalt.wire.wam.type.PlaybackStateType;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebMediaStreamPlaybackWamEvent")
@WamEvent(id = 1584)
public interface MediaStreamPlaybackEvent extends WamEventSpec {
    @WamProperty(index = 4, type = WamType.FLOAT)
    OptionalDouble bytesDownloadedStart();

    @WamProperty(index = 5, type = WamType.FLOAT)
    OptionalDouble bytesTransferred();

    @WamProperty(index = 15, type = WamType.BOOLEAN)
    Optional<Boolean> didPlay();

    @WamProperty(index = 12, type = WamType.INTEGER)
    OptionalLong forcedPlayCount();

    @WamProperty(index = 7, type = WamType.TIMER)
    Optional<Instant> initialBufferingT();

    @WamProperty(index = 2, type = WamType.FLOAT)
    OptionalDouble mediaSize();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<MediaType> mediaType();

    @WamProperty(index = 10, type = WamType.TIMER)
    Optional<Instant> overallPlayT();

    @WamProperty(index = 1, type = WamType.TIMER)
    Optional<Instant> overallT();

    @WamProperty(index = 14, type = WamType.INTEGER)
    OptionalLong playbackCount();

    @WamProperty(index = 17, type = WamType.INTEGER)
    OptionalLong playbackError();

    @WamProperty(index = 16, type = WamType.ENUM)
    Optional<PlaybackOriginType> playbackOrigin();

    @WamProperty(index = 11, type = WamType.ENUM)
    Optional<PlaybackStateType> playbackState();

    @WamProperty(index = 13, type = WamType.INTEGER)
    OptionalLong seekCount();

    @WamProperty(index = 18, type = WamType.STRING)
    Optional<String> statusId();

    @WamProperty(index = 9, type = WamType.INTEGER)
    OptionalLong totalRebufferingCount();

    @WamProperty(index = 8, type = WamType.TIMER)
    Optional<Instant> totalRebufferingT();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong videoDuration();
}
