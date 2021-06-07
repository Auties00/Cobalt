package it.auties.whatsapp4j.response.impl.json;

import it.auties.whatsapp4j.response.model.json.JsonResponseModel;

/**
 * A json model that contains information about the properties of a session
 */
public record PropsResponse(boolean webCatalogManagement, boolean catalogManagement, boolean webCatalogCart,
                                  int webGroupV2Enabled, boolean stickerSearchEnabled, String webVoipWindowsOsMinVersion,
                                  String webVoipMacOsMinVersion, boolean webVoip, boolean webListMessage,
                                  boolean ctwaContextCompose, boolean aliteContactUsEnabled, boolean pollingUpdates,
                                  boolean camelotWeb, boolean preloadStickers, int webBizProfileOptions,
                                  boolean webVoipInternalTester, int webCleanIncomingFilename, boolean webEnableModelStorage,
                                  boolean wsCanCacheRequests, boolean fbCrashlog, String bucket, String gifSearch,
                                  int maxFileSize, int media, int maxSubject, int maxParticipants, int imageMaxKBytes,
                                  int imageMaxEdge, int statusVideoMaxDuration, int frequentlyForwardedMessages,
                                  int suspiciousLinks, int fwdUiStartTs, int sharechatInlinePlayerEnabled,
                                  int ephemeralMessages, int newCommerceEntryPointEnabled, int ephemeralMessagesSetting,
                                  int catalogPdpNewDesign, int mmsVcardAutodownloadSizeKb, int restrictGroups, int userNotice,
                                  int productCatalogOpenDeeplink, int multicastLimitGlobal, int wallpapersV2,
                                  int vcardAsDocumentSizeKb, int ctwaContextRender, int quickMessageSearch,
                                  int frequentlyForwardedMax, int vcardMaxSizeKb, int hfmStringChanges, int stickers,
                                  int muteAlways, int announceGroups, int catalogMessage, int groupDescLength,
                                  int productCatalogDeeplink) implements JsonResponseModel {
}