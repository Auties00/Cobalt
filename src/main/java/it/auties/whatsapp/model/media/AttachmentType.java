package it.auties.whatsapp.model.media;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * The constants of this enumerated type describe the various types of attachments supported by Whatsapp
 */
@AllArgsConstructor
@Accessors(fluent = true)
public enum AttachmentType {
    IMAGE("mms/image", "WhatsApp Image Keys", false),
    AUDIO("mms/audio", "WhatsApp Audio Keys", false),
    VIDEO("mms/video", "WhatsApp Video Keys", false),
    DOCUMENT("mms/document", "WhatsApp Document Keys", false),
    HISTORY_SYNC("mms/md-msg-hist", "WhatsApp History Keys", true),
    THUMBNAIL_DOCUMENT("mms/thumbnail-document", "WhatsApp Document Thumbnail Keys", false),
    THUMBNAIL_IMAGE("mms/thumbnail-image", "WhatsApp Image Thumbnail Keys", false),
    THUMBNAIL_LINK("mms/thumbnail-link", "WhatsApp Link Thumbnail Keys", false),
    THUMBNAIL_VIDEO("mms/thumbnail-video", "WhatsApp Video Thumbnail Keys", false),
    APP_STATE("mms/md-app-state", "WhatsApp App State Keys", false);

    @Getter
    private final String path;

    @Getter
    private final String keyName;

    @Getter
    private final boolean inflatable;
}
