package com.github.auties00.cobalt.wire.linked.business.crossposting;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * Per-destination flags reporting whether the caller may link the WhatsApp
 * account to a Meta foreground app.
 *
 * <p>The server reports six flags, one per (destination, prior-link-state) pair:
 * Facebook / Instagram / Reels in the already-linked and not-yet-linked
 * configurations. Each absent flag defaults to {@code false}.
 */
@ProtobufMessage(name = "CrossPostingLinkEligibility")
public final class CrossPostingLinkEligibility {
    /**
     * Whether the caller is eligible to link to an already-linked Facebook
     * account.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    final boolean linkedFacebook;

    /**
     * Whether the caller is eligible to link to an already-linked Instagram
     * account.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
    final boolean linkedInstagram;

    /**
     * Whether the caller is eligible to link to an already-linked Reels
     * surface.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BOOL)
    final boolean linkedReels;

    /**
     * Whether the caller is eligible to link to a not-yet-linked Facebook
     * account.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.BOOL)
    final boolean unlinkedFacebook;

    /**
     * Whether the caller is eligible to link to a not-yet-linked Instagram
     * account.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.BOOL)
    final boolean unlinkedInstagram;

    /**
     * Whether the caller is eligible to link to a not-yet-linked Reels surface.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.BOOL)
    final boolean unlinkedReels;

    /**
     * Constructs a new {@code CrossPostingLinkEligibility}.
     *
     * @param linkedFacebook    eligibility to link to an already-linked
     *                          Facebook account
     * @param linkedInstagram   eligibility to link to an already-linked
     *                          Instagram account
     * @param linkedReels       eligibility to link to an already-linked Reels
     *                          surface
     * @param unlinkedFacebook  eligibility to link to a not-yet-linked Facebook
     *                          account
     * @param unlinkedInstagram eligibility to link to a not-yet-linked Instagram
     *                          account
     * @param unlinkedReels     eligibility to link to a not-yet-linked Reels
     *                          surface
     */
    CrossPostingLinkEligibility(boolean linkedFacebook,
                                boolean linkedInstagram,
                                boolean linkedReels,
                                boolean unlinkedFacebook,
                                boolean unlinkedInstagram,
                                boolean unlinkedReels) {
        this.linkedFacebook = linkedFacebook;
        this.linkedInstagram = linkedInstagram;
        this.linkedReels = linkedReels;
        this.unlinkedFacebook = unlinkedFacebook;
        this.unlinkedInstagram = unlinkedInstagram;
        this.unlinkedReels = unlinkedReels;
    }

    /**
     * Returns whether the caller is eligible to link to an already-linked
     * Facebook account.
     *
     * @return {@code true} when eligible, {@code false} otherwise
     */
    public boolean linkedFacebook() {
        return linkedFacebook;
    }

    /**
     * Returns whether the caller is eligible to link to an already-linked
     * Instagram account.
     *
     * @return {@code true} when eligible, {@code false} otherwise
     */
    public boolean linkedInstagram() {
        return linkedInstagram;
    }

    /**
     * Returns whether the caller is eligible to link to an already-linked
     * Reels surface.
     *
     * @return {@code true} when eligible, {@code false} otherwise
     */
    public boolean linkedReels() {
        return linkedReels;
    }

    /**
     * Returns whether the caller is eligible to link to a not-yet-linked
     * Facebook account.
     *
     * @return {@code true} when eligible, {@code false} otherwise
     */
    public boolean unlinkedFacebook() {
        return unlinkedFacebook;
    }

    /**
     * Returns whether the caller is eligible to link to a not-yet-linked
     * Instagram account.
     *
     * @return {@code true} when eligible, {@code false} otherwise
     */
    public boolean unlinkedInstagram() {
        return unlinkedInstagram;
    }

    /**
     * Returns whether the caller is eligible to link to a not-yet-linked Reels
     * surface.
     *
     * @return {@code true} when eligible, {@code false} otherwise
     */
    public boolean unlinkedReels() {
        return unlinkedReels;
    }
}
