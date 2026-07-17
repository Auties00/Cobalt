package com.github.auties00.cobalt.ctwa;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.ctwa.CtwaOrderStatus;
import com.github.auties00.cobalt.wire.core.jid.JidProvider;
import com.github.auties00.cobalt.wire.linked.preference.Label;

/**
 * Emits the WhatsApp Business Click-To-WhatsApp (CTWA) conversion-signal telemetry when a business
 * client labels or progresses an order in a chat that originated from a Click-To-WhatsApp ad.
 *
 * <p>WhatsApp Business shares two telemetry signals with the ad platform so that an advertiser can
 * attribute downstream conversions back to the ad that started the conversation: the
 * {@code Ctwa3pdConversion} third-party conversion and the {@code CtwaLabelSignal} label signal. A chat
 * is eligible only when one of its inbound messages carries an {@code FB_Ads} Click-To-WhatsApp
 * conversion, and emission is further gated by the business account's global data-sharing setting and,
 * for the third-party conversion, the per-customer sharing preference keyed by the chat's ads-account
 * LID.
 *
 * <p>The two entry points are event-driven: a label association ({@link #emitLabelConversion}) and an
 * order status or payment change ({@link #emitOrderConversion}). The service holds no schedule and owns
 * no lifecycle; it reacts to the trigger and commits whatever signals the gates allow.
 *
 * @see CtwaOrderStatus
 */
@WhatsAppWebModule(moduleName = "WAWebSmb3pdConversionSignalAction")
@WhatsAppWebModule(moduleName = "WAWebSmbMarkAsXLabelAction")
public interface CtwaConversionSignalService {
    /**
     * Emits the CTWA conversion signals for a label that has just been associated with a chat.
     *
     * <p>When the label carries one of the allowed CTWA predefined identifiers (new customer, new order,
     * pending payment, paid, order complete, important, follow up, or lead) this commits both the
     * {@code CtwaLabelSignal} label signal and the {@code Ctwa3pdConversion} third-party conversion for
     * the resolved chat, each subject to its own gating. Labels without a predefined identifier, or with
     * one outside the allowed set, produce no signal.
     *
     * @implSpec Implementations resolve the label's predefined identifier to the matching CTWA label type,
     *           conversion type, conversion subtype and paid-data metadata, and emit nothing when the
     *           label has no predefined identifier, when the predefined identifier is not an allowed CTWA
     *           identifier, or when the chat cannot be resolved from the store.
     * @param chat  the chat the label was associated with
     * @param label the associated label
     */
    void emitLabelConversion(JidProvider chat, Label label);

    /**
     * Emits the CTWA third-party conversion signal for an order status or payment change in a chat.
     *
     * <p>This commits a {@code Ctwa3pdConversion} with the {@code order} surface, the conversion type and
     * subtype derived from {@code status}, and the paid-data metadata derived from {@code paid}, subject
     * to the third-party conversion gating.
     *
     * @implSpec Implementations derive the conversion type and subtype from {@code status}, encode
     *           {@code paid} as the {@code {"paid":...}} metadata, and emit nothing when the chat cannot
     *           be resolved from the store or the third-party conversion gates fail.
     * @param chat   the chat whose order changed
     * @param status the order lifecycle status driving the conversion subtype
     * @param paid   whether the order is in a paid state, encoded into the conversion metadata
     */
    void emitOrderConversion(JidProvider chat, CtwaOrderStatus status, boolean paid);
}
