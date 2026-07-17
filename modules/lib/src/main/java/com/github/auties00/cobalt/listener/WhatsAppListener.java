package com.github.auties00.cobalt.listener;

import com.github.auties00.cobalt.listener.cloud.CloudListener;
import com.github.auties00.cobalt.listener.linked.LinkedListener;

/**
 * The sealed root of every Cobalt event-listener interface.
 *
 * <p>The listener hierarchy is split by client flavour: {@link LinkedListener} groups the
 * events emitted by the socket-based Linked client and {@link CloudListener} groups the events
 * emitted by the Cloud API client. Events that both flavours emit extend this interface directly
 * and are generic in the client flavour they observe ({@code <C extends WhatsAppClient<?>>}), so
 * the callback receives the exact flavour the listener was registered on: {@link NewMessageListener},
 * {@link MessageStatusListener}, {@link MessageDeletedListener}, {@link LoggedInListener}, and
 * {@link DisconnectedListener}. Every other per-event listener extends one of the two flavour
 * markers, so a registration surface typed to a flavour marker accepts only the listeners that
 * flavour can emit.
 */
public sealed interface WhatsAppListener permits
        CloudListener,
        LinkedListener,
        NewMessageListener,
        MessageStatusListener,
        MessageDeletedListener,
        LoggedInListener,
        DisconnectedListener {
}
