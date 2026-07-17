package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * The audience-picker contents shown while a merchant chooses who a WhatsApp
 * Business advertisement will reach.
 *
 * <p>When a merchant opens the audience step of a "Click-to-WhatsApp" ad (a paid
 * promotion that opens a chat with the business when tapped), the server returns
 * everything the picker needs: a set of suggested ready-made audiences for the
 * chosen objective and budget, a default targeting specification to pre-fill
 * when the merchant has not picked one, and the audiences the merchant has
 * previously saved and can reapply. This model collects those three parts.
 *
 * <p>{@link #suggestedAudiences()} lists the ready-made audiences offered for the
 * ad; {@link #templateTargetingSpec()} is the default serialized targeting to
 * pre-fill; and {@link #savedAudiences()} lists the merchant's reusable saved
 * audiences.
 */
@ProtobufMessage(name = "BusinessAdAudienceSection")
public final class BusinessAdAudienceSection {
    /**
     * Ready-made audiences the server suggests for the ad, in the order it
     * returned them. Never {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final List<BusinessAdSavedAudience> suggestedAudiences;

    /**
     * Default serialized targeting specification the picker pre-fills when no
     * audience is chosen, with placement details stripped. Empty when the server
     * omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String templateTargetingSpec;

    /**
     * Audiences the merchant has previously saved and can reapply, in the order
     * the server returned them. Never {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    final List<BusinessAdSavedAudience> savedAudiences;

    /**
     * Constructs a new {@code BusinessAdAudienceSection}. A {@code null}
     * {@code suggestedAudiences} or {@code savedAudiences} is coerced to an empty
     * list, and {@code templateTargetingSpec} may be {@code null} when the server
     * omitted it.
     *
     * @param suggestedAudiences    the suggested audiences; {@code null} treated as empty
     * @param templateTargetingSpec the default targeting specification, or {@code null}
     * @param savedAudiences        the merchant's saved audiences; {@code null} treated as empty
     */
    BusinessAdAudienceSection(List<BusinessAdSavedAudience> suggestedAudiences,
                              String templateTargetingSpec,
                              List<BusinessAdSavedAudience> savedAudiences) {
        this.suggestedAudiences = suggestedAudiences == null ? List.of() : suggestedAudiences;
        this.templateTargetingSpec = templateTargetingSpec;
        this.savedAudiences = savedAudiences == null ? List.of() : savedAudiences;
    }

    /**
     * Returns the ready-made audiences the server suggests for the ad.
     *
     * @return an unmodifiable view of the suggested audiences; never
     *         {@code null}, possibly empty
     */
    public List<BusinessAdSavedAudience> suggestedAudiences() {
        return Collections.unmodifiableList(suggestedAudiences);
    }

    /**
     * Returns the default serialized targeting specification the picker
     * pre-fills.
     *
     * @return the default targeting specification, or empty when the server
     *         omitted it
     */
    public Optional<String> templateTargetingSpec() {
        return Optional.ofNullable(templateTargetingSpec);
    }

    /**
     * Returns the audiences the merchant has previously saved and can reapply.
     *
     * @return an unmodifiable view of the saved audiences; never {@code null},
     *         possibly empty
     */
    public List<BusinessAdSavedAudience> savedAudiences() {
        return Collections.unmodifiableList(savedAudiences);
    }
}
