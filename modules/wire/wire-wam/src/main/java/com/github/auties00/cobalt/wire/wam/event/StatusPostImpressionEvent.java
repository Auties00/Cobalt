package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ChannelUserType;
import com.github.auties00.cobalt.wire.wam.type.GroupStatusSizeBucket;
import com.github.auties00.cobalt.wire.wam.type.InlineVideoType;
import com.github.auties00.cobalt.wire.wam.type.MediaType;
import com.github.auties00.cobalt.wire.wam.type.PairedMediaType;
import com.github.auties00.cobalt.wire.wam.type.StatusCategory;
import com.github.auties00.cobalt.wire.wam.type.StatusContentType;
import com.github.auties00.cobalt.wire.wam.type.StatusItemViewResult;
import com.github.auties00.cobalt.wire.wam.type.StatusPosterContactType;
import com.github.auties00.cobalt.wire.wam.type.StatusRowSection;
import com.github.auties00.cobalt.wire.wam.type.StatusViewEntryMethod;
import com.github.auties00.cobalt.wire.wam.type.StatusViewExitMethod;
import com.github.auties00.cobalt.wire.wam.type.UrlStatusType;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebStatusPostImpressionWamEvent")
@WamEvent(id = 6364)
public interface StatusPostImpressionEvent extends WamEventSpec {
    @WamProperty(index = 46, type = WamType.INTEGER)
    OptionalLong channelStatusId();

    @WamProperty(index = 47, type = WamType.ENUM)
    Optional<ChannelUserType> channelUserType();

    @WamProperty(index = 48, type = WamType.STRING)
    Optional<String> cid();

    @WamProperty(index = 29, type = WamType.ENUM)
    Optional<StatusViewEntryMethod> entryMethod();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<InlineVideoType> externalSourceDomainType();

    @WamProperty(index = 41, type = WamType.ENUM)
    Optional<GroupStatusSizeBucket> groupStatusSizeBucket();

    @WamProperty(index = 42, type = WamType.BOOLEAN)
    Optional<Boolean> isCloseSharingPost();

    @WamProperty(index = 45, type = WamType.BOOLEAN)
    Optional<Boolean> isEngagementCard();

    @WamProperty(index = 2, type = WamType.BOOLEAN)
    Optional<Boolean> isFirstView();

    @WamProperty(index = 43, type = WamType.BOOLEAN)
    Optional<Boolean> isLastStatus();

    @WamProperty(index = 3, type = WamType.BOOLEAN)
    Optional<Boolean> isPosterBiz();

    @WamProperty(index = 49, type = WamType.BOOLEAN)
    Optional<Boolean> isResharable();

    @WamProperty(index = 50, type = WamType.BOOLEAN)
    Optional<Boolean> isReshare();

    @WamProperty(index = 24, type = WamType.BOOLEAN)
    Optional<Boolean> isSelfView();

    @WamProperty(index = 4, type = WamType.BOOLEAN)
    Optional<Boolean> isSubImpression();

    @WamProperty(index = 33, type = WamType.BOOLEAN)
    Optional<Boolean> isSubscribed();

    @WamProperty(index = 30, type = WamType.BOOLEAN)
    Optional<Boolean> isSuccessfulView();

    @WamProperty(index = 5, type = WamType.BOOLEAN)
    Optional<Boolean> isViewedInLandscape();

    @WamProperty(index = 6, type = WamType.BOOLEAN)
    Optional<Boolean> musicBlocked();

    @WamProperty(index = 51, type = WamType.ENUM)
    Optional<PairedMediaType> pairedMediaType();

    @WamProperty(index = 37, type = WamType.INTEGER)
    OptionalLong pogViewSequenceIndex();

    @WamProperty(index = 7, type = WamType.STRING)
    Optional<String> psaCampaignId();

    @WamProperty(index = 8, type = WamType.BOOLEAN)
    Optional<Boolean> psaLinkAvailable();

    @WamProperty(index = 38, type = WamType.STRING)
    Optional<String> statusAttributionTypes();

    @WamProperty(index = 25, type = WamType.ENUM)
    Optional<StatusCategory> statusCategory();

    @WamProperty(index = 9, type = WamType.BOOLEAN)
    Optional<Boolean> statusContainsMusic();

    @WamProperty(index = 28, type = WamType.BOOLEAN)
    Optional<Boolean> statusContainsQuestion();

    @WamProperty(index = 34, type = WamType.BOOLEAN)
    Optional<Boolean> statusContainsReactionSticker();

    @WamProperty(index = 10, type = WamType.ENUM)
    Optional<StatusContentType> statusContentType();

    @WamProperty(index = 39, type = WamType.STRING)
    Optional<String> statusGroupId();

    @WamProperty(index = 11, type = WamType.STRING)
    Optional<String> statusId();

    @WamProperty(index = 31, type = WamType.ENUM)
    Optional<StatusItemViewResult> statusItemViewResult();

    @WamProperty(index = 12, type = WamType.TIMER)
    Optional<Instant> statusLoadTime();

    @WamProperty(index = 13, type = WamType.ENUM)
    Optional<MediaType> statusMediaType();

    @WamProperty(index = 15, type = WamType.INTEGER)
    OptionalLong statusPogIndex();

    @WamProperty(index = 16, type = WamType.INTEGER)
    OptionalLong statusPostIndex();

    @WamProperty(index = 17, type = WamType.TIMER)
    Optional<Instant> statusPostPlaybackDuration();

    @WamProperty(index = 35, type = WamType.ENUM)
    Optional<StatusPosterContactType> statusPosterContactType();

    @WamProperty(index = 26, type = WamType.STRING)
    Optional<String> statusPosterHashId();

    @WamProperty(index = 40, type = WamType.STRING)
    Optional<String> statusPosterId();

    @WamProperty(index = 18, type = WamType.ENUM)
    Optional<StatusRowSection> statusViewEntrypoint();

    @WamProperty(index = 44, type = WamType.ENUM)
    Optional<StatusViewExitMethod> statusViewExitMethod();

    @WamProperty(index = 19, type = WamType.TIMER)
    Optional<Instant> statusViewTime();

    @WamProperty(index = 20, type = WamType.INTEGER)
    OptionalLong statusViewerSessionId();

    @WamProperty(index = 52, type = WamType.INTEGER)
    OptionalLong traceIdInt();

    @WamProperty(index = 21, type = WamType.STRING)
    Optional<String> unifiedSessionId();

    @WamProperty(index = 22, type = WamType.INTEGER)
    OptionalLong updatesTabSessionId();

    @WamProperty(index = 23, type = WamType.ENUM)
    Optional<UrlStatusType> urlStatusType();

    @WamProperty(index = 32, type = WamType.INTEGER)
    OptionalLong viewSequenceIndex();
}
