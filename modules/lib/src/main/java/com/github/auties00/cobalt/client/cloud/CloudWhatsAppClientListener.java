package com.github.auties00.cobalt.client.cloud;
import com.github.auties00.cobalt.listener.MessageStatusListener;
import com.github.auties00.cobalt.listener.DisconnectedListener;
import com.github.auties00.cobalt.listener.LoggedInListener;
import com.github.auties00.cobalt.listener.NewMessageListener;
import com.github.auties00.cobalt.client.WhatsAppClientDisconnectReason;
import com.github.auties00.cobalt.listener.MessageDeletedListener;

import com.github.auties00.cobalt.listener.cloud.*;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.listener.cloud.CloudMessagePricingListener;
import com.github.auties00.cobalt.wire.cloud.CloudAccountUpdate;
import com.github.auties00.cobalt.wire.cloud.CloudAppStateSyncContact;
import com.github.auties00.cobalt.wire.cloud.CloudBusinessCapabilityUpdate;
import com.github.auties00.cobalt.wire.cloud.CloudCallEvent;
import com.github.auties00.cobalt.wire.cloud.CloudMessagePricing;
import com.github.auties00.cobalt.wire.cloud.CloudCallSettings;
import com.github.auties00.cobalt.wire.cloud.flow.CloudFlowStatusUpdate;
import com.github.auties00.cobalt.wire.cloud.CloudHistorySync;
import com.github.auties00.cobalt.wire.cloud.commerce.CloudPaymentConfiguration;
import com.github.auties00.cobalt.wire.cloud.phone.CloudPhoneNumberUpdate;
import com.github.auties00.cobalt.wire.cloud.CloudSecurityUpdate;
import com.github.auties00.cobalt.wire.cloud.CloudSystemUpdate;
import com.github.auties00.cobalt.wire.cloud.template.CloudTemplateCategoryUpdate;
import com.github.auties00.cobalt.wire.cloud.template.CloudTemplateComponentsUpdate;
import com.github.auties00.cobalt.wire.cloud.template.CloudTemplatePauseUpdate;
import com.github.auties00.cobalt.wire.cloud.template.CloudTemplateQualityUpdate;
import com.github.auties00.cobalt.wire.cloud.template.CloudTemplateStatusUpdate;
import com.github.auties00.cobalt.wire.cloud.CloudUserPreferenceUpdate;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageInfo;
import com.github.auties00.cobalt.wire.core.message.MessageInfo;
import com.github.auties00.cobalt.wire.core.message.MessageKey;

/**
 * Aggregator listener for every event a {@link CloudWhatsAppClient} emits.
 *
 * <p>This interface extends each single-method Cloud listener in this package and supplies an empty
 * default implementation for every callback, so an embedder can implement only the events they care
 * about. It mirrors the structure of {@code LinkedWhatsAppClientListener}: register an instance once
 * through {@link CloudWhatsAppClient#addListener(CloudListener)} to observe everything, or register
 * any of the single-method relatives as a lambda to observe one event in isolation.
 *
 * @see CloudWhatsAppClient
 */
