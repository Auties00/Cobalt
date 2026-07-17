package com.github.auties00.cobalt.wam.threadlogging;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

/**
 * A single thread-interaction event reported by a producer hook to
 * {@link ThreadLoggingService#recordActivity(com.github.auties00.cobalt.wire.core.jid.JidProvider, ThreadLoggingActivity)}.
 *
 * <p>Each variant carries only the classification flags the service needs to decide which counters on
 * the thread's day-bucket row to bump; the service performs the actual increments. The variants mirror
 * the dispatch arms of WhatsApp Web's {@code WAWebChatThreadLogging} message and call handlers: an
 * outbound send, an inbound receive, a read sweep, a view-once open, and a completed call.
 *
 * @see ThreadLoggingService
 * @see ThreadLoggingCounters
 */
@WhatsAppWebModule(moduleName = "WAWebChatThreadLogging")
public sealed interface ThreadLoggingActivity {
    /**
     * An outbound message send.
     *
     * <p>The flags are mutually shaped after WhatsApp Web's send classifier: a reaction send bumps the
     * reactions-sent counter only; an edit send bumps the edited-messages-sent counter only; any other
     * send bumps the messages-sent counter plus whichever of the forwarded, view-once, reply, and
     * commerce sub-counters apply. The {@code reply} flag is passed by the caller only for non-group
     * threads, matching the one-on-one scope of the replies-sent counter.
     *
     * @param viewOnce  whether the sent message is a view-once message
     * @param reply     whether the sent message quotes another message in a one-on-one thread
     * @param forwarded whether the sent message is forwarded
     * @param edit      whether the send is an edit of an existing message
     * @param reaction  whether the send is a reaction
     * @param commerce  whether the sent message is a commerce message
     */
    record MessageSent(boolean viewOnce, boolean reply, boolean forwarded, boolean edit, boolean reaction,
                       boolean commerce) implements ThreadLoggingActivity {
    }

    /**
     * An inbound message receive.
     *
     * <p>A reaction receive bumps the reactions-received counter only; any other receive bumps the
     * messages-received counter plus whichever of the forwarded, view-once, and commerce sub-counters
     * apply.
     *
     * @param viewOnce  whether the received message is a view-once message
     * @param forwarded whether the received message is forwarded
     * @param reaction  whether the receive is a reaction
     * @param commerce  whether the received message is a commerce message
     */
    record MessageReceived(boolean viewOnce, boolean forwarded, boolean reaction,
                           boolean commerce) implements ThreadLoggingActivity {
    }

    /**
     * A read sweep marking one or more messages read in the thread.
     *
     * @param count the number of messages marked read
     */
    record MessagesRead(int count) implements ThreadLoggingActivity {
    }

    /**
     * A view-once message open, bumping the view-once-messages-opened counter by one.
     */
    record ViewOnceOpened() implements ThreadLoggingActivity {
    }

    /**
     * A completed call, reported once at call end when both its direction and connected duration are
     * known.
     *
     * <p>It bumps the call-offers-sent or call-offers-received counter by one according to the
     * direction, and adds the connected duration to the total-call-duration counter.
     *
     * @param outgoing        whether the local party placed the call
     * @param durationSeconds the connected call duration, in seconds
     */
    record Call(boolean outgoing, long durationSeconds) implements ThreadLoggingActivity {
    }
}
