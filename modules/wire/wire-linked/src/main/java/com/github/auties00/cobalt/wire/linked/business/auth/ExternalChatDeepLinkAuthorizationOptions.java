package com.github.auties00.cobalt.wire.linked.business.auth;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidProvider;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * Input model for authorising a chat opened on behalf of a third-party
 * partner via an external deep link.
 *
 * <p>When a third-party application or web surface opens a WhatsApp chat
 * with one of its customers through a deep link, the WhatsApp client shows
 * a confirmation sheet identifying the partner. Before showing the sheet
 * the client asks the server to authorise the deep link and resolve the
 * partner display copy.
 *
 * <p>{@link #recipient()} names the chat being opened.
 * {@link #deeplinkType()} is the deep-link kind carried by the link; the
 * full server-side value set is not declared in the WhatsApp client and is
 * therefore carried as an opaque string. {@link #fromExternalApp()}
 * distinguishes a deep link that came from another app launching WhatsApp
 * ({@code true}) from in-app navigation ({@code false}).
 * {@link #partnerToken()} is the integration-partner attestation token
 * carried by the deep link.
 */
@ProtobufMessage(name = "ExternalChatDeepLinkAuthorizationOptions")
public final class ExternalChatDeepLinkAuthorizationOptions {
    /**
     * Chat being opened by the deep link. Unset omits the variable.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final Jid recipient;

    /**
     * Deep-link kind carried by the link, as an opaque server-defined
     * marker. Unset omits the variable.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String deeplinkType;

    /**
     * Whether the deep link came from another app launching WhatsApp
     * ({@code true}) rather than in-app navigation ({@code false}). Unset
     * omits the variable.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BOOL)
    final Boolean fromExternalApp;

    /**
     * Integration-partner attestation token carried by the deep link.
     * Unset omits the variable.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String partnerToken;

    /**
     * Constructs a new {@code ExternalChatDeepLinkAuthorizationOptions}.
     * Every argument may be {@code null} to omit the corresponding variable
     * from the request.
     *
     * @param recipient       the chat being opened, or {@code null}
     * @param deeplinkType    the deep-link kind, or {@code null}
     * @param fromExternalApp the external-app source flag, or {@code null}
     * @param partnerToken    the integration-partner attestation token, or
     *                        {@code null}
     */
    public ExternalChatDeepLinkAuthorizationOptions(Jid recipient, String deeplinkType,
                                                    Boolean fromExternalApp, String partnerToken) {
        this.recipient = recipient;
        this.deeplinkType = deeplinkType;
        this.fromExternalApp = fromExternalApp;
        this.partnerToken = partnerToken;
    }

    /**
     * Convenience constructor that accepts any {@link JidProvider} for the
     * recipient and resolves it to a {@link Jid}.
     *
     * @param recipient       the chat being opened, or {@code null}
     * @param deeplinkType    the deep-link kind, or {@code null}
     * @param fromExternalApp the external-app source flag, or {@code null}
     * @param partnerToken    the integration-partner attestation token, or
     *                        {@code null}
     */
    public ExternalChatDeepLinkAuthorizationOptions(JidProvider recipient, String deeplinkType,
                                                    Boolean fromExternalApp, String partnerToken) {
        this(recipient == null ? null : recipient.toJid(), deeplinkType, fromExternalApp, partnerToken);
    }

    /**
     * Returns the chat being opened by the deep link.
     *
     * @return an {@link Optional} carrying the chat address, or empty when
     *         unset
     */
    public Optional<Jid> recipient() {
        return Optional.ofNullable(recipient);
    }

    /**
     * Returns the deep-link kind.
     *
     * @return an {@link Optional} carrying the kind marker, or empty when
     *         unset
     */
    public Optional<String> deeplinkType() {
        return Optional.ofNullable(deeplinkType);
    }

    /**
     * Returns the external-app source flag.
     *
     * @return an {@link Optional} carrying the flag, or empty when unset
     */
    public Optional<Boolean> fromExternalApp() {
        return Optional.ofNullable(fromExternalApp);
    }

    /**
     * Returns the integration-partner attestation token.
     *
     * @return an {@link Optional} carrying the token, or empty when unset
     */
    public Optional<String> partnerToken() {
        return Optional.ofNullable(partnerToken);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ExternalChatDeepLinkAuthorizationOptions) obj;
        return Objects.equals(recipient, that.recipient)
                && Objects.equals(deeplinkType, that.deeplinkType)
                && Objects.equals(fromExternalApp, that.fromExternalApp)
                && Objects.equals(partnerToken, that.partnerToken);
    }

    @Override
    public int hashCode() {
        return Objects.hash(recipient, deeplinkType, fromExternalApp, partnerToken);
    }

    @Override
    public String toString() {
        return "ExternalChatDeepLinkAuthorizationOptions[" +
                "recipient=" + recipient + ", " +
                "deeplinkType=" + deeplinkType + ", " +
                "fromExternalApp=" + fromExternalApp + ", " +
                "partnerToken=" + partnerToken + ']';
    }
}