public non-sealed interface CloudWhatsAppClientListener extends CloudListener, NewMessageListener<CloudWhatsAppClient>,
        MessageStatusListener<CloudWhatsAppClient>,
        CloudMessageEchoListener,
        CloudMessagePricingListener,
        CloudCallListener,
        CloudCallStatusListener,
        CloudCallPermissionListener,
        CloudTemplateStatusListener,
        CloudTemplateQualityListener,
        CloudTemplateCategoryListener,
        CloudTemplateComponentsListener,
        CloudTemplatePauseListener,
        CloudSecurityListener,
        CloudPaymentConfigurationListener,
        CloudPhoneNumberListener,
        CloudAccountUpdateListener,
        CloudBusinessCapabilityListener,
        CloudUserPreferenceListener,
        CloudFlowListener,
        CloudHistoryListener,
        CloudAppStateSyncListener,
        CloudAccountSettingsListener,
        CloudSystemListener,
        LoggedInListener<CloudWhatsAppClient>,
        DisconnectedListener<CloudWhatsAppClient>,
        MessageDeletedListener<CloudWhatsAppClient>,
        CloudWebhookReceivedListener,
        CloudErrorListener {
    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation does nothing; override to handle the event.
     */
    @Override
    default void onNewMessage(CloudWhatsAppClient whatsapp, MessageInfo info) {

    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation does nothing; override to handle the event.
     */
    @Override
    default void onMessageStatus(CloudWhatsAppClient whatsapp, MessageInfo info) {

    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation does nothing; override to handle the event.
     */
    @Override
    default void onMessageEcho(CloudWhatsAppClient whatsapp, MessageInfo info) {

    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation does nothing; override to handle the event.
     */
    @Override
    default void onMessagePricing(CloudWhatsAppClient whatsapp, MessageKey messageKey, CloudMessagePricing pricing) {

    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation does nothing; override to handle the event.
     */
    @Override
    default void onCall(CloudWhatsAppClient whatsapp, CloudCallEvent.Signaling event) {

    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation does nothing; override to handle the event.
     */
    @Override
    default void onCallStatus(CloudWhatsAppClient whatsapp, CloudCallEvent.Status event) {

    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation does nothing; override to handle the event.
     */
    @Override
    default void onCallPermission(CloudWhatsAppClient whatsapp, CloudCallEvent.PermissionReply event) {

    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation does nothing; override to handle the event.
     */
    @Override
    default void onTemplateStatus(CloudWhatsAppClient whatsapp, CloudTemplateStatusUpdate update) {

    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation does nothing; override to handle the event.
     */
    @Override
    default void onTemplatePause(CloudWhatsAppClient whatsapp, CloudTemplatePauseUpdate update) {

    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation does nothing; override to handle the event.
     */
    @Override
    default void onTemplateQuality(CloudWhatsAppClient whatsapp, CloudTemplateQualityUpdate update) {

    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation does nothing; override to handle the event.
     */
    @Override
    default void onTemplateCategory(CloudWhatsAppClient whatsapp, CloudTemplateCategoryUpdate update) {

    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation does nothing; override to handle the event.
     */
    @Override
    default void onTemplateComponents(CloudWhatsAppClient whatsapp, CloudTemplateComponentsUpdate update) {

    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation does nothing; override to handle the event.
     */
    @Override
    default void onSecurity(CloudWhatsAppClient whatsapp, CloudSecurityUpdate update) {

    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation does nothing; override to handle the event.
     */
    @Override
    default void onPaymentConfiguration(CloudWhatsAppClient whatsapp, CloudPaymentConfiguration update) {

    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation does nothing; override to handle the event.
     */
    @Override
    default void onPhoneNumberUpdate(CloudWhatsAppClient whatsapp, CloudPhoneNumberUpdate update) {

    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation does nothing; override to handle the event.
     */
    @Override
    default void onAccountUpdate(CloudWhatsAppClient whatsapp, CloudAccountUpdate update) {

    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation does nothing; override to handle the event.
     */
    @Override
    default void onBusinessCapabilityUpdate(CloudWhatsAppClient whatsapp, CloudBusinessCapabilityUpdate update) {

    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation does nothing; override to handle the event.
     */
    @Override
    default void onUserPreference(CloudWhatsAppClient whatsapp, CloudUserPreferenceUpdate update) {

    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation does nothing; override to handle the event.
     */
    @Override
    default void onFlowStatus(CloudWhatsAppClient whatsapp, CloudFlowStatusUpdate update) {

    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation does nothing; override to handle the event.
     */
    @Override
    default void onHistorySync(CloudWhatsAppClient whatsapp, CloudHistorySync chunk) {

    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation does nothing; override to handle the event.
     */
    @Override
    default void onAppStateSync(CloudWhatsAppClient whatsapp, CloudAppStateSyncContact contact) {

    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation does nothing; override to handle the event.
     */
    @Override
    default void onAccountSettings(CloudWhatsAppClient whatsapp, CloudCallSettings settings) {

    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation does nothing; override to handle the event.
     */
    @Override
    default void onSystemUpdate(CloudWhatsAppClient whatsapp, CloudSystemUpdate update) {

    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation does nothing; override to handle the event.
     */
    @Override
    default void onLoggedIn(CloudWhatsAppClient whatsapp) {

    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation does nothing; override to handle the event.
     */
    @Override
    default void onDisconnected(CloudWhatsAppClient whatsapp, WhatsAppClientDisconnectReason reason) {

    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation does nothing; override to handle the event.
     */
    @Override
    default void onWebhookReceived(CloudWhatsAppClient whatsapp, JSONObject envelope) {

    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation does nothing; override to handle the event.
     */
    @Override
    default void onError(CloudWhatsAppClient whatsapp, Throwable error) {

    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation does nothing; override to handle the event.
     */
    @Override
    default void onMessageDeleted(CloudWhatsAppClient whatsapp, MessageInfo info, boolean everyone) {

    }
}
