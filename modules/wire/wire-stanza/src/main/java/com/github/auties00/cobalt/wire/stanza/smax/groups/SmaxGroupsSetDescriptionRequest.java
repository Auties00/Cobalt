package com.github.auties00.cobalt.wire.stanza.smax.groups;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

/**
 * Models the outbound {@code <iq type="set" xmlns="w:g2">} stanza that replaces, sets, or clears a group's
 * description.
 *
 * <p>This request backs editing a group's description. The {@link #descriptionId()} and
 * {@link #descriptionPrev()} attributes implement the relay's revision-chain check: the client passes the
 * existing revision id as {@code prev} and the freshly minted revision id as {@code id} so the relay can detect
 * concurrent edits. Setting {@link #delete()} clears the description; {@link #body()} carries the new text on a
 * replace.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsSetDescriptionRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsBaseSetGroupMixin")
public final class SmaxGroupsSetDescriptionRequest implements SmaxStanza.Request {
    /**
     * The group {@link Jid} whose description is being mutated.
     */
    private final Jid groupJid;

    /**
     * The new description revision identifier.
     */
    private final String descriptionId;

    /**
     * The previous description revision identifier being replaced.
     */
    private final String descriptionPrev;

    /**
     * Whether this request clears the description (the {@code delete="true"} attribute).
     */
    private final boolean delete;

    /**
     * The new description body text.
     */
    private final String body;

    /**
     * Constructs a set-description request.
     *
     * <p>The relay enforces the revision-chain semantics: a concurrent edit (a {@code descriptionPrev} that no
     * longer matches the server-side current revision) produces a
     * {@link SmaxGroupsSetDescriptionResponse.ClientError}. Setting {@code delete} clears the description and the
     * relay ignores any {@code body}; on a replace, {@code body} carries the UTF-8 text.
     *
     * @param groupJid        the group {@link Jid}
     * @param descriptionId   the new revision identifier; may be {@code null} when only {@code prev} is supplied
     * @param descriptionPrev the previous revision identifier; may be {@code null} on the very first description
     * @param delete          {@code true} to clear the description
     * @param body            the new description text; required when {@code delete} is {@code false}, may be
     *                        {@code null} otherwise
     * @throws NullPointerException if {@code groupJid} is {@code null}
     */
    public SmaxGroupsSetDescriptionRequest(Jid groupJid, String descriptionId, String descriptionPrev, boolean delete, String body) {
        this.groupJid = Objects.requireNonNull(groupJid, "groupJid cannot be null");
        this.descriptionId = descriptionId;
        this.descriptionPrev = descriptionPrev;
        this.delete = delete;
        this.body = body;
    }

    /**
     * Returns the target group {@link Jid}.
     *
     * <p>The value routes verbatim into the IQ's {@code to} attribute.
     *
     * @return the group {@link Jid}; never {@code null}
     */
    public Jid groupJid() {
        return groupJid;
    }

    /**
     * Returns the new description revision identifier.
     *
     * @return an {@link Optional} carrying the new revision id, or empty when not set
     */
    public Optional<String> descriptionId() {
        return Optional.ofNullable(descriptionId);
    }

    /**
     * Returns the previous description revision identifier being replaced.
     *
     * <p>The relay rejects the mutation with a {@link SmaxGroupsSetDescriptionResponse.ClientError} when the
     * value disagrees with the server-side current revision.
     *
     * @return an {@link Optional} carrying the previous revision id, or empty when no chain check is requested
     */
    public Optional<String> descriptionPrev() {
        return Optional.ofNullable(descriptionPrev);
    }

    /**
     * Returns whether this request clears the description rather than replacing it.
     *
     * @return {@code true} when this is a delete request
     */
    public boolean delete() {
        return delete;
    }

    /**
     * Returns the new description body.
     *
     * @return an {@link Optional} carrying the new body text, or empty
     */
    public Optional<String> body() {
        return Optional.ofNullable(body);
    }

    /**
     * Materialises the outbound IQ stanza ready for dispatch.
     *
     * <p>The resulting envelope is
     * {@snippet :
     *     <iq xmlns="w:g2" to="<groupJid>" type="set">
     *         <description id="<descriptionId>" prev="<descriptionPrev>" delete="true">
     *             <body>...UTF-8 bytes...</body>
     *         </description>
     *     </iq>
     * }
     * The {@code id}, {@code prev}, and {@code delete} attributes are emitted only when the corresponding fields
     * are populated, and the {@code <body>} child is omitted when {@link #body()} is empty.
     *
     * @return a {@link StanzaBuilder} carrying the IQ envelope and the {@code <description>} payload
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutGroupsSetDescriptionRequest",
            exports = "makeSetDescriptionRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var descriptionBuilder = new StanzaBuilder().description("description");
        if (descriptionId != null) {
            descriptionBuilder.attribute("id", descriptionId);
        }
        if (descriptionPrev != null) {
            descriptionBuilder.attribute("prev", descriptionPrev);
        }
        if (delete) {
            descriptionBuilder.attribute("delete", "true");
        }
        if (body != null) {
            var bodyNode = new StanzaBuilder()
                    .description("body")
                    .content(body.getBytes(StandardCharsets.UTF_8))
                    .build();
            descriptionBuilder.content(bodyNode);
        }
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "w:g2")
                .attribute("to", groupJid)
                .attribute("type", "set")
                .content(descriptionBuilder.build());
    }

    /**
     * Compares this request to {@code obj} for value equality across every field.
     *
     * @param obj the other object
     * @return {@code true} when {@code obj} is a {@link SmaxGroupsSetDescriptionRequest} with identical fields
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxGroupsSetDescriptionRequest) obj;
        return this.delete == that.delete
                && Objects.equals(this.groupJid, that.groupJid)
                && Objects.equals(this.descriptionId, that.descriptionId)
                && Objects.equals(this.descriptionPrev, that.descriptionPrev)
                && Objects.equals(this.body, that.body);
    }

    /**
     * Returns a hash composed of every field.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(groupJid, descriptionId, descriptionPrev, delete, body);
    }

    /**
     * Returns a debug string carrying every field.
     *
     * @return the debug representation
     */
    @Override
    public String toString() {
        return "SmaxGroupsSetDescriptionRequest[groupJid=" + groupJid
                + ", descriptionId=" + descriptionId
                + ", descriptionPrev=" + descriptionPrev
                + ", delete=" + delete
                + ", body=" + body + ']';
    }
}
