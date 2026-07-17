package com.github.auties00.cobalt.listener.cloud;

import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClientListener;
import com.github.auties00.cobalt.listener.WhatsAppListener;

/**
 * The sealed marker for every event a {@link com.github.auties00.cobalt.client.cloud.CloudWhatsAppClient}
 * emits.
 *
 * <p>Each single-method Cloud listener in this package extends this marker, and the aggregator
 * {@link CloudWhatsAppClientListener} extends them all. Registering a listener of this type observes
 * the Cloud webhook event stream; the concrete event is recovered by the dispatcher through a
 * pattern-match. Events that both client flavours emit (new message, message status, message
 * deletion, login, disconnect) extend the root {@link WhatsAppListener} directly instead of this
 * marker.
 */
public sealed interface CloudListener extends WhatsAppListener permits
        CloudAccountSettingsListener,
        CloudAccountUpdateListener,
        CloudAppStateSyncListener,
        CloudBusinessCapabilityListener,
        CloudCallListener,
        CloudCallPermissionListener,
        CloudCallStatusListener,
        CloudErrorListener,
        CloudFlowListener,
        CloudHistoryListener,
        CloudMessageEchoListener,
        CloudMessagePricingListener,
        CloudPaymentConfigurationListener,
        CloudSecurityListener,
        CloudSystemListener,
        CloudWebhookReceivedListener,
        CloudPhoneNumberListener,
        CloudTemplateCategoryListener,
        CloudTemplateComponentsListener,
        CloudTemplatePauseListener,
        CloudTemplateQualityListener,
        CloudTemplateStatusListener,
        CloudUserPreferenceListener,
        CloudWhatsAppClientListener {
}
