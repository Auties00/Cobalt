package com.github.auties00.cobalt.node.mex.json.response;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.model.jid.JidServer;

import java.util.Optional;

/**
 * Response model for LidChangeNotification mex events.
 * <p>
 * This notification informs the client when:
 * <ul>
 *     <li>A contact's LID has changed (e.g., account migration, recovery)</li>
 *     <li>The user's own LID has been updated</li>
 *     <li>A LID-to-phone mapping has been invalidated or updated</li>
 *     <li>A contact has completed LID migration</li>
 * </ul>
 */
public final class LidChangeNotificationResponse {
    private final Jid jid;
    private final Jid oldLid;
    private final Jid newLid;
    private final LidChangeType changeType;
    private final long timestamp;
    private final boolean isSelf;
    private final boolean migrationComplete;

    private LidChangeNotificationResponse(
            Jid jid,
            Jid oldLid,
            Jid newLid,
            LidChangeType changeType,
            long timestamp,
            boolean isSelf,
            boolean migrationComplete
    ) {
        this.jid = jid;
        this.oldLid = oldLid;
        this.newLid = newLid;
        this.changeType = changeType;
        this.timestamp = timestamp;
        this.isSelf = isSelf;
        this.migrationComplete = migrationComplete;
    }

    /**
     * Parses a LidChangeNotificationResponse from JSON string.
     *
     * @param json the JSON string
     * @return Optional containing the response if valid
     */
    public static Optional<LidChangeNotificationResponse> ofJson(byte[] json) {
        if (json == null) {
            return Optional.empty();
        }

        var jsonObject = JSON.parseObject(json);
        if (jsonObject == null) {
            return Optional.empty();
        }

        var data = jsonObject.getJSONObject("data");
        if (data == null) {
            data = jsonObject;
        }

        var payload = data.getJSONObject("xwa2_notify_lid_change");
        if (payload == null) {
            payload = data;
        }

        // Parse new_lid (required)
        var newLidStr = payload.getString("new_lid");
        if (newLidStr == null || newLidStr.isBlank()) {
            return Optional.empty();
        }

        // Parse change_type (required)
        var changeTypeStr = payload.getString("change_type");
        var changeType = LidChangeType.of(changeTypeStr).orElse(null);
        if (changeType == null) {
            return Optional.empty();
        }

        // Parse timestamp (required)
        var timestampStr = payload.getString("timestamp");
        long timestamp;
        try {
            timestamp = timestampStr != null ? Long.parseLong(timestampStr) : 0L;
        } catch (NumberFormatException e) {
            timestamp = 0L;
        }

        // Parse is_self
        var isSelf = payload.getBooleanValue("is_self", false);

        // Parse jid (required unless is_self)
        var jidStr = payload.getString("jid");
        if (jidStr == null && !isSelf) {
            return Optional.empty();
        }
        var jid = jidStr != null ? Jid.of(jidStr) : null;

        // Parse old_lid (optional)
        var oldLidStr = payload.getString("old_lid");
        var oldLid = oldLidStr != null && !oldLidStr.isBlank()
                ? Jid.of(oldLidStr, JidServer.lid())
                : null;

        // Parse new_lid as JID
        var newLid = Jid.of(newLidStr, JidServer.lid());

        // Parse migration_complete
        var migrationComplete = payload.getBooleanValue("migration_complete", false);

        var result = new LidChangeNotificationResponse(
                jid,
                oldLid,
                newLid,
                changeType,
                timestamp,
                isSelf,
                migrationComplete
        );
        return Optional.of(result);
    }

    /**
     * Returns the JID (phone-based or server ID) affected.
     *
     * @return the JID, may be null if is_self is true
     */
    public Jid jid() {
        return jid;
    }

    /**
     * Returns the old LID (if applicable).
     *
     * @return Optional containing the old LID
     */
    public Optional<Jid> oldLid() {
        return Optional.ofNullable(oldLid);
    }

    /**
     * Returns the new LID.
     *
     * @return the new LID
     */
    public Jid newLid() {
        return newLid;
    }

    /**
     * Returns the type of change.
     *
     * @return the change type
     */
    public LidChangeType changeType() {
        return changeType;
    }

    /**
     * Returns the timestamp of the change (Unix epoch seconds).
     *
     * @return the timestamp
     */
    public long timestamp() {
        return timestamp;
    }

    /**
     * Returns whether this affects the current user.
     *
     * @return true if this is a self LID change
     */
    public boolean isSelf() {
        return isSelf;
    }

    /**
     * Returns whether migration is complete.
     *
     * @return true if migration is complete
     */
    public boolean migrationComplete() {
        return migrationComplete;
    }

    @Override
    public String toString() {
        return "LidChangeNotificationResponse[" +
                "jid=" + jid +
                ", oldLid=" + oldLid +
                ", newLid=" + newLid +
                ", changeType=" + changeType +
                ", timestamp=" + timestamp +
                ", isSelf=" + isSelf +
                ", migrationComplete=" + migrationComplete +
                ']';
    }
}
