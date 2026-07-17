package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.AndroidKeystoreStateType;
import com.github.auties00.cobalt.wire.wam.type.BackupEncryptionMethod;
import com.github.auties00.cobalt.wire.wam.type.BackupNetworkSetting;
import com.github.auties00.cobalt.wire.wam.type.BackupSchedule;
import com.github.auties00.cobalt.wire.wam.type.ContactsPermissionAuthorizationStatusType;
import com.github.auties00.cobalt.wire.wam.type.EmailState;
import com.github.auties00.cobalt.wire.wam.type.GalleryPermissionState;
import com.github.auties00.cobalt.wire.wam.type.MediaAutoDownloadQuality;
import com.github.auties00.cobalt.wire.wam.type.MediaQuality;
import com.github.auties00.cobalt.wire.wam.type.MetaAiModelTierType;
import com.github.auties00.cobalt.wire.wam.type.NotificationSettingType;
import com.github.auties00.cobalt.wire.wam.type.PeripheralDisplayConnectivityType;
import com.github.auties00.cobalt.wire.wam.type.PrivacySettingsContactsBuckets;
import com.github.auties00.cobalt.wire.wam.type.PrivacySettingsValueType;
import com.github.auties00.cobalt.wire.wam.type.UsernameState;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebDailyWamEvent")
@WamEvent(id = 1158)
public interface DailyEvent extends WamEventSpec {
    @WamProperty(index = 108, type = WamType.BOOLEAN)
    Optional<Boolean> accessibilityVoiceover();

    @WamProperty(index = 11, type = WamType.INTEGER)
    OptionalLong addressbookSize();

    @WamProperty(index = 12, type = WamType.INTEGER)
    OptionalLong addressbookWhatsappSize();

    @WamProperty(index = 223, type = WamType.INTEGER)
    OptionalLong aiMemorySavedCnt();

    @WamProperty(index = 210, type = WamType.BOOLEAN)
    Optional<Boolean> aiWidgetInstalled();

    @WamProperty(index = 135, type = WamType.STRING)
    Optional<String> androidAdvertisingId();

    @WamProperty(index = 139, type = WamType.ENUM)
    Optional<AndroidKeystoreStateType> androidKeystoreState();

    @WamProperty(index = 103, type = WamType.STRING)
    Optional<String> appCodeHash();

    @WamProperty(index = 212, type = WamType.INTEGER)
    OptionalLong appLastOpenTimestamp();

    @WamProperty(index = 121, type = WamType.INTEGER)
    OptionalLong appStandbyBucket();

    @WamProperty(index = 90, type = WamType.BOOLEAN)
    Optional<Boolean> autoDlAudioCellular();

    @WamProperty(index = 91, type = WamType.BOOLEAN)
    Optional<Boolean> autoDlAudioRoaming();

    @WamProperty(index = 89, type = WamType.BOOLEAN)
    Optional<Boolean> autoDlAudioWifi();

    @WamProperty(index = 96, type = WamType.BOOLEAN)
    Optional<Boolean> autoDlDocCellular();

    @WamProperty(index = 97, type = WamType.BOOLEAN)
    Optional<Boolean> autoDlDocRoaming();

    @WamProperty(index = 95, type = WamType.BOOLEAN)
    Optional<Boolean> autoDlDocWifi();

    @WamProperty(index = 87, type = WamType.BOOLEAN)
    Optional<Boolean> autoDlImageCellular();

    @WamProperty(index = 88, type = WamType.BOOLEAN)
    Optional<Boolean> autoDlImageRoaming();

    @WamProperty(index = 86, type = WamType.BOOLEAN)
    Optional<Boolean> autoDlImageWifi();

    @WamProperty(index = 93, type = WamType.BOOLEAN)
    Optional<Boolean> autoDlVideoCellular();

    @WamProperty(index = 94, type = WamType.BOOLEAN)
    Optional<Boolean> autoDlVideoRoaming();

    @WamProperty(index = 92, type = WamType.BOOLEAN)
    Optional<Boolean> autoDlVideoWifi();

    @WamProperty(index = 231, type = WamType.ENUM)
    Optional<BackupEncryptionMethod> backupEncryptionMethod();

