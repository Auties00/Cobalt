package com.github.auties00.cobalt.stream.message;

/**
 * Classifies the direction and mechanism of a WhatsApp payment transaction.
 *
 * <p>Instances are not constructed directly by application code; they are
 * produced by the message stream and notification handlers when they
 * classify the {@code transaction-type} attribute carried by an inbound
 * {@code <transaction>} stanza together with the {@code fromMe} direction
 * flag, and are then consumed to look up the matching
 * {@link PaymentMessageStatus} for the transaction. The enum is the typed
 * Cobalt counterpart of the {@code PaymentTransactionType} enum exposed by
 * {@code WAWebPaymentStatusUtils}.
 */
public enum PaymentMessageTransactionType {
    /**
     * Indicates that the transaction type could not be determined.
     *
     * <p>Serves as the default sentinel value, corresponding to the
     * {@code TYPE_UNSET = 0} member of
     * {@code WAWebPaymentStatusUtils.PaymentTransactionType}.
     */
    TYPE_UNSET,

    /**
     * Classifies a peer-to-peer payment outgoing from the current account.
     *
     * <p>Produced when the {@code transaction-type} attribute is
     * {@code "p2p"} and the {@code fromMe} flag is {@code true},
     * corresponding to the {@code TYPE_P2P_SENT = 1} member of
     * {@code WAWebPaymentStatusUtils.PaymentTransactionType}.
     */
    TYPE_P2P_SENT,

    /**
     * Classifies a peer-to-peer payment incoming to the current account.
     *
     * <p>Produced when the {@code transaction-type} attribute is
     * {@code "p2p"} and the {@code fromMe} flag is {@code false},
     * including when the attribute is missing entirely. Corresponds to the
     * {@code TYPE_P2P_RCVD = 2} member of
     * {@code WAWebPaymentStatusUtils.PaymentTransactionType}.
     */
    TYPE_P2P_RCVD,

    /**
     * Classifies a peer-to-peer payment exchanged inside a group chat.
     *
     * <p>Corresponds to the {@code TYPE_P2P_GRP = 3} member of
     * {@code WAWebPaymentStatusUtils.PaymentTransactionType}.
     */
    TYPE_P2P_GRP,

    /**
     * Classifies a peer-to-peer payment for which the server provided no
     * direction information.
     *
     * <p>Corresponds to the {@code TYPE_P2P_NO_INFO = 4} member of
     * {@code WAWebPaymentStatusUtils.PaymentTransactionType}.
     */
    TYPE_P2P_NO_INFO,

    /**
     * Classifies a future-dated peer-to-peer payment.
     *
     * <p>Corresponds to the {@code TYPE_FUTURE = 5} member of
     * {@code WAWebPaymentStatusUtils.PaymentTransactionType}.
     */
    TYPE_FUTURE,

    /**
     * Classifies an outgoing peer-to-peer payment request.
     *
     * <p>Drives the {@link PaymentMessageStatus#REQUEST_PAY_SUCCESS} branch
     * that maps to {@code PaymentInfo.Status.WAITING_FOR_PAYER} in
     * {@code WAWebPaymentStatusUtils.getPaymentWebStatus}. Corresponds to
     * the {@code TYPE_P2P_REQ_SENT = 10} member of
     * {@code WAWebPaymentStatusUtils.PaymentTransactionType}.
     */
    TYPE_P2P_REQ_SENT,

    /**
     * Classifies an incoming peer-to-peer payment request.
     *
     * <p>Corresponds to the {@code TYPE_P2P_REQ_RCVD = 20} member of
     * {@code WAWebPaymentStatusUtils.PaymentTransactionType}.
     */
    TYPE_P2P_REQ_RCVD,

    /**
     * Classifies a scheduled payment authorisation for a received
     * peer-to-peer request.
     *
     * <p>Corresponds to the {@code TYPE_P2P_REQ_SCHEDULED_PAYMENT_RCVD = 21}
     * member of {@code WAWebPaymentStatusUtils.PaymentTransactionType}.
     */
    TYPE_P2P_REQ_SCHEDULED_PAYMENT_RCVD,

    /**
     * Classifies a peer-to-peer payment request exchanged inside a group.
     *
     * <p>Corresponds to the {@code TYPE_P2P_REQ_GRP = 30} member of
     * {@code WAWebPaymentStatusUtils.PaymentTransactionType}.
     */
    TYPE_P2P_REQ_GRP,

    /**
     * Classifies a person-to-merchant payment outgoing from the current
     * account.
     *
     * <p>Corresponds to the {@code TYPE_P2M_SENT = 100} member of
     * {@code WAWebPaymentStatusUtils.PaymentTransactionType}.
     */
    TYPE_P2M_SENT,

    /**
     * Classifies a person-to-merchant payment incoming to the current
     * account, in which the current account is the merchant.
     *
     * <p>Corresponds to the {@code TYPE_P2M_RCVD = 200} member of
     * {@code WAWebPaymentStatusUtils.PaymentTransactionType}.
     */
    TYPE_P2M_RCVD,

    /**
     * Classifies a payout to a merchant.
     *
     * <p>Produced when the {@code transaction-type} attribute is
     * {@code "payout"}, corresponding to the {@code TYPE_P2M_PAYOUT = 300}
     * member of {@code WAWebPaymentStatusUtils.PaymentTransactionType}.
     */
    TYPE_P2M_PAYOUT,

    /**
     * Classifies a transaction whose details are incomplete.
     *
     * <p>Corresponds to the {@code TYPE_MISSING_DETAILS = 400} member of
     * {@code WAWebPaymentStatusUtils.PaymentTransactionType}.
     */
    TYPE_MISSING_DETAILS,

    /**
     * Classifies a deposit transaction.
     *
     * <p>Produced when the {@code transaction-type} attribute is
     * {@code "deposit"}, corresponding to the {@code TYPE_DEPOSIT = 500}
     * member of {@code WAWebPaymentStatusUtils.PaymentTransactionType}.
     */
    TYPE_DEPOSIT,

    /**
     * Classifies a refund transaction.
     *
     * <p>Produced when the {@code transaction-type} attribute is
     * {@code "refund"}, corresponding to the {@code TYPE_REFUND = 600}
     * member of {@code WAWebPaymentStatusUtils.PaymentTransactionType}.
     */
    TYPE_REFUND,

    /**
     * Classifies a withdrawal transaction.
     *
     * <p>Produced when the {@code transaction-type} attribute is
     * {@code "withdrawal"}, corresponding to the {@code TYPE_WITHDRAWAL = 700}
     * member of {@code WAWebPaymentStatusUtils.PaymentTransactionType}.
     */
    TYPE_WITHDRAWAL
}
