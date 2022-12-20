package it.auties.whatsapp.model.signal.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class WebFeatures
        implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = MESSAGE, implementation = WebFeatures.WebFeaturesFlag.class)
    private WebFeaturesFlag labelsDisplay;

    @ProtobufProperty(index = 2, type = MESSAGE, implementation = WebFeatures.WebFeaturesFlag.class)
    private WebFeaturesFlag voipIndividualOutgoing;

    @ProtobufProperty(index = 3, type = MESSAGE, implementation = WebFeatures.WebFeaturesFlag.class)
    private WebFeaturesFlag groupsV3;

    @ProtobufProperty(index = 4, type = MESSAGE, implementation = WebFeatures.WebFeaturesFlag.class)
    private WebFeaturesFlag groupsV3Create;

    @ProtobufProperty(index = 5, type = MESSAGE, implementation = WebFeatures.WebFeaturesFlag.class)
    private WebFeaturesFlag changeNumberV2;

    @ProtobufProperty(index = 6, type = MESSAGE, implementation = WebFeatures.WebFeaturesFlag.class)
    private WebFeaturesFlag queryStatusV3Thumbnail;

    @ProtobufProperty(index = 7, type = MESSAGE, implementation = WebFeatures.WebFeaturesFlag.class)
    private WebFeaturesFlag liveLocations;

    @ProtobufProperty(index = 8, type = MESSAGE, implementation = WebFeatures.WebFeaturesFlag.class)
    private WebFeaturesFlag queryVname;

    @ProtobufProperty(index = 9, type = MESSAGE, implementation = WebFeatures.WebFeaturesFlag.class)
    private WebFeaturesFlag voipIndividualIncoming;

    @ProtobufProperty(index = 10, type = MESSAGE, implementation = WebFeatures.WebFeaturesFlag.class)
    private WebFeaturesFlag quickRepliesQuery;

    @ProtobufProperty(index = 11, type = MESSAGE, implementation = WebFeatures.WebFeaturesFlag.class)
    private WebFeaturesFlag payments;

    @ProtobufProperty(index = 12, type = MESSAGE, implementation = WebFeatures.WebFeaturesFlag.class)
    private WebFeaturesFlag stickerPackQuery;

    @ProtobufProperty(index = 13, type = MESSAGE, implementation = WebFeatures.WebFeaturesFlag.class)
    private WebFeaturesFlag liveLocationsFinal;

    @ProtobufProperty(index = 14, type = MESSAGE, implementation = WebFeatures.WebFeaturesFlag.class)
    private WebFeaturesFlag labelsEdit;

    @ProtobufProperty(index = 15, type = MESSAGE, implementation = WebFeatures.WebFeaturesFlag.class)
    private WebFeaturesFlag mediaUpload;

    @ProtobufProperty(index = 18, type = MESSAGE, implementation = WebFeatures.WebFeaturesFlag.class)
    private WebFeaturesFlag mediaUploadRichQuickReplies;

    @ProtobufProperty(index = 19, type = MESSAGE, implementation = WebFeatures.WebFeaturesFlag.class)
    private WebFeaturesFlag vnameV2;

    @ProtobufProperty(index = 20, type = MESSAGE, implementation = WebFeatures.WebFeaturesFlag.class)
    private WebFeaturesFlag videoPlaybackUrl;

    @ProtobufProperty(index = 21, type = MESSAGE, implementation = WebFeatures.WebFeaturesFlag.class)
    private WebFeaturesFlag statusRanking;

    @ProtobufProperty(index = 22, type = MESSAGE, implementation = WebFeatures.WebFeaturesFlag.class)
    private WebFeaturesFlag voipIndividualVideo;

    @ProtobufProperty(index = 23, type = MESSAGE, implementation = WebFeatures.WebFeaturesFlag.class)
    private WebFeaturesFlag thirdPartyStickers;

    @ProtobufProperty(index = 24, type = MESSAGE, implementation = WebFeatures.WebFeaturesFlag.class)
    private WebFeaturesFlag frequentlyForwardedSetting;

    @ProtobufProperty(index = 25, type = MESSAGE, implementation = WebFeatures.WebFeaturesFlag.class)
    private WebFeaturesFlag groupsV4JoinPermission;

    @ProtobufProperty(index = 26, type = MESSAGE, implementation = WebFeatures.WebFeaturesFlag.class)
    private WebFeaturesFlag recentStickers;

    @ProtobufProperty(index = 27, type = MESSAGE, implementation = WebFeatures.WebFeaturesFlag.class)
    private WebFeaturesFlag catalog;

    @ProtobufProperty(index = 28, type = MESSAGE, implementation = WebFeatures.WebFeaturesFlag.class)
    private WebFeaturesFlag starredStickers;

    @ProtobufProperty(index = 29, type = MESSAGE, implementation = WebFeatures.WebFeaturesFlag.class)
    private WebFeaturesFlag voipGroupCall;

    @ProtobufProperty(index = 30, type = MESSAGE, implementation = WebFeatures.WebFeaturesFlag.class)
    private WebFeaturesFlag templateMessage;

    @ProtobufProperty(index = 31, type = MESSAGE, implementation = WebFeatures.WebFeaturesFlag.class)
    private WebFeaturesFlag templateMessageInteractivity;

    @ProtobufProperty(index = 32, type = MESSAGE, implementation = WebFeatures.WebFeaturesFlag.class)
    private WebFeaturesFlag ephemeralMessages;

    @ProtobufProperty(index = 33, type = MESSAGE, implementation = WebFeatures.WebFeaturesFlag.class)
    private WebFeaturesFlag e2ENotificationSync;

    @ProtobufProperty(index = 34, type = MESSAGE, implementation = WebFeatures.WebFeaturesFlag.class)
    private WebFeaturesFlag recentStickersV2;

    @ProtobufProperty(index = 36, type = MESSAGE, implementation = WebFeatures.WebFeaturesFlag.class)
    private WebFeaturesFlag recentStickersV3;

    @ProtobufProperty(index = 37, type = MESSAGE, implementation = WebFeatures.WebFeaturesFlag.class)
    private WebFeaturesFlag userNotice;

    @ProtobufProperty(index = 39, type = MESSAGE, implementation = WebFeatures.WebFeaturesFlag.class)
    private WebFeaturesFlag support;

    @ProtobufProperty(index = 40, type = MESSAGE, implementation = WebFeatures.WebFeaturesFlag.class)
    private WebFeaturesFlag groupUiiCleanup;

    @ProtobufProperty(index = 41, type = MESSAGE, implementation = WebFeatures.WebFeaturesFlag.class)
    private WebFeaturesFlag groupDogfoodingInternalOnly;

    @ProtobufProperty(index = 42, type = MESSAGE, implementation = WebFeatures.WebFeaturesFlag.class)
    private WebFeaturesFlag settingsSync;

    @ProtobufProperty(index = 43, type = MESSAGE, implementation = WebFeatures.WebFeaturesFlag.class)
    private WebFeaturesFlag archiveV2;

    @ProtobufProperty(index = 44, type = MESSAGE, implementation = WebFeatures.WebFeaturesFlag.class)
    private WebFeaturesFlag ephemeralAllowGroupMembers;

    @ProtobufProperty(index = 45, type = MESSAGE, implementation = WebFeatures.WebFeaturesFlag.class)
    private WebFeaturesFlag ephemeral24HDuration;

    @ProtobufProperty(index = 46, type = MESSAGE, implementation = WebFeatures.WebFeaturesFlag.class)
    private WebFeaturesFlag mdForceUpgrade;

    @ProtobufProperty(index = 47, type = MESSAGE, implementation = WebFeatures.WebFeaturesFlag.class)
    private WebFeaturesFlag disappearingMode;

    @ProtobufProperty(index = 48, type = MESSAGE, implementation = WebFeatures.WebFeaturesFlag.class)
    private WebFeaturesFlag externalMdOptInAvailable;

    @ProtobufProperty(index = 49, type = MESSAGE, implementation = WebFeatures.WebFeaturesFlag.class)
    private WebFeaturesFlag noDeleteMessageTimeLimit;

    @AllArgsConstructor
    @Accessors(fluent = true)
    @ProtobufName("Flag")
    public enum WebFeaturesFlag
            implements ProtobufMessage {

        NOT_STARTED(0),
        FORCE_UPGRADE(1),
        DEVELOPMENT(2),
        PRODUCTION(3);
        @Getter
        private final int index;

        @JsonCreator
        public static WebFeaturesFlag of(int index) {
            return Arrays.stream(values())
                    .filter(entry -> entry.index() == index)
                    .findFirst()
                    .orElse(null);
        }
    }
}