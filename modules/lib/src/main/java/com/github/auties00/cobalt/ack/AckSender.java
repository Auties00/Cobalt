package com.github.auties00.cobalt.ack;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.lang.System.Logger.Level;
import java.util.Objects;

/**
 * Provides the single entry point for shipping outbound {@code <ack>} stanzas, both positive acks
 * and {@code <ack error=...>} nacks, in response to an inbound stanza.
 *
 * <p>Stream handlers receive this service via constructor injection. They call
 * {@link #sendAck(AckClass, Stanza)} or {@link #sendNack(AckClass, Stanza, NackReason)} for the common
 * shapes, and obtain an {@link AckBuilder} via {@link #ack(AckClass, Stanza)} for any call site that
 * needs to override attributes such as a custom {@code type}, an explicit {@code participant},
 * additional child nodes, or a custom {@code from}. The per-class default behaviour for the
 * {@code type} and {@code participant} attributes is handled by {@link AckBuilder}, with the
 * resolution rules documented there.
 *
 * @implNote This implementation collapses three WA Web entry points into one service: the positive
 * ack path, the receipt-class sub-shape, and the synthesised-nack path.
 */
@WhatsAppWebModule(moduleName = "WAWebHandleMsgSendAck")
@WhatsAppWebModule(moduleName = "WAWebReceiptAck")
@WhatsAppWebModule(moduleName = "WAWebCreateNackFromStanza")
public final class AckSender {
    /**
     * The logger for {@link AckSender}.
     */
    private static final System.Logger LOGGER = Log.get(AckSender.class);

    /**
     * The {@link LinkedWhatsAppClient} that ships the assembled ack stanza fire-and-forget through
     * {@link LinkedWhatsAppClient#sendNodeWithNoResponse(Stanza)}.
     */
    private final LinkedWhatsAppClient whatsapp;

    /**
     * Constructs a sender bound to the given {@link LinkedWhatsAppClient}.
     *
     * <p>One {@code AckSender} is created per logical client; the lifecycle wiring threads the same
     * instance into every stream handler that needs to emit an ack.
     *
     * @param whatsapp the {@link LinkedWhatsAppClient} used to dispatch the assembled ack stanza
     */
    public AckSender(LinkedWhatsAppClient whatsapp) {
        this.whatsapp = Objects.requireNonNull(whatsapp, "whatsapp");
    }

    /**
     * Ships a plain {@code <ack>} stanza for the given inbound stanza, applying the per-class
     * defaults for {@code type} and {@code participant} without further overrides.
     *
     * <p>The shortcut form for the common case where the handler just needs to confirm receipt of an
     * inbound stanza. The stanza is dropped silently when the inbound stanza lacks either an
     * {@code id} or a {@code from} attribute.
     *
     * @param cls     the {@link AckClass} for the {@code class} attribute on the outbound ack
     * @param inbound the inbound stanza being acknowledged
     * @return {@code true} when the ack was dispatched, {@code false} when it was dropped due to a
     *         missing {@code id} or {@code from}
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgSendAck", exports = "sendAck",
            adaptation = WhatsAppAdaptation.DIRECT)
    public boolean sendAck(AckClass cls, Stanza inbound) {
        return ack(cls, inbound).send();
    }

    /**
     * Ships an {@code <ack error="N">} stanza (a NACK) for the given inbound stanza, applying the
     * per-class defaults for {@code type} and {@code participant} without further overrides.
     *
     * <p>The shortcut form for the common case where a handler classifies the inbound stanza as
     * unprocessable and the reason maps directly to a known {@link NackReason}. For
     * {@link NackReason#INVALID_PROTOBUF} use {@link #ack(AckClass, Stanza)} instead and add the
     * failure reason via {@link AckBuilder#failureReason(String)}.
     *
     * @param cls     the {@link AckClass} for the {@code class} attribute on the outbound ack
     * @param inbound the inbound stanza being nacked
     * @param reason  the {@link NackReason} stamped into the {@code error} attribute
     * @return {@code true} when the ack was dispatched, {@code false} when it was dropped due to a
     *         missing {@code id} or {@code from}
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgSendAck", exports = "sendNack",
            adaptation = WhatsAppAdaptation.DIRECT)
    public boolean sendNack(AckClass cls, Stanza inbound, NackReason reason) {
        return ack(cls, inbound).error(reason).send();
    }

    /**
     * Returns a fluent {@link AckBuilder} bound to this sender, the given stanza class, and the
     * inbound stanza.
     *
     * <p>This is the entry point for any outbound ack that needs to deviate from the per-class
     * defaults: an explicit {@code type}, a non-default {@code participant}, a custom {@code from},
     * appended child nodes, or a NACK with a {@code <meta failure_reason=...>} child. The builder
     * defers the actual stanza assembly and dispatch to its {@link AckBuilder#send()} method, so a
     * caller can short-circuit by simply not calling {@code send()}.
     *
     * @param cls     the {@link AckClass} for the {@code class} attribute on the outbound ack
     * @param inbound the inbound stanza being acknowledged
     * @return a fresh {@link AckBuilder}
     */
    public AckBuilder ack(AckClass cls, Stanza inbound) {
        return new AckBuilder(this, cls, inbound);
    }

