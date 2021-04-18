package it.auties.whatsapp4j.response.impl;

import it.auties.whatsapp4j.response.model.JsonResponseModel;

import java.util.Objects;

/**
 * A json model that contains information about the properties of a session
 */
public final class PropsResponse implements JsonResponseModel {
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

    /**
     */
    public PropsResponse(boolean webCatalogManagement, boolean catalogManagement, boolean webCatalogCart,
                         int webGroupV2Enabled, boolean stickerSearchEnabled,
                         String webVoipWindowsOsMinVersion, String webVoipMacOsMinVersion,
                         boolean webVoip, boolean webListMessage, boolean ctwaContextCompose,
                         boolean aliteContactUsEnabled, boolean pollingUpdates, boolean camelotWeb,
                         boolean preloadStickers, int webBizProfileOptions, boolean webVoipInternalTester,
                         int webCleanIncomingFilename, boolean webEnableModelStorage, boolean wsCanCacheRequests,
                         boolean fbCrashlog, String bucket, String gifSearch, int maxFileSize,
                         int media, int maxSubject, int maxParticipants, int imageMaxKBytes, int imageMaxEdge,
                         int statusVideoMaxDuration, int frequentlyForwardedMessages, int suspiciousLinks,
                         int fwdUiStartTs, int sharechatInlinePlayerEnabled, int ephemeralMessages,
                         int newCommerceEntryPointEnabled, int ephemeralMessagesSetting, int catalogPdpNewDesign,
                         int mmsVcardAutodownloadSizeKb, int restrictGroups, int userNotice,
                         int productCatalogOpenDeeplink, int multicastLimitGlobal, int wallpapersV2,
                         int vcardAsDocumentSizeKb, int ctwaContextRender, int quickMessageSearch,
                         int frequentlyForwardedMax, int vcardMaxSizeKb, int hfmStringChanges, int stickers,
                         int muteAlways, int announceGroups, int catalogMessage, int groupDescLength,
                         int productCatalogDeeplink) {
        this.webCatalogManagement = webCatalogManagement;
        this.catalogManagement = catalogManagement;
        this.webCatalogCart = webCatalogCart;
        this.webGroupV2Enabled = webGroupV2Enabled;
        this.stickerSearchEnabled = stickerSearchEnabled;
        this.webVoipWindowsOsMinVersion = webVoipWindowsOsMinVersion;
        this.webVoipMacOsMinVersion = webVoipMacOsMinVersion;
        this.webVoip = webVoip;
        this.webListMessage = webListMessage;
        this.ctwaContextCompose = ctwaContextCompose;
        this.aliteContactUsEnabled = aliteContactUsEnabled;
        this.pollingUpdates = pollingUpdates;
        this.camelotWeb = camelotWeb;
        this.preloadStickers = preloadStickers;
        this.webBizProfileOptions = webBizProfileOptions;
        this.webVoipInternalTester = webVoipInternalTester;
        this.webCleanIncomingFilename = webCleanIncomingFilename;
        this.webEnableModelStorage = webEnableModelStorage;
        this.wsCanCacheRequests = wsCanCacheRequests;
        this.fbCrashlog = fbCrashlog;
        this.bucket = bucket;
        this.gifSearch = gifSearch;
        this.maxFileSize = maxFileSize;
        this.media = media;
        this.maxSubject = maxSubject;
        this.maxParticipants = maxParticipants;
        this.imageMaxKBytes = imageMaxKBytes;
        this.imageMaxEdge = imageMaxEdge;
        this.statusVideoMaxDuration = statusVideoMaxDuration;
        this.frequentlyForwardedMessages = frequentlyForwardedMessages;
        this.suspiciousLinks = suspiciousLinks;
        this.fwdUiStartTs = fwdUiStartTs;
        this.sharechatInlinePlayerEnabled = sharechatInlinePlayerEnabled;
        this.ephemeralMessages = ephemeralMessages;
        this.newCommerceEntryPointEnabled = newCommerceEntryPointEnabled;
        this.ephemeralMessagesSetting = ephemeralMessagesSetting;
        this.catalogPdpNewDesign = catalogPdpNewDesign;
        this.mmsVcardAutodownloadSizeKb = mmsVcardAutodownloadSizeKb;
        this.restrictGroups = restrictGroups;
        this.userNotice = userNotice;
        this.productCatalogOpenDeeplink = productCatalogOpenDeeplink;
        this.multicastLimitGlobal = multicastLimitGlobal;
        this.wallpapersV2 = wallpapersV2;
        this.vcardAsDocumentSizeKb = vcardAsDocumentSizeKb;
        this.ctwaContextRender = ctwaContextRender;
        this.quickMessageSearch = quickMessageSearch;
        this.frequentlyForwardedMax = frequentlyForwardedMax;
        this.vcardMaxSizeKb = vcardMaxSizeKb;
        this.hfmStringChanges = hfmStringChanges;
        this.stickers = stickers;
        this.muteAlways = muteAlways;
        this.announceGroups = announceGroups;
        this.catalogMessage = catalogMessage;
        this.groupDescLength = groupDescLength;
        this.productCatalogDeeplink = productCatalogDeeplink;
    }

