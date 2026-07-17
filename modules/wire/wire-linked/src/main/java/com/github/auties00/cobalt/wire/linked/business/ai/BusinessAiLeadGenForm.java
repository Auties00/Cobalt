package com.github.auties00.cobalt.wire.linked.business.ai;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * One lead-capture flow of a WhatsApp Business AI agent.
 *
 * <p>A merchant can configure the auto-reply assistant to collect contact
 * details from interested customers: at a chosen moment in the conversation
 * the assistant offers a short form, and each customer who fills it in becomes
 * a captured lead. This model is one such configured flow together with the
 * leads it has gathered.
 *
 * <p>{@link #moment()} carries the merchant's custom prompt offered to the
 * customer, and {@link #momentType()} discriminates when in the conversation
 * it is offered. {@link #fields()} lists the details the flow may ask for.
 * {@link #capturedLeads()} lists the leads gathered so far, and
 * {@link #hasUnseenLeads()} together with {@link #totalLeadCount()} summarise
 * the lead inbox.
 */
@ProtobufMessage(name = "BusinessAiLeadGenForm")
public final class BusinessAiLeadGenForm {
    /**
     * Server-issued identifier of this lead-capture flow. This is the handle
     * used to update, delete, or refer to the flow; it is not a WhatsApp
     * address. Empty when the server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String id;

    /**
     * Merchant's custom prompt offered to the customer when the flow runs.
     * Empty when the server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String moment;

    /**
     * Server-defined marker discriminating when in the conversation the flow
     * is offered. The full value set is not recoverable from the WhatsApp
     * client, so the raw marker is exposed as a string. Empty when the server
     * omitted it.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String momentType;

    /**
     * Details the flow may ask the customer for, in the order the server
     * returned them. Never {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    final List<BusinessAiLeadGenField> fields;

    /**
     * Leads gathered by the flow so far, in the order the server returned
     * them. Never {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    final List<BusinessAiCapturedLead> capturedLeads;

    /**
     * Whether the flow has captured leads the merchant has not yet seen in the
     * lead inbox.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.BOOL)
    final boolean hasUnseenLeads;

    /**
     * Total number of leads the flow has captured, as reported by the server.
     * This may exceed the size of {@link #capturedLeads()} when the server
     * returns only a page of leads.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.INT64)
    final long totalLeadCount;

    /**
     * Constructs a new {@code BusinessAiLeadGenForm}. Each {@code null} list
     * argument is coerced to an empty list, and the scalar reference arguments
     * may be {@code null} when the server omitted them.
     *
     * @param id             the flow identifier, or {@code null}
     * @param moment         the merchant's custom prompt, or {@code null}
     * @param momentType     the moment-kind marker, or {@code null}
     * @param fields         the capture fields; {@code null} treated as empty
     * @param capturedLeads  the gathered leads; {@code null} treated as empty
     * @param hasUnseenLeads whether the flow has unseen captured leads
     * @param totalLeadCount the total number of captured leads
     */
    BusinessAiLeadGenForm(String id,
                          String moment,
                          String momentType,
                          List<BusinessAiLeadGenField> fields,
                          List<BusinessAiCapturedLead> capturedLeads,
                          boolean hasUnseenLeads,
                          long totalLeadCount) {
        this.id = id;
        this.moment = moment;
        this.momentType = momentType;
        this.fields = fields == null ? List.of() : fields;
        this.capturedLeads = capturedLeads == null ? List.of() : capturedLeads;
        this.hasUnseenLeads = hasUnseenLeads;
        this.totalLeadCount = totalLeadCount;
    }

    /**
     * Returns the server-issued identifier of this lead-capture flow.
     *
     * @return the flow id, or empty when the server omitted it
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Returns the merchant's custom prompt offered to the customer.
     *
     * @return the custom prompt, or empty when the server omitted it
     */
    public Optional<String> moment() {
        return Optional.ofNullable(moment);
    }

    /**
     * Returns the marker discriminating when the flow is offered.
     *
     * @return the moment-kind marker, or empty when the server omitted it
     */
    public Optional<String> momentType() {
        return Optional.ofNullable(momentType);
    }

    /**
     * Returns the details the flow may ask the customer for.
     *
     * @return an unmodifiable view of the capture fields; never {@code null},
     *         possibly empty
     */
    public List<BusinessAiLeadGenField> fields() {
        return Collections.unmodifiableList(fields);
    }

    /**
     * Returns the leads gathered by the flow so far.
     *
     * @return an unmodifiable view of the captured leads; never {@code null},
     *         possibly empty
     */
    public List<BusinessAiCapturedLead> capturedLeads() {
        return Collections.unmodifiableList(capturedLeads);
    }

    /**
     * Returns whether the flow has captured leads the merchant has not yet
     * seen.
     *
     * @return {@code true} when there are unseen leads, {@code false} otherwise
     */
    public boolean hasUnseenLeads() {
        return hasUnseenLeads;
    }

    /**
     * Returns the total number of leads the flow has captured.
     *
     * @return the total lead count, possibly greater than the number of leads
     *         in {@link #capturedLeads()}
     */
    public long totalLeadCount() {
        return totalLeadCount;
    }
}
