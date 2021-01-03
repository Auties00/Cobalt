package it.auties.whatsapp4j.constant;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Optional;

public enum Metric {
    DEBUG_LOG,
    QUERY_RESUME,
    QUERY_RECEIPT,
    QUERY_MEDIA,
    QUERY_CHAT,
    QUERY_CONTACTS,
    QUERY_MESSAGES,
    PRESENCE,
    PRESENCE_SUBSCRIBE,
    GROUP,
    READ,
    CHAT,
    RECEIVED,
    PIC,
    STATUS,
    MESSAGE,
    QUERY_ACTIONS,
    BLOCK,
    QUERY_GROUP,
    QUERY_PREVIEW,
    QUERY_EMOJI,
    QUERY_MESSAGE_INFO,
    SPAM,
    QUERY_SEARCH,
    QUERY_IDENTITY,
    QUERY_URL,
    PROFILE,
    CONTACT,
    QUERY_VCARD,
    QUERY_STATUS,
    QUERY_STATUS_UPDATE,
    PRIVACY_STATUS,
    QUERY_LIVE_LOCATIONS,
    LIVE_LOCATION,
    QUERY_VNAME,
    QUERY_LABELS,
    CALL,
    QUERY_CALL,
    QUERY_QUICK_REPLIES,
    QUERY_CALL_OFFER,
    QUERY_RESPONSE,
    QUERY_STICKER_PACKS,
    QUERY_STICKERS,
    ADD_OR_REMOVE_LABELS,
    QUERY_NEXT_LABEL_COLOR,
    QUERY_LABEL_PALETTE,
    CREATE_OR_DELETE_LABELS,
    EDIT_LABELS;

    public int data(){
        return ordinal() + 1;
    }

    public static Optional<Metric> forName(@NotNull String tag){
        return Arrays.stream(values()).filter(entry -> entry.name().equals(tag)).findAny();
    }
}