    public boolean webCatalogManagement() {
        return webCatalogManagement;
    }

    public boolean catalogManagement() {
        return catalogManagement;
    }

    public boolean webCatalogCart() {
        return webCatalogCart;
    }

    public int webGroupV2Enabled() {
        return webGroupV2Enabled;
    }

    public boolean stickerSearchEnabled() {
        return stickerSearchEnabled;
    }

    public String webVoipWindowsOsMinVersion() {
        return webVoipWindowsOsMinVersion;
    }

    public String webVoipMacOsMinVersion() {
        return webVoipMacOsMinVersion;
    }

    public boolean webVoip() {
        return webVoip;
    }

    public boolean webListMessage() {
        return webListMessage;
    }

    public boolean ctwaContextCompose() {
        return ctwaContextCompose;
    }

    public boolean aliteContactUsEnabled() {
        return aliteContactUsEnabled;
    }

    public boolean pollingUpdates() {
        return pollingUpdates;
    }

    public boolean camelotWeb() {
        return camelotWeb;
    }

    public boolean preloadStickers() {
        return preloadStickers;
    }

    public int webBizProfileOptions() {
        return webBizProfileOptions;
    }

    public boolean webVoipInternalTester() {
        return webVoipInternalTester;
    }

    public int webCleanIncomingFilename() {
        return webCleanIncomingFilename;
    }

    public boolean webEnableModelStorage() {
        return webEnableModelStorage;
    }

    public boolean wsCanCacheRequests() {
        return wsCanCacheRequests;
    }

    public boolean fbCrashlog() {
        return fbCrashlog;
    }

    public String bucket() {
        return bucket;
    }

    public String gifSearch() {
        return gifSearch;
    }

    public int maxFileSize() {
        return maxFileSize;
    }

    public int media() {
        return media;
    }

    public int maxSubject() {
        return maxSubject;
    }

    public int maxParticipants() {
        return maxParticipants;
    }

    public int imageMaxKBytes() {
        return imageMaxKBytes;
    }

    public int imageMaxEdge() {
        return imageMaxEdge;
    }

    public int statusVideoMaxDuration() {
        return statusVideoMaxDuration;
    }

    public int frequentlyForwardedMessages() {
        return frequentlyForwardedMessages;
    }

    public int suspiciousLinks() {
        return suspiciousLinks;
    }

    public int fwdUiStartTs() {
        return fwdUiStartTs;
    }

    public int sharechatInlinePlayerEnabled() {
        return sharechatInlinePlayerEnabled;
    }

    public int ephemeralMessages() {
        return ephemeralMessages;
    }

    public int newCommerceEntryPointEnabled() {
        return newCommerceEntryPointEnabled;
    }

    public int ephemeralMessagesSetting() {
        return ephemeralMessagesSetting;
    }

    public int catalogPdpNewDesign() {
        return catalogPdpNewDesign;
    }

    public int mmsVcardAutodownloadSizeKb() {
        return mmsVcardAutodownloadSizeKb;
    }

    public int restrictGroups() {
        return restrictGroups;
    }

    public int userNotice() {
        return userNotice;
    }

    public int productCatalogOpenDeeplink() {
        return productCatalogOpenDeeplink;
    }

    public int multicastLimitGlobal() {
        return multicastLimitGlobal;
    }

    public int wallpapersV2() {
        return wallpapersV2;
    }

    public int vcardAsDocumentSizeKb() {
        return vcardAsDocumentSizeKb;
    }

    public int ctwaContextRender() {
        return ctwaContextRender;
    }

    public int quickMessageSearch() {
        return quickMessageSearch;
    }

    public int frequentlyForwardedMax() {
        return frequentlyForwardedMax;
    }

    public int vcardMaxSizeKb() {
        return vcardMaxSizeKb;
    }

    public int hfmStringChanges() {
        return hfmStringChanges;
    }

    public int stickers() {
        return stickers;
    }

    public int muteAlways() {
        return muteAlways;
    }

    public int announceGroups() {
        return announceGroups;
    }

