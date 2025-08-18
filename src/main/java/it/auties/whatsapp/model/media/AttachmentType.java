package it.auties.whatsapp.model.media;

import java.util.Optional;

/**
 * The constants of this enumerated type describe the various types of attachments supported by Whatsapp
 */
public enum AttachmentType {
    NONE(null, null, false),
    AUDIO("mms/audio", "WhatsApp Audio Keys", false),
    DOCUMENT("mms/document", "WhatsApp Document Keys", false),
    GIF("mms/gif", "WhatsApp Video Keys", false),
    IMAGE("mms/image", "WhatsApp Image Keys", false),
    PROFILE_PICTURE("pps/photo", null, false),
    PRODUCT("mms/image", "WhatsApp Image Keys", false),
    VOICE("mms/ptt", "WhatsApp Audio Keys", false),
    STICKER("mms/sticker", "WhatsApp Image Keys", false),
    THUMBNAIL_DOCUMENT("mms/thumbnail-document", "WhatsApp Document Thumbnail Keys", false),
    THUMBNAIL_LINK("mms/thumbnail-link", "WhatsApp Link Thumbnail Keys", false),
    VIDEO("mms/video", "WhatsApp Video Keys", false),
    APP_STATE("mms/md-app-state", "WhatsApp App State Keys", true),
    HISTORY_SYNC("mms/md-msg-hist", "WhatsApp History Keys", true),
    PRODUCT_CATALOG_IMAGE("product/image", null, false),
    BUSINESS_COVER_PHOTO("pps/biz-cover-photo", null, false),
    NEWSLETTER_AUDIO("newsletter/newsletter-audio", null, false),
    NEWSLETTER_IMAGE("newsletter/newsletter-image", null, false),
    NEWSLETTER_DOCUMENT("newsletter/newsletter-document", null, false),
    NEWSLETTER_GIF("newsletter/newsletter-gif", null, false),
    NEWSLETTER_VOICE("newsletter/newsletter-ptt", null, false),
    NEWSLETTER_STICKER("newsletter/newsletter-sticker", null, false),
    NEWSLETTER_THUMBNAIL_LINK("newsletter/newsletter-thumbnail-link", null, false),
    NEWSLETTER_VIDEO("newsletter/newsletter-video", null, false);

    private final String path;
    private final String keyName;
    private final boolean inflatable;

    AttachmentType(String path, String keyName, boolean inflatable) {
        this.path = path;
        this.keyName = keyName;
        this.inflatable = inflatable;
    }

    public Optional<String> path() {
        return Optional.ofNullable(path);
    }

    public Optional<String> keyName() {
        return Optional.ofNullable(keyName);
    }

    public boolean inflatable() {
        return this.inflatable;
    }
}
