package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.MediaPickerOriginType;
import com.github.auties00.cobalt.wire.wam.type.MediaQuality;
import com.github.auties00.cobalt.wire.wam.type.MediaType;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebMediaPickerWamEvent")
@WamEvent(id = 1038)
public interface MediaPickerEvent extends WamEventSpec {
    @WamProperty(index = 24, type = WamType.BOOLEAN)
    Optional<Boolean> audienceSelectorClicked();

    @WamProperty(index = 25, type = WamType.BOOLEAN)
    Optional<Boolean> audienceSelectorUpdated();

    @WamProperty(index = 51, type = WamType.INTEGER)
    OptionalLong autoScaleCount();

    @WamProperty(index = 37, type = WamType.STRING)
    Optional<String> captionPositions();

    @WamProperty(index = 16, type = WamType.INTEGER)
    OptionalLong chatRecipients();

    @WamProperty(index = 38, type = WamType.BOOLEAN)
    Optional<Boolean> hasCollectionCaption();

    @WamProperty(index = 34, type = WamType.INTEGER)
    OptionalLong hdToggleChange();

    @WamProperty(index = 35, type = WamType.BOOLEAN)
    Optional<Boolean> hdToggleEligible();

    @WamProperty(index = 36, type = WamType.ENUM)
    Optional<MediaQuality> hdToggleState();

    @WamProperty(index = 54, type = WamType.BOOLEAN)
    Optional<Boolean> isFbCrosspostingEnabled();

    @WamProperty(index = 55, type = WamType.BOOLEAN)
    Optional<Boolean> isIgCrosspostingEnabled();

    @WamProperty(index = 41, type = WamType.BOOLEAN)
    Optional<Boolean> isSentInLandscape();

    @WamProperty(index = 22, type = WamType.BOOLEAN)
    Optional<Boolean> isViewOnce();

    @WamProperty(index = 39, type = WamType.INTEGER)
    OptionalLong itemCaptionCount();

    @WamProperty(index = 42, type = WamType.INTEGER)
    OptionalLong mediaPickerArBackground();

    @WamProperty(index = 43, type = WamType.INTEGER)
    OptionalLong mediaPickerArFilter();

    @WamProperty(index = 44, type = WamType.INTEGER)
    OptionalLong mediaPickerArFunEffect();

    @WamProperty(index = 33, type = WamType.INTEGER)
    OptionalLong mediaPickerAvatarStickers();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong mediaPickerChanged();

    @WamProperty(index = 10, type = WamType.INTEGER)
    OptionalLong mediaPickerCroppedRotated();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong mediaPickerDeleted();

    @WamProperty(index = 11, type = WamType.INTEGER)
    OptionalLong mediaPickerDrawing();

    @WamProperty(index = 18, type = WamType.INTEGER)
    OptionalLong mediaPickerFilter();

    @WamProperty(index = 26, type = WamType.BOOLEAN)
    Optional<Boolean> mediaPickerHasLocationSticker();

    @WamProperty(index = 47, type = WamType.INTEGER)
    OptionalLong mediaPickerIgluLowlight();

    @WamProperty(index = 48, type = WamType.INTEGER)
    OptionalLong mediaPickerIgluTouchup();

    @WamProperty(index = 19, type = WamType.INTEGER)
    OptionalLong mediaPickerLikeDoc();

    @WamProperty(index = 20, type = WamType.INTEGER)
    OptionalLong mediaPickerNotLikeDoc();

    @WamProperty(index = 14, type = WamType.ENUM)
    Optional<MediaPickerOriginType> mediaPickerOrigin();

    @WamProperty(index = 21, type = WamType.BOOLEAN)
    Optional<Boolean> mediaPickerOriginThirdParty();

    @WamProperty(index = 53, type = WamType.STRING)
    Optional<String> mediaPickerPosition();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong mediaPickerSent();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong mediaPickerSentUnchanged();

    @WamProperty(index = 29, type = WamType.STRING)
    Optional<String> mediaPickerSessionId();

    @WamProperty(index = 12, type = WamType.INTEGER)
    OptionalLong mediaPickerStickers();

    @WamProperty(index = 15, type = WamType.TIMER)
    Optional<Instant> mediaPickerT();

    @WamProperty(index = 13, type = WamType.INTEGER)
    OptionalLong mediaPickerText();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<MediaType> mediaType();

    @WamProperty(index = 31, type = WamType.INTEGER)
    OptionalLong motionPhotoImpressionCount();

    @WamProperty(index = 32, type = WamType.INTEGER)
    OptionalLong motionPhotoSentCount();

    @WamProperty(index = 45, type = WamType.INTEGER)
    OptionalLong numberOfArPostCapture();

    @WamProperty(index = 46, type = WamType.INTEGER)
    OptionalLong numberOfArPreCapture();

    @WamProperty(index = 49, type = WamType.INTEGER)
    OptionalLong numberOfIgluPostCapture();

    @WamProperty(index = 50, type = WamType.INTEGER)
    OptionalLong numberOfIgluPreCapture();

    @WamProperty(index = 23, type = WamType.TIMER)
    Optional<Instant> photoGalleryDurationT();

    @WamProperty(index = 27, type = WamType.ENUM)
    Optional<MediaQuality> photoQualitySetting();

    @WamProperty(index = 30, type = WamType.INTEGER)
    OptionalLong pickerSessionId();

    @WamProperty(index = 17, type = WamType.INTEGER)
    OptionalLong statusRecipients();

    @WamProperty(index = 52, type = WamType.INTEGER)
    OptionalLong transformCount();

    @WamProperty(index = 28, type = WamType.ENUM)
    Optional<MediaQuality> videoQualitySetting();
}
