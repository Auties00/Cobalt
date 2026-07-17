package com.github.auties00.cobalt.stream.message;

/**
 * Enumerates the fine-grained internal payment-message status values used by
 * the inbound payment notification pipeline.
 *
 * <p>Each constant corresponds to a specific
 * ({@link PaymentMessageTransactionType}, server-reported status string)
 * pair. The message stream and notification handlers resolve a value of
 * this enum from an inbound {@code <transaction>} stanza via
 * {@code WAWebPaymentStatusUtils.getNotificationTransactionStatus} and then
 * map it onto {@code PaymentInfo.Status} and {@code PaymentInfo.TxnStatus}
 * through {@code WAWebPaymentStatusUtils.getPaymentWebStatus} and
 * {@code WAWebPaymentStatusUtils.getPaymentTxnWebStatus}. The enum is the
 * typed Cobalt counterpart of the {@code NotificationTransactionStatus}
 * enum exposed by {@code WAWebPaymentStatusUtils}.
 */
public enum PaymentMessageStatus {
    /**
     * Indicates that no status has been resolved.
     *
     * <p>Produced whenever the server-reported {@code status} attribute
     * fails to match any case of
     * {@code getNotificationTransactionStatus}, corresponding to the
     * {@code STATUS_UNSET = 0} member of
     * {@code WAWebPaymentStatusUtils.NotificationTransactionStatus}.
     */
    STATUS_UNSET,

    /**
     * Indicates that a payment request has just been initialised.
     *
     * <p>Corresponds to {@code REQUEST_PAY_INIT = 11}.
     */
    REQUEST_PAY_INIT,

    /**
     * Indicates that a payment request has been successfully collected.
     *
     * <p>Produced when the server reports {@code "COLLECT_SUCCESS"} for a
     * request-type transaction, corresponding to
     * {@code REQUEST_PAY_SUCCESS = 12}.
     */
    REQUEST_PAY_SUCCESS,

    /**
     * Indicates that a payment request has failed.
     *
     * <p>Produced when the server reports {@code "COLLECT_FAILED"} for a
     * request-type transaction, corresponding to
     * {@code REQUEST_PAY_FAILED = 13}.
     */
    REQUEST_PAY_FAILED,

    /**
     * Indicates that a payment request was blocked by the payment
     * provider's risk system.
     *
     * <p>Produced when the server reports {@code "COLLECT_FAILED_RISK"} for
     * a request-type transaction, corresponding to
     * {@code REQUEST_PAY_FAILED_RISK = 14}.
     */
    REQUEST_PAY_FAILED_RISK,

    /**
     * Indicates that a payment request has been rejected by the payer.
     *
     * <p>Produced when the server reports {@code "COLLECT_REJECTED"} for a
     * request-type transaction, corresponding to
     * {@code REQUEST_PAY_REJECTED = 15}.
     */
    REQUEST_PAY_REJECTED,

    /**
     * Indicates that a payment request has expired before being fulfilled.
     *
     * <p>Produced when the server reports {@code "COLLECT_EXPIRED"} for a
     * request-type transaction, corresponding to
     * {@code REQUEST_PAY_EXPIRED = 16}.
     */
    REQUEST_PAY_EXPIRED,

    /**
     * Indicates that a payment request has been fulfilled by the payer.
     *
     * <p>Corresponds to {@code REQUEST_PAY_FULFILLED = 17}.
     */
    REQUEST_PAY_FULFILLED,

    /**
     * Indicates that a payment request has been cancelled.
     *
     * <p>Produced when the server reports {@code "COLLECT_CANCELED"} for a
     * request-type transaction, corresponding to
     * {@code REQUEST_PAY_CANCELLED = 18}.
     */
    REQUEST_PAY_CANCELLED,

    /**
     * Indicates that a payment request is in the process of being
     * cancelled.
     *
     * <p>Corresponds to {@code REQUEST_PAY_CANCELLING = 19}.
     */
    REQUEST_PAY_CANCELLING,

    /**
     * Indicates that a scheduled payment for a received peer-to-peer
     * request has been collected.
     *
     * <p>Resolved only under the
     * {@link PaymentMessageTransactionType#TYPE_P2P_REQ_SCHEDULED_PAYMENT_RCVD}
     * branch, corresponding to
     * {@code REQUEST_PAY_SCHEDULED_PAYMENT_SUCCESS = 20}.
     */
    REQUEST_PAY_SCHEDULED_PAYMENT_SUCCESS,

    /**
     * Indicates that an incoming payment has been initialised.
     *
     * <p>Corresponds to {@code RECV_PAY_INIT = 101}.
     */
    RECV_PAY_INIT,

    /**
     * Indicates that an incoming payment is pending until the receiver
     * finishes setup.
     *
     * <p>Produced when the server reports {@code "PENDING_SETUP"} on a
     * P2P/P2M-received transaction, corresponding to
     * {@code RECV_PAY_PENDING_SETUP = 102}.
     */
    RECV_PAY_PENDING_SETUP,

