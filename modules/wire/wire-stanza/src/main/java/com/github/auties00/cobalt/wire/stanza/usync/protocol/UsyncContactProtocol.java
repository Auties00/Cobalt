package com.github.auties00.cobalt.wire.stanza.usync.protocol;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.usync.UsyncAddressingMode;
import com.github.auties00.cobalt.wire.stanza.usync.UsyncProtocol;
import com.github.auties00.cobalt.wire.stanza.usync.UsyncProtocolResult;
import com.github.auties00.cobalt.wire.stanza.usync.result.UsyncProtocolError;
import com.github.auties00.cobalt.wire.stanza.usync.UsyncUser;
import com.github.auties00.cobalt.wire.stanza.usync.result.ContactResult;

import java.time.Duration;
import java.util.Optional;

/**
 * Describes the USync {@code contact} protocol.
 *
 * This descriptor asks the relay whether each peer is a registered WhatsApp
 * user and, when the addressing mode is LID, resolves usernames or phone
 * numbers to LIDs in the same round trip. It also hosts the shared
 * {@link #parseError(Stanza)} helper that every other protocol parser reuses,
 * keeping the per-protocol error decode single-sourced.
 *
 * @implNote
 * This implementation centralises {@link #parseError(Stanza)} here rather than
 * duplicating it across the eleven parsers; the contact protocol is the
 * natural owner because it is the addressing protocol most queries start
 * from.
 */
@WhatsAppWebModule(moduleName = "WAWebUsyncContact")
public final class UsyncContactProtocol implements UsyncProtocol {
    /**
     * Holds the wire literal for the protocol tag name.
     */
    public static final String NAME = "contact";

    /**
     * Holds the addressing mode this descriptor applies to; {@code null}
     * means the default phone-number addressing.
     */
    private final UsyncAddressingMode addressingMode;

    /**
     * Creates a contact-protocol descriptor for the given addressing mode.
     *
     * Pass {@link UsyncAddressingMode#LID} when the local contact database has
     * been migrated to LID; pass {@link UsyncAddressingMode#PN} or
     * {@code null} otherwise.
     *
     * @param addressingMode the addressing mode, or {@code null} for the
     *                       default phone-number addressing
     */
    @WhatsAppWebExport(moduleName = "WAWebUsyncContact",
            exports = "USyncContactProtocol", adaptation = WhatsAppAdaptation.DIRECT)
    public UsyncContactProtocol(UsyncAddressingMode addressingMode) {
        this.addressingMode = addressingMode;
    }

    /**
     * Creates a contact-protocol descriptor with phone-number addressing.
     *
     * Convenience shortcut for {@code new UsyncContactProtocol(UsyncAddressingMode.PN)}
     * used by call sites that build queries without consulting the username
     * gating utility.
     */
    public UsyncContactProtocol() {
        this(UsyncAddressingMode.PN);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebUsyncContact",
            exports = "USyncContactProtocol.getName", adaptation = WhatsAppAdaptation.DIRECT)
    public String name() {
        return NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation emits the {@code addressing_mode} attribute only
     * when {@link UsyncAddressingMode#LID} is selected, dropping it for the
     * phone-number mode.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebUsyncContact",
            exports = "USyncContactProtocol.getQueryElement", adaptation = WhatsAppAdaptation.DIRECT)
    public Stanza buildQueryElement() {
        var builder = new StanzaBuilder().description(NAME);
        if (addressingMode == UsyncAddressingMode.LID) {
            builder.attribute("addressing_mode", UsyncAddressingMode.LID.wireValue());
        }
        return builder.build();
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation picks the addressing shape from the slots the user
     * carries, in priority order: phone number (inline text), username (with
     * optional {@code pin} and {@code lid} attributes), then contact type
     * (the {@code type} attribute alone). Users carrying none of these slots
     * produce {@link Optional#empty()}.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebUsyncContact",
            exports = "USyncContactProtocol.getUserElement", adaptation = WhatsAppAdaptation.DIRECT)
    public Optional<Stanza> buildUserElement(UsyncUser user) {
        if (user.phoneNumber().isPresent()) {
            return Optional.of(new StanzaBuilder()
                    .description(NAME)
                    .content(user.phoneNumber().get().getBytes())
                    .build());
        }
        if (user.username().isPresent()) {
            var builder = new StanzaBuilder().description(NAME)
                    .attribute("username", user.username().get());
            user.pin().ifPresent(p -> builder.attribute("pin", p));
            user.lid().ifPresent(l -> builder.attribute("lid", l.toString()));
            return Optional.of(builder.build());
        }
        if (user.contactType().isPresent()) {
            return Optional.of(new StanzaBuilder()
                    .description(NAME)
                    .attribute("type", user.contactType().get())
                    .build());
        }
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException {@inheritDoc} Also thrown when the
     *     required {@code type} attribute is missing on a non-error response.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebUsyncContact",
            exports = "contactParser", adaptation = WhatsAppAdaptation.ADAPTED)
    public UsyncProtocolResult parseUserResult(Stanza child) {
        if (!child.hasDescription(NAME)) {
            throw new IllegalStateException("expected <" + NAME + ">, got <" + child.description() + ">");
        }
        var error = parseError(child);
        if (error.isPresent()) {
            return error.get();
        }
        var type = child.getAttributeAsString("type")
                .orElseThrow(() -> new IllegalStateException("[usync] contact stanza has missing type attribute"));
        var username = child.getAttributeAsString("username").orElse(null);
        var content = child.toContentString().orElse(null);
        return new ContactResult(type, username, content);
    }

    /**
     * Probes the optional {@code <error/>} child of a USync protocol response.
     *
     * Reused by every protocol parser so the per-protocol error decode lives
     * in one place; the {@code error_backoff} attribute (in seconds) is parsed
     * into a {@link Duration} that the caller can hand back to
     * {@link com.github.auties00.cobalt.wire.stanza.usync.UsyncBackoff#setProtocolBackoffMs(String, long)}.
     *
     * @param child the protocol-tagged response stanza
     * @return the parsed error, or empty when no {@code <error/>} child is
     *     present
     */
    public static Optional<UsyncProtocolError> parseError(Stanza child) {
        return child.getChild("error").map(err -> new UsyncProtocolError(
                err.getRequiredAttributeAsInt("code"),
                err.getAttributeAsString("text", ""),
                err.getAttributeAsLong("error_backoff").stream().boxed()
                        .map(Duration::ofSeconds).findFirst().orElse(null)));
    }
}
