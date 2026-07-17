package com.github.auties00.cobalt.wire.stanza.iq.group;

/**
 * Selects the dispatch mode for an {@link IqQueryGroupInviteProfilePicRequest}.
 *
 * <p>{@link #INVITE_LINK} previews the group avatar attached to a {@code chat.whatsapp.com/...}
 * share, and {@link #INVITE_MESSAGE} previews the avatar attached to an in-chat invite stanza that
 * carries an {@code add_request} payload. The two modes ship different IQ envelopes: the link mode
 * targets the group JID under {@code w:g2}, while the message mode targets the
 * {@code s.whatsapp.net} server under {@code w:profile:picture}. The request builder picks between
 * them based on this enum.
 *
 * @implNote
 * This implementation models the public and private split as a single
 * {@link IqQueryGroupInviteProfilePicRequest} class with one enum-driven branch rather than two
 * separate request types.
 */
public enum IqQueryGroupInviteProfilePicMode {
    /**
     * Selects the invite-link variant.
     *
     * <p>Applies when the caller holds a bare invite code (the
     * {@code https://chat.whatsapp.com/<code>} share format) and wants to preview the group avatar
     * before deciding whether to join. The request emits
     * {@code <iq xmlns="w:g2" type="get" to="<group-jid>"><picture invite="<code>"/></iq>}.
     */
    INVITE_LINK,

    /**
     * Selects the invite-message variant.
     *
     * <p>Applies when the caller has received an in-chat invite stanza with an admin JID and
     * expiration timestamp (as embedded in a group-invite-message attachment) and wants to preview
     * the group avatar before accepting. The request emits
     * {@code <iq xmlns="w:profile:picture" type="get" to="s.whatsapp.net" target="<group-jid>"><picture><add_request code expiration admin/></picture></iq>}.
     */
    INVITE_MESSAGE
}
