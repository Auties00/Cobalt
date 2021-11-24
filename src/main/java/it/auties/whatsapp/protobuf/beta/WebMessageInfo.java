package it.auties.whatsapp.protobuf.beta;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class WebMessageInfo {

  @JsonProperty(value = "39", required = false)
  @JsonPropertyDescription("PhotoChange")
  private PhotoChange photoChange;

  @JsonProperty(value = "38", required = false)
  @JsonPropertyDescription("MediaData")
  private MediaData mediaData;

  @JsonProperty(value = "37", required = false)
  @JsonPropertyDescription("string")
  private String verifiedBizName;

  @JsonProperty(value = "36", required = false)
  @JsonPropertyDescription("WebMessageInfoBizPrivacyStatus")
  private WebMessageInfoBizPrivacyStatus bizPrivacyStatus;

  @JsonProperty(value = "35", required = false)
  @JsonPropertyDescription("bool")
  private boolean ephemeralOutOfSync;

  @JsonProperty(value = "34", required = false)
  @JsonPropertyDescription("bool")
  private boolean ephemeralOffToOn;

  @JsonProperty(value = "33", required = false)
  @JsonPropertyDescription("uint32")
  private int ephemeralDuration;

  @JsonProperty(value = "32", required = false)
  @JsonPropertyDescription("uint64")
  private long ephemeralStartTimestamp;

  @JsonProperty(value = "31", required = false)
  @JsonPropertyDescription("PaymentInfo")
  private PaymentInfo quotedPaymentInfo;

  @JsonProperty(value = "30", required = false)
  @JsonPropertyDescription("LiveLocationMessage")
  private LiveLocationMessage finalLiveLocation;

  @JsonProperty(value = "29", required = false)
  @JsonPropertyDescription("PaymentInfo")
  private PaymentInfo paymentInfo;

  @JsonProperty(value = "28", required = false)
  @JsonPropertyDescription("string")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<String> labels;

  @JsonProperty(value = "27", required = false)
  @JsonPropertyDescription("uint32")
  private int duration;

  @JsonProperty(value = "26", required = false)
  @JsonPropertyDescription("string")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<String> messageStubParameters;

  @JsonProperty(value = "25", required = false)
  @JsonPropertyDescription("bool")
  private boolean clearMedia;

  @JsonProperty(value = "24", required = false)
  @JsonPropertyDescription("WebMessageInfoStubType")
  private WebMessageInfoStubType messageStubType;

  @JsonProperty(value = "23", required = false)
  @JsonPropertyDescription("bool")
  private boolean urlNumber;

  @JsonProperty(value = "22", required = false)
  @JsonPropertyDescription("bool")
  private boolean urlText;

  @JsonProperty(value = "21", required = false)
  @JsonPropertyDescription("bool")
  private boolean multicast;

  @JsonProperty(value = "20", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] mediaCiphertextSha256;

  @JsonProperty(value = "19", required = false)
  @JsonPropertyDescription("string")
  private String pushName;

  @JsonProperty(value = "18", required = false)
  @JsonPropertyDescription("bool")
  private boolean broadcast;

  @JsonProperty(value = "17", required = false)
  @JsonPropertyDescription("bool")
  private boolean starred;

  @JsonProperty(value = "16", required = false)
  @JsonPropertyDescription("bool")
  private boolean ignore;

  @JsonProperty(value = "6", required = false)
  @JsonPropertyDescription("uint64")
  private long messageC2STimestamp;

  @JsonProperty(value = "5", required = false)
  @JsonPropertyDescription("string")
  private String participant;

  @JsonProperty(value = "4", required = false)
  @JsonPropertyDescription("WebMessageInfoStatus")
  private WebMessageInfoStatus status;

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("uint64")
  private long messageTimestamp;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("Message")
  private Message message;

  @JsonProperty(value = "1", required = true)
  @JsonPropertyDescription("MessageKey")
  private MessageKey key;

  @Accessors(fluent = true)
  public enum WebMessageInfoStatus {
    ERROR(0),
    PENDING(1),
    SERVER_ACK(2),
    DELIVERY_ACK(3),
    READ(4),
    PLAYED(5);

    private final @Getter int index;

    WebMessageInfoStatus(int index) {
      this.index = index;
    }

    @JsonCreator
    public static WebMessageInfoStatus forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }

  @Accessors(fluent = true)
  public enum WebMessageInfoStubType {
    UNKNOWN(0),
    REVOKE(1),
    CIPHERTEXT(2),
    FUTUREPROOF(3),
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
    PAYMENT_FUTUREPROOF(48),
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
    OVERSIZED(68),
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
    BIZ_PRIVACY_MODE_TO_BSP(129);

    private final @Getter int index;

    WebMessageInfoStubType(int index) {
      this.index = index;
    }

    @JsonCreator
    public static WebMessageInfoStubType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }

  @Accessors(fluent = true)
  public enum WebMessageInfoBizPrivacyStatus {
    E2EE(0),
    FB(2),
    BSP(1),
    BSP_AND_FB(3);

    private final @Getter int index;

    WebMessageInfoBizPrivacyStatus(int index) {
      this.index = index;
    }

    @JsonCreator
    public static WebMessageInfoBizPrivacyStatus forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
