package it.auties.whatsapp4j.event;

public enum WhatsappEvent {
    // Connection related events
    ON_CONNECTING,
    ON_OPEN,
    ON_CLOSE,

    // Phone related events
    ON_PHONE_STATUS_UPDATE,

    // Contacts related events
    ON_CONTACTS_RECEIVED,
    ON_CONTACT_UPDATE,

    // Chats related events
    ON_CHATS_RECEIVED,
    ON_CHAT_UPDATE,

    // Blacklist related events
    ON_BLACKLIST_RECEIVED,
    ON_BLACKLIST_UPDATE
}
