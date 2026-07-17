package com.github.auties00.cobalt.wire.stanza.smax.biz;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.util.Objects;
import java.util.OptionalInt;

/**
 * Models a single {@code <campaign/>} grandchild of the outbound {@code <pending_campaigns>}
 * block on the SMB metered-messaging checkout request.
 * <p>
 * Each entry declares a previously-reserved send whose free-message impact must be accounted
 * for in the new quote; the relay subtracts the declared reservations from the campaign's
 * free-message allowance before computing the cost. Callers assemble one entry per reserved
 * send and attach the list to a {@link SmaxGetSMBMeteredMessagingCheckoutRequest}.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutSmbMeteredMessagingAccountGetSMBMeteredMessagingCheckoutRequest")
public final class SmaxGetSMBMeteredMessagingCheckoutPendingCampaign {
    /**
     * The number of free reserved messages on the previously-issued campaign.
     */
    private final int freeReservedMsgs;

    /**
     * The optional send-timestamp of the previously-issued campaign, in epoch seconds.
     */
    private final Integer sendTimestamp;

    /**
     * Constructs an entry for one previously-reserved send to be accounted for.
     *
     * @param freeReservedMsgs the number of reserved messages
     * @param sendTimestamp    the optional send timestamp in epoch seconds; may be {@code null}
     */
    public SmaxGetSMBMeteredMessagingCheckoutPendingCampaign(int freeReservedMsgs, Integer sendTimestamp) {
        this.freeReservedMsgs = freeReservedMsgs;
        this.sendTimestamp = sendTimestamp;
    }

    /**
     * Returns the reserved-message count.
     *
     * @return the count
     */
    public int freeReservedMsgs() {
        return freeReservedMsgs;
    }

    /**
     * Returns the optional send timestamp in epoch seconds.
     *
     * @return an {@link OptionalInt} carrying the timestamp, or empty when the entry omitted it
     */
    public OptionalInt sendTimestamp() {
        if (sendTimestamp == null) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(sendTimestamp);
    }

    /**
     * Builds the {@code <campaign/>} child stanza.
     * <p>
     * Stamps the mandatory {@code free_reserved_msgs} attribute and emits {@code send_timestamp}
     * only when supplied.
     *
     * @return the materialised {@link Stanza}
     */
    public Stanza toStanza() {
        var builder = new StanzaBuilder()
                .description("campaign")
                .attribute("free_reserved_msgs", freeReservedMsgs);
        if (sendTimestamp != null) {
            builder.attribute("send_timestamp", sendTimestamp.intValue());
        }
        return builder.build();
    }

    /**
     * Compares this entry to another object for value equality across both fields.
     *
     * @param obj the object to compare against; may be {@code null}
     * @return {@code true} when {@code obj} is a {@link SmaxGetSMBMeteredMessagingCheckoutPendingCampaign} with equal fields
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxGetSMBMeteredMessagingCheckoutPendingCampaign) obj;
        return this.freeReservedMsgs == that.freeReservedMsgs
                && Objects.equals(this.sendTimestamp, that.sendTimestamp);
    }

    /**
     * Returns a hash code derived from both fields.
     *
     * @return the combined hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(freeReservedMsgs, sendTimestamp);
    }

    /**
     * Returns a debug rendering listing the reserved-message count and the optional timestamp.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return "SmaxGetSMBMeteredMessagingCheckoutPendingCampaign[freeReservedMsgs=" + freeReservedMsgs
                + ", sendTimestamp=" + sendTimestamp + ']';
    }
}
