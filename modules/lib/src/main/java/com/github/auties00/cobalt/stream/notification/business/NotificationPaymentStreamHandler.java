package com.github.auties00.cobalt.stream.notification.business;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stream.SocketStreamHandler;
import com.github.auties00.cobalt.ack.AckClass;
import com.github.auties00.cobalt.ack.AckSender;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.listener.MessageStatusListener;
import com.github.auties00.cobalt.listener.NewMessageListener;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo.StubType;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfoBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageInfo;
import com.github.auties00.cobalt.wire.core.message.MessageKeyBuilder;
import com.github.auties00.cobalt.wire.core.message.MessageStatus;
import com.github.auties00.cobalt.wire.linked.payment.OrphanPaymentNotificationBuilder;
import com.github.auties00.cobalt.wire.linked.payment.PaymentInfo;
import com.github.auties00.cobalt.wire.linked.payment.PaymentInfoBuilder;
import com.github.auties00.cobalt.stream.message.PaymentMessageStatus;
import com.github.auties00.cobalt.stream.message.PaymentMessageTransactionType;
import com.github.auties00.cobalt.wire.core.util.RandomIdUtils;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Handles {@code type="pay"} notifications carrying payment invites and payment-transaction state changes.
 *
 * <p>Dispatched by {@link NotificationBusinessDispatcher}. Each notification contains either an {@code <invite>} child
 * (an account-setup invite or a pre-payment prompt) or a {@code <transaction>} child (a transaction lifecycle update
 * for an already-sent payment request or transfer). Invite takes priority over transaction, so a stanza carrying both
 * children processes only the invite. A transaction whose referenced message has not yet arrived is recorded as an
 * orphan-payment entry on the store and reconciled when the message later arrives via the chat-message stream handler.
 * Every processed stanza is followed by a protocol-level {@code <ack class="notification" type="pay"/>}.
 */
@WhatsAppWebModule(moduleName = "WAWebPaymentNotificationHandler")
@WhatsAppWebModule(moduleName = "WAWebPaymentNotificationParser")
final class NotificationPaymentStreamHandler extends SocketStreamHandler.Concurrent {
    /**
     * The logger for {@link NotificationPaymentStreamHandler}.
     */
    private static final System.Logger LOGGER = Log.get(NotificationPaymentStreamHandler.class);

    /**
     * Reads the store, looks up messages, and writes orphan-payment records.
     */
    private final LinkedWhatsAppClient whatsapp;

    /**
     * Ships the post-processing {@code <ack class="notification" type="pay"/>} stanza.
     */
    private final AckSender ackSender;

    /**
     * Constructs the handler with the shared client and ack sender.
     *
     * <p>Called once by {@link NotificationBusinessDispatcher}.
     *
     * @param whatsapp  the client used for store reads, message lookups, and orphan-payment writes
     * @param ackSender the ack sender used for the post-processing ack
     */
    NotificationPaymentStreamHandler(LinkedWhatsAppClient whatsapp, AckSender ackSender) {
        this.whatsapp = whatsapp;
        this.ackSender = ackSender;
    }

