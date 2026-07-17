package com.github.auties00.cobalt.wire.linked.privacy;

import it.auties.protobuf.annotation.ProtobufEnum;

/**
 * Distribution modes available for the local user's WhatsApp Status updates.
 *
 * <p>This is the user-facing audience selector for Status (the 24-hour
 * stories feature): "share with all my contacts", "share with all my contacts
 * except a blocklist", or "share only with an explicit allowlist". Whichever
 * mode is chosen, the matching JID list is carried separately in
 * {@link StatusPrivacySetting#jids()}.
 *
 * <p>This enum is the user-prefs-level view of the setting: it does not model
 * the lower-level options (close friends, custom list) that only show up
 * inside the app-state sync stream.
 */
@ProtobufEnum
public enum StatusPrivacyMode {
    /**
     * Share status updates with every contact in the address book. The
     * paired JID list is empty.
     */
    CONTACTS,

    /**
     * Share status updates with every contact except for an explicit
     * blocklist. The paired JID list carries the excluded JIDs.
     */
    CONTACTS_EXCEPT,

    /**
     * Share status updates only with an explicit allowlist and hide the
     * status from everyone else. The paired JID list carries the allowed
     * JIDs.
     */
    WHITELIST
}
