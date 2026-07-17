package com.github.auties00.cobalt.wire.linked.business.ai;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;
import java.util.Optional;

/**
 * Input model describing a WhatsApp Business AI agent lead-generation flow to
 * create or update.
 *
 * <p>A lead-generation flow asks an interested customer for a small set of
 * details at a chosen moment of the conversation. This model carries the
 * {@link #momentType() moment type} selecting when the flow triggers, an
 * optional {@link #customMoment() custom moment} refining it, and the ordered
 * list of {@link #fields() capture fields} the flow collects. When the flow
 * already exists, its {@link #id() server-assigned identifier} is present; a
 * creation leaves it unset.
 */
@ProtobufMessage(name = "AiLeadGenFlowInput")
public final class AiLeadGenFlowInput {
    /**
     * Free-text custom moment refining when the flow triggers. Empty when the
     * flow uses only its {@link #momentType() moment type}.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String customMoment;

    /**
     * Server-defined discriminator selecting the conversation moment at which
     * the flow triggers. Empty when the server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String momentType;

    /**
     * Capture fields the flow collects, in the order they are asked. Never
     * {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    final List<BusinessAiLeadGenField> fields;

    /**
     * Server-assigned identifier of the flow being updated. Empty on a
     * creation, present on an update.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String id;

    /**
     * Constructs a new {@code AiLeadGenFlowInput}. A {@code null} {@code fields}
     * is coerced to an empty list; every other argument may be {@code null} to
     * leave the corresponding field unset.
     *
     * @param customMoment the custom moment, or {@code null}
     * @param momentType   the moment-type discriminator, or {@code null}
     * @param fields       the capture fields; {@code null} treated as empty
     * @param id           the flow identifier, or {@code null} on a creation
     */
    AiLeadGenFlowInput(String customMoment, String momentType, List<BusinessAiLeadGenField> fields, String id) {
        this.customMoment = customMoment;
        this.momentType = momentType;
        this.fields = fields == null ? List.of() : List.copyOf(fields);
        this.id = id;
    }

    /**
     * Returns the custom moment refining when the flow triggers.
     *
     * @return an {@link Optional} carrying the custom moment, or empty when unset
     */
    public Optional<String> customMoment() {
        return Optional.ofNullable(customMoment);
    }

    /**
     * Returns the discriminator selecting the conversation moment at which the
     * flow triggers.
     *
     * @return an {@link Optional} carrying the moment type, or empty when unset
     */
    public Optional<String> momentType() {
        return Optional.ofNullable(momentType);
    }

    /**
     * Returns the capture fields the flow collects.
     *
     * @return an unmodifiable view of the capture fields; never {@code null},
     *         possibly empty
     */
    public List<BusinessAiLeadGenField> fields() {
        return fields;
    }

    /**
     * Returns the server-assigned identifier of the flow being updated.
     *
     * @return an {@link Optional} carrying the flow id, or empty on a creation
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }
}