    /**
     * Synthesises a NACK for an inbound stanza of unknown or uncategorised origin.
     *
     * <p>Used by the central socket-stream error model when a stanza cannot be routed to a
     * registered handler at all. The outbound stanza's {@code class} attribute is derived from the
     * inbound stanza tag: {@code <message>}, {@code <receipt>}, and {@code <notification>} map to
     * {@link AckClass#MESSAGE}, {@link AckClass#RECEIPT}, and {@link AckClass#NOTIFICATION}
     * respectively. Any other inbound tag yields no class and the method returns {@code false}
     * without dispatching.
     *
     * @implNote For {@link NackReason#UNRECOGNIZED_STANZA} WA Web reuses the inbound stanza tag
     * verbatim as the {@code class} attribute; that case is out of scope for the current Cobalt model
     * and is treated as an unsupported tag.
     *
     * @param inbound the inbound stanza for which to synthesise a NACK
     * @param reason  the {@link NackReason} stamped into the {@code error} attribute
     * @return {@code true} when the NACK was dispatched, {@code false} when the inbound stanza tag is
     *         not one of the three supported classes or when the inbound stanza lacks an {@code id}
     *         or {@code from} attribute
     */
    @WhatsAppWebExport(moduleName = "WAWebCreateNackFromStanza",
            exports = "createNackFromStanza", adaptation = WhatsAppAdaptation.DIRECT)
    public boolean synthesiseNack(Stanza inbound, NackReason reason) {
        var cls = switch (inbound.description()) {
            case "message" -> AckClass.MESSAGE;
            case "receipt" -> AckClass.RECEIPT;
            case "notification" -> AckClass.NOTIFICATION;
            default -> null;
        };
        if (cls == null) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "nack synthesis skipped: unsupported stanza tag={0}",
                        inbound.description());
            }
            return false;
        }
        return ack(cls, inbound).error(reason).send();
    }

    /**
     * Dispatches the given assembled ack stanza through the underlying {@link LinkedWhatsAppClient}.
     *
     * <p>Called only from {@link AckBuilder#send()}; package-private so callers cannot bypass the
     * builder's id and from precondition.
     *
     * @implNote This implementation routes through
     * {@link LinkedWhatsAppClient#sendNodeWithNoResponse(Stanza)} so the ack is sent fire-and-forget; the
     * socket layer raises a
     * {@link com.github.auties00.cobalt.exception.linked.WhatsAppSessionException.Closed} when the
     * connection is down at the time of the send.
     *
     * @param ack the assembled {@code <ack>} stanza
     */
    void dispatch(Stanza ack) {
        whatsapp.sendNodeWithNoResponse(ack);
    }
}