    public int catalogMessage() {
        return catalogMessage;
    }

    public int groupDescLength() {
        return groupDescLength;
    }

    public int productCatalogDeeplink() {
        return productCatalogDeeplink;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (PropsResponse) obj;
        return this.webCatalogManagement == that.webCatalogManagement &&
                this.catalogManagement == that.catalogManagement &&
                this.webCatalogCart == that.webCatalogCart &&
                this.webGroupV2Enabled == that.webGroupV2Enabled &&
                this.stickerSearchEnabled == that.stickerSearchEnabled &&
                Objects.equals(this.webVoipWindowsOsMinVersion, that.webVoipWindowsOsMinVersion) &&
                Objects.equals(this.webVoipMacOsMinVersion, that.webVoipMacOsMinVersion) &&
                this.webVoip == that.webVoip &&
                this.webListMessage == that.webListMessage &&
                this.ctwaContextCompose == that.ctwaContextCompose &&
                this.aliteContactUsEnabled == that.aliteContactUsEnabled &&
                this.pollingUpdates == that.pollingUpdates &&
                this.camelotWeb == that.camelotWeb &&
                this.preloadStickers == that.preloadStickers &&
                this.webBizProfileOptions == that.webBizProfileOptions &&
                this.webVoipInternalTester == that.webVoipInternalTester &&
                this.webCleanIncomingFilename == that.webCleanIncomingFilename &&
                this.webEnableModelStorage == that.webEnableModelStorage &&
                this.wsCanCacheRequests == that.wsCanCacheRequests &&
                this.fbCrashlog == that.fbCrashlog &&
                Objects.equals(this.bucket, that.bucket) &&
                Objects.equals(this.gifSearch, that.gifSearch) &&
                this.maxFileSize == that.maxFileSize &&
                this.media == that.media &&
                this.maxSubject == that.maxSubject &&
                this.maxParticipants == that.maxParticipants &&
                this.imageMaxKBytes == that.imageMaxKBytes &&
                this.imageMaxEdge == that.imageMaxEdge &&
                this.statusVideoMaxDuration == that.statusVideoMaxDuration &&
                this.frequentlyForwardedMessages == that.frequentlyForwardedMessages &&
                this.suspiciousLinks == that.suspiciousLinks &&
                this.fwdUiStartTs == that.fwdUiStartTs &&
                this.sharechatInlinePlayerEnabled == that.sharechatInlinePlayerEnabled &&
                this.ephemeralMessages == that.ephemeralMessages &&
                this.newCommerceEntryPointEnabled == that.newCommerceEntryPointEnabled &&
                this.ephemeralMessagesSetting == that.ephemeralMessagesSetting &&
                this.catalogPdpNewDesign == that.catalogPdpNewDesign &&
                this.mmsVcardAutodownloadSizeKb == that.mmsVcardAutodownloadSizeKb &&
                this.restrictGroups == that.restrictGroups &&
                this.userNotice == that.userNotice &&
                this.productCatalogOpenDeeplink == that.productCatalogOpenDeeplink &&
                this.multicastLimitGlobal == that.multicastLimitGlobal &&
                this.wallpapersV2 == that.wallpapersV2 &&
                this.vcardAsDocumentSizeKb == that.vcardAsDocumentSizeKb &&
                this.ctwaContextRender == that.ctwaContextRender &&
                this.quickMessageSearch == that.quickMessageSearch &&
                this.frequentlyForwardedMax == that.frequentlyForwardedMax &&
                this.vcardMaxSizeKb == that.vcardMaxSizeKb &&
                this.hfmStringChanges == that.hfmStringChanges &&
                this.stickers == that.stickers &&
                this.muteAlways == that.muteAlways &&
                this.announceGroups == that.announceGroups &&
                this.catalogMessage == that.catalogMessage &&
                this.groupDescLength == that.groupDescLength &&
                this.productCatalogDeeplink == that.productCatalogDeeplink;
    }

    @Override
    public int hashCode() {
        return Objects.hash(webCatalogManagement, catalogManagement, webCatalogCart, webGroupV2Enabled, stickerSearchEnabled, webVoipWindowsOsMinVersion, webVoipMacOsMinVersion, webVoip, webListMessage, ctwaContextCompose, aliteContactUsEnabled, pollingUpdates, camelotWeb, preloadStickers, webBizProfileOptions, webVoipInternalTester, webCleanIncomingFilename, webEnableModelStorage, wsCanCacheRequests, fbCrashlog, bucket, gifSearch, maxFileSize, media, maxSubject, maxParticipants, imageMaxKBytes, imageMaxEdge, statusVideoMaxDuration, frequentlyForwardedMessages, suspiciousLinks, fwdUiStartTs, sharechatInlinePlayerEnabled, ephemeralMessages, newCommerceEntryPointEnabled, ephemeralMessagesSetting, catalogPdpNewDesign, mmsVcardAutodownloadSizeKb, restrictGroups, userNotice, productCatalogOpenDeeplink, multicastLimitGlobal, wallpapersV2, vcardAsDocumentSizeKb, ctwaContextRender, quickMessageSearch, frequentlyForwardedMax, vcardMaxSizeKb, hfmStringChanges, stickers, muteAlways, announceGroups, catalogMessage, groupDescLength, productCatalogDeeplink);
    }