    /**
     * Dispatches to the invite or transaction branch and always sends the protocol-level ack.
     *
     * <p>Stanzas whose description is not {@code notification} or whose {@code type} is not {@code pay} are dropped.
     * Invite takes priority over transaction. A failure during processing is logged and the ack is still sent.
     *
     * @param stanza the incoming {@code <notification>} stanza
     */
    @Override
    public void handle(Stanza stanza) {
        if (!stanza.hasDescription("notification") || !stanza.hasAttribute("type", "pay")) {
            return;
        }

        try {
            var invite = stanza.getChild("invite").orElse(null);
            if (invite != null) {
                handlePaymentInvite(stanza, invite);
            } else {
                stanza.getChild("transaction").ifPresent(this::handlePaymentTransaction);
            }
        } catch (Throwable throwable) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING,
                        "failed to handle payment notification id=" + stanza.getAttributeAsString("id", "[missing-id]"),
                        throwable);
            }
        } finally {
            sendNotificationAck(stanza);
        }
    }

    /**
     * Materialises an account-setup invite as a local stub message in the inviter's chat and fires
     * {@link LinkedWhatsAppClientListener#onNewMessage(LinkedWhatsAppClient, LinkedMessageInfo)} for
     * listeners.
     *
     * <p>Only invites whose {@code type} is {@code account-set-up} produce a stub; other invite types are logged and
     * otherwise ignored. An invite with no {@code from} JID is dropped. The stub message is appended to the inviter's
     * chat, the chat timestamps are advanced to the invite time, and the unread count is incremented.
     *
     * @implNote
     * This implementation uses {@link StubType#PAYMENT_ACTION_ACCOUNT_SETUP_REMINDER} as Cobalt's nearest model
     * equivalent for the account-setup invite stub.
     *
     * @param stanza   the parent {@code <notification>} stanza
     * @param invite the {@code <invite>} child stanza
     */
    private void handlePaymentInvite(Stanza stanza, Stanza invite) {
        var type = invite.getAttributeAsString("type", null);
        var service = invite.getAttributeAsString("service", null);
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG,
                    "received payment invite notification type={0} service={1} id={2}",
                    type, service, stanza.getAttributeAsString("id", "[missing-id]"));
        }
        if (!"account-set-up".equals(type)) {
            return;
        }

        var from = invite.getAttributeAsJid("from").orElse(null);
        if (from == null) {
            return;
        }

        var key = new MessageKeyBuilder()
                .id(RandomIdUtils.newId())
                .parentJid(from)
                .fromMe(false)
                .senderJid(from)
                .build();

        var timestampAttr = invite.getAttributeAsLong("t");
        var timestamp = timestampAttr.isPresent()
                ? Instant.ofEpochSecond(timestampAttr.getAsLong())
                : Instant.now();

        var info = new ChatMessageInfoBuilder()
                .key(key)
                .senderJid(from)
                .timestamp(timestamp)
                .status(MessageStatus.DELIVERED)
                .stubType(StubType.PAYMENT_ACTION_ACCOUNT_SETUP_REMINDER)
                .stubParameters(List.of(from.toString()))
                .message(LinkedMessageContainer.empty())
                .build();

        var chat = whatsapp.store().chatStore().findChatByJid(from)
                .orElseGet(() -> whatsapp.store().chatStore().addNewChat(from));
        chat.setLastMsgTimestamp(timestamp);
        chat.setConversationTimestamp(timestamp);
        chat.setUnreadCount(chat.unreadCount().orElse(0) + 1);
        chat.addMessage(info);

        for (var listener : whatsapp.store().listeners()) {
            if (listener instanceof NewMessageListener typed) {
                Thread.startVirtualThread(() -> typed.onNewMessage(whatsapp, info));
            }
        }
    }

    /**
     * Applies a transaction state change to the referenced message, or records the data as an orphan payment when the
     * message is not yet in the store.
     *
     * <p>The message is resolved by exact key and then by id within the chat. When neither finds the message, an
     * orphan-payment record carrying the transaction fields is written for later reconciliation. The chat or group
     * routing is derived from the {@code group} attribute and the {@code fromMe} flag.
     *
     * @implNote
     * This implementation skips Novi-service transactions ({@code service="NOVI"}) with a warning, as those payments
     * are not supported.
     *
     * @param transaction the {@code <transaction>} child stanza
     */
    private void handlePaymentTransaction(Stanza transaction) {
        var service = transaction.getAttributeAsString("service", null);
        if (service != null && service.equalsIgnoreCase("NOVI")) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "payment notification from Novi not supported");
            }
            return;
        }

        var sender = transaction.getAttributeAsJid("sender").orElse(null);
        var receiver = transaction.getAttributeAsJid("receiver").orElse(null);
        var messageId = transaction.getAttributeAsString("message-id", null);
        if (sender == null || receiver == null || messageId == null) {
            return;
        }

        var self = whatsapp.store().accountStore().jid().orElse(null);
        var fromMe = self != null && Objects.equals(self.toUserJid(), sender.toUserJid());
        var group = transaction.getAttributeAsJid("group").orElse(null);
        var remote = group != null ? group : fromMe ? receiver : sender;
        var participant = group != null ? sender : null;

        var message = findPaymentMessage(remote, participant, messageId, fromMe);
        if (!(message instanceof ChatMessageInfo chatMessageInfo)) {
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG,
                        "recording orphan payment notification for message id={0}, referenced message not yet in store",
                        messageId);
            }
            whatsapp.store().businessStore().addOrphanPaymentNotification(new OrphanPaymentNotificationBuilder()
                    .messageId(messageId)
                    .receiverJid(receiver)
                    .currency(transaction.getAttributeAsString("currency", null))
                    .amount1000(transaction.getAttributeAsLong("amount_1000", (Long) null))
                    .transactionType(transaction.getAttributeAsString("transaction-type", null))
                    .status(transaction.getAttributeAsString("status", null))
                    .transactionTimestamp(transaction.getAttributeAsLong("ts", (Long) null))
                    .build());
            return;
        }

        applyPaymentTransaction(chatMessageInfo, transaction, fromMe);
    }

    /**
     * Writes transaction fields onto the resolved chat message, propagates the status onto the originating payment
     * request when one exists, and fires
     * {@link LinkedWhatsAppClientListener#onMessageStatus(LinkedWhatsAppClient, LinkedMessageInfo)} for
     * listeners.
     *
     * <p>When this transaction fulfils a payment request, the originating request message is located by key and then by
     * id, and its status is coerced through {@link #determinePaymentRequestFulfilledStatus(PaymentInfo.TxnStatus)} so a
     * successful payment marks the request fulfilled and any other status resets it to
     * {@link PaymentInfo.TxnStatus#COLLECT_INIT}. The orphan-payment record for this message id is removed on success.
     *
     * @param chatMessageInfo the resolved chat message
     * @param transaction     the {@code <transaction>} child stanza
     * @param fromMe          whether the local account is the sender
     */
    private void applyPaymentTransaction(ChatMessageInfo chatMessageInfo, Stanza transaction, boolean fromMe) {
        var receiver = transaction.getAttributeAsJid("receiver").orElse(null);
        var type = transaction.getAttributeAsString("transaction-type", null);
        var status = transaction.getAttributeAsString("status", null);

        var paymentInfo = chatMessageInfo.paymentInfo().orElseGet(this::newPaymentInfo);
        paymentInfo.setReceiverJid(receiver);
        paymentInfo.setAmount1000(transaction.getAttributeAsLong("amount_1000", (Long) null));
        paymentInfo.setCurrency(transaction.getAttributeAsString("currency", null));
        paymentInfo.setTransactionTimestamp(transaction.getAttributeAsLong("ts", (Long) null));
        paymentInfo.setStatus(mapPaymentStatus(type, status, fromMe));
        paymentInfo.setTxnStatus(mapTxnStatus(type, status, fromMe));
        chatMessageInfo.setPaymentInfo(paymentInfo);
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG,
                    "applied payment transaction type={0} status={1} -> {2}",
                    type, status, paymentInfo.status().orElse(PaymentInfo.Status.UNKNOWN_STATUS));
        }

        paymentInfo.requestMessageKey().ifPresent(requestKey -> {
            var requestIdOpt = requestKey.id();
            var requestRemoteOpt = requestKey.parentJid();
            if (requestIdOpt.isEmpty() || requestRemoteOpt.isEmpty()) {
                return;
            }
            var requestMessage = whatsapp.store().chatStore().findMessageByKey(requestKey).orElse(null);
            if (requestMessage == null) {
                requestMessage = whatsapp.store().chatStore().findMessageById(requestRemoteOpt.get(), requestIdOpt.get()).orElse(null);
            }
            if (!(requestMessage instanceof ChatMessageInfo requestChat)) {
                return;
            }
            var requestPaymentInfo = requestChat.paymentInfo().orElseGet(this::newPaymentInfo);
            requestPaymentInfo.setStatus(paymentInfo.status().orElse(PaymentInfo.Status.UNKNOWN_STATUS));
            requestPaymentInfo.setTxnStatus(determinePaymentRequestFulfilledStatus(
                    paymentInfo.txnStatus().orElse(PaymentInfo.TxnStatus.UNKNOWN)));
            requestChat.setPaymentInfo(requestPaymentInfo);
            for (var listener : whatsapp.store().listeners()) {
                if (listener instanceof MessageStatusListener typed) {
                    Thread.startVirtualThread(() -> typed.onMessageStatus(whatsapp, requestChat));
                }
            }
        });

        for (var listener : whatsapp.store().listeners()) {
            if (listener instanceof MessageStatusListener typed) {
                Thread.startVirtualThread(() -> typed.onMessageStatus(whatsapp, chatMessageInfo));
            }
        }

        chatMessageInfo.key().id().ifPresent(whatsapp.store().businessStore()::removeOrphanPaymentNotification);
    }

    /**
     * Coerces a transaction status into the status that should propagate onto the originating payment-request message.
     *
     * <p>Returns the input status when it is {@link PaymentInfo.TxnStatus#COMPLETED} or
     * {@link PaymentInfo.TxnStatus#SUCCESS}, and {@link PaymentInfo.TxnStatus#COLLECT_INIT} otherwise.
     *
     * @param txnStatus the transaction status from the fulfilling payment
     * @return the coerced status to apply to the request
     */
    private PaymentInfo.TxnStatus determinePaymentRequestFulfilledStatus(PaymentInfo.TxnStatus txnStatus) {
        if (txnStatus == PaymentInfo.TxnStatus.COMPLETED || txnStatus == PaymentInfo.TxnStatus.SUCCESS) {
            return txnStatus;
        }
        return PaymentInfo.TxnStatus.COLLECT_INIT;
    }

    /**
     * Locates the chat message that this transaction targets, first by exact key, then by id within the chat.
     *
     * @implNote
     * This implementation collapses the in-memory and persistent lookups into one call because the Cobalt store
     * unifies the two storage layers.
     *
     * @param remote      the chat or group JID
     * @param participant the sender JID in a group, or {@code null} for one-to-one chats
     * @param messageId   the message id to look up
     * @param fromMe      whether the local account is the sender
     * @return the matching {@link LinkedMessageInfo}, or {@code null} when not found
     */
    private LinkedMessageInfo findPaymentMessage(Jid remote, Jid participant, String messageId, boolean fromMe) {
        var direct = whatsapp.store().chatStore().findMessageByKey(new MessageKeyBuilder()
                        .id(messageId)
                        .parentJid(remote)
                        .fromMe(fromMe)
                        .senderJid(participant != null ? participant : remote)
                        .build())
                .orElse(null);
        if (direct != null) {
            return direct;
        }

        return whatsapp.store().chatStore().findMessageById(remote, messageId)
                .map(LinkedMessageInfo.class::cast)
                .orElse(null);
    }

    /**
     * Returns a fresh {@link PaymentInfo} with status {@link PaymentInfo.Status#UNKNOWN_STATUS} and transaction status
     * {@link PaymentInfo.TxnStatus#UNKNOWN}.
     *
     * <p>Used by {@link #applyPaymentTransaction(ChatMessageInfo, Stanza, boolean)} when neither the resolved message nor
     * the originating request message has an existing {@link PaymentInfo}.
     *
     * @return the new {@link PaymentInfo}
     */
    private PaymentInfo newPaymentInfo() {
        return new PaymentInfoBuilder()
                .status(PaymentInfo.Status.UNKNOWN_STATUS)
                .txnStatus(PaymentInfo.TxnStatus.UNKNOWN)
                .build();
    }

    /**
     * Maps a resolved {@link PaymentMessageStatus} to the user-facing {@link PaymentInfo.Status}.
     *
     * <p>This is the source of truth for the status-to-display-state mapping shown in the chat thread.
     *
     * @param type   the raw transaction-type string
     * @param status the raw status string
     * @param fromMe whether the local account is the sender
     * @return the mapped {@link PaymentInfo.Status}
     */
    private PaymentInfo.Status mapPaymentStatus(String type, String status, boolean fromMe) {
        return switch (paymentMessageStatus(type, status, fromMe)) {
            case SEND_PAY_INIT, SEND_PAY_PENDING, RECV_PAY_INIT, RECV_PAY_PENDING, RECV_PAY_RETRY_ON_FAILURE, REQUEST_PAY_INIT -> PaymentInfo.Status.PROCESSING;
            case SEND_PAY_PENDING_RECEIVER, SEND_PAY_FAILURE_RECEIVER -> PaymentInfo.Status.SENT;
            case REQUEST_PAY_SUCCESS -> paymentMessageTransactionType(type, fromMe) == PaymentMessageTransactionType.TYPE_P2P_REQ_SENT ? PaymentInfo.Status.WAITING_FOR_PAYER : PaymentInfo.Status.WAITING;
            case RECV_PAY_PENDING_SETUP -> PaymentInfo.Status.NEED_TO_ACCEPT;
            case SEND_PAY_SUCCESS, RECV_PAY_SUCCESS, REQUEST_PAY_FULFILLED -> PaymentInfo.Status.COMPLETE;
            case SEND_PAY_FAILURE, SEND_PAY_FAILURE_RISK, SEND_PAY_PENDING_REFUND, SEND_PAY_REFUND_PENDING, SEND_PAY_REFUND_FAILED, SEND_PAY_REFUND_FAILED_PROCESSING, RECV_PAY_FAILURE, REQUEST_PAY_FAILED, REQUEST_PAY_FAILED_RISK -> PaymentInfo.Status.COULD_NOT_COMPLETE;
            case SEND_PAY_REFUNDED -> PaymentInfo.Status.REFUNDED;
            case RECV_PAY_EXPIRED, REQUEST_PAY_EXPIRED, SEND_PAY_AUTH_CANCELED, SEND_PAY_AUTH_CANCEL_FAILED, SEND_PAY_AUTH_CANCEL_FAILED_PROCESSING -> PaymentInfo.Status.EXPIRED;
            case REQUEST_PAY_REJECTED -> PaymentInfo.Status.REJECTED;
            case REQUEST_PAY_CANCELLED -> PaymentInfo.Status.CANCELLED;
            default -> PaymentInfo.Status.UNKNOWN_STATUS;
        };
    }

    /**
     * Maps a resolved {@link PaymentMessageStatus} to the fine-grained {@link PaymentInfo.TxnStatus} that tracks the
     * internal state machine.
     *
     * @param type   the raw transaction-type string
     * @param status the raw status string
     * @param fromMe whether the local account is the sender
     * @return the mapped {@link PaymentInfo.TxnStatus}
     */
    private PaymentInfo.TxnStatus mapTxnStatus(String type, String status, boolean fromMe) {
        return switch (paymentMessageStatus(type, status, fromMe)) {
            case RECV_PAY_EXPIRED, SEND_PAY_EXPIRED -> PaymentInfo.TxnStatus.EXPIRED_TXN;
            case RECV_PAY_FAILURE, SEND_PAY_FAILURE -> PaymentInfo.TxnStatus.FAILED;
            case RECV_PAY_INIT, SEND_PAY_INIT -> PaymentInfo.TxnStatus.INIT;
            case RECV_PAY_PENDING_SETUP -> PaymentInfo.TxnStatus.PENDING_SETUP;
            case RECV_PAY_PENDING, SEND_PAY_PENDING -> PaymentInfo.TxnStatus.FAILED_DA;
            case RECV_PAY_RETRY_ON_FAILURE -> PaymentInfo.TxnStatus.FAILED_PROCESSING;
            case RECV_PAY_SUCCESS, SEND_PAY_SUCCESS, REQUEST_PAY_FULFILLED -> PaymentInfo.TxnStatus.SUCCESS;
            case REQUEST_PAY_CANCELLED -> PaymentInfo.TxnStatus.COLLECT_CANCELED;
            case REQUEST_PAY_CANCELLING -> PaymentInfo.TxnStatus.COLLECT_CANCELLING;
            case REQUEST_PAY_EXPIRED -> PaymentInfo.TxnStatus.COLLECT_EXPIRED;
            case REQUEST_PAY_FAILED_RISK -> PaymentInfo.TxnStatus.COLLECT_FAILED_RISK;
            case REQUEST_PAY_FAILED -> PaymentInfo.TxnStatus.COLLECT_FAILED;
            case REQUEST_PAY_INIT -> PaymentInfo.TxnStatus.COLLECT_INIT;
            case REQUEST_PAY_REJECTED -> PaymentInfo.TxnStatus.COLLECT_REJECTED;
            case REQUEST_PAY_SUCCESS -> PaymentInfo.TxnStatus.COLLECT_SUCCESS;
            case SEND_PAY_AUTH_CANCELED -> PaymentInfo.TxnStatus.AUTH_CANCELED;
            case SEND_PAY_AUTH_CANCEL_FAILED_PROCESSING -> PaymentInfo.TxnStatus.AUTH_CANCEL_FAILED_PROCESSING;
            case SEND_PAY_AUTH_CANCEL_FAILED -> PaymentInfo.TxnStatus.AUTH_CANCEL_FAILED;
            case SEND_PAY_FAILURE_RECEIVER -> PaymentInfo.TxnStatus.FAILED_RECEIVER_PROCESSING;
            case SEND_PAY_FAILURE_RISK, RECV_PAY_FAILURE_RISK -> PaymentInfo.TxnStatus.FAILED_RISK;
            case SEND_PAY_PENDING_RECEIVER -> PaymentInfo.TxnStatus.PENDING_RECEIVER_SETUP;
            case SEND_PAY_PENDING_REFUND -> PaymentInfo.TxnStatus.FAILED_DA_FINAL;
            case SEND_PAY_REFUNDED -> PaymentInfo.TxnStatus.REFUNDED_TXN;
            case SEND_PAY_REFUND_FAILED_PROCESSING -> PaymentInfo.TxnStatus.REFUND_FAILED_PROCESSING;
            case SEND_PAY_REFUND_FAILED -> PaymentInfo.TxnStatus.REFUND_FAILED;
            case SEND_PAY_REFUND_PENDING -> PaymentInfo.TxnStatus.REFUND_FAILED_DA;
            case SEND_PAY_IN_REVIEW -> PaymentInfo.TxnStatus.IN_REVIEW;
            default -> PaymentInfo.TxnStatus.UNKNOWN;
        };
    }

    /**
     * Resolves a raw transaction-type and status pair into the fine-grained {@link PaymentMessageStatus} consumed by
     * {@link #mapPaymentStatus(String, String, boolean)} and {@link #mapTxnStatus(String, String, boolean)}.
     *
     * <p>The resolved {@link PaymentMessageTransactionType} is the outer switch key and the uppercased status string is
     * the inner key; an unrecognised combination resolves to {@link PaymentMessageStatus#STATUS_UNSET}.
     *
     * @param type   the raw transaction-type string, or {@code null}
     * @param status the raw status string, or {@code null}
     * @param fromMe whether the local account is the sender
     * @return the resolved {@link PaymentMessageStatus}
     */
    private PaymentMessageStatus paymentMessageStatus(String type, String status, boolean fromMe) {
        var statusValue = status == null ? "" : status.toUpperCase();
        return switch (paymentMessageTransactionType(type, fromMe)) {
            case TYPE_P2M_PAYOUT -> PaymentMessageStatus.STATUS_UNSET;
            case TYPE_P2P_SENT, TYPE_P2M_SENT, TYPE_DEPOSIT -> switch (statusValue) {
                case "PENDING_RECEIVER_SETUP" -> PaymentMessageStatus.SEND_PAY_PENDING_RECEIVER;
                case "FAILED_DA" -> PaymentMessageStatus.SEND_PAY_PENDING;
                case "REFUND_FAILED_DA" -> PaymentMessageStatus.SEND_PAY_REFUND_PENDING;
                case "FAILED_RISK" -> PaymentMessageStatus.SEND_PAY_FAILURE_RISK;
                case "INITIAL" -> PaymentMessageStatus.SEND_PAY_INIT;
                case "SUCCESS", "COMPLETED" -> PaymentMessageStatus.SEND_PAY_SUCCESS;
                case "FAILURE", "FAILED" -> PaymentMessageStatus.SEND_PAY_FAILURE;
                case "REFUNDED" -> PaymentMessageStatus.SEND_PAY_REFUNDED;
                case "REFUND_FAILED" -> PaymentMessageStatus.SEND_PAY_REFUND_FAILED;
                case "FAILED_RECEIVER_PROCESSING" -> PaymentMessageStatus.SEND_PAY_FAILURE_RECEIVER;
                case "REFUND_FAILED_PROCESSING" -> PaymentMessageStatus.SEND_PAY_REFUND_FAILED_PROCESSING;
                case "FAILED_DA_FINAL" -> PaymentMessageStatus.SEND_PAY_PENDING_REFUND;
                case "AUTH_CANCEL_FAILED_PROCESSING" -> PaymentMessageStatus.SEND_PAY_AUTH_CANCEL_FAILED_PROCESSING;
                case "AUTH_CANCEL_FAILED" -> PaymentMessageStatus.SEND_PAY_AUTH_CANCEL_FAILED;
                case "AUTH_CANCELED" -> PaymentMessageStatus.SEND_PAY_AUTH_CANCELED;
                case "CANCELLED" -> PaymentMessageStatus.SEND_PAY_USER_CANCELED;
                case "EXPIRED" -> PaymentMessageStatus.SEND_PAY_EXPIRED;
                case "IN_REVIEW" -> PaymentMessageStatus.SEND_PAY_IN_REVIEW;
                case "PENDING" -> PaymentMessageStatus.SEND_PAY_PENDING_PROCESSING;
                default -> PaymentMessageStatus.STATUS_UNSET;
            };
            case TYPE_P2P_RCVD, TYPE_P2M_RCVD -> switch (statusValue) {
                case "PENDING_SETUP" -> PaymentMessageStatus.RECV_PAY_PENDING_SETUP;
                case "FAILED_DA" -> PaymentMessageStatus.RECV_PAY_PENDING;
                case "FAILED_PROCESSING" -> PaymentMessageStatus.RECV_PAY_RETRY_ON_FAILURE;
                case "SUCCESS", "COMPLETED" -> PaymentMessageStatus.RECV_PAY_SUCCESS;
                case "FAILURE", "FAILED" -> PaymentMessageStatus.RECV_PAY_FAILURE;
                case "EXPIRED" -> PaymentMessageStatus.RECV_PAY_EXPIRED;
                case "FAILED_RISK" -> PaymentMessageStatus.RECV_PAY_FAILURE_RISK;
                case "WITHDRAWAL_PROCESSING" -> PaymentMessageStatus.RECV_PAY_WITHDRAWAL_PROCESSING;
                case "WITHDRAWAL_FAILURE" -> PaymentMessageStatus.RECV_PAY_WITHDRAWAL_FAILURE;
                case "WITHDRAWAL_PERMANENT_FAILED" -> PaymentMessageStatus.RECV_PAY_WITHDRAWAL_PERMANENT_FAILED;
                case "CANCELLED" -> PaymentMessageStatus.RECV_PAY_SENDER_CANCELED;
                default -> PaymentMessageStatus.STATUS_UNSET;
            };
            case TYPE_P2P_REQ_SENT, TYPE_P2P_REQ_RCVD -> switch (statusValue) {
                case "COLLECT_SUCCESS" -> PaymentMessageStatus.REQUEST_PAY_SUCCESS;
                case "COLLECT_FAILED" -> PaymentMessageStatus.REQUEST_PAY_FAILED;
                case "COLLECT_FAILED_RISK" -> PaymentMessageStatus.REQUEST_PAY_FAILED_RISK;
                case "COLLECT_REJECTED" -> PaymentMessageStatus.REQUEST_PAY_REJECTED;
                case "COLLECT_EXPIRED" -> PaymentMessageStatus.REQUEST_PAY_EXPIRED;
                case "COLLECT_CANCELED" -> PaymentMessageStatus.REQUEST_PAY_CANCELLED;
                default -> PaymentMessageStatus.STATUS_UNSET;
            };
            case TYPE_P2P_REQ_SCHEDULED_PAYMENT_RCVD -> switch (statusValue) {
                case "COLLECT_SUCCESS" -> PaymentMessageStatus.REQUEST_PAY_SCHEDULED_PAYMENT_SUCCESS;
                case "AUTH_SUCCESS" -> PaymentMessageStatus.SEND_PAY_AUTH_SUCCESS;
                default -> PaymentMessageStatus.STATUS_UNSET;
            };
            case TYPE_REFUND -> switch (statusValue) {
                case "SUCCESS", "COMPLETED" -> PaymentMessageStatus.RECV_PAY_SUCCESS;
                default -> PaymentMessageStatus.STATUS_UNSET;
            };
            case TYPE_WITHDRAWAL -> switch (statusValue) {
                case "PENDING" -> PaymentMessageStatus.WITHDRAWAL_PENDING;
                case "IN_REVIEW" -> PaymentMessageStatus.WITHDRAWAL_IN_REVIEW;
                case "SUCCESS", "COMPLETED" -> PaymentMessageStatus.WITHDRAWAL_SUCCESS;
                case "FAILED", "DECLINED" -> PaymentMessageStatus.WITHDRAWAL_FAILED;
                case "CANCELLED" -> PaymentMessageStatus.WITHDRAWAL_USER_CANCELED;
                case "EXPIRED" -> PaymentMessageStatus.WITHDRAWAL_EXPIRED;
                case "WITHDRAWAL_ACTIVE" -> PaymentMessageStatus.WITHDRAWAL_ACTIVE;
                default -> PaymentMessageStatus.STATUS_UNSET;
            };
            case TYPE_UNSET, TYPE_P2P_GRP, TYPE_P2P_NO_INFO, TYPE_FUTURE, TYPE_P2P_REQ_GRP, TYPE_MISSING_DETAILS ->
                    PaymentMessageStatus.STATUS_UNSET;
        };
    }

    /**
     * Resolves the raw transaction-type string and {@code fromMe} flag into a {@link PaymentMessageTransactionType}.
     *
     * <p>A {@code null} or unrecognised type falls back to {@link PaymentMessageTransactionType#TYPE_P2P_SENT} when
     * {@code fromMe} is {@code true} and {@link PaymentMessageTransactionType#TYPE_P2P_RCVD} otherwise.
     *
     * @param type   the raw transaction-type string, or {@code null}
     * @param fromMe whether the local account is the sender
     * @return the resolved {@link PaymentMessageTransactionType}
     */
    private PaymentMessageTransactionType paymentMessageTransactionType(String type, boolean fromMe) {
        if (type == null) {
            return fromMe ? PaymentMessageTransactionType.TYPE_P2P_SENT : PaymentMessageTransactionType.TYPE_P2P_RCVD;
        }

        return switch (type.toLowerCase()) {
            case "p2p" -> fromMe ? PaymentMessageTransactionType.TYPE_P2P_SENT : PaymentMessageTransactionType.TYPE_P2P_RCVD;
            case "p2m" -> fromMe ? PaymentMessageTransactionType.TYPE_P2M_SENT : PaymentMessageTransactionType.TYPE_P2M_RCVD;
            case "payout" -> PaymentMessageTransactionType.TYPE_P2M_PAYOUT;
            case "deposit" -> PaymentMessageTransactionType.TYPE_DEPOSIT;
            case "refund" -> PaymentMessageTransactionType.TYPE_REFUND;
            case "withdrawal" -> PaymentMessageTransactionType.TYPE_WITHDRAWAL;
            default -> fromMe ? PaymentMessageTransactionType.TYPE_P2P_SENT : PaymentMessageTransactionType.TYPE_P2P_RCVD;
        };
    }

    /**
     * Sends the {@code <ack class="notification" type="pay"/>} stanza.
     *
     * <p>Fire-and-forget.
     *
     * @param stanza the original {@code <notification>} stanza
     */
    private void sendNotificationAck(Stanza stanza) {
        ackSender.ack(AckClass.NOTIFICATION, stanza).type("pay").send();
    }
}
