package it.auties.whatsapp.protobuf.info;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.protobuf.contact.Contact;
import it.auties.whatsapp.protobuf.contact.ContactJid;
import it.auties.whatsapp.protobuf.message.model.*;
import it.auties.whatsapp.protobuf.message.server.ProtocolMessage;
import it.auties.whatsapp.protobuf.message.standard.LiveLocationMessage;
import it.auties.whatsapp.api.Unsupported;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.Delegate;

import java.time.Instant;
import java.util.*;

/**
 * A model class that holds the information related to a {@link Message}.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(builderMethodName = "newMessageInfo", buildMethodName = "create")
@Accessors(fluent = true)
public final class MessageInfo implements WhatsappInfo {
  /**
   * The MessageKey of this message
   */
  @JsonProperty(value = "1", required = true)
  @NonNull
  @Delegate
  private MessageKey key;

  /**
   * The container of this message
   */
  @JsonProperty("2")
  @NonNull
  @Builder.Default
  private MessageContainer content = new MessageContainer();

  /**
   * A map that holds the read status of this message for each participant.
   * If the chat associated with this chat is not a group, this map's size will always be 1.
   * In this case it is guaranteed that the value stored in this map for the contact associated with this chat equals {@link MessageInfo#globalStatus()}.
   * Otherwise, it is guaranteed to have a size of participants - 1.
   * In this case it is guaranteed that every value stored in this map for each participant of this chat is equal or higher hierarchically then {@link MessageInfo#globalStatus()}.
   * It is important to remember that it is guaranteed that every participant will be present as a key.
   */
  @NonNull
  @Builder.Default
  private Map<Contact, MessageStatus> individualReadStatus = new HashMap<>();

  /**
   * The global status of this message.
   * If the chat associated with this message is a group it is guaranteed that this field is equal or lower hierarchically then every value stored by {@link MessageInfo#individualReadStatus()}.
   * Otherwise, this field is guaranteed to be equal to the single value stored by {@link MessageInfo#individualReadStatus()} for the contact associated with the chat associated with this message.
   */
  @JsonProperty("4")
  @NonNull
  @Builder.Default
  private MessageStatus globalStatus = MessageStatus.ERROR;

  /**
   * The timestamp, that is the seconds since {@link java.time.Instant#EPOCH}, when this message was sent
   */
  @JsonProperty("3")
  private long timestamp;

  /**
   * The jid of the participant that sent the message in a group.
   * This property is only populated if {@link MessageInfo#chat()} refers to a group.
   */
  @JsonProperty("5")
  private ContactJid senderId;

  /**
   * Duration
   */
  @JsonProperty("27")
  private int duration;

  /**
   * Whether this message should be ignored or counted as an unread message
   */
  @JsonProperty("16")
  private boolean ignore;

  /**
   * Whether this message is starred
   */
  @JsonProperty("17")
  private boolean starred;

  /**
   * Whether this message was sent using a broadcast list
   */
  @JsonProperty("18")
  private boolean broadcast;

  /**
   * Multicast
   */
  @JsonProperty("21")
  private boolean multicast;

  /**
   * Url text
   */
  @JsonProperty("22")
  private boolean urlText;

  /**
   * Url number
   */
  @JsonProperty("23")
  private boolean urlNumber;

  /**
   * Clear media
   */
  @JsonProperty("25")
  private boolean clearMedia;

  /**
   * Push name
   */
  @JsonProperty("19")
  private String pushName;

  /**
   * Ephemeral start timestamp
   */
  @JsonProperty("32")
  private long ephemeralStartTimestamp;

  /**
   * Ephemeral duration
   */
  @JsonProperty("33")
  private int ephemeralDuration;

  /**
   * The stub type of this message.
   * This property is populated only if the message that {@link MessageInfo#content} wraps is a {@link ProtocolMessage}.
   */
  @JsonProperty("24")
  private StubType stubType;

  /**
   * Message stub parameters
   */
  @JsonProperty("26")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<String> stubParameters;

  /**
   * Labels
   */
  @JsonProperty("28")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<String> labels;

  /**
   * PaymentInfo
   */
  @JsonProperty("29")
  @Unsupported
  private PaymentInfo paymentInfo;

  /**
   * Final live location
   */
  @JsonProperty("30")
  @Unsupported
  private LiveLocationMessage finalLiveLocation;

  /**
   * Quoted payment info
   */
  @JsonProperty("31")
  @Unsupported
  private PaymentInfo quotedPaymentInfo;

  /**
   * Media Cipher Text SHA256
   */
  @JsonProperty("20")
  @Unsupported
  private byte[] mediaCiphertextSha256;

  /**
   * Constructs a new MessageInfo from a MessageKey and a MessageContainer
   *
   * @param key       the key of the message
   * @param container the container of the message
   */
  public MessageInfo(@NonNull MessageKey key, @NonNull MessageContainer container) {
    this.key = key;
    this.timestamp = Instant.now().getEpochSecond();
    this.globalStatus = MessageStatus.PENDING;
    this.content = container;
    this.individualReadStatus = new HashMap<>();
  }

  /**
   * Checks whether this message wraps a stub type
   *
   * @return true if this message wraps a stub type
   */
  public boolean hasStub(){
    return stubType != null;
  }

  /**
   * Returns the jid of the contact that sent the message
   *
   * @return a non-null ContactId
   */
  public ContactJid senderJid(){
    return Objects.requireNonNullElse(senderId, chatId());
  }

  /**
   * Returns the contact that sent the message
   *
   * @return an optional wrapping a {@link Contact}
   */
  public Optional<Contact> sender(){
    return key.store()
            .findContactByJid(senderJid().toString());
  }

  /**
   * Returns an optional {@link MessageInfo} representing the message quoted by this message if said message is in memory
   *
   * @return a non-empty optional {@link MessageInfo} if this message quotes a message in memory
   */
  public Optional<MessageInfo> quotedMessage(){
    return Optional.of(content)
            .flatMap(MessageContainer::contentWithContext)
            .map(ContextualMessage::contextInfo)
            .flatMap(contextualMessage -> key.store().findMessageById(key.chat().orElseThrow(), contextualMessage.quotedMessageId()));
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
    CHANGE_EPHEMERAL_SETTING(72);

    private final @Getter int index;

    @JsonCreator
    public static StubType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
