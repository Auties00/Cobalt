package com.github.auties00.cobalt.listener.linked.internal;

import com.github.auties00.cobalt.listener.linked.LinkedListener;

/**
 * The sealed marker for Linked listeners the client registers for its own internal use, never on
 * behalf of the application.
 *
 * <p>An internal listener rides the same dispatch path as an application listener, so the stream
 * handlers reach it through the store's listener collection, but it is hidden from the public
 * listener-management surface: the client refuses to register or remove any
 * {@code InternalLinkedListener} through
 * {@link com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient#addListener} and
 * {@link com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient#removeListener}. The client installs
 * exactly one always-registered instance per concrete internal concern at construction and never
 * removes it, so an internal mechanism that needs to observe the event stream does not consume an
 * application-visible registration slot.
 *
 * <p>Each concrete internal concern is a non-sealed subtype permitted here; the only one modelled
 * today is {@link LinkedTrustedContactTokenListener}.
 */
public sealed interface InternalLinkedListener extends LinkedListener
        permits LinkedTrustedContactTokenListener {
}