    @Override
    public String toString() {
        return "PropsResponse[" +
                "webCatalogManagement=" + webCatalogManagement + ", " +
                "catalogManagement=" + catalogManagement + ", " +
                "webCatalogCart=" + webCatalogCart + ", " +
                "webGroupV2Enabled=" + webGroupV2Enabled + ", " +
                "stickerSearchEnabled=" + stickerSearchEnabled + ", " +
                "webVoipWindowsOsMinVersion=" + webVoipWindowsOsMinVersion + ", " +
                "webVoipMacOsMinVersion=" + webVoipMacOsMinVersion + ", " +
                "webVoip=" + webVoip + ", " +
                "webListMessage=" + webListMessage + ", " +
                "ctwaContextCompose=" + ctwaContextCompose + ", " +
                "aliteContactUsEnabled=" + aliteContactUsEnabled + ", " +
                "pollingUpdates=" + pollingUpdates + ", " +
                "camelotWeb=" + camelotWeb + ", " +
                "preloadStickers=" + preloadStickers + ", " +
                "webBizProfileOptions=" + webBizProfileOptions + ", " +
                "webVoipInternalTester=" + webVoipInternalTester + ", " +
                "webCleanIncomingFilename=" + webCleanIncomingFilename + ", " +
                "webEnableModelStorage=" + webEnableModelStorage + ", " +
                "wsCanCacheRequests=" + wsCanCacheRequests + ", " +
                "fbCrashlog=" + fbCrashlog + ", " +
                "bucket=" + bucket + ", " +
                "gifSearch=" + gifSearch + ", " +
                "maxFileSize=" + maxFileSize + ", " +
                "media=" + media + ", " +
                "maxSubject=" + maxSubject + ", " +
                "maxParticipants=" + maxParticipants + ", " +
                "imageMaxKBytes=" + imageMaxKBytes + ", " +
                "imageMaxEdge=" + imageMaxEdge + ", " +
                "statusVideoMaxDuration=" + statusVideoMaxDuration + ", " +
                "frequentlyForwardedMessages=" + frequentlyForwardedMessages + ", " +
                "suspiciousLinks=" + suspiciousLinks + ", " +
                "fwdUiStartTs=" + fwdUiStartTs + ", " +
                "sharechatInlinePlayerEnabled=" + sharechatInlinePlayerEnabled + ", " +
                "ephemeralMessages=" + ephemeralMessages + ", " +
                "newCommerceEntryPointEnabled=" + newCommerceEntryPointEnabled + ", " +
                "ephemeralMessagesSetting=" + ephemeralMessagesSetting + ", " +
                "catalogPdpNewDesign=" + catalogPdpNewDesign + ", " +
                "mmsVcardAutodownloadSizeKb=" + mmsVcardAutodownloadSizeKb + ", " +
                "restrictGroups=" + restrictGroups + ", " +
                "userNotice=" + userNotice + ", " +
                "productCatalogOpenDeeplink=" + productCatalogOpenDeeplink + ", " +
                "multicastLimitGlobal=" + multicastLimitGlobal + ", " +
                "wallpapersV2=" + wallpapersV2 + ", " +
                "vcardAsDocumentSizeKb=" + vcardAsDocumentSizeKb + ", " +
                "ctwaContextRender=" + ctwaContextRender + ", " +
                "quickMessageSearch=" + quickMessageSearch + ", " +
                "frequentlyForwardedMax=" + frequentlyForwardedMax + ", " +
                "vcardMaxSizeKb=" + vcardMaxSizeKb + ", " +
                "hfmStringChanges=" + hfmStringChanges + ", " +
                "stickers=" + stickers + ", " +
                "muteAlways=" + muteAlways + ", " +
                "announceGroups=" + announceGroups + ", " +
                "catalogMessage=" + catalogMessage + ", " +
                "groupDescLength=" + groupDescLength + ", " +
                "productCatalogDeeplink=" + productCatalogDeeplink + ']';
    }

}