package com.github.auties00.cobalt.wire.stanza.smax.groups;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The inbound {@code <notification type="w:gp2">} stanza that signals one or more groups' metadata has gone stale on
 * the client and must be re-queried.
 * <p>
 * The relay pushes this notification when group-side caches invalidate, for example on group renames or ownership
 * transfers. After parsing, the client re-queries the affected groups and then acks the notification via
 * {@link SmaxGroupsGroupsDirtyNotificationAcknowledgement}. The affected-group list is capped at 10000 entries
 * server-side.
 */
@WhatsAppWebModule(moduleName = "WASmaxInGroupsGroupsDirtyNotificationRequest")
@WhatsAppWebModule(moduleName = "WASmaxInGroupsServerNotificationMixin")
public final class SmaxGroupsGroupsDirtyNotificationResponse implements SmaxStanza.Response {
    /**
     * The list of stale group {@link Jid}s carried by the inbound notification.
     */
    private final List<Jid> dirtyGroups;

    /**
     * Constructs a {@link SmaxGroupsGroupsDirtyNotificationResponse} projection.
     * <p>
     * A {@code null} argument normalises to {@link List#of()} so callers can construct an empty notification directly.
     *
     * @param dirtyGroups the list of stale group {@link Jid}s; may be {@code null}
     */
    public SmaxGroupsGroupsDirtyNotificationResponse(List<Jid> dirtyGroups) {
        this.dirtyGroups = List.copyOf(Objects.requireNonNullElse(dirtyGroups, List.of()));
    }

    /**
     * Returns the list of stale group {@link Jid}s.
     *
     * @return an unmodifiable list of dirty group JIDs; never {@code null}
     */
    public List<Jid> dirtyGroups() {
        return dirtyGroups;
    }

    /**
     * Tries to parse a {@link SmaxGroupsGroupsDirtyNotificationResponse} from the given {@code <notification/>} stanza.
     * <p>
     * Parsing succeeds when the notification carries {@code type="w:gp2"}, a {@code from} JID on the {@code g.us}
     * domain, and a {@code <groups_dirty>} child; each {@code <group jid="..."/>} entry contributes one dirty JID and
     * entries without a {@code jid} attribute are skipped.
     *
     * @implNote This implementation accepts zero {@code <group/>} entries rather than enforcing the WA Web
     * {@code [1, 10000]} bound, so callers can detect an empty payload without exception handling.
     *
     * @param stanza the inbound notification stanza
     * @return an {@link Optional} carrying the parsed projection, or empty when the stanza shape does not match
     * @throws NullPointerException if {@code stanza} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxInGroupsGroupsDirtyNotificationRequest",
            exports = "parseGroupsDirtyNotificationRequest",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<SmaxGroupsGroupsDirtyNotificationResponse> of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        if (!stanza.hasDescription("notification")) {
            return Optional.empty();
        }
        if (!stanza.hasAttribute("type", "w:gp2")) {
            return Optional.empty();
        }
        var from = stanza.getAttributeAsJid("from").orElse(null);
        if (from == null || !"g.us".equals(from.server().toString())) {
            return Optional.empty();
        }
        var groupsDirty = stanza.getChild("groups_dirty").orElse(null);
        if (groupsDirty == null) {
            return Optional.empty();
        }
        var groups = groupsDirty.streamChildren("group")
                .map(child -> child.getAttributeAsJid("jid").orElse(null))
                .filter(Objects::nonNull)
                .toList();
        return Optional.of(new SmaxGroupsGroupsDirtyNotificationResponse(groups));
    }

    /**
     * Compares this notification to {@code obj} for value equality across every field.
     *
     * @param obj the other object
     * @return {@code true} when {@code obj} is a {@link SmaxGroupsGroupsDirtyNotificationResponse} with identical
     *         fields
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxGroupsGroupsDirtyNotificationResponse) obj;
        return Objects.equals(this.dirtyGroups, that.dirtyGroups);
    }

    /**
     * Returns a hash composed of every field.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(dirtyGroups);
    }

    /**
     * Returns a debug string carrying every field.
     *
     * @return the debug representation
     */
    @Override
    public String toString() {
        return "SmaxGroupsGroupsDirtyNotificationResponse[dirtyGroups=" + dirtyGroups + ']';
    }
}