    @WamProperty(index = 10, type = WamType.ENUM)
    Optional<BackupNetworkSetting> backupNetworkSetting();

    @WamProperty(index = 138, type = WamType.INTEGER)
    OptionalLong backupRestoreEncryptionVersion();

    @WamProperty(index = 9, type = WamType.ENUM)
    Optional<BackupSchedule> backupSchedule();

    @WamProperty(index = 241, type = WamType.INTEGER)
    OptionalLong canonicalEntLastValidationTsMs();

    @WamProperty(index = 186, type = WamType.INTEGER)
    OptionalLong channelsMediaFolderSize();

    @WamProperty(index = 19, type = WamType.INTEGER)
    OptionalLong chatDatabaseSize();

    @WamProperty(index = 168, type = WamType.INTEGER)
    OptionalLong chatLockFolderCount();

    @WamProperty(index = 200, type = WamType.INTEGER)
    OptionalLong contactsCreatedOnWhatsappSize();

    @WamProperty(index = 201, type = WamType.ENUM)
    Optional<ContactsPermissionAuthorizationStatusType> contactsPermissionAuthorizationStatus();

    @WamProperty(index = 238, type = WamType.INTEGER)
    OptionalLong contactsRequiringSyncBeforeDisplaySize();

    @WamProperty(index = 216, type = WamType.INTEGER)
    OptionalLong count1on1Threads();

    @WamProperty(index = 181, type = WamType.INTEGER)
    OptionalLong countEphemeralThreads();

    @WamProperty(index = 182, type = WamType.INTEGER)
    OptionalLong countEphemeralThreadsEnabledByMe();

    @WamProperty(index = 217, type = WamType.INTEGER)
    OptionalLong countGroupThreads();

    @WamProperty(index = 218, type = WamType.INTEGER)
    OptionalLong countLimitSharing1on1Threads();

    @WamProperty(index = 219, type = WamType.INTEGER)
    OptionalLong countLimitSharingGroupThreads();

    @WamProperty(index = 247, type = WamType.INTEGER)
    OptionalLong dailyPasskeyCount();

    @WamProperty(index = 206, type = WamType.INTEGER)
    OptionalLong dbAddressbookTableSize();

    @WamProperty(index = 248, type = WamType.INTEGER)
    OptionalLong defaultAfterReadDuration();

    @WamProperty(index = 249, type = WamType.BOOLEAN)
    Optional<Boolean> defaultAfterReadEnabled();

    @WamProperty(index = 140, type = WamType.INTEGER)
    OptionalLong defaultDisappearingDuration();

    @WamProperty(index = 214, type = WamType.INTEGER)
    OptionalLong defenseMode();

    @WamProperty(index = 228, type = WamType.INTEGER)
    OptionalLong deprecatedContactsSize();

    @WamProperty(index = 153, type = WamType.STRING)
    Optional<String> deviceLanguage();

    @WamProperty(index = 174, type = WamType.ENUM)
    Optional<EmailState> emailState();

    @WamProperty(index = 134, type = WamType.BOOLEAN)
    Optional<Boolean> entSecurityNotificationsEnabled();

    @WamProperty(index = 166, type = WamType.INTEGER)
    OptionalLong experimentTmoPreloadGroupDaily();

    @WamProperty(index = 113, type = WamType.INTEGER)
    OptionalLong favoritedAnimatedStickerCount();

    @WamProperty(index = 112, type = WamType.INTEGER)
    OptionalLong favoritedFirstPartyStickerCount();

    @WamProperty(index = 111, type = WamType.INTEGER)
    OptionalLong favoritedTotalStickerCount();

    @WamProperty(index = 164, type = WamType.ENUM)
    Optional<GalleryPermissionState> galleryPermission();

    @WamProperty(index = 175, type = WamType.BOOLEAN)
    Optional<Boolean> hasTextstatusEmojiModified24h();

    @WamProperty(index = 176, type = WamType.BOOLEAN)
    Optional<Boolean> hasTextstatusModified24h();

    @WamProperty(index = 177, type = WamType.BOOLEAN)
    Optional<Boolean> hasTextstatusTextModified24h();

