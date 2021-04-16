package it.auties.whatsapp4j.response.impl;

import it.auties.whatsapp4j.response.model.JsonResponseModel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Objects;

/**
 * A json model that contains information about the properties of a session
 */
@AllArgsConstructor
@Accessors(chain = true,fluent = true)
@Getter
@Setter
@EqualsAndHashCode
public final class PropsResponse implements JsonResponseModel<PropsResponse> {
    private final boolean webCatalogManagement;
    private final boolean catalogManagement;
    private final boolean webCatalogCart;
    private final int webGroupV2Enabled;
    private final boolean stickerSearchEnabled;
    private final String webVoipWindowsOsMinVersion;
    private final String webVoipMacOsMinVersion;
    private final boolean webVoip;
    private final boolean webListMessage;
    private final boolean ctwaContextCompose;
    private final boolean aliteContactUsEnabled;
    private final boolean pollingUpdates;
    private final boolean camelotWeb;
    private final boolean preloadStickers;
    private final int webBizProfileOptions;
    private final boolean webVoipInternalTester;
    private final int webCleanIncomingFilename;
    private final boolean webEnableModelStorage;
    private final boolean wsCanCacheRequests;
    private final boolean fbCrashlog;
    private final String bucket;
    private final String gifSearch;
    private final int maxFileSize;
    private final int media;
    private final int maxSubject;
    private final int maxParticipants;
    private final int imageMaxKBytes;
    private final int imageMaxEdge;
    private final int statusVideoMaxDuration;
    private final int frequentlyForwardedMessages;
    private final int suspiciousLinks;
    private final int fwdUiStartTs;
    private final int sharechatInlinePlayerEnabled;
    private final int ephemeralMessages;
    private final int newCommerceEntryPointEnabled;
    private final int ephemeralMessagesSetting;
    private final int catalogPdpNewDesign;
    private final int mmsVcardAutodownloadSizeKb;
    private final int restrictGroups;
    private final int userNotice;
    private final int productCatalogOpenDeeplink;
    private final int multicastLimitGlobal;
    private final int wallpapersV2;
    private final int vcardAsDocumentSizeKb;
    private final int ctwaContextRender;
    private final int quickMessageSearch;
    private final int frequentlyForwardedMax;
    private final int vcardMaxSizeKb;
    private final int hfmStringChanges;
    private final int stickers;
    private final int muteAlways;
    private final int announceGroups;
    private final int catalogMessage;
    private final int groupDescLength;
    private final int productCatalogDeeplink;

}