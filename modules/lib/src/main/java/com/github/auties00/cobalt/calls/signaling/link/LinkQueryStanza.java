package com.github.auties00.cobalt.calls.signaling.link;

import com.github.auties00.cobalt.calls.engine.control.CallLinkQueryAction;
import com.github.auties00.cobalt.wire.linked.call.CallLinkMedia;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import com.github.auties00.cobalt.calls.signaling.CallMessage;
import com.github.auties00.cobalt.calls.signaling.SignalingType;

/**
 * Represents a {@code <link_query>} signal, a request to resolve an existing call link token.
 *
 * <p>A link query request asks the relay to expand a call link {@link #token() token} into its metadata
 * before the local user joins or edits it. It pins the {@link #media() media kind} the caller intends to
 * use so the relay can confirm it matches the link's configuration, and optionally carries an
 * {@link #action() action} selecting a passive preview versus an edit lookup, plus the
 * {@link #linkCreator() creator} and {@link #linkCreatorPn() creator phone number} hints when known.
 * The relay answers on a {@link LinkQueryAck}, which surfaces the resolved link metadata.
 *
 * <p>Like the other call link control requests, {@code link_query} carries no universal
 * {@code call-id}/{@code call-creator} header: it is addressed to the {@code call} service rather than
 * to a peer.
 *
 * <p>On the wire the element is {@code <link_query token="..." media="<type>" action="preview"
 * link_creator="..." link_creator_pn="..."/>}. The {@code action} attribute carries the {@code preview}
 * or {@code link_edit} verb.
 *
 * @param token         the call link token to resolve; never {@code null}
 * @param media         the media kind the caller intends to use; never {@code null}
 * @param action        the query action verb ({@code preview}/{@code link_edit}), if present
 * @param linkCreator   the link creator's device JID hint, if known
 * @param linkCreatorPn the link creator's phone number JID hint, if known
 * @see SignalingType#LINK_QUERY
 * @see LinkQueryAck
 */
public record LinkQueryStanza(String token,
                              CallLinkMedia media,
                              Optional<String> action,
                              Optional<Jid> linkCreator,
                              Optional<Jid> linkCreatorPn) implements CallMessage {
    /**
     * The wire element tag for a link query signal.
     */
    public static final String ELEMENT = "link_query";

    /**
     * The wire attribute naming the call link token.
     */
    private static final String TOKEN_ATTRIBUTE = "token";

    /**
     * The wire attribute naming the media kind.
     */
    private static final String MEDIA_ATTRIBUTE = "media";

    /**
     * The wire attribute naming the query action verb.
     */
    private static final String ACTION_ATTRIBUTE = "action";

    /**
     * The wire attribute naming the link creator's device JID hint.
     */
    private static final String LINK_CREATOR_ATTRIBUTE = "link_creator";

    /**
     * The wire attribute naming the link creator's phone number JID hint.
     */
    private static final String LINK_CREATOR_PN_ATTRIBUTE = "link_creator_pn";

    /**
     * Validates the record components.
     *
     * @throws NullPointerException if any component is {@code null}
     */
    public LinkQueryStanza {
        Objects.requireNonNull(token, "token cannot be null");
        Objects.requireNonNull(media, "media cannot be null");
        Objects.requireNonNull(action, "action cannot be null");
        Objects.requireNonNull(linkCreator, "linkCreator cannot be null");
        Objects.requireNonNull(linkCreatorPn, "linkCreatorPn cannot be null");
    }

    /**
     * Returns a link query signal carrying only the token and the intended media kind.
     *
     * <p>The action and creator hints are left absent, producing the passive resolve a joiner issues
     * when it follows a call link URL.
     *
     * @param token the call link token to resolve
     * @param media the media kind the caller intends to use
     * @return the link query signal
     * @throws NullPointerException if {@code token} or {@code media} is {@code null}
     */
    public static LinkQueryStanza of(String token, CallLinkMedia media) {
        return new LinkQueryStanza(token, media, Optional.empty(), Optional.empty(), Optional.empty());
    }

    /**
     * {@inheritDoc}
     *
     * @return {@linkplain Optional#empty() empty}; a call link query is keyed by its link token, not a
     *         {@code call-id} header
     */
    @Override
    public Optional<String> callId() {
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@linkplain Optional#empty() empty}; a call link query carries no {@code call-creator} header
     */
    @Override
    public Optional<Jid> callCreator() {
        return Optional.empty();
    }

    @Override
    public SignalingType type() {
        return SignalingType.LINK_QUERY;
    }

    /**
     * Builds the {@code <link_query token media action link_creator link_creator_pn/>} action stanza.
     *
     * <p>The {@code action}, {@code link_creator}, and {@code link_creator_pn} attributes are omitted
     * when their backing components are absent.
     *
     * @return the link query action stanza
     */
    @Override
    public Stanza toStanza() {
        return new StanzaBuilder()
                .description(ELEMENT)
                .attribute(TOKEN_ATTRIBUTE, token)
                .attribute(MEDIA_ATTRIBUTE, media.wireValue())
                .attribute(ACTION_ATTRIBUTE, action.orElse(null), action.isPresent())
                .attribute(LINK_CREATOR_ATTRIBUTE, linkCreator.orElse(null), linkCreator.isPresent())
                .attribute(LINK_CREATOR_PN_ATTRIBUTE, linkCreatorPn.orElse(null), linkCreatorPn.isPresent())
                .build();
    }

    /**
     * Decodes a {@code <link_query>} action stanza into a {@link LinkQueryStanza}.
     *
     * <p>The {@code action} attribute is decoded through {@link CallLinkQueryAction#ofWire(String)} and
     * re serialized to its {@linkplain CallLinkQueryAction#wireValue() canonical wire literal}, so only
     * the {@code preview} and {@code link_edit} verbs the taxonomy names survive the round trip; an
     * absent or unrecognized {@code action} decodes to an absent action rather than an opaque string.
     *
     * @param stanza the {@code <link_query>} stanza
     * @return the decoded link query signal
     * @throws NullPointerException   if {@code stanza} is {@code null}
     * @throws NoSuchElementException if the required {@code token} attribute is absent or the
     *                                {@code media} attribute is absent or unrecognized
     */
    public static LinkQueryStanza of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        var token = stanza.getRequiredAttributeAsString(TOKEN_ATTRIBUTE);
        var media = CallLinkMedia.ofWire(stanza.getAttributeAsString(MEDIA_ATTRIBUTE).orElse(null))
                .orElseThrow(() -> new NoSuchElementException("link_query is missing a recognized media attribute"));
        var action = CallLinkQueryAction.ofWire(stanza.getAttributeAsString(ACTION_ATTRIBUTE).orElse(null))
                .map(CallLinkQueryAction::wireValue);
        var linkCreator = stanza.getAttributeAsJid(LINK_CREATOR_ATTRIBUTE);
        var linkCreatorPn = stanza.getAttributeAsJid(LINK_CREATOR_PN_ATTRIBUTE);
        return new LinkQueryStanza(token, media, action, linkCreator, linkCreatorPn);
    }
}