    @WamProperty(index = 187, type = WamType.BOOLEAN)
    Optional<Boolean> hasUsername();

    @WamProperty(index = 222, type = WamType.BOOLEAN)
    Optional<Boolean> hasUsernamePin();

    @WamProperty(index = 190, type = WamType.INTEGER)
    OptionalLong inNetworkContactsSize();

    @WamProperty(index = 116, type = WamType.INTEGER)
    OptionalLong installedAnimatedStickerPackCount();

    @WamProperty(index = 137, type = WamType.INTEGER)
    OptionalLong installedAnimatedThirdPartyStickerPackCount();

    @WamProperty(index = 115, type = WamType.INTEGER)
    OptionalLong installedFirstPartyStickerPackCount();

    @WamProperty(index = 114, type = WamType.INTEGER)
    OptionalLong installedTotalStickerPackCount();

    @WamProperty(index = 234, type = WamType.BOOLEAN)
    Optional<Boolean> isCanonicalEntPresent();

    @WamProperty(index = 202, type = WamType.BOOLEAN)
    Optional<Boolean> isContactSyncToOsDefaultOn();

    @WamProperty(index = 183, type = WamType.BOOLEAN)
    Optional<Boolean> isDefaultDisappearingMessagingUser();

    @WamProperty(index = 184, type = WamType.BOOLEAN)
    Optional<Boolean> isEphemeralMessagingUser();

    @WamProperty(index = 195, type = WamType.BOOLEAN)
    Optional<Boolean> isProfilePhotoSet();

    @WamProperty(index = 154, type = WamType.STRING)
    Optional<String> keyboardLanguage();

    @WamProperty(index = 5, type = WamType.STRING)
    Optional<String> languageCode();

    @WamProperty(index = 63, type = WamType.INTEGER)
    OptionalLong lastBackupTimestamp();

    @WamProperty(index = 185, type = WamType.INTEGER)
    OptionalLong lastCloudBackupSize();

    @WamProperty(index = 253, type = WamType.INTEGER)
    OptionalLong lidToPnMappingCount();

    @WamProperty(index = 254, type = WamType.INTEGER)
    OptionalLong lidToUsernameMappingCount();

    @WamProperty(index = 6, type = WamType.STRING)
    Optional<String> locationCode();

    @WamProperty(index = 171, type = WamType.BOOLEAN)
    Optional<Boolean> lockFolderHidden();

    @WamProperty(index = 160, type = WamType.INTEGER)
    OptionalLong lowestAppStandbyBucket();

    @WamProperty(index = 124, type = WamType.INTEGER)
    OptionalLong mdPairTime();

    @WamProperty(index = 244, type = WamType.ENUM)
    Optional<MediaAutoDownloadQuality> mediaAutoDownloadQualitySetting();

    @WamProperty(index = 21, type = WamType.INTEGER)
    OptionalLong mediaFolderFileCount();

    @WamProperty(index = 20, type = WamType.INTEGER)
    OptionalLong mediaFolderSize();

    @WamProperty(index = 188, type = WamType.ENUM)
    Optional<MediaQuality> mediaQualitySetting();

    @WamProperty(index = 239, type = WamType.INTEGER)
    OptionalLong mediaSizeCacheAgeSeconds();

    @WamProperty(index = 198, type = WamType.ENUM)
    Optional<MetaAiModelTierType> metaAiModelActual();

    @WamProperty(index = 199, type = WamType.ENUM)
    Optional<MetaAiModelTierType> metaAiModelSelected();

    @WamProperty(index = 220, type = WamType.BOOLEAN)
    Optional<Boolean> metaAiVoiceDefaultOn();

    @WamProperty(index = 221, type = WamType.STRING)
    Optional<String> metaAiVoiceSelection();

    @WamProperty(index = 155, type = WamType.BOOLEAN)
    Optional<Boolean> modifiedInternalProps();

    @WamProperty(index = 205, type = WamType.BOOLEAN)
    Optional<Boolean> nativeContactsGlobalSettingEnabled();

    @WamProperty(index = 7, type = WamType.BOOLEAN)
    Optional<Boolean> networkIsRoaming();

    @WamProperty(index = 159, type = WamType.STRING)
    Optional<String> networkOperatorName();

