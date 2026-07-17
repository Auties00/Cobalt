package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ConnectionType;
import com.github.auties00.cobalt.wire.wam.type.HttpProtocolVersionType;
import com.github.auties00.cobalt.wire.wam.type.MediaQuality;
import com.github.auties00.cobalt.wire.wam.type.MediaType;
import com.github.auties00.cobalt.wire.wam.type.MediaUploadModeType;
import com.github.auties00.cobalt.wire.wam.type.MediaUploadResultType;
import com.github.auties00.cobalt.wire.wam.type.NetworkStackType;
import com.github.auties00.cobalt.wire.wam.type.OptimisticFlagType;
import com.github.auties00.cobalt.wire.wam.type.OverallLastUploadRetryPhaseType;
import com.github.auties00.cobalt.wire.wam.type.OverallMediaKeyReuseType;
import com.github.auties00.cobalt.wire.wam.type.PairedMediaType;
import com.github.auties00.cobalt.wire.wam.type.UploadOriginType;
import com.github.auties00.cobalt.wire.wam.type.UploadSourceType;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebMediaUpload2WamEvent")
@WamEvent(id = 1588)
public interface MediaUpload2Event extends WamEventSpec {
    @WamProperty(index = 63, type = WamType.STRING)
    Optional<String> appContext();

    @WamProperty(index = 64, type = WamType.INTEGER)
    OptionalLong appContextBitfield();

    @WamProperty(index = 57, type = WamType.INTEGER)
    OptionalLong batchSize();

    @WamProperty(index = 43, type = WamType.ENUM)
    Optional<ConnectionType> connectionType();

    @WamProperty(index = 34, type = WamType.STRING)
    Optional<String> debugMediaException();

    @WamProperty(index = 32, type = WamType.STRING)
    Optional<String> debugMediaIp();

    @WamProperty(index = 33, type = WamType.STRING)
    Optional<String> debugUrl();

    @WamProperty(index = 45, type = WamType.FLOAT)
    OptionalDouble estimatedBandwidth();

    @WamProperty(index = 61, type = WamType.FLOAT)
    OptionalDouble estimatedBandwidthV2();

    @WamProperty(index = 55, type = WamType.INTEGER)
    OptionalLong fileHeight();

    @WamProperty(index = 56, type = WamType.INTEGER)
    OptionalLong fileWidth();

    @WamProperty(index = 28, type = WamType.TIMER)
    Optional<Instant> finalizeConnectT();

    @WamProperty(index = 31, type = WamType.INTEGER)
    OptionalLong finalizeHttpCode();

    @WamProperty(index = 30, type = WamType.BOOLEAN)
    Optional<Boolean> finalizeIsReuse();

    @WamProperty(index = 29, type = WamType.TIMER)
    Optional<Instant> finalizeNetworkT();

    @WamProperty(index = 51, type = WamType.ENUM)
    Optional<HttpProtocolVersionType> httpProtocolVersionType();

    @WamProperty(index = 49, type = WamType.BOOLEAN)
    Optional<Boolean> isViewOnce();

    @WamProperty(index = 60, type = WamType.INTEGER)
    OptionalLong mediaCollectionId();

    @WamProperty(index = 46, type = WamType.INTEGER)
    OptionalLong mediaId();

    @WamProperty(index = 58, type = WamType.STRING)
    Optional<String> messageKeyHash();

    @WamProperty(index = 42, type = WamType.ENUM)
    Optional<NetworkStackType> networkStack();

    @WamProperty(index = 53, type = WamType.INTEGER)
    OptionalLong originalSize();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong overallAttemptCount();

    @WamProperty(index = 10, type = WamType.TIMER)
    Optional<Instant> overallConnBlockFetchT();

    @WamProperty(index = 41, type = WamType.STRING)
    Optional<String> overallConnectionClass();

    @WamProperty(index = 37, type = WamType.TIMER)
    Optional<Instant> overallCumT();

