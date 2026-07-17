package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.MediaType;
import com.github.auties00.cobalt.wire.wam.type.PairedMediaType;
import com.github.auties00.cobalt.wire.wam.type.PrivacySettingsValueType;
import com.github.auties00.cobalt.wire.wam.type.StatusCategory;
import com.github.auties00.cobalt.wire.wam.type.StatusPairedMediaQuality;
import com.github.auties00.cobalt.wire.wam.type.StatusPostOrigin;
import com.github.auties00.cobalt.wire.wam.type.StatusPostResult;
import com.github.auties00.cobalt.wire.wam.type.StatusType;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebStatusPostWamEvent")
@WamEvent(id = 1176)
public interface StatusPostEvent extends WamEventSpec {
    @WamProperty(index = 57, type = WamType.INTEGER)
    OptionalLong channelStatusId();

    @WamProperty(index = 58, type = WamType.STRING)
    Optional<String> cid();

    @WamProperty(index = 19, type = WamType.BOOLEAN)
    Optional<Boolean> containsPrompt();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<PrivacySettingsValueType> defaultStatusPrivacySetting();

    @WamProperty(index = 44, type = WamType.STRING)
    Optional<String> dualUploadPairedMediaId();

    @WamProperty(index = 15, type = WamType.BOOLEAN)
    Optional<Boolean> editable();

    @WamProperty(index = 16, type = WamType.INTEGER)
    OptionalLong externalInteractables();

    @WamProperty(index = 17, type = WamType.STRING)
    Optional<String> externalPackageName();

    @WamProperty(index = 22, type = WamType.STRING)
    Optional<String> extraStickersData();

    @WamProperty(index = 48, type = WamType.STRING)
    Optional<String> groupMentionCount();

    @WamProperty(index = 23, type = WamType.BOOLEAN)
    Optional<Boolean> hasArFilters();

    @WamProperty(index = 24, type = WamType.BOOLEAN)
    Optional<Boolean> hasCaption();

    @WamProperty(index = 25, type = WamType.BOOLEAN)
    Optional<Boolean> hasDrawings();

    @WamProperty(index = 26, type = WamType.BOOLEAN)
    Optional<Boolean> hasFilters();

    @WamProperty(index = 49, type = WamType.INTEGER)
    OptionalLong individualMentionCount();

    @WamProperty(index = 59, type = WamType.BOOLEAN)
    Optional<Boolean> isBatched();

    @WamProperty(index = 36, type = WamType.BOOLEAN)
    Optional<Boolean> isCropped();

    @WamProperty(index = 55, type = WamType.BOOLEAN)
    Optional<Boolean> isDraft();

    @WamProperty(index = 51, type = WamType.BOOLEAN)
    Optional<Boolean> isForwardable();

    @WamProperty(index = 52, type = WamType.BOOLEAN)
    Optional<Boolean> isForwarded();

    @WamProperty(index = 41, type = WamType.BOOLEAN)
    Optional<Boolean> isFromLayouts();

    @WamProperty(index = 43, type = WamType.BOOLEAN)
    Optional<Boolean> isMediaAiImagineGenerated();

    @WamProperty(index = 20, type = WamType.BOOLEAN)
    Optional<Boolean> isPromptResponse();

    @WamProperty(index = 47, type = WamType.BOOLEAN)
    Optional<Boolean> isResharable();

    @WamProperty(index = 13, type = WamType.BOOLEAN)
    Optional<Boolean> isReshare();

    @WamProperty(index = 11, type = WamType.BOOLEAN)
    Optional<Boolean> isResultTerminal();

    @WamProperty(index = 37, type = WamType.BOOLEAN)
    Optional<Boolean> isRotated();

    @WamProperty(index = 40, type = WamType.BOOLEAN)
    Optional<Boolean> isSameSongFromAttribution();

    @WamProperty(index = 38, type = WamType.BOOLEAN)
    Optional<Boolean> isVideoManuallyTrimmed();

    @WamProperty(index = 27, type = WamType.BOOLEAN)
    Optional<Boolean> isVideoMuted();

    @WamProperty(index = 28, type = WamType.BOOLEAN)
    Optional<Boolean> isVideoTrimmed();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<MediaType> mediaType();

    @WamProperty(index = 21, type = WamType.STRING)
    Optional<String> messageKeyHash();

    @WamProperty(index = 12, type = WamType.TIMER)
    Optional<Instant> messageSendT();

    @WamProperty(index = 45, type = WamType.ENUM)
    Optional<PairedMediaType> pairedMediaType();

    @WamProperty(index = 7, type = WamType.ENUM)
    Optional<PrivacySettingsValueType> perPostStatusPrivacySetting();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong retryCount();

    @WamProperty(index = 18, type = WamType.STRING)
    Optional<String> shareType();

    @WamProperty(index = 56, type = WamType.INTEGER)
    OptionalLong statusAudienceSelected();

    @WamProperty(index = 8, type = WamType.BOOLEAN)
    Optional<Boolean> statusAudienceSelectorClicked();

    @WamProperty(index = 9, type = WamType.BOOLEAN)
    Optional<Boolean> statusAudienceSelectorUpdated();

    @WamProperty(index = 39, type = WamType.INTEGER)
    OptionalLong statusAudienceSize();

    @WamProperty(index = 53, type = WamType.ENUM)
    Optional<StatusCategory> statusCategory();

    @WamProperty(index = 14, type = WamType.BOOLEAN)
    Optional<Boolean> statusContainsMusic();

    @WamProperty(index = 29, type = WamType.STRING)
    Optional<String> statusId();

    @WamProperty(index = 10, type = WamType.INTEGER)
    OptionalLong statusMentionCount();

    @WamProperty(index = 46, type = WamType.ENUM)
    Optional<StatusPairedMediaQuality> statusPairedMediaQuality();

    @WamProperty(index = 60, type = WamType.STRING)
    Optional<String> statusPartCode();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<StatusPostOrigin> statusPostOrigin();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<StatusPostResult> statusPostResult();

    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong statusSessionId();

    @WamProperty(index = 30, type = WamType.ENUM)
    Optional<StatusType> statusType();

    @WamProperty(index = 31, type = WamType.INTEGER)
    OptionalLong stickersCount();

    @WamProperty(index = 32, type = WamType.BOOLEAN)
    Optional<Boolean> textStatusColorChanged();

    @WamProperty(index = 33, type = WamType.BOOLEAN)
    Optional<Boolean> textStatusFontChanged();

    @WamProperty(index = 34, type = WamType.INTEGER)
    OptionalLong textToolCount();

    @WamProperty(index = 35, type = WamType.STRING)
    Optional<String> unifiedSessionId();

    @WamProperty(index = 54, type = WamType.INTEGER)
    OptionalLong updatesTabSessionId();

    @WamProperty(index = 42, type = WamType.BOOLEAN)
    Optional<Boolean> urlHasAdditionalText();
}
