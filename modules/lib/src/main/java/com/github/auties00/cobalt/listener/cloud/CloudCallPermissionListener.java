package com.github.auties00.cobalt.listener.cloud;

import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClient;
import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClientListener;
import com.github.auties00.cobalt.wire.cloud.CloudCallEvent;

/**
 * A functional interface for the {@link CloudWhatsAppClientListener#onCallPermission onCallPermission}
 * event.
 *
 * <p>{@link CloudWhatsAppClientListener} extends this interface and supplies an empty default
 * implementation, so the event can also be observed in isolation as a lambda. The event is raised for
 * each consumer reply to a call-permission request, delivered as an interactive message whose
 * {@code interactive.type} is {@code call_permission_reply} on the {@code messages} webhook change.
 *
 * @see CloudWhatsAppClientListener
 */
@FunctionalInterface
public non-sealed interface CloudCallPermissionListener extends CloudListener {
    /**
     * Notifies the listener of a consumer's call-permission reply.
     *
     * @param whatsapp the client emitting the event
     * @param event    the call-permission reply event
     */
    void onCallPermission(CloudWhatsAppClient whatsapp, CloudCallEvent.PermissionReply event);
}
