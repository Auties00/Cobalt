package com.github.auties00.cobalt.model.media;

import it.auties.protobuf.annotation.ProtobufEnum;

import java.util.*;

/**
 * The constants of this enumerated type describe the various types of attachments supported by Whatsapp
 */
@ProtobufEnum
public enum MediaPath {
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
    THUMBNAIL_IMAGE("mms/thumbnail-image", "WhatsApp Image Thumbnail Keys", false),
    THUMBNAIL_VIDEO("mms/thumbnail-video", "WhatsApp Video Thumbnail Keys", false),
    VIDEO("mms/video", "WhatsApp Video Keys", false),
    PTV("mms/ptv", "WhatsApp Video Keys", false),
    APP_STATE("mms/md-app-state", "WhatsApp App State Keys", true),
    HISTORY_SYNC("mms/md-msg-hist", "WhatsApp History Keys", true),
    PRODUCT_CATALOG_IMAGE("product/image", null, false),
    PAYMENT_BG_IMAGE("mms/payment-bg-image", "WhatsApp Payment Background Keys", false),
    BUSINESS_COVER_PHOTO("pps/biz-cover-photo", null, false),
    NATIVE_AD_IMAGE("mms/ads-image", "ads-image", false),
    NATIVE_AD_VIDEO("mms/ads-video", "ads-video", false),
    STICKER_PACK("mms/sticker-pack", "WhatsApp Sticker Pack Keys", false),
    THUMBNAIL_STICKER_PACK("mms/thumbnail-sticker-pack", "WhatsApp Sticker Pack Thumbnail Keys", false),
    MUSIC_ARTWORK("mms/music-artwork", "WhatsApp Music Artwork Keys", false),
    GROUP_HISTORY("mms/group-history", "Group History", false),
    NEWSLETTER_AUDIO("newsletter/newsletter-audio", null, false),
    NEWSLETTER_IMAGE("newsletter/newsletter-image", null, false),
    NEWSLETTER_DOCUMENT("newsletter/newsletter-document", null, false),
    NEWSLETTER_GIF("newsletter/newsletter-gif", null, false),
    NEWSLETTER_VOICE("newsletter/newsletter-ptt", null, false),
    NEWSLETTER_PTV("newsletter/newsletter-ptv", null, false),
    NEWSLETTER_STICKER("newsletter/newsletter-sticker", null, false),
    NEWSLETTER_STICKER_PACK("newsletter/newsletter-sticker-pack", null, false),
    NEWSLETTER_THUMBNAIL_LINK("newsletter/newsletter-thumbnail-link", null, false),
    NEWSLETTER_VIDEO("newsletter/newsletter-video", null, false),
    NEWSLETTER_MUSIC_ARTWORK("mms/newsletter-music-artwork", null, false);

    private final String path;
    private final String keyName;
    private final boolean inflatable;

    private static final Set<MediaPath> KNOWN;
    private static final Map<String, MediaPath> BY_ID;

    static {
        var known = new HashSet<MediaPath>();
        for(var value : values()) {
            if(value != NONE) {
                known.add(value);
            }
        }
        KNOWN = Collections.unmodifiableSet(known);

        Map<String, MediaPath> byId = HashMap.newHashMap(known.size());
        for(var value : known) {
            var path = value.path;
            var separator = path.indexOf('/');
            var id = separator == -1 ? path : path.substring(separator + 1);
            byId.put(id, value);
        }
        BY_ID = Collections.unmodifiableMap(byId);
    }

    public static Set<MediaPath> known() {
        return KNOWN;
    }

    public static Optional<MediaPath> ofId(String id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    MediaPath(String path, String keyName, boolean inflatable) {
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