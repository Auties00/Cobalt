package com.github.auties00.cobalt.wire.stanza.iq.group;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Models the outbound {@code <iq xmlns="w:g2" type="set" to="g.us"><leave>...</leave></iq>} request that leaves one or more groups or communities in a single batched stanza.
 *
 * <p>This request backs the "leave group", "leave community", and "leave several communities" admin
 * actions. {@link Mode#GROUP} targets regular groups (each encodes as {@code <group id="..."/>})
 * and {@link Mode#LINKED_GROUPS} targets communities (each encodes as
 * {@code <linked_groups parent_group_jid="..."/>}, which the relay expands server-side to leave the
 * parent and every linked sub-group atomically). The two shapes cannot be mixed inside a single
 * request.
 *
 * @implNote
 * This implementation collapses the three separate single-group, single-community, and bulk-leave
 * entry points into one request class keyed by {@link Mode} and a list of targets; the
 * single-target paths share the same wire format as the bulk path and differ only in the size of
 * the {@code <leave>} payload.
 */
@WhatsAppWebModule(moduleName = "WAWebGroupExitJob")
public final class IqGroupExitRequest implements IqStanza.Request {
    /**
     * Discriminates the grandchild shape carried inside the {@code <leave>} payload.
     *
     * <p>Selects between regular-group leave (one {@code <group id="..."/>} per target) and
     * community leave (one {@code <linked_groups parent_group_jid="..."/>} per target). The two
     * shapes cannot be mixed inside a single request.
     */
    public enum Mode {
        /**
         * Selects the regular-group leave shape.
         *
         * <p>Each target encodes as {@code <group id="GROUP_JID"/>} and causes the relay to leave
         * that group only. Applies to both regular groups and announcement-only groups.
         */
        GROUP,
        /**
         * Selects the community-leave shape.
         *
         * <p>Each target encodes as {@code <linked_groups parent_group_jid="COMMUNITY_JID"/>} and
         * causes the relay to leave the named community plus every sub-group linked to it in one
         * atomic step.
         */
        LINKED_GROUPS
    }

    /**
     * Holds the list of target JIDs to leave.
     */
    private final List<Jid> targets;

    /**
     * Holds the grandchild-shape discriminator.
     */
    private final Mode mode;

    /**
     * Constructs a request that leaves the given list of targets in the given mode.
     *
     * <p>For a single-target leave, pass a one-element list; the batched and single-target paths
     * use the same wire format and differ only in the size of the {@code <leave>} payload.
     *
     * @implNote
     * This implementation defensively copies {@code targets} via
     * {@link List#copyOf(java.util.Collection)} so later mutation of the caller's list cannot reach
     * the dispatched stanza.
     *
     * @param targets the list of group or community {@link Jid}s to leave; never {@code null} and never empty
     * @param mode    the grandchild-shape discriminator; never {@code null}
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IllegalArgumentException if {@code targets} is empty
     */
    public IqGroupExitRequest(List<Jid> targets, Mode mode) {
        Objects.requireNonNull(targets, "targets cannot be null");
        Objects.requireNonNull(mode, "mode cannot be null");
        if (targets.isEmpty()) {
            throw new IllegalArgumentException("targets cannot be empty");
        }
        this.targets = List.copyOf(targets);
        this.mode = mode;
    }

    /**
     * Returns the list of JIDs being left.
     *
     * @return an unmodifiable {@link List} of target {@link Jid}s; never {@code null}
     */
    public List<Jid> targets() {
        return targets;
    }

    /**
     * Returns the grandchild-shape discriminator.
     *
     * @return the {@link Mode}; never {@code null}
     */
    public Mode mode() {
        return mode;
    }

    /**
     * Builds the outbound IQ stanza as a {@link StanzaBuilder} ready for dispatch.
     *
     * <p>Each target in {@link #targets()} becomes one {@code <group id="..."/>} child in
     * {@link Mode#GROUP} or one {@code <linked_groups parent_group_jid="..."/>} child in
     * {@link Mode#LINKED_GROUPS}.
     *
     * @implNote
     * This implementation sets the IQ {@code to} attribute to the
     * {@link JidServer#groupOrCommunity()} group server rather than any per-target group JID, so a
     * single envelope addresses the whole batch.
     *
     * @return the IQ envelope builder
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebGroupExitJob",
            exports = "leaveGroup", adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WAWebGroupExitJob",
            exports = "leaveCommunity", adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WAWebGroupExitJob",
            exports = "leaveCommunities", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var grandchildren = new ArrayList<Stanza>(targets.size());
        for (var target : targets) {
            StanzaBuilder childBuilder;
            if (mode == Mode.GROUP) {
                childBuilder = new StanzaBuilder()
                        .description("group")
                        .attribute("id", target);
            } else {
                childBuilder = new StanzaBuilder()
                        .description("linked_groups")
                        .attribute("parent_group_jid", target);
            }
            grandchildren.add(childBuilder.build());
        }
        var leaveNode = new StanzaBuilder()
                .description("leave")
                .content(grandchildren)
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "w:g2")
                .attribute("to", JidServer.groupOrCommunity())
                .attribute("type", "set")
                .content(leaveNode);
    }

    /**
     * Compares this request with another object for equality.
     *
     * <p>Two requests are equal when they carry the same {@link #targets()} list and the same
     * {@link #mode()}.
     *
     * @param obj the object to compare with; may be {@code null}
     * @return {@code true} when {@code obj} is an equal request, {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (IqGroupExitRequest) obj;
        return Objects.equals(this.targets, that.targets)
                && this.mode == that.mode;
    }

    /**
     * Returns a hash code derived from the targets and mode.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(targets, mode);
    }

    /**
     * Returns a debug string describing the targets and mode.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return "IqGroupExitRequest[targets=" + targets
                + ", mode=" + mode + ']';
    }
}
