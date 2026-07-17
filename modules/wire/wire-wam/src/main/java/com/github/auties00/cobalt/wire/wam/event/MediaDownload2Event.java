package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.AiFetchMediaType;
import com.github.auties00.cobalt.wire.wam.type.BackendStoreType;
import com.github.auties00.cobalt.wire.wam.type.ConnectionType;
import com.github.auties00.cobalt.wire.wam.type.DownloadOriginType;
import com.github.auties00.cobalt.wire.wam.type.DownloadQualityType;
import com.github.auties00.cobalt.wire.wam.type.ExpressPathDownloadState;
import com.github.auties00.cobalt.wire.wam.type.HashVerificationFailureType;
import com.github.auties00.cobalt.wire.wam.type.HttpProtocolVersionType;
import com.github.auties00.cobalt.wire.wam.type.MediaDownloadModeType;
import com.github.auties00.cobalt.wire.wam.type.MediaDownloadResultType;
import com.github.auties00.cobalt.wire.wam.type.MediaType;
import com.github.auties00.cobalt.wire.wam.type.NetworkStackType;
import com.github.auties00.cobalt.wire.wam.type.PairedMediaType;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebMediaDownload2WamEvent")
@WamEvent(id = 1590, releaseWeight = 50)
public interface MediaDownload2Event extends WamEventSpec {
    @WamProperty(index = 55, type = WamType.INTEGER)
    OptionalLong activeThreadCount();

    @WamProperty(index = 62, type = WamType.ENUM)
    Optional<AiFetchMediaType> aiFetchMediaType();

    @WamProperty(index = 64, type = WamType.STRING)
    Optional<String> appContext();

    @WamProperty(index = 65, type = WamType.INTEGER)
    OptionalLong appContextBitfield();

    @WamProperty(index = 50, type = WamType.STRING)
    Optional<String> clientMessageId();

    @WamProperty(index = 31, type = WamType.ENUM)
    Optional<ConnectionType> connectionType();

    @WamProperty(index = 46, type = WamType.INTEGER)
    OptionalLong daysSinceReceive();

    @WamProperty(index = 24, type = WamType.STRING)
    Optional<String> debugMediaException();

    @WamProperty(index = 22, type = WamType.STRING)
    Optional<String> debugMediaIp();

    @WamProperty(index = 23, type = WamType.STRING)
    Optional<String> debugUrl();

    @WamProperty(index = 49, type = WamType.INTEGER)
    OptionalLong deviceCount();

    @WamProperty(index = 20, type = WamType.FLOAT)
    OptionalDouble downloadBytesTransferred();

    @WamProperty(index = 15, type = WamType.TIMER)
    Optional<Instant> downloadConnectT();

    @WamProperty(index = 18, type = WamType.INTEGER)
    OptionalLong downloadHttpCode();

    @WamProperty(index = 17, type = WamType.BOOLEAN)
    Optional<Boolean> downloadIsReuse();

    @WamProperty(index = 19, type = WamType.BOOLEAN)
    Optional<Boolean> downloadIsStreaming();

    @WamProperty(index = 16, type = WamType.TIMER)
    Optional<Instant> downloadNetworkT();

    @WamProperty(index = 37, type = WamType.ENUM)
    Optional<DownloadQualityType> downloadQuality();

    @WamProperty(index = 68, type = WamType.INTEGER)
    OptionalLong downloadQueueSize();

    @WamProperty(index = 14, type = WamType.INTEGER)
    OptionalLong downloadResumePoint();

    @WamProperty(index = 21, type = WamType.TIMER)
    Optional<Instant> downloadTimeToFirstByteT();

    @WamProperty(index = 36, type = WamType.FLOAT)
    OptionalDouble estimatedBandwidth();

    @WamProperty(index = 59, type = WamType.FLOAT)
    OptionalDouble estimatedBandwidthV2();

    @WamProperty(index = 42, type = WamType.FLOAT)
    OptionalDouble expressPathBytesSaved();

    @WamProperty(index = 56, type = WamType.ENUM)
    Optional<ExpressPathDownloadState> expressPathDownloadState();

    @WamProperty(index = 43, type = WamType.TIMER)
    Optional<Instant> expressPathTimeSavedMs();