    @WamProperty(index = 163, type = WamType.INTEGER)
    OptionalLong numAccounts();

    @WamProperty(index = 178, type = WamType.INTEGER)
    OptionalLong numContactsWithTextstatus24h();

    @WamProperty(index = 179, type = WamType.INTEGER)
    OptionalLong numContactsWithTextstatusEmoji24h();

    @WamProperty(index = 180, type = WamType.INTEGER)
    OptionalLong numContactsWithTextstatusText24h();

    @WamProperty(index = 250, type = WamType.INTEGER)
    OptionalLong numberOfSim();

    @WamProperty(index = 4, type = WamType.STRING)
    Optional<String> osBuildNumber();

    @WamProperty(index = 118, type = WamType.ENUM)
    Optional<NotificationSettingType> osNotificationSetting();

    @WamProperty(index = 191, type = WamType.INTEGER)
    OptionalLong outOfNetworkContactsSize();

    @WamProperty(index = 102, type = WamType.STRING)
    Optional<String> packageName();

    @WamProperty(index = 165, type = WamType.BOOLEAN)
    Optional<Boolean> passkeyExists();

    @WamProperty(index = 100, type = WamType.BOOLEAN)
    Optional<Boolean> paymentsIsEnabled();

    @WamProperty(index = 209, type = WamType.STRING)
    Optional<String> peripheralConnected();

    @WamProperty(index = 255, type = WamType.ENUM)
    Optional<PeripheralDisplayConnectivityType> peripheralDisplayConnectivityType();

    @WamProperty(index = 229, type = WamType.STRING)
    Optional<String> peripheralLinkedProductLine();

    @WamProperty(index = 57, type = WamType.INTEGER)
    OptionalLong permissionAccessCoarseLocation();

    @WamProperty(index = 58, type = WamType.INTEGER)
    OptionalLong permissionAccessFineLocation();

    @WamProperty(index = 56, type = WamType.INTEGER)
    OptionalLong permissionCamera();

    @WamProperty(index = 104, type = WamType.BOOLEAN)
    Optional<Boolean> permissionContacts();

    @WamProperty(index = 53, type = WamType.INTEGER)
    OptionalLong permissionReadExternalStorage();

    @WamProperty(index = 55, type = WamType.INTEGER)
    OptionalLong permissionRecordAudio();

    @WamProperty(index = 156, type = WamType.INTEGER)
    OptionalLong phoneCores();

    @WamProperty(index = 251, type = WamType.INTEGER)
    OptionalLong phoneNumberHintAvailableCount();

    @WamProperty(index = 162, type = WamType.STRING)
    Optional<String> phoneyid();

    @WamProperty(index = 224, type = WamType.STRING)
    Optional<String> preloadsAppManagerId();

    @WamProperty(index = 227, type = WamType.STRING)
    Optional<String> preloadsAttributionJson();

    @WamProperty(index = 141, type = WamType.ENUM)
    Optional<PrivacySettingsValueType> privacySettingsAbout();

    @WamProperty(index = 142, type = WamType.ENUM)
    Optional<PrivacySettingsContactsBuckets> privacySettingsAboutExceptNum();

    @WamProperty(index = 235, type = WamType.ENUM)
    Optional<PrivacySettingsValueType> privacySettingsCoverPhoto();

    @WamProperty(index = 143, type = WamType.ENUM)
    Optional<PrivacySettingsValueType> privacySettingsGroups();

    @WamProperty(index = 144, type = WamType.ENUM)
    Optional<PrivacySettingsContactsBuckets> privacySettingsGroupsExceptNum();

    @WamProperty(index = 145, type = WamType.ENUM)
    Optional<PrivacySettingsValueType> privacySettingsLastSeen();

    @WamProperty(index = 146, type = WamType.ENUM)
    Optional<PrivacySettingsContactsBuckets> privacySettingsLastSeenExceptNum();

    @WamProperty(index = 225, type = WamType.ENUM)
    Optional<PrivacySettingsValueType> privacySettingsProfileLinks();

    @WamProperty(index = 226, type = WamType.ENUM)
    Optional<PrivacySettingsContactsBuckets> privacySettingsProfileLinksExceptNum();

