package com.github.auties00.cobalt.wire.stanza.smax.support;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.reporting.SupportContactFormContextFlow;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

/**
 * Submits the user's free-form support contact form as an outbound {@code smax_id=3} IQ.
 *
 * <p>The request carries the textarea description plus a set of optional topic, debug, crashlog
 * and context-flow fields, and is dispatched against the {@code fb:thrift_iq} relay. The relay's
 * reply is parsed by {@link SmaxContactFormResponse}.
 *
 * @implNote
 * This implementation flattens the WA Web mixin chain into a single {@link StanzaBuilder} that pins
 * {@code xmlns="fb:thrift_iq"}, {@code smax_id=3}, {@code to=Jid.userServer()} and
 * {@code type="set"}; each optional child is attached only when the corresponding field is
 * non-{@code null}.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutSupportContactFormRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutSupportHackBaseIQSetRequestMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutSupportBaseIQSetRequestMixin")
public final class SmaxContactFormRequest implements SmaxStanza.Request {
    /**
     * Holds the optional {@code from} JID forwarded into the IQ envelope.
     *
     * <p>{@code null} lets the relay infer the sender from the connection.
     */
    private final Jid iqFrom;

    /**
     * Holds the textarea content the user typed into the support form.
     *
     * <p>Routed into the {@code <description/>} child; never {@code null}.
     */
    private final String descriptionElementValue;

    /**
     * Holds the optional pre-filled topic title routed into the {@code <topic/>} child.
     */
    private final String topicElementValue;

    /**
     * Holds the optional canonical topic id routed into the {@code <topic_id/>} child.
     */
    private final String topicIdElementValue;

    /**
     * Holds the optional debug-information JSON blob routed into the
     * {@code <debug_information_json/>} child.
     */
    private final String debugInformationJsonElementValue;

    /**
     * Holds the optional handle returned by a prior crashlog upload, routed into the
     * {@code <uploaded_logs_id/>} child.
     */
    private final String uploadedLogsIdElementValue;

    /**
     * Holds the optional {@code context_flow} marker routed into
     * {@code <additional_attributes context_flow="..."/>}, naming the surface that opened the
     * support form.
     */
    private final SupportContactFormContextFlow additionalAttributesContextFlow;

    /**
     * Constructs a contact-form request from the supplied scalar fields.
     *
     * <p>Only {@code descriptionElementValue} is required; a {@code null} value for any other
     * argument omits the corresponding child or attribute.
     *
     * @param iqFrom                           the optional sender JID; may be {@code null}
     * @param descriptionElementValue          the free-form description; never {@code null}
     * @param topicElementValue                the optional topic title; may be {@code null}
     * @param topicIdElementValue              the optional topic id; may be {@code null}
     * @param debugInformationJsonElementValue the optional debug-information JSON blob; may be
     *                                         {@code null}
     * @param uploadedLogsIdElementValue       the optional uploaded-crashlog handle; may be
     *                                         {@code null}
     * @param additionalAttributesContextFlow  the optional context-flow marker; may be
     *                                         {@code null}
     * @throws NullPointerException if {@code descriptionElementValue} is {@code null}
     */
    public SmaxContactFormRequest(Jid iqFrom,
                   String descriptionElementValue,
                   String topicElementValue,
                   String topicIdElementValue,
                   String debugInformationJsonElementValue,
                   String uploadedLogsIdElementValue,
                   SupportContactFormContextFlow additionalAttributesContextFlow) {
        this.iqFrom = iqFrom;
        this.descriptionElementValue = Objects.requireNonNull(descriptionElementValue,
                "descriptionElementValue cannot be null");
        this.topicElementValue = topicElementValue;
        this.topicIdElementValue = topicIdElementValue;
        this.debugInformationJsonElementValue = debugInformationJsonElementValue;
        this.uploadedLogsIdElementValue = uploadedLogsIdElementValue;
        this.additionalAttributesContextFlow = additionalAttributesContextFlow;
    }

    /**
     * Returns the optional {@code from} JID forwarded into the IQ envelope.
     *
     * <p>Empty when the relay is left to infer the sender from the connection.
     *
     * @return an {@link Optional} carrying the JID, or empty when omitted
     */
    public Optional<Jid> iqFrom() {
        return Optional.ofNullable(iqFrom);
    }

    /**
     * Returns the user-typed support description.
     *
     * @return the description; never {@code null}
     */
    public String descriptionElementValue() {
        return descriptionElementValue;
    }

    /**
     * Returns the optional topic title.
     *
     * @return an {@link Optional} carrying the title, or empty when omitted
     */
    public Optional<String> topicElementValue() {
        return Optional.ofNullable(topicElementValue);
    }

    /**
     * Returns the optional canonical topic id.
     *
     * @return an {@link Optional} carrying the id, or empty when omitted
     */
    public Optional<String> topicIdElementValue() {
        return Optional.ofNullable(topicIdElementValue);
    }

    /**
     * Returns the optional debug-information JSON blob.
     *
     * @return an {@link Optional} carrying the JSON blob, or empty when omitted
     */
    public Optional<String> debugInformationJsonElementValue() {
        return Optional.ofNullable(debugInformationJsonElementValue);
    }

    /**
     * Returns the optional uploaded-crashlog handle.
     *
     * @return an {@link Optional} carrying the handle, or empty when omitted
     */
    public Optional<String> uploadedLogsIdElementValue() {
        return Optional.ofNullable(uploadedLogsIdElementValue);
    }

    /**
     * Returns the optional {@code context_flow} marker.
     *
     * @return an {@link Optional} carrying the marker, or empty when omitted
     */
    public Optional<SupportContactFormContextFlow> additionalAttributesContextFlow() {
        return Optional.ofNullable(additionalAttributesContextFlow);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Emits the {@code <iq>} envelope with the mandatory {@code <description/>} child and any
     * optional children whose backing field is set.
     *
     * @implNote
     * This implementation appends each optional child only when the matching field is
     * non-{@code null} and forwards {@link #iqFrom} into the envelope's {@code from} attribute
     * through {@link StanzaBuilder#attribute(String, com.github.auties00.cobalt.wire.core.jid.JidProvider)},
     * which accepts a {@code null} JID.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutSupportContactFormRequest",
            exports = "makeContactFormRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var children = new ArrayList<Stanza>();
        children.add(new StanzaBuilder()
                .description("description")
                .content(descriptionElementValue)
                .build());
        if (topicElementValue != null) {
            children.add(new StanzaBuilder()
                    .description("topic")
                    .content(topicElementValue)
                    .build());
        }
        if (topicIdElementValue != null) {
            children.add(new StanzaBuilder()
                    .description("topic_id")
                    .content(topicIdElementValue)
                    .build());
        }
        if (debugInformationJsonElementValue != null) {
            children.add(new StanzaBuilder()
                    .description("debug_information_json")
                    .content(debugInformationJsonElementValue)
                    .build());
        }
        if (uploadedLogsIdElementValue != null) {
            children.add(new StanzaBuilder()
                    .description("uploaded_logs_id")
                    .content(uploadedLogsIdElementValue)
                    .build());
        }
        if (additionalAttributesContextFlow != null) {
            children.add(new StanzaBuilder()
                    .description("additional_attributes")
                    .attribute("context_flow", additionalAttributesContextFlow.wireValue())
                    .build());
        }
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "fb:thrift_iq")
                .attribute("smax_id", 3)
                .attribute("from", iqFrom)
                .attribute("to", Jid.userServer())
                .attribute("type", "set")
                .content(children);
    }

    /**
     * Compares this request to another for value equality across all fields.
     *
     * @param obj the object to compare against; may be {@code null}
     * @return {@code true} when {@code obj} is a {@link SmaxContactFormRequest} with equal fields
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxContactFormRequest) obj;
        return Objects.equals(this.iqFrom, that.iqFrom)
                && Objects.equals(this.descriptionElementValue, that.descriptionElementValue)
                && Objects.equals(this.topicElementValue, that.topicElementValue)
                && Objects.equals(this.topicIdElementValue, that.topicIdElementValue)
                && Objects.equals(this.debugInformationJsonElementValue, that.debugInformationJsonElementValue)
                && Objects.equals(this.uploadedLogsIdElementValue, that.uploadedLogsIdElementValue)
                && Objects.equals(this.additionalAttributesContextFlow, that.additionalAttributesContextFlow);
    }

    /**
     * Returns a hash code derived from all fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(iqFrom, descriptionElementValue, topicElementValue, topicIdElementValue,
                debugInformationJsonElementValue, uploadedLogsIdElementValue,
                additionalAttributesContextFlow);
    }

    /**
     * Returns a debug string listing every field.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return "SmaxContactFormRequest[iqFrom=" + iqFrom
                + ", descriptionElementValue=" + descriptionElementValue
                + ", topicElementValue=" + topicElementValue
                + ", topicIdElementValue=" + topicIdElementValue
                + ", debugInformationJsonElementValue=" + debugInformationJsonElementValue
                + ", uploadedLogsIdElementValue=" + uploadedLogsIdElementValue
                + ", additionalAttributesContextFlow=" + additionalAttributesContextFlow + ']';
    }
}
