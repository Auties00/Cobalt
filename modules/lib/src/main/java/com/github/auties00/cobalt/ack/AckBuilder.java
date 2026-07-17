package com.github.auties00.cobalt.ack;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Assembles and dispatches the outbound {@code <ack>} stanza shipped in response to an inbound
 * stanza.
 *
 * <p>An instance is obtained through {@link AckSender#ack(AckClass, Stanza)}, bound to a target
 * {@link AckClass} and the inbound {@link Stanza} being acknowledged. The fluent setters layer
 * per-call overrides on top of the per-class defaults, and {@link #send()} resolves the final
 * attribute set, builds the stanza, and dispatches it through the owning {@link AckSender}. The
 * per-class defaults make the common shapes ({@code <ack class="message">},
 * {@code <ack class="notification" type="xxx">}) require zero override calls; the resolution rules
 * for the {@code type} and {@code participant} attributes are spelled out on {@link #resolveType()}
 * and {@link #resolveParticipant(Jid)}.
 *
 * <p>The builder is not thread-safe and is intended for single use: create one instance per ack and
 * call {@link #send()} once.
 *
 * @implNote This implementation collapses four WA Web call shapes ({@code sendAck},
 * {@code buildReceiptAck}, the synthesised nack, and the {@code <meta failure_reason=...>} append on
 * the {@code InvalidProtobuf} path) into a single override matrix so consumers no longer hand-roll a
 * {@link StanzaBuilder} per call site.
 */
@WhatsAppWebModule(moduleName = "WAWebHandleMsgSendAck")
@WhatsAppWebModule(moduleName = "WAWebReceiptAck")
@WhatsAppWebModule(moduleName = "WAWebCreateNackFromStanza")
public final class AckBuilder {
    /**
     * The logger for {@link AckBuilder}.
     */
    private static final System.Logger LOGGER = Log.get(AckBuilder.class);

    /**
     * The {@link AckSender} that dispatches the assembled stanza on {@link #send()}.
     */
    private final AckSender owner;

    /**
     * The class of stanza this ack acknowledges.
     *
     * <p>Drives the {@code class} attribute and the per-class default behaviour of {@code type} and
     * {@code participant} when those attributes are not overridden.
     */
    private final AckClass ackClass;

    /**
     * The inbound stanza being acknowledged.
     *
     * <p>Sources the {@code id} attribute, the {@code to} attribute (from the inbound {@code from}),
     * and the inherited {@code type} and {@code participant} values when the per-class default
     * inherits from the inbound stanza.
     */
    private final Stanza inbound;

    /**
     * Tracks whether {@link #type(String)} has been called.
     *
     * <p>Distinguishes "not set" (fall back to the per-class default) from "explicitly set to
     * {@code null}" (force the {@code type} attribute to be omitted).
     */
    private boolean typeOverrideSet = false;

    /**
     * Holds the explicit {@code type} override.
     *
     * <p>Only meaningful when {@link #typeOverrideSet} is {@code true}. A {@code null} value
     * combined with {@code typeOverrideSet == true} forces the attribute to be omitted.
     */
    private String typeOverride = null;

    /**
     * Tracks whether {@link #participant(Jid)} has been called.
     *
     * <p>Mutually exclusive with {@link #participantIfDifferentSet}.
     */
    private boolean participantOverrideSet = false;

    /**
     * Holds the explicit {@code participant} override.
     *
     * <p>Only meaningful when {@link #participantOverrideSet} is {@code true}.
     */
    private Jid participantOverride = null;

    /**
     * Tracks whether {@link #participantIfDifferent(Jid)} has been called.
     *
     * <p>Distinguishes "not set" from "set to {@code null}" for the if-different branch. Mutually
     * exclusive with {@link #participantOverrideSet}.
     */
    private boolean participantIfDifferentSet = false;

    /**
     * Holds the conditional {@code participant} value, written only when it differs from the
     * resolved {@code to} value.
     *
     * <p>Only meaningful when {@link #participantIfDifferentSet} is {@code true}.
     */
    private Jid participantIfDifferent = null;

    /**
     * Tracks whether {@link #to(Jid)} has been called.
     *
     * <p>Distinguishes "fall back to the inbound stanza's {@code from} attribute" from "the caller
     * forced an explicit {@code to}".
     */
    private boolean toOverrideSet = false;

    /**
     * Holds the explicit {@code to} override.
     *
     * <p>Only meaningful when {@link #toOverrideSet} is {@code true}. When no override is set the
     * {@code to} value defaults to the inbound stanza's {@code from} attribute.
     */
    private Jid toOverride = null;

    /**
     * The optional {@code from} attribute on the outbound ack.
     *
     * <p>Left omitted unless explicitly set by a caller, in which case it is written verbatim. The
     * server fills the attribute in when it is absent.
     *
     * @implNote This implementation diverges from WA Web, which defaults the {@code from} attribute
     * to the local device JID for class {@code message}; Cobalt instead requires the caller to set
     * it (notably the call-receipt path, which forces the local user PN).
     */
    private Jid fromAttribute = null;

    /**
     * The {@link NackReason} that drives the {@code error} attribute, or {@code null} when the
     * stanza is a plain ack.
     */
    private NackReason error = null;

    /**
     * The {@code failure_reason} string carried on a child {@code <meta>} stanza for the
     * {@link NackReason#INVALID_PROTOBUF} nack, or {@code null} when no such child is required.
     */
    private String failureReason = null;

    /**
     * The list of arbitrary child nodes appended to the outbound ack stanza, in insertion order, or
     * {@code null} when no children have been added.
     */
    private List<Stanza> children = null;

    /**
     * Constructs a builder bound to the given {@link AckSender}, stanza class, and inbound stanza.
     *
     * <p>Package-private so instance creation stays centralised on {@link AckSender}; callers obtain
     * a builder through {@link AckSender#ack(AckClass, Stanza)}.
     *
     * @param owner    the {@link AckSender} that dispatches the assembled stanza on {@link #send()}
     * @param ackClass the {@link AckClass} written into the {@code class} attribute
     * @param inbound  the inbound stanza being acknowledged
     */
    AckBuilder(AckSender owner, AckClass ackClass, Stanza inbound) {
        this.owner = owner;
        this.ackClass = ackClass;
        this.inbound = inbound;
    }

    /**
     * Overrides the {@code type} attribute on the outbound ack.
     *
     * <p>An explicit value is written verbatim; a {@code null} value forces the attribute to be
     * omitted even when the per-class default would inherit it from the inbound stanza.
     *
     * @param type the explicit {@code type} value, or {@code null} to omit the attribute entirely
     * @return this builder for chaining
     */
    public AckBuilder type(String type) {
        this.typeOverride = type;
        this.typeOverrideSet = true;
        return this;
    }

    /**
     * Overrides the {@code participant} attribute on the outbound ack.
     *
     * <p>An explicit {@link Jid} is written verbatim; a {@code null} value forces the attribute to
     * be omitted even when the per-class default would inherit it from the inbound stanza. This call
     * is mutually exclusive with {@link #participantIfDifferent(Jid)}: invoking it clears any pending
     * if-different value.
     *
     * @param participant the explicit {@link Jid}, or {@code null} to omit the attribute
     * @return this builder for chaining
     */
    public AckBuilder participant(Jid participant) {
        this.participantOverride = participant;
        this.participantOverrideSet = true;
        this.participantIfDifferent = null;
        this.participantIfDifferentSet = false;
        return this;
    }

    /**
     * Sets the {@code participant} attribute to the given value only when it differs from the
     * resolved {@code to} value.
     *
     * <p>The attribute is omitted when {@code participant} is {@code null} or equal to the resolved
     * {@code to} value. This call is mutually exclusive with {@link #participant(Jid)}: invoking it
     * clears any pending unconditional override.
     *
     * @param participant the candidate {@link Jid}; omitted when {@code null} or equal to the
     *                    resolved {@code to}
     * @return this builder for chaining
     */
    @WhatsAppWebExport(moduleName = "WAWebReceiptAck", exports = "buildReceiptAck",
            adaptation = WhatsAppAdaptation.DIRECT)
    public AckBuilder participantIfDifferent(Jid participant) {
        this.participantIfDifferent = participant;
        this.participantIfDifferentSet = true;
        this.participantOverride = null;
        this.participantOverrideSet = false;
        return this;
    }

    /**
     * Overrides the {@code to} attribute on the outbound ack.
     *
     * <p>When no override is set the {@code to} value defaults to the inbound stanza's {@code from}
     * attribute. An explicit {@link Jid} is written verbatim, which is needed when the target
     * identity differs from the inbound sender. A {@code null} value clears the {@code to} value
     * entirely, in which case {@link #send()} drops the ack as if the inbound {@code from} were
     * missing.
     *
     * @param to the explicit {@link Jid} for the {@code to} attribute, or {@code null} to clear
     * @return this builder for chaining
     */
    public AckBuilder to(Jid to) {
        this.toOverride = to;
        this.toOverrideSet = true;
        return this;
    }

    /**
     * Sets the {@code from} attribute on the outbound ack.
     *
     * <p>Needed only for stanza classes where the client must echo a specific identity back to the
     * server; all other call sites omit {@code from} so the server fills it in.
     *
     * @param from the {@link Jid} to write as the {@code from} attribute, or {@code null} to clear a
     *             previous override
     * @return this builder for chaining
     */
    public AckBuilder from(Jid from) {
        this.fromAttribute = from;
        return this;
    }

    /**
     * Marks the outbound ack as a NACK with the given {@link NackReason}.
     *
     * <p>Writes the integer error code returned by {@link NackReason#code()} into the {@code error}
     * attribute. For {@link NackReason#INVALID_PROTOBUF} the caller should also supply a failure
     * reason via {@link #failureReason(String)} so the server receives the mandatory
     * {@code <meta failure_reason="..."/>} child stanza.
     *
     * @param reason the {@link NackReason} stamped into the {@code error} attribute, or {@code null}
     *               to clear a previous error
     * @return this builder for chaining
     */
    @WhatsAppWebExport(moduleName = "WAWebCreateNackFromStanza",
            exports = "createNackFromStanza", adaptation = WhatsAppAdaptation.DIRECT)
    public AckBuilder error(NackReason reason) {
        this.error = reason;
        return this;
    }

    /**
     * Records the failure-reason hint carried on the {@code <meta failure_reason="..."/>} child stanza
     * of the outbound NACK.
     *
     * <p>The child stanza is emitted by {@link #send()} only when the NACK reason is
     * {@link NackReason#INVALID_PROTOBUF} and a non-{@code null} reason is present; the server logs
     * the value against the offending stanza. A {@code null} value clears any previous failure
     * reason.
     *
     * @param e2eFailureReason the textual failure-reason hint, or {@code null} to clear
     * @return this builder for chaining
     */
    @WhatsAppWebExport(moduleName = "WAWebCreateNackFromStanza",
            exports = "createNackFromStanza", adaptation = WhatsAppAdaptation.DIRECT)
    public AckBuilder failureReason(String e2eFailureReason) {
        this.failureReason = e2eFailureReason;
        return this;
    }

    /**
     * Appends an arbitrary child stanza to the outbound ack stanza.
     *
     * <p>Multiple calls preserve insertion order. A {@code null} argument is ignored.
     *
     * @param child the {@link Stanza} to append, or {@code null} to skip
     * @return this builder for chaining
     */
    public AckBuilder child(Stanza child) {
        if (child == null) {
            return this;
        }
        if (children == null) {
            children = new ArrayList<>(1);
        }
        children.add(child);
        return this;
    }

    /**
     * Builds the outbound ack stanza and dispatches it through the owning {@link AckSender}.
     *
     * <p>Resolves the {@code id} from the inbound stanza and the {@code to} value from the
     * {@link #to(Jid)} override or, failing that, the inbound {@code from} attribute. When either
     * the {@code id} or the {@code to} value is missing the ack is silently dropped and the method
     * returns {@code false}. Otherwise the per-class defaults for {@code type} and
     * {@code participant} are resolved, the {@code error} attribute and any
     * {@code <meta failure_reason=...>} and arbitrary child nodes are appended, and the assembled
     * stanza is shipped fire-and-forget.
     *
     * @implNote This implementation resolves the per-class defaults before applying the fluent
     * overrides. For {@link NackReason#INVALID_PROTOBUF} without a failure reason the {@code <meta>}
     * child is omitted and the bare ack is still shipped, matching WA Web's missing-failure-reason
     * fallback.
     *
     * @return {@code true} when the stanza was dispatched, {@code false} when it was dropped due to
     *         a missing {@code id} or {@code to} on the inbound stanza
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgSendAck", exports = "sendAck",
            adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WAWebReceiptAck", exports = "buildReceiptAck",
            adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WAWebCreateNackFromStanza",
            exports = "createNackFromStanza", adaptation = WhatsAppAdaptation.DIRECT)
    public boolean send() {
        var id = inbound.getAttributeAsString("id", null);
        var to = toOverrideSet ? toOverride : inbound.getAttributeAsJid("from").orElse(null);
        if (id == null || to == null) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "ack drop class={0}: missing id or to", ackClass);
            }
            return false;
        }

        var resolvedType = resolveType();
        var resolvedParticipant = resolveParticipant(to);

        var builder = new StanzaBuilder()
                .description("ack")
                .attribute("id", id)
                .attribute("class", ackClass.wireToken())
                .attribute("to", to)
                .attribute("type", resolvedType)
                .attribute("from", fromAttribute)
                .attribute("participant", resolvedParticipant);

        if (error != null) {
            builder.attribute("error", error.code());
        }

        var metaChild = buildMetaChild();
        if (metaChild != null) {
            builder.content(metaChild);
        }
        if (children != null) {
            for (var child : children) {
                builder.content(child);
            }
        }

        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "ack send class={0} id={1} to={2} type={3} error={4}",
                    ackClass, id, to, resolvedType, error);
        }

        owner.dispatch(builder.build());
        return true;
    }

    /**
     * Resolves the value to write for the {@code type} attribute, applying the per-class default
     * when no override has been specified.
     *
     * <p>When {@link #type(String)} has been called the override value is returned verbatim,
     * including {@code null} to omit the attribute. Otherwise the value of the {@code type}
     * attribute on the inbound stanza is copied verbatim when present, and {@code null} is returned
     * when it is absent.
     *
     * @return the resolved {@code type} value, or {@code null} to omit the attribute
     */
    private String resolveType() {
        if (typeOverrideSet) {
            return typeOverride;
        }
        return inbound.getAttributeAsString("type", null);
    }

    /**
     * Resolves the value to write for the {@code participant} attribute, applying the per-class
     * default when no override has been specified.
     *
     * <p>When an if-different value is pending it is returned only if it is non-{@code null} and
     * differs from {@code to}, otherwise {@code null} is returned. When an unconditional override is
     * pending it is returned verbatim. With no override the per-class default applies:
     * {@link AckClass#MESSAGE} inherits the inbound {@code participant};
     * {@link AckClass#RECEIPT} inherits it but drops the attribute when the inherited value equals
     * {@code to}; {@link AckClass#NOTIFICATION} and {@link AckClass#CALL} omit it.
     *
     * @param to the resolved {@code to} value, used by the {@link AckClass#RECEIPT} default to drop
     *           the attribute when it would equal {@code to}
     * @return the resolved {@code participant} value, or {@code null} to omit the attribute
     */
    private Jid resolveParticipant(Jid to) {
        if (participantIfDifferentSet) {
            var candidate = participantIfDifferent;
            if (candidate == null || Objects.equals(candidate, to)) {
                return null;
            }
            return candidate;
        }
        if (participantOverrideSet) {
            return participantOverride;
        }
        return switch (ackClass) {
            case MESSAGE -> inbound.getAttributeAsJid("participant").orElse(null);
            case RECEIPT -> {
                var inherited = inbound.getAttributeAsJid("participant").orElse(null);
                yield inherited != null && !Objects.equals(inherited, to) ? inherited : null;
            }
            case NOTIFICATION, CALL -> null;
        };
    }

    /**
     * Builds the optional {@code <meta failure_reason="..."/>} child stanza.
     *
     * <p>Returns {@code null} unless the NACK reason is {@link NackReason#INVALID_PROTOBUF} and a
     * non-{@code null} failure reason was supplied via {@link #failureReason(String)}, in which case
     * the value is written into the {@code failure_reason} attribute of the child stanza.
     *
     * @return the {@code <meta>} child, or {@code null} when no such child is needed
     */
    private Stanza buildMetaChild() {
        if (error != NackReason.INVALID_PROTOBUF || failureReason == null) {
            return null;
        }
        return new StanzaBuilder()
                .description("meta")
                .attribute("failure_reason", failureReason)
                .build();
    }
}