    /**
     * Indicates that an incoming payment is pending because of a direct
     * account settlement failure.
     *
     * <p>Produced when the server reports {@code "FAILED_DA"} on a
     * P2P/P2M-received transaction, corresponding to
     * {@code RECV_PAY_PENDING = 103}.
     */
    RECV_PAY_PENDING,

    /**
     * Indicates that an incoming payment failed but the server is going to
     * retry.
     *
     * <p>Produced when the server reports {@code "FAILED_PROCESSING"} on a
     * P2P/P2M-received transaction, corresponding to
     * {@code RECV_PAY_RETRY_ON_FAILURE = 104}.
     */
    RECV_PAY_RETRY_ON_FAILURE,

    /**
     * Indicates that an incoming payment failed terminally.
     *
     * <p>Corresponds to {@code RECV_PAY_FAILURE = 105}.
     */
    RECV_PAY_FAILURE,

    /**
     * Indicates that an incoming payment was successfully credited.
     *
     * <p>Corresponds to {@code RECV_PAY_SUCCESS = 106}.
     */
    RECV_PAY_SUCCESS,

    /**
     * Indicates that an incoming payment expired before completion.
     *
     * <p>Corresponds to {@code RECV_PAY_EXPIRED = 107}.
     */
    RECV_PAY_EXPIRED,

    /**
     * Indicates that an incoming payment was blocked by the payment
     * provider's risk system.
     *
     * <p>Corresponds to {@code RECV_PAY_FAILURE_RISK = 108}.
     */
    RECV_PAY_FAILURE_RISK,

    /**
     * Indicates that the receiver-side withdrawal of an incoming payment is
     * in progress.
     *
     * <p>Corresponds to {@code RECV_PAY_WITHDRAWAL_PROCESSING = 109}.
     */
    RECV_PAY_WITHDRAWAL_PROCESSING,

    /**
     * Indicates that the receiver-side withdrawal of an incoming payment
     * failed transiently.
     *
     * <p>Corresponds to {@code RECV_PAY_WITHDRAWAL_FAILURE = 110}.
     */
    RECV_PAY_WITHDRAWAL_FAILURE,

    /**
     * Indicates that the receiver-side withdrawal of an incoming payment
     * has failed permanently.
     *
     * <p>Corresponds to {@code RECV_PAY_WITHDRAWAL_PERMANENT_FAILED = 111}.
     */
    RECV_PAY_WITHDRAWAL_PERMANENT_FAILED,

    /**
     * Indicates that an incoming payment was cancelled by the sender.
     *
     * <p>Corresponds to {@code RECV_PAY_SENDER_CANCELED = 112}.
     */
    RECV_PAY_SENDER_CANCELED,

    /**
     * Indicates that an outgoing payment has been initialised.
     *
     * <p>Produced when the server reports {@code "INITIAL"} on a
     * P2P/P2M-sent or deposit transaction, corresponding to
     * {@code SEND_PAY_INIT = 401}.
     */
    SEND_PAY_INIT,

    /**
     * Indicates that an outgoing payment is pending until the receiver
     * completes setup.
     *
     * <p>Corresponds to {@code SEND_PAY_PENDING_RECEIVER = 402}.
     */
    SEND_PAY_PENDING_RECEIVER,

    /**
     * Indicates that an outgoing payment is pending because of a direct
     * account settlement failure.
     *
     * <p>Produced when the server reports {@code "FAILED_DA"} on a
     * P2P/P2M-sent or deposit transaction, corresponding to
     * {@code SEND_PAY_PENDING = 403}.
     */
    SEND_PAY_PENDING,

    /**
     * Indicates that the refund of an outgoing payment is pending because
     * of a direct account settlement failure.
     *
     * <p>Corresponds to {@code SEND_PAY_REFUND_PENDING = 404}.
     */
    SEND_PAY_REFUND_PENDING,

    /**
     * Indicates that an outgoing payment has been completed successfully.
     *
     * <p>Corresponds to {@code SEND_PAY_SUCCESS = 405}.
     */
    SEND_PAY_SUCCESS,

    /**
     * Indicates that an outgoing payment has failed.
     *
     * <p>Corresponds to {@code SEND_PAY_FAILURE = 406}.
     */
    SEND_PAY_FAILURE,

    /**
     * Indicates that an outgoing payment was blocked by the payment
     * provider's risk system.
     *
     * <p>Corresponds to {@code SEND_PAY_FAILURE_RISK = 407}.
     */
    SEND_PAY_FAILURE_RISK,

    /**
     * Indicates that an outgoing payment has been refunded.
     *
     * <p>Corresponds to {@code SEND_PAY_REFUNDED = 408}.
     */
    SEND_PAY_REFUNDED,

    /**
     * Indicates that the refund of an outgoing payment has failed.
     *
     * <p>Corresponds to {@code SEND_PAY_REFUND_FAILED = 409}.
     */
    SEND_PAY_REFUND_FAILED,

    /**
     * Indicates that an outgoing payment failed during receiver-side
     * processing.
     *
     * <p>Corresponds to {@code SEND_PAY_FAILURE_RECEIVER = 410}.
     */
    SEND_PAY_FAILURE_RECEIVER,

