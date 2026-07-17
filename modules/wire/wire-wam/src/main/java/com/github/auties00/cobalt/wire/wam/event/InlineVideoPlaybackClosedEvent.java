package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.InlineVideoCtaClick;
import com.github.auties00.cobalt.wire.wam.type.InlineVideoType;
import com.github.auties00.cobalt.wire.wam.type.MessageType;
import com.github.auties00.cobalt.wire.wam.type.PlatformType;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebInlineVideoPlaybackClosedWamEvent")
@WamEvent(id = 2032)
public interface InlineVideoPlaybackClosedEvent extends WamEventSpec {
    @WamProperty(index = 14, type = WamType.INTEGER)
    OptionalLong chatSize();

    @WamProperty(index = 7, type = WamType.TIMER)
    Optional<Instant> inlineVideoCancelBeforePlayStateT();

    @WamProperty(index = 8, type = WamType.BOOLEAN)
    Optional<Boolean> inlineVideoComplete();

    @WamProperty(index = 9, type = WamType.INTEGER)
    OptionalLong inlineVideoCompletionRate();

    @WamProperty(index = 10, type = WamType.ENUM)
    Optional<InlineVideoCtaClick> inlineVideoCtaClick();

    @WamProperty(index = 2, type = WamType.TIMER)
    Optional<Instant> inlineVideoDurationT();

    @WamProperty(index = 11, type = WamType.STRING)
    Optional<String> inlineVideoError();

    @WamProperty(index = 13, type = WamType.BOOLEAN)
    Optional<Boolean> inlineVideoHasRcat();

    @WamProperty(index = 6, type = WamType.TIMER)
    Optional<Instant> inlineVideoPlayStartT();

    @WamProperty(index = 3, type = WamType.BOOLEAN)
    Optional<Boolean> inlineVideoPlayed();

    @WamProperty(index = 4, type = WamType.TIMER)
    Optional<Instant> inlineVideoStallT();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<InlineVideoType> inlineVideoType();

    @WamProperty(index = 5, type = WamType.TIMER)
    Optional<Instant> inlineVideoWatchT();

    @WamProperty(index = 15, type = WamType.BOOLEAN)
    Optional<Boolean> isSentByMe();

    @WamProperty(index = 12, type = WamType.ENUM)
    Optional<MessageType> messageType();

    @WamProperty(index = 16, type = WamType.ENUM)
    Optional<PlatformType> rcatSenderPlatform();
}
