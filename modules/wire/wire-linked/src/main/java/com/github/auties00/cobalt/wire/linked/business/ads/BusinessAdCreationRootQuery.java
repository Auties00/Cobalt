package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * Input model for the opening-screen query of the Click-to-WhatsApp ad
 * creation flow.
 *
 * <p>A Click-to-WhatsApp ad is a paid promotion that opens a chat with the
 * business when tapped. When the merchant enters the ad-creation flow the
 * first screen needs the budget steps the merchant can pick, the
 * platforms the ad may run on, the promoted page, how many advertising
 * accounts are linked, and any onboarding email to confirm. This input
 * carries the parameters the server uses to resolve those values.
 *
 * <p>The {@link #input() creation context} is a typed
 * {@link BusinessAdCreationRootInput} passed to the server-side resolver. The
 * {@link #draftId() draft id} resumes an in-progress draft, the
 * {@link #pageId() page id} names the page being promoted, and the
 * {@link #facebookAccountLinked() Facebook}/
 * {@link #whatsAppAccountLinked() WhatsApp} flags report which kinds of
 * account the user has linked. The
 * {@link #instagramUserIdDoubleWriteEnabled() Instagram user-id
 * double-write flag} mirrors the server-side
 * {@code IGUserIdDoubleWriteEnabled} provider that controls whether the
 * Instagram user id is double-written alongside the Facebook user id.
 */
@ProtobufMessage(name = "BusinessAdCreationRootQuery")
public final class BusinessAdCreationRootQuery {
    /**
     * Creation context passed to the server-side resolver. Unset omits the
     * variable.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final BusinessAdCreationRootInput input;

    /**
     * Draft identifier the flow resumes from. Unset starts a fresh ad
     * with no draft.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String draftId;

    /**
     * Whether the user has a Facebook account linked. Unset omits the
     * variable.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BOOL)
    final Boolean facebookAccountLinked;

    /**
     * Whether the user has a WhatsApp account linked. Unset omits the
     * variable.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.BOOL)
    final Boolean whatsAppAccountLinked;

    /**
     * Identifier of the page being promoted. Unset omits the variable.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String pageId;

    /**
     * Whether the Instagram user id is double-written alongside the
     * Facebook user id by the server-side
     * {@code IGUserIdDoubleWriteEnabled} provider. Unset omits the
     * variable.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.BOOL)
    final Boolean instagramUserIdDoubleWriteEnabled;

    /**
     * Constructs a new {@code BusinessAdCreationRootQuery}. Every argument
     * may be {@code null} to omit the corresponding variable from the
     * request.
     *
     * @param input                             the creation context, or
     *                                          {@code null}
     * @param draftId                           the draft identifier, or
     *                                          {@code null}
     * @param facebookAccountLinked             whether a Facebook account is
     *                                          linked, or {@code null}
     * @param whatsAppAccountLinked             whether a WhatsApp account is
     *                                          linked, or {@code null}
     * @param pageId                            the promoted page identifier,
     *                                          or {@code null}
     * @param instagramUserIdDoubleWriteEnabled whether Instagram user-id
     *                                          double-write is enabled, or
     *                                          {@code null}
     */
    public BusinessAdCreationRootQuery(BusinessAdCreationRootInput input, String draftId, Boolean facebookAccountLinked,
                                       Boolean whatsAppAccountLinked, String pageId,
                                       Boolean instagramUserIdDoubleWriteEnabled) {
        this.input = input;
        this.draftId = draftId;
        this.facebookAccountLinked = facebookAccountLinked;
        this.whatsAppAccountLinked = whatsAppAccountLinked;
        this.pageId = pageId;
        this.instagramUserIdDoubleWriteEnabled = instagramUserIdDoubleWriteEnabled;
    }

    /**
     * Returns the creation context.
     *
     * @return an {@link Optional} carrying the context, or empty when
     *         unset
     */
    public Optional<BusinessAdCreationRootInput> input() {
        return Optional.ofNullable(input);
    }

    /**
     * Returns the draft identifier.
     *
     * @return an {@link Optional} carrying the identifier, or empty when
     *         unset
     */
    public Optional<String> draftId() {
        return Optional.ofNullable(draftId);
    }

    /**
     * Returns whether the user has a Facebook account linked. The
     * accessor mirrors the wire-level tri-state ({@code true},
     * {@code false}, unset).
     *
     * @return the {@code Boolean} value, or {@code null} when unset
     */
    public Boolean facebookAccountLinked() {
        return facebookAccountLinked;
    }

    /**
     * Returns whether the user has a WhatsApp account linked. The
     * accessor mirrors the wire-level tri-state ({@code true},
     * {@code false}, unset).
     *
     * @return the {@code Boolean} value, or {@code null} when unset
     */
    public Boolean whatsAppAccountLinked() {
        return whatsAppAccountLinked;
    }

    /**
     * Returns the identifier of the page being promoted.
     *
     * @return an {@link Optional} carrying the identifier, or empty when
     *         unset
     */
    public Optional<String> pageId() {
        return Optional.ofNullable(pageId);
    }

    /**
     * Returns whether the Instagram user id is double-written alongside
     * the Facebook user id. The accessor mirrors the wire-level
     * tri-state ({@code true}, {@code false}, unset).
     *
     * @return the {@code Boolean} value, or {@code null} when unset
     */
    public Boolean instagramUserIdDoubleWriteEnabled() {
        return instagramUserIdDoubleWriteEnabled;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BusinessAdCreationRootQuery) obj;
        return Objects.equals(input, that.input)
                && Objects.equals(draftId, that.draftId)
                && Objects.equals(facebookAccountLinked, that.facebookAccountLinked)
                && Objects.equals(whatsAppAccountLinked, that.whatsAppAccountLinked)
                && Objects.equals(pageId, that.pageId)
                && Objects.equals(instagramUserIdDoubleWriteEnabled, that.instagramUserIdDoubleWriteEnabled);
    }

    @Override
    public int hashCode() {
        return Objects.hash(input, draftId, facebookAccountLinked, whatsAppAccountLinked,
                pageId, instagramUserIdDoubleWriteEnabled);
    }

    @Override
    public String toString() {
        return "BusinessAdCreationRootQuery[" +
                "input=" + input + ", " +
                "draftId=" + draftId + ", " +
                "facebookAccountLinked=" + facebookAccountLinked + ", " +
                "whatsAppAccountLinked=" + whatsAppAccountLinked + ", " +
                "pageId=" + pageId + ", " +
                "instagramUserIdDoubleWriteEnabled=" + instagramUserIdDoubleWriteEnabled + ']';
    }
}
