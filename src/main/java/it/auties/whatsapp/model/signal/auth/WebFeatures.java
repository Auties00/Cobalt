package it.auties.whatsapp.model.signal.auth;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufEnum;
import it.auties.protobuf.model.ProtobufMessage;

import static it.auties.protobuf.model.ProtobufType.OBJECT;

@ProtobufMessageName("WebFeatures")
public record WebFeatures(@ProtobufProperty(index = 1, type = OBJECT) WebFeaturesFlag labelsDisplay,
                          @ProtobufProperty(index = 2, type = OBJECT) WebFeaturesFlag voipIndividualOutgoing,
                          @ProtobufProperty(index = 3, type = OBJECT) WebFeaturesFlag groupsV3,
                          @ProtobufProperty(index = 4, type = OBJECT) WebFeaturesFlag groupsV3Create,
                          @ProtobufProperty(index = 5, type = OBJECT) WebFeaturesFlag changeNumberV2,
                          @ProtobufProperty(index = 6, type = OBJECT) WebFeaturesFlag queryStatusV3Thumbnail,
                          @ProtobufProperty(index = 7, type = OBJECT) WebFeaturesFlag liveLocations,
                          @ProtobufProperty(index = 8, type = OBJECT) WebFeaturesFlag queryVname,
                          @ProtobufProperty(index = 9, type = OBJECT) WebFeaturesFlag voipIndividualIncoming,
                          @ProtobufProperty(index = 10, type = OBJECT) WebFeaturesFlag quickRepliesQuery,
                          @ProtobufProperty(index = 11, type = OBJECT) WebFeaturesFlag payments,
                          @ProtobufProperty(index = 12, type = OBJECT) WebFeaturesFlag stickerPackQuery,
                          @ProtobufProperty(index = 13, type = OBJECT) WebFeaturesFlag liveLocationsFinal,
                          @ProtobufProperty(index = 14, type = OBJECT) WebFeaturesFlag labelsEdit,
                          @ProtobufProperty(index = 15, type = OBJECT) WebFeaturesFlag mediaUpload,
                          @ProtobufProperty(index = 18, type = OBJECT) WebFeaturesFlag mediaUploadRichQuickReplies,
                          @ProtobufProperty(index = 19, type = OBJECT) WebFeaturesFlag vnameV2,
                          @ProtobufProperty(index = 20, type = OBJECT) WebFeaturesFlag videoPlaybackUrl,
                          @ProtobufProperty(index = 21, type = OBJECT) WebFeaturesFlag statusRanking,
                          @ProtobufProperty(index = 22, type = OBJECT) WebFeaturesFlag voipIndividualVideo,
                          @ProtobufProperty(index = 23, type = OBJECT) WebFeaturesFlag thirdPartyStickers,
                          @ProtobufProperty(index = 24, type = OBJECT) WebFeaturesFlag frequentlyForwardedSetting,
                          @ProtobufProperty(index = 25, type = OBJECT) WebFeaturesFlag groupsV4JoinPermission,
                          @ProtobufProperty(index = 26, type = OBJECT) WebFeaturesFlag recentStickers,
                          @ProtobufProperty(index = 27, type = OBJECT) WebFeaturesFlag catalog,
                          @ProtobufProperty(index = 28, type = OBJECT) WebFeaturesFlag starredStickers,
                          @ProtobufProperty(index = 29, type = OBJECT) WebFeaturesFlag voipGroupCall,
                          @ProtobufProperty(index = 30, type = OBJECT) WebFeaturesFlag templateMessage,
                          @ProtobufProperty(index = 31, type = OBJECT) WebFeaturesFlag templateMessageInteractivity,
                          @ProtobufProperty(index = 32, type = OBJECT) WebFeaturesFlag ephemeralMessages,
                          @ProtobufProperty(index = 33, type = OBJECT) WebFeaturesFlag e2ENotificationSync,
                          @ProtobufProperty(index = 34, type = OBJECT) WebFeaturesFlag recentStickersV2,
                          @ProtobufProperty(index = 36, type = OBJECT) WebFeaturesFlag recentStickersV3,
                          @ProtobufProperty(index = 37, type = OBJECT) WebFeaturesFlag userNotice,
                          @ProtobufProperty(index = 39, type = OBJECT) WebFeaturesFlag support,
                          @ProtobufProperty(index = 40, type = OBJECT) WebFeaturesFlag groupUiiCleanup,
                          @ProtobufProperty(index = 41, type = OBJECT) WebFeaturesFlag groupDogfoodingInternalOnly,
                          @ProtobufProperty(index = 42, type = OBJECT) WebFeaturesFlag settingsSync,
                          @ProtobufProperty(index = 43, type = OBJECT) WebFeaturesFlag archiveV2,
                          @ProtobufProperty(index = 44, type = OBJECT) WebFeaturesFlag ephemeralAllowGroupMembers,
                          @ProtobufProperty(index = 45, type = OBJECT) WebFeaturesFlag ephemeral24HDuration,
                          @ProtobufProperty(index = 46, type = OBJECT) WebFeaturesFlag mdForceUpgrade,
                          @ProtobufProperty(index = 47, type = OBJECT) WebFeaturesFlag disappearingMode,
                          @ProtobufProperty(index = 48, type = OBJECT) WebFeaturesFlag externalMdOptInAvailable,
                          @ProtobufProperty(index = 49, type = OBJECT) WebFeaturesFlag noDeleteMessageTimeLimit) implements ProtobufMessage {

    public enum WebFeaturesFlag implements ProtobufEnum {

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
