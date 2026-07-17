package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * A reusable audience a merchant has saved for WhatsApp Business advertisements.
 *
 * <p>When a merchant assembles the targeting for a "Click-to-WhatsApp" ad (a paid
 * promotion that opens a chat with the business when tapped), they may save that
 * targeting under a name so it can be applied to future ads without rebuilding
 * it. The same saved audience can later be renamed or have its targeting
 * revised. This model is that saved audience as the server echoes it back after
 * a save or an edit: the name shown to the merchant, the identifier used to
 * apply or revise it later, the serialized targeting it stands for, and whether
 * it falls under the European Union advertising-transparency rules that require
 * extra disclosure.
 *
 * <p>{@link #id()} is the handle the merchant reuses to apply, rename, or revise
 * the audience; {@link #name()} is the display name; {@link #targetingSpec()} is
 * the serialized targeting the audience materialises; and
 * {@link #subjectToEuComplianceRules()} reports whether the targeting triggers
 * the EU advertising-transparency obligations.
 */
@ProtobufMessage(name = "BusinessAdSavedAudience")
public final class BusinessAdSavedAudience {
    /**
     * Display name of the saved audience shown to the merchant. Empty when the
     * server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String name;

    /**
     * Server-issued identifier of the saved audience. This is the handle used to
     * apply, rename, or revise the audience; it is a numeric advertising
     * identifier, not a WhatsApp address. Empty when the server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String id;

    /**
     * Serialized targeting specification the saved audience stands for, with
     * placement details stripped. Exposed as the raw server-defined string
     * because its structure is not modelled. Empty when the server omitted it.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String targetingSpec;

    /**
     * Whether the audience's targeting triggers the European Union
     * advertising-transparency obligations that require extra disclosure.
     * Reported by the server; {@code false} when the server omitted it.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.BOOL)
    final boolean subjectToEuComplianceRules;

    /**
     * Constructs a new {@code BusinessAdSavedAudience}. The reference arguments
     * may be {@code null} when the server omitted them.
     *
     * @param name                       the display name, or {@code null}
     * @param id                         the audience identifier, or {@code null}
     * @param targetingSpec              the serialized targeting, or {@code null}
     * @param subjectToEuComplianceRules whether the audience is subject to the EU rules
     */
    BusinessAdSavedAudience(String name, String id, String targetingSpec, boolean subjectToEuComplianceRules) {
        this.name = name;
        this.id = id;
        this.targetingSpec = targetingSpec;
        this.subjectToEuComplianceRules = subjectToEuComplianceRules;
    }

    /**
     * Returns the display name of the saved audience.
     *
     * @return the display name, or empty when the server omitted it
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the server-issued identifier of the saved audience.
     *
     * @return the audience id, or empty when the server omitted it
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Returns the serialized targeting specification the saved audience stands
     * for.
     *
     * @return the targeting specification, or empty when the server omitted it
     */
    public Optional<String> targetingSpec() {
        return Optional.ofNullable(targetingSpec);
    }

    /**
     * Returns whether the audience's targeting triggers the European Union
     * advertising-transparency obligations.
     *
     * @return {@code true} when the audience is subject to the EU rules,
     *         {@code false} otherwise
     */
    public boolean subjectToEuComplianceRules() {
        return subjectToEuComplianceRules;
    }
}
