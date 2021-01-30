package it.auties.whatsapp4j.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Data
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PropsResponse {
    @JsonProperty("webCatalogManagement")
    public boolean webCatalogManagement;
    @JsonProperty("catalogManagement")
    public boolean catalogManagement;
    @JsonProperty("webCatalogCart")
    public boolean webCatalogCart;
    @JsonProperty("webGroupV2Enabled")
    public int webGroupV2Enabled;
    @JsonProperty("stickerSearchEnabled")
    public boolean stickerSearchEnabled;
    @JsonProperty("webVoipWindowsOsMinVersion")
    public String webVoipWindowsOsMinVersion;
    @JsonProperty("webVoipMacOsMinVersion")
    public String webVoipMacOsMinVersion;
    @JsonProperty("webVoip")
    public boolean webVoip;
    @JsonProperty("webListMessage")
    public boolean webListMessage;
    @JsonProperty("ctwaContextCompose")
    public boolean ctwaContextCompose;
    @JsonProperty("aliteContactUsEnabled")
    public boolean aliteContactUsEnabled;
    @JsonProperty("pollingUpdates")
    public boolean pollingUpdates;
    @JsonProperty("camelotWeb")
    public boolean camelotWeb;
    @JsonProperty("preloadStickers")
    public boolean preloadStickers;
    @JsonProperty("webBizProfileOptions")
    public int webBizProfileOptions;
    @JsonProperty("webVoipInternalTester")
    public boolean webVoipInternalTester;
    @JsonProperty("webCleanIncomingFilename")
    public int webCleanIncomingFilename;
    @JsonProperty("webEnableModelStorage")
    public boolean webEnableModelStorage;
    @JsonProperty("wsCanCacheRequests")
    public boolean wsCanCacheRequests;
    @JsonProperty("fbCrashlog")
    public boolean fbCrashlog;
    @JsonProperty("bucket")
    public String bucket;
    @JsonProperty("gifSearch")
    public String gifSearch;
    @JsonProperty("maxFileSize")
    public int maxFileSize;
    @JsonProperty("media")
    public int media;
    @JsonProperty("maxSubject")
    public int maxSubject;
    @JsonProperty("maxParticipants")
    public int maxParticipants;
    @JsonProperty("imageMaxKBytes")
    public int imageMaxKBytes;
    @JsonProperty("imageMaxEdge")
    public int imageMaxEdge;
    @JsonProperty("statusVideoMaxDuration")
    public int statusVideoMaxDuration;
    @JsonProperty("frequentlyForwardedMessages")
    public int frequentlyForwardedMessages;
    @JsonProperty("suspiciousLinks")
    public int suspiciousLinks;
    @JsonProperty("fwdUiStartTs")
    public int fwdUiStartTs;
    @JsonProperty("sharechatInlinePlayerEnabled")
    public int sharechatInlinePlayerEnabled;
    @JsonProperty("ephemeralMessages")
    public int ephemeralMessages;
    @JsonProperty("newCommerceEntryPointEnabled")
    public int newCommerceEntryPointEnabled;
    @JsonProperty("ephemeralMessagesSetting")
    public int ephemeralMessagesSetting;
    @JsonProperty("catalogPdpNewDesign")
    public int catalogPdpNewDesign;
    @JsonProperty("mmsVcardAutodownloadSizeKb")
    public int mmsVcardAutodownloadSizeKb;
    @JsonProperty("restrictGroups")
    public int restrictGroups;
    @JsonProperty("userNotice")
    public int userNotice;
    @JsonProperty("productCatalogOpenDeeplink")
    public int productCatalogOpenDeeplink;
    @JsonProperty("multicastLimitGlobal")
    public int multicastLimitGlobal;
    @JsonProperty("wallpapersV2")
    public int wallpapersV2;
    @JsonProperty("vcardAsDocumentSizeKb")
    public int vcardAsDocumentSizeKb;
    @JsonProperty("ctwaContextRender")
    public int ctwaContextRender;
    @JsonProperty("quickMessageSearch")
    public int quickMessageSearch;
    @JsonProperty("frequentlyForwardedMax")
    public int frequentlyForwardedMax;
    @JsonProperty("vcardMaxSizeKb")
    public int vcardMaxSizeKb;
    @JsonProperty("hfmStringChanges")
    public int hfmStringChanges;
    @JsonProperty("stickers")
    public int stickers;
    @JsonProperty("muteAlways")
    public int muteAlways;
    @JsonProperty("announceGroups")
    public int announceGroups;
    @JsonProperty("catalogMessage")
    public int catalogMessage;
    @JsonProperty("groupDescLength")
    public int groupDescLength;
    @JsonProperty("productCatalogDeeplink")
    public int productCatalogDeeplink;
}