package com.github.auties00.cobalt.wire.stanza.usync;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.usync.protocol.UsyncBotProfileProtocol;
import com.github.auties00.cobalt.wire.stanza.usync.protocol.UsyncBusinessProtocol;
import com.github.auties00.cobalt.wire.stanza.usync.protocol.UsyncContactProtocol;
import com.github.auties00.cobalt.wire.stanza.usync.protocol.UsyncDeviceProtocol;
import com.github.auties00.cobalt.wire.stanza.usync.protocol.UsyncDisappearingModeProtocol;
import com.github.auties00.cobalt.wire.stanza.usync.protocol.UsyncFeatureProtocol;
import com.github.auties00.cobalt.wire.stanza.usync.protocol.UsyncLidProtocol;
import com.github.auties00.cobalt.wire.stanza.usync.protocol.UsyncPictureProtocol;
import com.github.auties00.cobalt.wire.stanza.usync.protocol.UsyncStatusProtocol;
import com.github.auties00.cobalt.wire.stanza.usync.protocol.UsyncTextStatusProtocol;
import com.github.auties00.cobalt.wire.stanza.usync.protocol.UsyncUsernameProtocol;

import java.util.Optional;

/**
 * Sealed descriptor of one of the eleven USync protocols.
 *
 * <p>A {@link UsyncQuery} carries one or more protocol descriptors plus a list
 * of {@link UsyncUser} entries. Each descriptor knows how to emit its
 * {@code <query>} child, how to emit the optional per-user element, and how to
 * parse the per-user response. The eleven permitted implementations correspond
 * one-to-one with the WA Web protocol classes for contact, devices, feature,
 * business, picture, status, disappearing-mode, lid, bot, username, and
 * text-status.
 *
 * @implSpec
 * Implementations must be stateless apart from constructor parameters; the same
 * instance is invoked for every user in the query and may be shared across
 * queries. {@link #name()} must return the literal tag name used both inside
 * the {@code <query>} element and inside each {@code <user>} response so
 * {@link UsyncQuery#parseResponse(Stanza)} can match per-user children.
 */
@WhatsAppWebModule(moduleName = "WAWebUsync")
public sealed interface UsyncProtocol permits
        UsyncContactProtocol,
        UsyncDeviceProtocol,
        UsyncFeatureProtocol,
        UsyncBusinessProtocol,
        UsyncPictureProtocol,
        UsyncStatusProtocol,
        UsyncDisappearingModeProtocol,
        UsyncLidProtocol,
        UsyncBotProfileProtocol,
        UsyncUsernameProtocol,
        UsyncTextStatusProtocol {

    /**
     * Returns the protocol's literal wire tag name.
     *
     * <p>Used by {@link UsyncQuery#parseResponse(Stanza)} to look up the matching
     * child inside each {@code <user>} response and by {@link UsyncBackoff} to
     * key per-protocol backoff state.
     *
     * @return the tag name (e.g. {@code "contact"}, {@code "devices"},
     *     {@code "feature"})
     */
    String name();

    /**
     * Builds the protocol's child element of the outbound {@code <query>}
     * block.
     *
     * <p>Most protocols emit an empty element (e.g. {@code <picture/>}); the
     * device protocol carries {@code version="2"}; the contact protocol
     * conditionally carries {@code addressing_mode="lid"}; the feature protocol
     * carries one empty child per requested feature key.
     *
     * @return the query-element stanza
     */
    Stanza buildQueryElement();

    /**
     * Builds the optional per-user child inside a {@code <user>} entry.
     *
     * <p>Returns {@link Optional#empty()} when the protocol has no per-user
     * payload to ship. The device, lid, bot-profile, contact, and status
     * protocols use this slot to attach hints (device hash, lid hint, persona
     * id, addressing data, trusted-contact token) that the relay needs to
     * compute the response.
     *
     * @param user the user the {@code <user>} entry refers to
     * @return the per-user element, or empty
     */
    Optional<Stanza> buildUserElement(UsyncUser user);

    /**
     * Parses the protocol's child of one {@code <user>} response into a Java
     * result.
     *
     * <p>Returns a {@link com.github.auties00.cobalt.wire.stanza.usync.result.UsyncProtocolError}
     * when the relay attached an {@code <error/>} child, otherwise returns the
     * protocol-specific success variant declared by the matching permit of
     * {@link com.github.auties00.cobalt.wire.stanza.usync.result.UsyncProtocolResponse}.
     *
     * @implSpec
     * Implementations must throw {@link IllegalStateException} when invoked on a
     * stanza whose tag is not {@link #name()}; this matches the tag-assertion
     * guard at the top of every JS parser.
     *
     * @param userChild the child stanza tagged with this protocol's
     *                  {@link #name()}, located inside a {@code <user>} entry
     * @return the parsed result, never {@code null}
     */
    UsyncProtocolResult parseUserResult(Stanza userChild);
}