    @WamProperty(index = 47, type = WamType.INTEGER)
    OptionalLong fileHeight();

    @WamProperty(index = 48, type = WamType.INTEGER)
    OptionalLong fileWidth();

    @WamProperty(index = 44, type = WamType.BOOLEAN)
    Optional<Boolean> hasLeveragedExpressPath();

    @WamProperty(index = 69, type = WamType.ENUM)
    Optional<HashVerificationFailureType> hashVerificationFailureType();

    @WamProperty(index = 45, type = WamType.ENUM)
    Optional<HttpProtocolVersionType> httpProtocolVersionType();

    @WamProperty(index = 58, type = WamType.BOOLEAN)
    Optional<Boolean> isProcessedVideo();

    @WamProperty(index = 52, type = WamType.BOOLEAN)
    Optional<Boolean> isSenderPlatformCapi();

    @WamProperty(index = 41, type = WamType.BOOLEAN)
    Optional<Boolean> isViewOnce();

    @WamProperty(index = 57, type = WamType.INTEGER)
    OptionalLong maxThreadCount();

    @WamProperty(index = 38, type = WamType.INTEGER)
    OptionalLong mediaId();

    @WamProperty(index = 30, type = WamType.ENUM)
    Optional<NetworkStackType> networkStack();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong overallAttemptCount();

    @WamProperty(index = 39, type = WamType.ENUM)
    Optional<BackendStoreType> overallBackendStore();

    @WamProperty(index = 10, type = WamType.TIMER)
    Optional<Instant> overallConnBlockFetchT();

    @WamProperty(index = 29, type = WamType.STRING)
    Optional<String> overallConnectionClass();

    @WamProperty(index = 27, type = WamType.TIMER)
    Optional<Instant> overallCumT();

    @WamProperty(index = 60, type = WamType.TIMER)
    Optional<Instant> overallCumUserVisibleT();

    @WamProperty(index = 53, type = WamType.TIMER)
    Optional<Instant> overallCumV2T();

    @WamProperty(index = 12, type = WamType.TIMER)
    Optional<Instant> overallDecryptT();

    @WamProperty(index = 5, type = WamType.STRING)
    Optional<String> overallDomain();

    @WamProperty(index = 11, type = WamType.ENUM)
    Optional<MediaDownloadModeType> overallDownloadMode();

    @WamProperty(index = 35, type = WamType.ENUM)
    Optional<DownloadOriginType> overallDownloadOrigin();

    @WamProperty(index = 25, type = WamType.ENUM)
    Optional<MediaDownloadResultType> overallDownloadResult();

    @WamProperty(index = 13, type = WamType.TIMER)
    Optional<Instant> overallFileValidationT();

    @WamProperty(index = 28, type = WamType.BOOLEAN)
    Optional<Boolean> overallIsEncrypted();

    @WamProperty(index = 26, type = WamType.BOOLEAN)
    Optional<Boolean> overallIsFinal();

    @WamProperty(index = 7, type = WamType.FLOAT)
    OptionalDouble overallMediaSize();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<MediaType> overallMediaType();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong overallMmsVersion();

    @WamProperty(index = 9, type = WamType.TIMER)
    Optional<Instant> overallQueueT();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong overallRetryCount();

    @WamProperty(index = 8, type = WamType.TIMER)
    Optional<Instant> overallT();

    @WamProperty(index = 61, type = WamType.TIMER)
    Optional<Instant> overallUserVisibleT();

    @WamProperty(index = 63, type = WamType.ENUM)
    Optional<PairedMediaType> pairedMediaType();

    @WamProperty(index = 66, type = WamType.INTEGER)
    OptionalLong prefetchOrder();

    @WamProperty(index = 51, type = WamType.BOOLEAN)
    Optional<Boolean> sleepModeAffected();

    @WamProperty(index = 67, type = WamType.STRING)
    Optional<String> statusId();

    @WamProperty(index = 70, type = WamType.BOOLEAN)
    Optional<Boolean> streamingUsedNonStreamingFallback();

    @WamProperty(index = 54, type = WamType.TIMER)
    Optional<Instant> timeDelayed();

    @WamProperty(index = 40, type = WamType.STRING)
    Optional<String> usedFallbackHint();
}
