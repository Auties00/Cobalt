package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.VideoTranscoderAlgorithmType;
import com.github.auties00.cobalt.wire.wam.type.VideoTranscoderResultType;
import com.github.auties00.cobalt.wire.wam.type.VideoTranscoderSourceFormatType;
import com.github.auties00.cobalt.wire.wam.type.VideoTranscoderTargetFormatType;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalDouble;

@WhatsAppWebModule(moduleName = "WAWebVideoTranscoderWamEvent")
@WamEvent(id = 1802)
public interface VideoTranscoderEvent extends WamEventSpec {
    @WamProperty(index = 12, type = WamType.FLOAT)
    OptionalDouble sourceAudioBitRate();

    @WamProperty(index = 23, type = WamType.STRING)
    Optional<String> sourceAudioCodec();

    @WamProperty(index = 24, type = WamType.STRING)
    Optional<String> sourceContainerFormat();

    @WamProperty(index = 8, type = WamType.TIMER)
    Optional<Instant> sourceDuration();

    @WamProperty(index = 25, type = WamType.STRING)
    Optional<String> sourceFileExtension();

    @WamProperty(index = 7, type = WamType.FLOAT)
    OptionalDouble sourceFileSize();

    @WamProperty(index = 14, type = WamType.ENUM)
    Optional<VideoTranscoderSourceFormatType> sourceFormat();

    @WamProperty(index = 13, type = WamType.FLOAT)
    OptionalDouble sourceFrameRate();

    @WamProperty(index = 10, type = WamType.FLOAT)
    OptionalDouble sourceHeight();

    @WamProperty(index = 26, type = WamType.STRING)
    Optional<String> sourceMimeType();

    @WamProperty(index = 11, type = WamType.FLOAT)
    OptionalDouble sourceVideoBitRate();

    @WamProperty(index = 27, type = WamType.STRING)
    Optional<String> sourceVideoCodec();

    @WamProperty(index = 9, type = WamType.FLOAT)
    OptionalDouble sourceWidth();

    @WamProperty(index = 20, type = WamType.FLOAT)
    OptionalDouble targetAudioBitRate();

    @WamProperty(index = 16, type = WamType.TIMER)
    Optional<Instant> targetDuration();

    @WamProperty(index = 15, type = WamType.FLOAT)
    OptionalDouble targetFileSize();

    @WamProperty(index = 22, type = WamType.ENUM)
    Optional<VideoTranscoderTargetFormatType> targetFormat();

    @WamProperty(index = 21, type = WamType.FLOAT)
    OptionalDouble targetFrameRate();

    @WamProperty(index = 18, type = WamType.FLOAT)
    OptionalDouble targetHeight();

    @WamProperty(index = 19, type = WamType.FLOAT)
    OptionalDouble targetVideoBitRate();

    @WamProperty(index = 17, type = WamType.FLOAT)
    OptionalDouble targetWidth();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<VideoTranscoderAlgorithmType> transcoderAlgorithm();

    @WamProperty(index = 5, type = WamType.BOOLEAN)
    Optional<Boolean> transcoderContainsVideocomposition();

    @WamProperty(index = 6, type = WamType.BOOLEAN)
    Optional<Boolean> transcoderHasEdits();

    @WamProperty(index = 4, type = WamType.BOOLEAN)
    Optional<Boolean> transcoderIsPassthrough();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<VideoTranscoderResultType> transcoderResult();

    @WamProperty(index = 3, type = WamType.TIMER)
    Optional<Instant> transcoderT();
}