    @WamProperty(index = 147, type = WamType.ENUM)
    Optional<PrivacySettingsValueType> privacySettingsProfilePhoto();

    @WamProperty(index = 148, type = WamType.ENUM)
    Optional<PrivacySettingsContactsBuckets> privacySettingsProfilePhotoExceptNum();

    @WamProperty(index = 150, type = WamType.ENUM)
    Optional<PrivacySettingsValueType> privacySettingsStatus();

    @WamProperty(index = 151, type = WamType.ENUM)
    Optional<PrivacySettingsContactsBuckets> privacySettingsStatusExceptNum();

    @WamProperty(index = 152, type = WamType.ENUM)
    Optional<PrivacySettingsContactsBuckets> privacySettingsStatusShareNum();

    @WamProperty(index = 211, type = WamType.INTEGER)
    OptionalLong profileLinksCount();

    @WamProperty(index = 8, type = WamType.BOOLEAN)
    Optional<Boolean> receiptsEnabled();

    @WamProperty(index = 172, type = WamType.BOOLEAN)
    Optional<Boolean> secretCodeActive();

    @WamProperty(index = 173, type = WamType.BOOLEAN)
    Optional<Boolean> showMetaAiButtonSetting();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong simMcc();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong simMnc();

    @WamProperty(index = 243, type = WamType.INTEGER)
    OptionalLong simPhoneNumberMatched();

    @WamProperty(index = 256, type = WamType.INTEGER)
    OptionalLong stickersFolderFileCount();

    @WamProperty(index = 257, type = WamType.INTEGER)
    OptionalLong stickersFolderSize();

    @WamProperty(index = 31, type = WamType.INTEGER)
    OptionalLong storageAvailSize();

    @WamProperty(index = 136, type = WamType.INTEGER)
    OptionalLong storageAvailSizeWithCache();

    @WamProperty(index = 32, type = WamType.INTEGER)
    OptionalLong storageTotalSize();

    @WamProperty(index = 169, type = WamType.STRING)
    Optional<String> supportedDecoders();

    @WamProperty(index = 170, type = WamType.STRING)
    Optional<String> supportedEncoders();

    @WamProperty(index = 192, type = WamType.INTEGER)
    OptionalLong syncedInNetworkContactsSize();

    @WamProperty(index = 193, type = WamType.INTEGER)
    OptionalLong totalCountInNetworkUniquePhoneNumber();

    @WamProperty(index = 194, type = WamType.INTEGER)
    OptionalLong totalCountSyncedInNetworkUniquePhoneNumber();

    @WamProperty(index = 232, type = WamType.INTEGER)
    OptionalLong totalMissedCalls();

    @WamProperty(index = 233, type = WamType.INTEGER)
    OptionalLong totalUnreadMessages();

    @WamProperty(index = 240, type = WamType.INTEGER)
    OptionalLong traceIdInt();

    @WamProperty(index = 215, type = WamType.INTEGER)
    OptionalLong trafficAnonymization();

    @WamProperty(index = 236, type = WamType.INTEGER)
    OptionalLong uniquePhoneNumberContactsSizeWithUsername();

    @WamProperty(index = 213, type = WamType.INTEGER)
    OptionalLong unreadBadgeFrequency();

    @WamProperty(index = 245, type = WamType.INTEGER)
    OptionalLong unverifiedProfileLinksCount();

    @WamProperty(index = 237, type = WamType.INTEGER)
    OptionalLong usernameOnlyContactsSize();

    @WamProperty(index = 242, type = WamType.ENUM)
    Optional<UsernameState> usernameState();

    @WamProperty(index = 246, type = WamType.INTEGER)
    OptionalLong verifiedProfileLinksCount();

    @WamProperty(index = 23, type = WamType.INTEGER)
    OptionalLong videoFolderFileCount();

    @WamProperty(index = 22, type = WamType.INTEGER)
    OptionalLong videoFolderSize();

    @WamProperty(index = 207, type = WamType.INTEGER)
    OptionalLong webcContactsTableSize();

    @WamProperty(index = 208, type = WamType.INTEGER)
    OptionalLong webcFilteredContactsSize();
}
