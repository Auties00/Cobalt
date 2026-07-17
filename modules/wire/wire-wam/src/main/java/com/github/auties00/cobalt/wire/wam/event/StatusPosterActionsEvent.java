package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.EngagementCardType;
import com.github.auties00.cobalt.wire.wam.type.EngagementCardVariant;
import com.github.auties00.cobalt.wire.wam.type.PairedMediaType;
import com.github.auties00.cobalt.wire.wam.type.SelectedLayoutConfigId;
import com.github.auties00.cobalt.wire.wam.type.StatusCategory;
import com.github.auties00.cobalt.wire.wam.type.StatusContentSource;
import com.github.auties00.cobalt.wire.wam.type.StatusContentType;
import com.github.auties00.cobalt.wire.wam.type.StatusCreationEntryPoint;
import com.github.auties00.cobalt.wire.wam.type.StatusEventType;
import com.github.auties00.cobalt.wire.wam.type.StatusMediaPickerFormatType;
import com.github.auties00.cobalt.wire.wam.type.StatusPairedMediaQuality;
import com.github.auties00.cobalt.wire.wam.type.StatusPrivacyType;
import com.github.auties00.cobalt.wire.wam.type.StickerType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebStatusPosterActionsWamEvent")
@WamEvent(id = 3546)
public interface StatusPosterActionsEvent extends WamEventSpec {
    @WamProperty(index = 32, type = WamType.BOOLEAN)
    Optional<Boolean> canSaveAsDraft();

    @WamProperty(index = 34, type = WamType.ENUM)
    Optional<EngagementCardType> cardType();

    @WamProperty(index = 35, type = WamType.ENUM)
    Optional<EngagementCardVariant> cardVariant();

    @WamProperty(index = 36, type = WamType.STRING)
    Optional<String> cid();

    @WamProperty(index = 26, type = WamType.INTEGER)
    OptionalLong defaultTrimmedVideoDuration();

    @WamProperty(index = 14, type = WamType.BOOLEAN)
    Optional<Boolean> editable();

    @WamProperty(index = 15, type = WamType.INTEGER)
    OptionalLong externalInteractables();

    @WamProperty(index = 16, type = WamType.STRING)
    Optional<String> externalPackageName();

    @WamProperty(index = 33, type = WamType.BOOLEAN)
    Optional<Boolean> hasDraftAvailable();

    @WamProperty(index = 37, type = WamType.BOOLEAN)
    Optional<Boolean> isBatchedPost();

    @WamProperty(index = 28, type = WamType.BOOLEAN)
    Optional<Boolean> isFavoured();

    @WamProperty(index = 29, type = WamType.INTEGER)
    OptionalLong mediaIndex();

    @WamProperty(index = 27, type = WamType.INTEGER)
    OptionalLong originalVideoDuration();

    @WamProperty(index = 24, type = WamType.ENUM)
    Optional<PairedMediaType> pairedMediaType();

    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong retryCount();

    @WamProperty(index = 21, type = WamType.ENUM)
    Optional<SelectedLayoutConfigId> selectedLayoutConfigId();

    @WamProperty(index = 22, type = WamType.INTEGER)
    OptionalLong selectedMediaCount();

    @WamProperty(index = 17, type = WamType.STRING)
    Optional<String> shareType();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong statusAudienceSize();

    @WamProperty(index = 30, type = WamType.ENUM)
    Optional<StatusCategory> statusCategory();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<StatusContentSource> statusContentSource();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<StatusContentType> statusContentType();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<StatusCreationEntryPoint> statusCreationEntryPoint();

    @WamProperty(index = 13, type = WamType.INTEGER)
    OptionalLong statusDuration();

    @WamProperty(index = 7, type = WamType.ENUM)
    Optional<StatusEventType> statusEventType();

    @WamProperty(index = 12, type = WamType.STRING)
    Optional<String> statusId();

    @WamProperty(index = 23, type = WamType.ENUM)
    Optional<StatusMediaPickerFormatType> statusMediaPickerFormatType();

    @WamProperty(index = 25, type = WamType.ENUM)
    Optional<StatusPairedMediaQuality> statusPairedMediaQuality();

    @WamProperty(index = 38, type = WamType.STRING)
    Optional<String> statusPartCode();

    @WamProperty(index = 8, type = WamType.STRING)
    Optional<String> statusPostFailureReason();

    @WamProperty(index = 20, type = WamType.INTEGER)
    OptionalLong statusPostingSessionId();

    @WamProperty(index = 10, type = WamType.ENUM)
    Optional<StatusPrivacyType> statusPrivacyType();

    @WamProperty(index = 11, type = WamType.INTEGER)
    OptionalLong statusSessionId();

    @WamProperty(index = 18, type = WamType.ENUM)
    Optional<StickerType> stickerType();

    @WamProperty(index = 19, type = WamType.STRING)
    Optional<String> unifiedSessionId();

    @WamProperty(index = 31, type = WamType.INTEGER)
    OptionalLong updatesTabSessionId();
}