    /**
     * Indicates that the refund of an outgoing payment is being processed
     * but cannot complete cleanly.
     *
     * <p>Corresponds to {@code SEND_PAY_REFUND_FAILED_PROCESSING = 411}.
     */
    SEND_PAY_REFUND_FAILED_PROCESSING,

    /**
     * Indicates that an outgoing payment is pending refund after a final
     * direct-account settlement failure.
     *
     * <p>Produced when the server reports {@code "FAILED_DA_FINAL"} on a
     * P2P/P2M-sent or deposit transaction, corresponding to
     * {@code SEND_PAY_PENDING_REFUND = 412}.
     */
    SEND_PAY_PENDING_REFUND,

    /**
     * Indicates that an outgoing payment authorisation cancellation is
     * being processed but cannot complete cleanly.
     *
     * <p>Corresponds to {@code SEND_PAY_AUTH_CANCEL_FAILED_PROCESSING = 413}.
     */
    SEND_PAY_AUTH_CANCEL_FAILED_PROCESSING,

    /**
     * Indicates that an outgoing payment authorisation cancellation has
     * failed.
     *
     * <p>Corresponds to {@code SEND_PAY_AUTH_CANCEL_FAILED = 414}.
     */
    SEND_PAY_AUTH_CANCEL_FAILED,

    /**
     * Indicates that an outgoing payment authorisation has been cancelled.
     *
     * <p>Corresponds to {@code SEND_PAY_AUTH_CANCELED = 415}.
     */
    SEND_PAY_AUTH_CANCELED,

    /**
     * Indicates that an outgoing payment has expired.
     *
     * <p>Corresponds to {@code SEND_PAY_EXPIRED = 416}.
     */
    SEND_PAY_EXPIRED,

    /**
     * Indicates that an outgoing payment authorisation has succeeded.
     *
     * <p>Resolved under the
     * {@link PaymentMessageTransactionType#TYPE_P2P_REQ_SCHEDULED_PAYMENT_RCVD}
     * branch when the server reports {@code "AUTH_SUCCESS"}, corresponding
     * to {@code SEND_PAY_AUTH_SUCCESS = 417}.
     */
    SEND_PAY_AUTH_SUCCESS,

    /**
     * Indicates that an outgoing payment authorisation success is in the
     * process of being cancelled.
     *
     * <p>Corresponds to {@code SEND_PAY_AUTH_SUCCESS_CANCELING = 418}.
     */
    SEND_PAY_AUTH_SUCCESS_CANCELING,

    /**
     * Indicates that an outgoing payment is being reviewed by the payment
     * provider.
     *
     * <p>Corresponds to {@code SEND_PAY_IN_REVIEW = 419}.
     */
    SEND_PAY_IN_REVIEW,

    /**
     * Indicates that an outgoing payment is pending processing.
     *
     * <p>Produced when the server reports {@code "PENDING"} on a
     * P2P/P2M-sent or deposit transaction, corresponding to
     * {@code SEND_PAY_PENDING_PROCESSING = 420}.
     */
    SEND_PAY_PENDING_PROCESSING,

    /**
     * Indicates that an outgoing payment was cancelled by the user.
     *
     * <p>Corresponds to {@code SEND_PAY_USER_CANCELED = 421}.
     */
    SEND_PAY_USER_CANCELED,

    /**
     * Indicates that a withdrawal has been initialised.
     *
     * <p>Corresponds to {@code WITHDRAWAL_INIT = 601}.
     */
    WITHDRAWAL_INIT,

    /**
     * Indicates that a withdrawal is pending processing.
     *
     * <p>Corresponds to {@code WITHDRAWAL_PENDING = 602}.
     */
    WITHDRAWAL_PENDING,

    /**
     * Indicates that a withdrawal is being reviewed.
     *
     * <p>Corresponds to {@code WITHDRAWAL_IN_REVIEW = 603}.
     */
    WITHDRAWAL_IN_REVIEW,

    /**
     * Indicates that a withdrawal completed successfully.
     *
     * <p>Corresponds to {@code WITHDRAWAL_SUCCESS = 604}.
     */
    WITHDRAWAL_SUCCESS,

    /**
     * Indicates that a withdrawal failed.
     *
     * <p>Produced when the server reports {@code "FAILED"} or
     * {@code "DECLINED"} on a withdrawal transaction, corresponding to
     * {@code WITHDRAWAL_FAILED = 605}.
     */
    WITHDRAWAL_FAILED,

    /**
     * Indicates that a withdrawal was cancelled by the user.
     *
     * <p>Corresponds to {@code WITHDRAWAL_USER_CANCELED = 606}.
     */
    WITHDRAWAL_USER_CANCELED,

    /**
     * Indicates that a withdrawal has expired.
     *
     * <p>Corresponds to {@code WITHDRAWAL_EXPIRED = 607}.
     */
    WITHDRAWAL_EXPIRED,

    /**
     * Indicates that a withdrawal is currently active.
     *
     * <p>Corresponds to {@code WITHDRAWAL_ACTIVE = 608}.
     */
    WITHDRAWAL_ACTIVE
}
