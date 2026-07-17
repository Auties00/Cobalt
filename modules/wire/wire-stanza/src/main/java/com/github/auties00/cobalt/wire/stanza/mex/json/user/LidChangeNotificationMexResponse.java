package com.github.auties00.cobalt.wire.stanza.mex.json.user;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;

import java.util.Optional;

/**
 * Decodes the reply to the LID-change notification query.
 *
 * <p>Carries the {@code old} and {@code new} LID pair from {@code xwa2_notify_lid_change}. Consume
 * this type after dispatching {@link LidChangeNotificationMexRequest}.
 *
 * @implNote WhatsApp Web treats a missing {@code old} or {@code new} field as a hard error; Cobalt
 * exposes both as {@link Optional} so callers can react without throwing.
 *
 * @see LidChangeNotificationMexRequest
 */
@WhatsAppWebModule(moduleName = "WAWebMexLidChangeNotification")
public final class LidChangeNotificationMexResponse implements MexStanza.Response.Json {
    /**
     * Holds the {@code old} field carrying the previous LID, possibly {@code null}.
     */
    private final String oldValue;

    /**
     * Holds the {@code new} field carrying the new LID, possibly {@code null}.
     */
    private final String newValue;

    /**
     * Wraps the decoded {@code old} and {@code new} LID pair.
     *
     * @param oldValue the {@code old} field
     * @param newValue the {@code new} field
     */
    private LidChangeNotificationMexResponse(String oldValue, String newValue) {
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    /**
     * Decodes the {@code <result>} child of an inbound MEX IQ.
     *
     * <p>Pass the IQ stanza received in reply to a stanza dispatched with
     * {@link LidChangeNotificationMexRequest#toStanza()}.
     *
     * @param stanza the IQ reply stanza
     * @return the decoded reply, or {@link Optional#empty()} when the payload is missing or malformed
     */
    @WhatsAppWebExport(moduleName = "WAWebMexLidChangeNotification", exports = "parseLidChangeNotification",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<LidChangeNotificationMexResponse> of(Stanza stanza) {
        return stanza.getChild("result")
                .flatMap(Stanza::toContentBytes)
                .flatMap(LidChangeNotificationMexResponse::of);
    }

    /**
     * Returns the previous LID value.
     *
     * @return the previous LID wrapped in an {@link Optional}, or {@link Optional#empty()} when the
     *         relay omitted the field
     */
    public Optional<String> oldValue() {
        return Optional.ofNullable(oldValue);
    }

    /**
     * Returns the new LID value.
     *
     * <p>The accessor is named {@code newValue()} to avoid clashing with the {@code new} Java keyword
     * that the wire field name maps to.
     *
     * @return the new LID wrapped in an {@link Optional}, or {@link Optional#empty()} when the relay
     *         omitted the field
     */
    public Optional<String> newValue() {
        return Optional.ofNullable(newValue);
    }

    /**
     * Decodes the {@code <result>} payload bytes into a {@link LidChangeNotificationMexResponse}.
     *
     * <p>The payload is projected from {@code data.xwa2_notify_lid_change}. Missing intermediate
     * envelopes yield {@link Optional#empty()}.
     *
     * @implNote Unlike WhatsApp Web's parser, missing {@code old} or {@code new} sub-fields are
     * surfaced as empty Optionals on the returned response rather than raised as errors.
     *
     * @param json the raw {@code <result>} payload bytes
     * @return the decoded reply, or {@link Optional#empty()} when the payload does not parse or lacks
     *         the {@code data} envelope
     */
    private static Optional<LidChangeNotificationMexResponse> of(byte[] json) {
        var jsonObject = JSON.parseObject(json);
        if (jsonObject == null) {
            return Optional.empty();
        }

        var data = jsonObject.getJSONObject("data");
        if (data == null) {
            return Optional.empty();
        }

        var root = data.getJSONObject("xwa2_notify_lid_change");
        if (root == null) {
            return Optional.empty();
        }

        var oldValue = root.getString("old");
        var newValue = root.getString("new");

        return Optional.of(new LidChangeNotificationMexResponse(oldValue, newValue));
    }
}
