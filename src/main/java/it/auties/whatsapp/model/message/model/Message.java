package it.auties.whatsapp.model.message.model;

import it.auties.whatsapp.model.message.standard.*;

/**
 * A model interface that represents a message sent by a contact or by Whatsapp.
 */
public sealed interface Message permits ButtonMessage, ContextualMessage, PaymentMessage, ServerMessage, CallMessage, EmptyMessage, KeepInChatMessage, NewsletterAdminInviteMessage, PollUpdateMessage, ReactionMessage {
    /**
     * Return message type
     *
     * @return a non-null message type
     */
    Type type();

    /**
     * Return message category
     *
     * @return a non-null message category
     */
    Category category();

    /**
     * The constants of this enumerated type describe the various types of messages that a
     * {@link MessageContainer} can wrap
     */
    enum Type {
        /**
         * Empty
         */
        EMPTY,
        /**
         * Text
         */
        TEXT,
        /**
         * Sender key distribution
         */
        SENDER_KEY_DISTRIBUTION,
        /**
         * Image
         */
        IMAGE,
        /**
         * Contact
         */
        CONTACT,
        /**
         * Location
         */
        LOCATION,
        /**
         * Document
         */
        DOCUMENT,
        /**
         * Audio
         */
        AUDIO,
        /**
         * Video
         */
        VIDEO,
        /**
         * Protocol
         */
        PROTOCOL,
        /**
         * Contact array
         */
        CONTACT_ARRAY,
        /**
         * Highly structured
         */
        HIGHLY_STRUCTURED,
        /**
         * Send payment
         */
        SEND_PAYMENT,
        /**
         * Live location
         */
        LIVE_LOCATION,
        /**
         * Request payment
         */
        REQUEST_PAYMENT,
        /**
         * Decline payment request
         */
        DECLINE_PAYMENT_REQUEST,
        /**
         * Cancel payment request
         */
        CANCEL_PAYMENT_REQUEST,
        /**
         * Template
         */
        TEMPLATE,
        /**
         * Sticker
         */
        STICKER,
        /**
         * Group invite
         */
        GROUP_INVITE,
        /**
         * Template reply
         */
        TEMPLATE_REPLY,
        /**
         * Product
         */
        PRODUCT,
        /**
         * Device sent
         */
        DEVICE_SENT,
        /**
         * Device sync
         */
        DEVICE_SYNC,
        /**
         * List
         */
        LIST,
        /**
         * View once
         */
        VIEW_ONCE,
        /**
         * Order
         */
        PAYMENT_ORDER,
        /**
         * List newsletters
         */
        LIST_RESPONSE,
        /**
         * Ephemeral
         */
        EPHEMERAL,
        /**
         * Payment invoice
         */
        PAYMENT_INVOICE,
        /**
         * Buttons newsletters
         */
        BUTTONS,
        /**
         * Buttons newsletters
         */
        BUTTONS_RESPONSE,
        /**
         * Payment invite
         */
        PAYMENT_INVITE,
        /**
         * Interactive
         */
        INTERACTIVE,
        /**
         * Reaction
         */
        REACTION,
        /**
         * Interactive newsletters
         */
        INTERACTIVE_RESPONSE,
        /**
         * Native flow newsletters
         */
        NATIVE_FLOW_RESPONSE,
        /**
         * Keep in chat
         */
        KEEP_IN_CHAT,
        /**
         * Poll creation
         */
        POLL_CREATION,
        /**
         * Poll update
         */
        POLL_UPDATE,
        /**
         * Request phone number
         */
        REQUEST_PHONE_NUMBER,
        /**
         * Encrypted reaction
         */
        ENCRYPTED_REACTION,
        /**
         * A call
         */
        CALL,
        /**
         * Sticker sync
         */
        STICKER_SYNC,
        /**
         * Text edit
         */
        EDITED,
        /**
         * Newsletter admin invite
         */
        NEWSLETTER_ADMIN_INVITE
    }

    /**
     * The constants of this enumerated type describe the various categories of messages that a
     * {@link MessageContainer} can wrap
     */
    enum Category {
        /**
         * Device message
         */
        BUTTON,
        /**
         * Payment message
         */
        PAYMENT,
        /**
         * Payment message
         */
        MEDIA,
        /**
         * Server message
         */
        SERVER,
        /**
         * Device message
         */
        DEVICE,
        /**
         * Standard message
         */
        STANDARD
    }
}
