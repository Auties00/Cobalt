package it.auties.whatsapp4j.binary;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * The constants of this utility class describe the various tokens used by WhatsappWeb's WebSocket.
 * These tags were extracted from JS code found at https://web.whatsapp.com/.
 */
@UtilityClass
public class BinaryTokens {
    /**
     * Double byte tokens
     */
    public final @NotNull List<String> DOUBLE_BYTE = List.of();


    /**
     * Single byte tokens
     */
    public final @Nullable List<String> SINGLE_BYTE = Arrays.asList(null, null, null, "200", "400", "404", "500", "501", "502", "action", "add", "after", "archive", "author", "available", "battery", "before", "body", "broadcast", "chat", "clear", "code", "composing", "contacts", "count", "create", "debug", "delete", "demote", "duplicate", "encoding", "error", "false", "filehash", "from", "g.us", "group", "groups_v2", "height", "id", "image", "in", "index", "invis", "item", "jid", "kind", "last", "leave", "live", "log", "media", "message", "mimetype", "missing", "modify", "name", "notification", "notify", "out", "owner", "participant", "paused", "picture", "played", "presence", "preview", "promote", "query", "raw", "read", "receipt", "received", "recipient", "recording", "relay", "remove", "response", "resume", "retry", "s.whatsapp.net", "seconds", "set", "size", "status", "subject", "subscribe", "t", "text", "to", "true", "type", "unarchive", "unavailable", "url", "user", "value", "web", "width", "mute", "read_only", "admin", "creator", "short", "update", "powersave", "checksum", "epoch", "block", "previous", "409", "replaced", "reason", "spam", "modify_tag", "message_info", "delivery", "emoji", "title", "description", "canonical-url", "matched-text", "star", "unstar", "media_key", "filename", "identity", "unread", "page", "page_count", "search", "media_message", "security", "call_log", "profile", "ciphertext", "invite", "gif", "vcard", "frequent", "privacy", "blacklist", "whitelist", "verify", "location", "document", "elapsed", "revoke_invite", "expiration", "unsubscribe", "disable", "vname", "old_jid", "new_jid", "announcement", "locked", "prop", "label", "color", "call", "offer", "call-jid", "quick_reply", "sticker", "pay_t", "accept", "reject", "sticker_pack", "invalid", "canceled", "missed", "connected", "result", "audio", "video", "recent");
}
