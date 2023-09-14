package it.auties.whatsapp.model.media;

/**
 * The constants of this enumerated type describe the various types of attachments supported by Whatsapp
 */
public enum AttachmentType {
    NONE("", "", false),
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

    private final String path;
    private final String keyName;
    private final boolean inflatable;

    AttachmentType(String path, String keyName, boolean inflatable) {
        this.path = path;
        this.keyName = keyName;
        this.inflatable = inflatable;
    }

    public String path() {
        return this.path;
    }

    public String keyName() {
        return this.keyName;
    }

    public boolean inflatable() {
        return this.inflatable;
    }
}