    @WamProperty(index = 38, type = WamType.TIMER)
    Optional<Instant> overallCumUserVisibleT();

    @WamProperty(index = 5, type = WamType.STRING)
    Optional<String> overallDomain();

    @WamProperty(index = 50, type = WamType.TIMER)
    Optional<Instant> overallEncryptT();

    @WamProperty(index = 36, type = WamType.BOOLEAN)
    Optional<Boolean> overallIsFinal();

    @WamProperty(index = 16, type = WamType.BOOLEAN)
    Optional<Boolean> overallIsForward();

    @WamProperty(index = 13, type = WamType.BOOLEAN)
    Optional<Boolean> overallIsManual();

    @WamProperty(index = 11, type = WamType.ENUM)
    Optional<OverallLastUploadRetryPhaseType> overallLastUploadRetryPhase();

    @WamProperty(index = 40, type = WamType.ENUM)
    Optional<OverallMediaKeyReuseType> overallMediaKeyReuse();

    @WamProperty(index = 7, type = WamType.FLOAT)
    OptionalDouble overallMediaSize();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<MediaType> overallMediaType();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong overallMmsVersion();

    @WamProperty(index = 12, type = WamType.ENUM)
    Optional<OptimisticFlagType> overallOptimisticFlag();

    @WamProperty(index = 9, type = WamType.TIMER)
    Optional<Instant> overallQueueT();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong overallRetryCount();

    @WamProperty(index = 8, type = WamType.TIMER)
    Optional<Instant> overallT();

    @WamProperty(index = 15, type = WamType.TIMER)
    Optional<Instant> overallTranscodeT();

    @WamProperty(index = 39, type = WamType.ENUM)
    Optional<MediaUploadModeType> overallUploadMode();

    @WamProperty(index = 44, type = WamType.ENUM)
    Optional<UploadOriginType> overallUploadOrigin();

    @WamProperty(index = 35, type = WamType.ENUM)
    Optional<MediaUploadResultType> overallUploadResult();

    @WamProperty(index = 14, type = WamType.TIMER)
    Optional<Instant> overallUserVisibleT();

    @WamProperty(index = 62, type = WamType.ENUM)
    Optional<PairedMediaType> pairedMediaType();

    @WamProperty(index = 52, type = WamType.ENUM)
    Optional<MediaQuality> photoQualitySetting();

    @WamProperty(index = 17, type = WamType.TIMER)
    Optional<Instant> resumeConnectT();

    @WamProperty(index = 20, type = WamType.INTEGER)
    OptionalLong resumeHttpCode();

    @WamProperty(index = 19, type = WamType.BOOLEAN)
    Optional<Boolean> resumeIsReuse();

    @WamProperty(index = 18, type = WamType.TIMER)
    Optional<Instant> resumeNetworkT();

    @WamProperty(index = 27, type = WamType.FLOAT)
    OptionalDouble uploadBytesTransferred();

    @WamProperty(index = 22, type = WamType.TIMER)
    Optional<Instant> uploadConnectT();

    @WamProperty(index = 25, type = WamType.INTEGER)
    OptionalLong uploadHttpCode();

    @WamProperty(index = 24, type = WamType.BOOLEAN)
    Optional<Boolean> uploadIsReuse();

    @WamProperty(index = 26, type = WamType.BOOLEAN)
    Optional<Boolean> uploadIsStreaming();

    @WamProperty(index = 23, type = WamType.TIMER)
    Optional<Instant> uploadNetworkT();

    @WamProperty(index = 21, type = WamType.INTEGER)
    OptionalLong uploadResumePoint();

    @WamProperty(index = 48, type = WamType.ENUM)
    Optional<UploadSourceType> uploadSource();

    @WamProperty(index = 47, type = WamType.STRING)
    Optional<String> usedFallbackHint();

    @WamProperty(index = 54, type = WamType.ENUM)
    Optional<MediaQuality> videoQualitySetting();
}
