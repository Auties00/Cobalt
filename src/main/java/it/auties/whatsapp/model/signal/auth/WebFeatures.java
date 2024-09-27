package it.auties.whatsapp.model.signal.auth;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;

import static it.auties.protobuf.model.ProtobufType.ENUM;

@ProtobufMessage(name = "WebFeatures")
public record WebFeatures(@ProtobufProperty(index = 1, type = ENUM) WebFeaturesFlag labelsDisplay,
                          @ProtobufProperty(index = 2, type = ENUM) WebFeaturesFlag voipIndividualOutgoing,
                          @ProtobufProperty(index = 3, type = ENUM) WebFeaturesFlag groupsV3,
                          @ProtobufProperty(index = 4, type = ENUM) WebFeaturesFlag groupsV3Create,
                          @ProtobufProperty(index = 5, type = ENUM) WebFeaturesFlag changeNumberV2,
                          @ProtobufProperty(index = 6, type = ENUM) WebFeaturesFlag queryStatusV3Thumbnail,
                          @ProtobufProperty(index = 7, type = ENUM) WebFeaturesFlag liveLocations,
                          @ProtobufProperty(index = 8, type = ENUM) WebFeaturesFlag queryVname,
                          @ProtobufProperty(index = 9, type = ENUM) WebFeaturesFlag voipIndividualIncoming,
                          @ProtobufProperty(index = 10, type = ENUM) WebFeaturesFlag quickRepliesQuery,
                          @ProtobufProperty(index = 11, type = ENUM) WebFeaturesFlag payments,
                          @ProtobufProperty(index = 12, type = ENUM) WebFeaturesFlag stickerPackQuery,
                          @ProtobufProperty(index = 13, type = ENUM) WebFeaturesFlag liveLocationsFinal,
                          @ProtobufProperty(index = 14, type = ENUM) WebFeaturesFlag labelsEdit,
                          @ProtobufProperty(index = 15, type = ENUM) WebFeaturesFlag mediaUpload,
                          @ProtobufProperty(index = 18, type = ENUM) WebFeaturesFlag mediaUploadRichQuickReplies,
                          @ProtobufProperty(index = 19, type = ENUM) WebFeaturesFlag vnameV2,
                          @ProtobufProperty(index = 20, type = ENUM) WebFeaturesFlag videoPlaybackUrl,
                          @ProtobufProperty(index = 21, type = ENUM) WebFeaturesFlag statusRanking,
                          @ProtobufProperty(index = 22, type = ENUM) WebFeaturesFlag voipIndividualVideo,
                          @ProtobufProperty(index = 23, type = ENUM) WebFeaturesFlag thirdPartyStickers,
                          @ProtobufProperty(index = 24, type = ENUM) WebFeaturesFlag frequentlyForwardedSetting,
                          @ProtobufProperty(index = 25, type = ENUM) WebFeaturesFlag groupsV4JoinPermission,
                          @ProtobufProperty(index = 26, type = ENUM) WebFeaturesFlag recentStickers,
                          @ProtobufProperty(index = 27, type = ENUM) WebFeaturesFlag catalog,
                          @ProtobufProperty(index = 28, type = ENUM) WebFeaturesFlag starredStickers,
                          @ProtobufProperty(index = 29, type = ENUM) WebFeaturesFlag voipGroupCall,
                          @ProtobufProperty(index = 30, type = ENUM) WebFeaturesFlag templateMessage,
                          @ProtobufProperty(index = 31, type = ENUM) WebFeaturesFlag templateMessageInteractivity,
                          @ProtobufProperty(index = 32, type = ENUM) WebFeaturesFlag ephemeralMessages,
                          @ProtobufProperty(index = 33, type = ENUM) WebFeaturesFlag e2ENotificationSync,
                          @ProtobufProperty(index = 34, type = ENUM) WebFeaturesFlag recentStickersV2,
                          @ProtobufProperty(index = 36, type = ENUM) WebFeaturesFlag recentStickersV3,
                          @ProtobufProperty(index = 37, type = ENUM) WebFeaturesFlag userNotice,
                          @ProtobufProperty(index = 39, type = ENUM) WebFeaturesFlag support,
                          @ProtobufProperty(index = 40, type = ENUM) WebFeaturesFlag groupUiiCleanup,
                          @ProtobufProperty(index = 41, type = ENUM) WebFeaturesFlag groupDogfoodingInternalOnly,
                          @ProtobufProperty(index = 42, type = ENUM) WebFeaturesFlag settingsSync,
                          @ProtobufProperty(index = 43, type = ENUM) WebFeaturesFlag archiveV2,
                          @ProtobufProperty(index = 44, type = ENUM) WebFeaturesFlag ephemeralAllowGroupMembers,
                          @ProtobufProperty(index = 45, type = ENUM) WebFeaturesFlag ephemeral24HDuration,
                          @ProtobufProperty(index = 46, type = ENUM) WebFeaturesFlag mdForceUpgrade,
                          @ProtobufProperty(index = 47, type = ENUM) WebFeaturesFlag disappearingMode,
                          @ProtobufProperty(index = 48, type = ENUM) WebFeaturesFlag externalMdOptInAvailable,
                          @ProtobufProperty(index = 49, type = ENUM) WebFeaturesFlag noDeleteMessageTimeLimit) {

    @ProtobufEnum
    public enum WebFeaturesFlag {
        NOT_STARTED(0),
        FORCE_UPGRADE(1),
        DEVELOPMENT(2),
        PRODUCTION(3);

        WebFeaturesFlag(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        final int index;

        public int index() {
            return this.index;
        }
    }
}
