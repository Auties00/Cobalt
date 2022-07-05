package it.auties.whatsapp.model.info;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.model.business.BusinessPrivacyStatus;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.media.MediaData;
import it.auties.whatsapp.model.message.model.*;
import it.auties.whatsapp.model.message.server.ProtocolMessage;
import it.auties.whatsapp.model.message.standard.LiveLocationMessage;
import it.auties.whatsapp.model.message.standard.ReactionMessage;
import it.auties.whatsapp.model.sync.PhotoChange;
import it.auties.whatsapp.util.Clock;
import lombok.*;
import lombok.Builder.Default;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.*;
import static java.util.Objects.requireNonNullElseGet;

/**
 * A model class that holds the information related to a {@link Message}.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(builderMethodName = "newMessageInfo")
@Jacksonized
@Accessors(fluent = true)
@ToString(exclude = {"storeId", "cachedStore"})
public final class MessageInfo implements Info {
    /**
     * The id of the store associated with this message
     */
    private int storeId;

    /**
     * The cached store
     */
    @JsonIgnore
    private Store cachedStore;

    /**
     * The MessageKey of this message
     */
    @ProtobufProperty(index = 1, type = MESSAGE, concreteType = MessageKey.class)
    @NonNull
    private MessageKey key;

    /**
     * The container of this message
     */
    @ProtobufProperty(index = 2, type = MESSAGE, concreteType = MessageContainer.class)
    @NonNull
    @Default
    private MessageContainer message = new MessageContainer();

    /**
     * The timestamp, that is the endTimeStamp since {@link java.time.Instant#EPOCH}, when this message was sent
     */
    @ProtobufProperty(index = 3, type = UINT64)
    private long timestamp;

    /**
     * The global status of this message.
     * If the chat associated with this message is a group it is guaranteed that this field is equal or lower hierarchically then every value stored by {@link MessageInfo#individualStatus()}.
     * Otherwise, this field is guaranteed to be equal to the single value stored by {@link MessageInfo#individualStatus()} for the contact associated with the chat associated with this message.
     */
    @ProtobufProperty(index = 4, type = MESSAGE, concreteType = MessageStatus.class)
    @NonNull
    @Default
    private MessageStatus status = MessageStatus.PENDING;

    /**
     * A map that holds the read status of this message for each participant.
     * If the chat associated with this chat is not a group, this map's size will always be 1.
     * In this case it is guaranteed that the value stored in this map for the contact associated with this chat equals {@link MessageInfo#status()}.
     * Otherwise, it is guaranteed to have a size of participants - 1.
     * In this case it is guaranteed that every value stored in this map for each participant of this chat is equal or higher hierarchically then {@link MessageInfo#status()}.
     * It is important to remember that it is guaranteed that every participant will be present as a key.
     */
    @NonNull
    @Default
    private Map<Contact, MessageStatus> individualStatus = new ConcurrentHashMap<>();

    /**
     * The jid of the sender
     */
    @ProtobufProperty(index = 5, type = STRING, concreteType = ContactJid.class, requiresConversion = true)
    private ContactJid senderJid;

    /**
     * Message C2 timestamp
     */
    @ProtobufProperty(index = 6, type = UINT64)
    private long messageC2STimestamp;

    /**
     * Whether this message should be ignored or counted as an unread message
     */
    @ProtobufProperty(index = 16, type = BOOLEAN)
    private boolean ignore;

    /**
     * Whether this message is starred
     */
    @ProtobufProperty(index = 17, type = BOOLEAN)
    private boolean starred;

    /**
     * Whether this message was sent using a broadcast list
     */
    @ProtobufProperty(index = 18, type = BOOLEAN)
    private boolean broadcast;

    /**
     * Push name
     */
    @ProtobufProperty(index = 19, type = STRING)
    private String pushName;

    /**
     * Media Cipher Text SHA256
     */
    @ProtobufProperty(index = 20, type = BYTES)
    private byte[] mediaCiphertextSha256;

    /**
     * Multicast
     */
    @ProtobufProperty(index = 21, type = BOOLEAN)
    private boolean multicast;

    /**
     * Url text
     */
    @ProtobufProperty(index = 22, type = BOOLEAN)
    private boolean urlText;

    /**
     * Url number
     */
    @ProtobufProperty(index = 23, type = BOOLEAN)
    private boolean urlNumber;

    /**
     * The stub type of this message.
     * This property is populated only if the message that {@link MessageInfo#message} wraps is a {@link ProtocolMessage}.
     */
    @ProtobufProperty(index = 24, type = MESSAGE, concreteType = StubType.class)
    private StubType stubType;

    /**
     * Clear media
     */
    @ProtobufProperty(index = 25, type = BOOLEAN)
    private boolean clearMedia;

    /**
     * Message stub parameters
     */
    @ProtobufProperty(index = 26, type = STRING, repeated = true)
    private List<String> stubParameters;

    /**
     * Duration
     */
    @ProtobufProperty(index = 27, type = UINT32)
    private int duration;

    /**
     * Labels
     */
    @ProtobufProperty(index = 28, type = STRING, repeated = true)
    private List<String> labels;

    /**
     * PaymentInfo
     */
    @ProtobufProperty(index = 29, type = MESSAGE, concreteType = PaymentInfo.class)
    private PaymentInfo paymentInfo;

    /**
     * Final live location
     */
    @ProtobufProperty(index = 30, type = MESSAGE, concreteType = LiveLocationMessage.class)
    private LiveLocationMessage finalLiveLocation;

    /**
     * Quoted payment info
     */
    @ProtobufProperty(index = 31, type = MESSAGE, concreteType = PaymentInfo.class)
    private PaymentInfo quotedPaymentInfo;

    /**
     * Ephemeral start timestamp
     */
    @ProtobufProperty(index = 32, type = UINT64)
    private long ephemeralStartTimestamp;

    /**
     * Ephemeral duration
     */
    @ProtobufProperty(index = 33, type = UINT32)
    private int ephemeralDuration;

    /**
     * Enable ephemeral
     */
    @ProtobufProperty(index = 34, type = BOOLEAN)
    private boolean enableEphemeral;

    /**
     * Ephemeral out of sync
     */
    @ProtobufProperty(index = 35, type = BOOLEAN)
    private boolean ephemeralOutOfSync;

    /**
     * Business privacy status
     */
    @ProtobufProperty(index = 36, type = MESSAGE, concreteType = BusinessPrivacyStatus.class)
    private BusinessPrivacyStatus businessPrivacyStatus;

    /**
     * Business verified name
     */
    @ProtobufProperty(index = 37, type = STRING)
    private String businessVerifiedName;

    /**
     * Media data
     */
    @ProtobufProperty(index = 38, type = MESSAGE, concreteType = MediaData.class)
    private MediaData mediaData;

    /**
     * Photo change
     */
    @ProtobufProperty(index = 39, type = MESSAGE, concreteType = PhotoChange.class)
    private PhotoChange photoChange;

    /**
     * Message receipt
     */
    @ProtobufProperty(index = 40, type = MESSAGE, concreteType = MessageReceipt.class)
    private MessageReceipt receipt;

    /**
     * Reactions
     */
    @ProtobufProperty(index = 41, type = MESSAGE, concreteType = ReactionMessage.class, repeated = true)
    private List<ReactionMessage> reactions;

    /**
     * Media data
     */
    @ProtobufProperty(index = 42, type = MESSAGE, concreteType = MediaData.class)
    private MediaData quotedStickerData;

    /**
     * Upcoming data
     */
    @ProtobufProperty(index = 43, type = BYTES)
    private String futureProofData;

    /**
     * Public service announcement status
     */
    @ProtobufProperty(index = 44, type = MESSAGE, concreteType = PublicServiceAnnouncementStatus.class)
    private PublicServiceAnnouncementStatus psaStatus;

    /**
     * Constructs a new MessageInfo from a MessageKey and a MessageContainer
     *
     * @param key       the key of the message
     * @param container the container of the message
     */
    public MessageInfo(@NonNull MessageKey key, @NonNull MessageContainer container) {
        this.key = key;
        this.timestamp = Clock.now();
        this.status = MessageStatus.PENDING;
        this.message = container;
        this.individualStatus = new ConcurrentHashMap<>();
    }

    public boolean equals(Object object) {
        return object instanceof MessageInfo that && Objects.equals(this.id(), that.id());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id());
    }

    /**
     * Returns the jid of the contact or group that sent the message.
     *
     * @return a non-null ContactJid
     */
    public ContactJid chatJid() {
        return key.chatJid();
    }

    /**
     * Determines whether the message was sent by you or by someone else
     *
     * @return a boolean
     */
    public boolean fromMe() {
        return key.fromMe();
    }

    /**
     * Returns the id of the message
     *
     * @return a non-null String
     */
    public String id() {
        return key.id();
    }

    /**
     * Returns the jid of the sender
     *
     * @return a non-null ContactJid
     */
    public ContactJid senderJid() {
        return requireNonNullElseGet(senderJid, () -> requireNonNullElseGet(key.senderJid(), key::chatJid));
    }

    /**
     * Returns the name of the chat where this message is or its pretty jid
     *
     * @return a non-null String
     */
    @JsonIgnore
    public String chatName() {
        return chat().map(Chat::name)
                .orElseGet(chatJid()::user);
    }

    /**
     * Returns the name of the person that sent this message or its pretty jid
     *
     * @return a non-null String
     */
    @JsonIgnore
    public String senderName() {
        return sender().map(Contact::name)
                .orElseGet(senderJid()::user);
    }

    /**
     * Returns the chat where the message was sent
     *
     * @return an optional wrapping a {@link Chat}
     */
    @JsonIgnore
    public Optional<Chat> chat() {
        return store().findChatByJid(chatJid());
    }

    /**
     * Returns the contact that sent the message
     *
     * @return an optional wrapping a {@link Contact}
     */
    @JsonIgnore
    public Optional<Contact> sender() {
        return store().findContactByJid(senderJid());
    }

    /**
     * Returns an optional {@link MessageInfo} representing the message quoted by this message if said message is in memory
     *
     * @return a non-empty optional {@link MessageInfo} if this message quotes a message in memory
     */
    @JsonIgnore
    public Optional<MessageInfo> quotedMessage() {
        return Optional.of(message)
                .flatMap(MessageContainer::contentWithContext)
                .map(ContextualMessage::contextInfo)
                .flatMap(this::quotedMessage);
    }

    private Optional<MessageInfo> quotedMessage(ContextInfo contextualMessage) {
        var chat = chat().orElseThrow(() -> new NoSuchElementException("Cannot get quoted message: missing chat"));
        return store().findMessageById(chat, contextualMessage.quotedMessageId());
    }

    public Store store() {
        return Objects.requireNonNullElseGet(cachedStore, () -> this.cachedStore = cacheStore());
    }

    private Store cacheStore() {
        return Store.findStoreById(storeId)
                .orElseThrow(() -> new NoSuchElementException("Missing store for id %s".formatted(storeId)));
    }

    /**
     * Checks whether this message wraps a stub type
     *
     * @return true if this message wraps a stub type
     */
    public boolean hasStub() {
        return stubType != null;
    }

    /**
     * The constants of this enumerated type describe the various types of server message that a {@link MessageInfo} can describe
     */
    @AllArgsConstructor
    @Accessors(fluent = true)
    public enum StubType {
        UNKNOWN(0),
        REVOKE(1),
        CIPHERTEXT(2),
        FUTURE_PROOF(3),
        NON_VERIFIED_TRANSITION(4),
        UNVERIFIED_TRANSITION(5),
        VERIFIED_TRANSITION(6),
        VERIFIED_LOW_UNKNOWN(7),
        VERIFIED_HIGH(8),
        VERIFIED_INITIAL_UNKNOWN(9),
        VERIFIED_INITIAL_LOW(10),
        VERIFIED_INITIAL_HIGH(11),
        VERIFIED_TRANSITION_ANY_TO_NONE(12),
        VERIFIED_TRANSITION_ANY_TO_HIGH(13),
        VERIFIED_TRANSITION_HIGH_TO_LOW(14),
        VERIFIED_TRANSITION_HIGH_TO_UNKNOWN(15),
        VERIFIED_TRANSITION_UNKNOWN_TO_LOW(16),
        VERIFIED_TRANSITION_LOW_TO_UNKNOWN(17),
        VERIFIED_TRANSITION_NONE_TO_LOW(18),
        VERIFIED_TRANSITION_NONE_TO_UNKNOWN(19),
        GROUP_CREATE(20),
        GROUP_CHANGE_SUBJECT(21),
        GROUP_CHANGE_ICON(22),
        GROUP_CHANGE_INVITE_LINK(23),
        GROUP_CHANGE_DESCRIPTION(24),
        GROUP_CHANGE_RESTRICT(25),
        GROUP_CHANGE_ANNOUNCE(26),
        GROUP_PARTICIPANT_ADD(27),
        GROUP_PARTICIPANT_REMOVE(28),
        GROUP_PARTICIPANT_PROMOTE(29),
        GROUP_PARTICIPANT_DEMOTE(30),
        GROUP_PARTICIPANT_INVITE(31),
        GROUP_PARTICIPANT_LEAVE(32),
        GROUP_PARTICIPANT_CHANGE_NUMBER(33),
        BROADCAST_CREATE(34),
        BROADCAST_ADD(35),
        BROADCAST_REMOVE(36),
        GENERIC_NOTIFICATION(37),
        E2E_IDENTITY_CHANGED(38),
        E2E_ENCRYPTED(39),
        CALL_MISSED_VOICE(40),
        CALL_MISSED_VIDEO(41),
        INDIVIDUAL_CHANGE_NUMBER(42),
        GROUP_DELETE(43),
        GROUP_ANNOUNCE_MODE_MESSAGE_BOUNCE(44),
        CALL_MISSED_GROUP_VOICE(45),
        CALL_MISSED_GROUP_VIDEO(46),
        PAYMENT_CIPHERTEXT(47),
        PAYMENT_FUTURE_PROOF(48),
        PAYMENT_TRANSACTION_STATUS_UPDATE_FAILED(49),
        PAYMENT_TRANSACTION_STATUS_UPDATE_REFUNDED(50),
        PAYMENT_TRANSACTION_STATUS_UPDATE_REFUND_FAILED(51),
        PAYMENT_TRANSACTION_STATUS_RECEIVER_PENDING_SETUP(52),
        PAYMENT_TRANSACTION_STATUS_RECEIVER_SUCCESS_AFTER_HICCUP(53),
        PAYMENT_ACTION_ACCOUNT_SETUP_REMINDER(54),
        PAYMENT_ACTION_SEND_PAYMENT_REMINDER(55),
        PAYMENT_ACTION_SEND_PAYMENT_INVITATION(56),
        PAYMENT_ACTION_REQUEST_DECLINED(57),
        PAYMENT_ACTION_REQUEST_EXPIRED(58),
        PAYMENT_ACTION_REQUEST_CANCELLED(59),
        BIZ_VERIFIED_TRANSITION_TOP_TO_BOTTOM(60),
        BIZ_VERIFIED_TRANSITION_BOTTOM_TO_TOP(61),
        BIZ_INTRO_TOP(62),
        BIZ_INTRO_BOTTOM(63),
        BIZ_NAME_CHANGE(64),
        BIZ_MOVE_TO_CONSUMER_APP(65),
        BIZ_TWO_TIER_MIGRATION_TOP(66),
        BIZ_TWO_TIER_MIGRATION_BOTTOM(67),
        OVER_SIZED(68),
        GROUP_CHANGE_NO_FREQUENTLY_FORWARDED(69),
        GROUP_V4_ADD_INVITE_SENT(70),
        GROUP_PARTICIPANT_ADD_REQUEST_JOIN(71),
        CHANGE_EPHEMERAL_SETTING(72),
        E2E_DEVICE_CHANGED(73),
        VIEWED_ONCE(74),
        E2E_ENCRYPTED_NOW(75),
        BLUE_MSG_BSP_FB_TO_BSP_PREMISE(76),
        BLUE_MSG_BSP_FB_TO_SELF_FB(77),
        BLUE_MSG_BSP_FB_TO_SELF_PREMISE(78),
        BLUE_MSG_BSP_FB_UNVERIFIED(79),
        BLUE_MSG_BSP_FB_UNVERIFIED_TO_SELF_PREMISE_VERIFIED(80),
        BLUE_MSG_BSP_FB_VERIFIED(81),
        BLUE_MSG_BSP_FB_VERIFIED_TO_SELF_PREMISE_UNVERIFIED(82),
        BLUE_MSG_BSP_PREMISE_TO_SELF_PREMISE(83),
        BLUE_MSG_BSP_PREMISE_UNVERIFIED(84),
        BLUE_MSG_BSP_PREMISE_UNVERIFIED_TO_SELF_PREMISE_VERIFIED(85),
        BLUE_MSG_BSP_PREMISE_VERIFIED(86),
        BLUE_MSG_BSP_PREMISE_VERIFIED_TO_SELF_PREMISE_UNVERIFIED(87),
        BLUE_MSG_CONSUMER_TO_BSP_FB_UNVERIFIED(88),
        BLUE_MSG_CONSUMER_TO_BSP_PREMISE_UNVERIFIED(89),
        BLUE_MSG_CONSUMER_TO_SELF_FB_UNVERIFIED(90),
        BLUE_MSG_CONSUMER_TO_SELF_PREMISE_UNVERIFIED(91),
        BLUE_MSG_SELF_FB_TO_BSP_PREMISE(92),
        BLUE_MSG_SELF_FB_TO_SELF_PREMISE(93),
        BLUE_MSG_SELF_FB_UNVERIFIED(94),
        BLUE_MSG_SELF_FB_UNVERIFIED_TO_SELF_PREMISE_VERIFIED(95),
        BLUE_MSG_SELF_FB_VERIFIED(96),
        BLUE_MSG_SELF_FB_VERIFIED_TO_SELF_PREMISE_UNVERIFIED(97),
        BLUE_MSG_SELF_PREMISE_TO_BSP_PREMISE(98),
        BLUE_MSG_SELF_PREMISE_UNVERIFIED(99),
        BLUE_MSG_SELF_PREMISE_VERIFIED(100),
        BLUE_MSG_TO_BSP_FB(101),
        BLUE_MSG_TO_CONSUMER(102),
        BLUE_MSG_TO_SELF_FB(103),
        BLUE_MSG_UNVERIFIED_TO_BSP_FB_VERIFIED(104),
        BLUE_MSG_UNVERIFIED_TO_BSP_PREMISE_VERIFIED(105),
        BLUE_MSG_UNVERIFIED_TO_SELF_FB_VERIFIED(106),
        BLUE_MSG_UNVERIFIED_TO_VERIFIED(107),
        BLUE_MSG_VERIFIED_TO_BSP_FB_UNVERIFIED(108),
        BLUE_MSG_VERIFIED_TO_BSP_PREMISE_UNVERIFIED(109),
        BLUE_MSG_VERIFIED_TO_SELF_FB_UNVERIFIED(110),
        BLUE_MSG_VERIFIED_TO_UNVERIFIED(111),
        BLUE_MSG_BSP_FB_UNVERIFIED_TO_BSP_PREMISE_VERIFIED(112),
        BLUE_MSG_BSP_FB_UNVERIFIED_TO_SELF_FB_VERIFIED(113),
        BLUE_MSG_BSP_FB_VERIFIED_TO_BSP_PREMISE_UNVERIFIED(114),
        BLUE_MSG_BSP_FB_VERIFIED_TO_SELF_FB_UNVERIFIED(115),
        BLUE_MSG_SELF_FB_UNVERIFIED_TO_BSP_PREMISE_VERIFIED(116),
        BLUE_MSG_SELF_FB_VERIFIED_TO_BSP_PREMISE_UNVERIFIED(117),
        E2E_IDENTITY_UNAVAILABLE(118),
        GROUP_CREATING(119),
        GROUP_CREATE_FAILED(120),
        GROUP_BOUNCED(121),
        BLOCK_CONTACT(122),
        EPHEMERAL_SETTING_NOT_APPLIED(123),
        SYNC_FAILED(124),
        SYNCING(125),
        BIZ_PRIVACY_MODE_INIT_FB(126),
        BIZ_PRIVACY_MODE_INIT_BSP(127),
        BIZ_PRIVACY_MODE_TO_FB(128),
        BIZ_PRIVACY_MODE_TO_BSP(129),
        DISAPPEARING_MODE(130),
        E2E_DEVICE_FETCH_FAILED(131),
        ADMIN_REVOKE(132),
        GROUP_INVITE_LINK_GROWTH_LOCKED(133);

        @Getter
        private final int index;

        @JsonCreator
        public static StubType forIndex(int index) {
            return Arrays.stream(values())
                    .filter(entry -> entry.index() == index)
                    .findFirst()
                    .orElse(null);
        }
    }
}
