package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.InlineVideoType;
import com.github.auties00.cobalt.wire.wam.type.MediaType;
import com.github.auties00.cobalt.wire.wam.type.PairedMediaType;
import com.github.auties00.cobalt.wire.wam.type.PsaLinkOpenResult;
import com.github.auties00.cobalt.wire.wam.type.ReshareSource;
import com.github.auties00.cobalt.wire.wam.type.StatusCategory;
import com.github.auties00.cobalt.wire.wam.type.StatusItemViewResult;
import com.github.auties00.cobalt.wire.wam.type.StatusPairedMediaQuality;
import com.github.auties00.cobalt.wire.wam.type.StatusPosterContactType;
import com.github.auties00.cobalt.wire.wam.type.StatusRowSection;
import com.github.auties00.cobalt.wire.wam.type.StatusType;
import com.github.auties00.cobalt.wire.wam.type.UrlStatusClicked;
import com.github.auties00.cobalt.wire.wam.type.UrlStatusType;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebStatusItemViewWamEvent")
@WamEvent(id = 1658)
public interface StatusItemViewEvent extends WamEventSpec {
    @WamProperty(index = 47, type = WamType.FLOAT)
    OptionalDouble bytesDownloadedStartView();

    @WamProperty(index = 48, type = WamType.FLOAT)
    OptionalDouble estimatedBandwidth();

    @WamProperty(index = 34, type = WamType.ENUM)
    Optional<InlineVideoType> externalSourceDomainType();

    @WamProperty(index = 35, type = WamType.BOOLEAN)
    Optional<Boolean> isAlreadyDownloaded();

    @WamProperty(index = 44, type = WamType.BOOLEAN)
    Optional<Boolean> isForwardable();

    @WamProperty(index = 45, type = WamType.BOOLEAN)
    Optional<Boolean> isForwarded();

    @WamProperty(index = 23, type = WamType.BOOLEAN)
    Optional<Boolean> isPosterBiz();

    @WamProperty(index = 25, type = WamType.BOOLEAN)
    Optional<Boolean> isPosterInAddressBook();

    @WamProperty(index = 37, type = WamType.BOOLEAN)
    Optional<Boolean> isResharable();

    @WamProperty(index = 40, type = WamType.BOOLEAN)
    Optional<Boolean> isReshare();

    @WamProperty(index = 52, type = WamType.BOOLEAN)
    Optional<Boolean> isSubscribed();

    @WamProperty(index = 29, type = WamType.BOOLEAN)
    Optional<Boolean> isViewedInLandscape();

    @WamProperty(index = 49, type = WamType.INTEGER)
    OptionalLong mediaFileSize();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<MediaType> mediaType();

    @WamProperty(index = 32, type = WamType.BOOLEAN)
    Optional<Boolean> musicBlocked();

    @WamProperty(index = 38, type = WamType.ENUM)
    Optional<PairedMediaType> pairedMediaType();

    @WamProperty(index = 17, type = WamType.STRING)
    Optional<String> psaCampaignId();

    @WamProperty(index = 18, type = WamType.INTEGER)
    OptionalLong psaCampaignItemIndex();

    @WamProperty(index = 19, type = WamType.BOOLEAN)
    Optional<Boolean> psaLinkAvailable();

    @WamProperty(index = 22, type = WamType.BOOLEAN)
    Optional<Boolean> psaLinkClick();

    @WamProperty(index = 21, type = WamType.TIMER)
    Optional<Instant> psaLinkLoadTime();

    @WamProperty(index = 20, type = WamType.ENUM)
    Optional<PsaLinkOpenResult> psaLinkOpenResult();

    @WamProperty(index = 41, type = WamType.ENUM)
    Optional<ReshareSource> reshareSource();

    @WamProperty(index = 46, type = WamType.ENUM)
    Optional<StatusCategory> statusCategory();

    @WamProperty(index = 30, type = WamType.BOOLEAN)
    Optional<Boolean> statusContainsMusic();

    @WamProperty(index = 51, type = WamType.BOOLEAN)
    Optional<Boolean> statusContainsQuestion();

    @WamProperty(index = 56, type = WamType.BOOLEAN)
    Optional<Boolean> statusContainsReactionSticker();

    @WamProperty(index = 53, type = WamType.STRING)
    Optional<String> statusId();

    @WamProperty(index = 31, type = WamType.INTEGER)
    OptionalLong statusItem3sViewCount();

    @WamProperty(index = 14, type = WamType.INTEGER)
    OptionalLong statusItemImpressionCount();

    @WamProperty(index = 16, type = WamType.INTEGER)
    OptionalLong statusItemIndex();

    @WamProperty(index = 7, type = WamType.TIMER)
    Optional<Instant> statusItemLength();

    @WamProperty(index = 5, type = WamType.TIMER)
    Optional<Instant> statusItemLoadTime();

    @WamProperty(index = 8, type = WamType.INTEGER)
    OptionalLong statusItemReplied();

    @WamProperty(index = 9, type = WamType.BOOLEAN)
    Optional<Boolean> statusItemUnread();

    @WamProperty(index = 10, type = WamType.INTEGER)
    OptionalLong statusItemViewCount();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<StatusItemViewResult> statusItemViewResult();

    @WamProperty(index = 6, type = WamType.TIMER)
    Optional<Instant> statusItemViewTime();

    @WamProperty(index = 42, type = WamType.INTEGER)
    OptionalLong statusMediaHeight();

    @WamProperty(index = 43, type = WamType.INTEGER)
    OptionalLong statusMediaWidth();

    @WamProperty(index = 39, type = WamType.ENUM)
    Optional<StatusPairedMediaQuality> statusPairedMediaQuality();

    @WamProperty(index = 57, type = WamType.ENUM)
    Optional<StatusPosterContactType> statusPosterContactType();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong statusRowIndex();

    @WamProperty(index = 11, type = WamType.ENUM)
    Optional<StatusRowSection> statusRowSection();

    @WamProperty(index = 36, type = WamType.ENUM)
    Optional<StatusType> statusType();

    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong statusViewerSessionId();

    @WamProperty(index = 58, type = WamType.INTEGER)
    OptionalLong traceIdInt();

    @WamProperty(index = 54, type = WamType.STRING)
    Optional<String> unifiedSessionId();

    @WamProperty(index = 55, type = WamType.INTEGER)
    OptionalLong updatesTabSessionId();

    @WamProperty(index = 26, type = WamType.ENUM)
    Optional<UrlStatusClicked> urlStatusClicked();

    @WamProperty(index = 27, type = WamType.ENUM)
    Optional<UrlStatusType> urlStatusType();
}
