package it.auties.whatsapp.model.info;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.whatsapp.model.node.Node;

import java.util.*;

/**
 * The constants of this enumerated type describe the various types of a server message that a {@link ChatMessageInfo} can describe
 */
// TODO: Implement getParameters() and a contextual filter for constants that have the same notificationType and bodyType
@ProtobufEnum
public enum ChatMessageStubType {
    UNKNOWN(0, null, null) {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    REVOKE(1, "revoked", "sender") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    CIPHERTEXT(2, "ciphertext", null) {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    FUTUREPROOF(3, null, "phone") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    NON_VERIFIED_TRANSITION(4, null, "non_verified_transition") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    UNVERIFIED_TRANSITION(5, null, "unverified_transition") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    VERIFIED_TRANSITION(6, null, "verified_transition") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    VERIFIED_LOW_UNKNOWN(7, null, "verified_low_unknown") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    VERIFIED_HIGH(8, null, "verified_high") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    VERIFIED_INITIAL_UNKNOWN(9, null, "verified_initial_unknown") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    VERIFIED_INITIAL_LOW(10, null, "verified_initial_low") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    VERIFIED_INITIAL_HIGH(11, null, "verified_initial_high") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    VERIFIED_TRANSITION_ANY_TO_NONE(12, null, "verified_transition_any_to_none") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    VERIFIED_TRANSITION_ANY_TO_HIGH(13, null, "verified_transition_any_to_high") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    VERIFIED_TRANSITION_HIGH_TO_LOW(14, null, "verified_transition_high_to_low") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    VERIFIED_TRANSITION_HIGH_TO_UNKNOWN(15, null, "verified_transition_high_to_unknown") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    VERIFIED_TRANSITION_UNKNOWN_TO_LOW(16, null, "verified_transition_unknown_to_low") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    VERIFIED_TRANSITION_LOW_TO_UNKNOWN(17, null, "verified_transition_low_to_unknown") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    VERIFIED_TRANSITION_NONE_TO_LOW(18, null, "verified_transition_none_to_low") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    VERIFIED_TRANSITION_NONE_TO_UNKNOWN(19, null, "verified_transition_none_to_unknown") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    GROUP_CREATE(20, "gp2", "create") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    GROUP_CHANGE_SUBJECT(21, "gp2", "subject") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    GROUP_CHANGE_ICON(22, "gp2", "picture") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    GROUP_CHANGE_INVITE_LINK(23, "gp2", "revoke_invite") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    GROUP_CHANGE_DESCRIPTION(24, "gp2", "description") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    GROUP_CHANGE_RESTRICT(25, "gp2", "restrict") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    GROUP_CHANGE_ANNOUNCE(26, "gp2", "announce") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    GROUP_PARTICIPANT_ADD(27, "gp2", "add") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    GROUP_PARTICIPANT_REMOVE(28, "gp2", "remove") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    GROUP_PARTICIPANT_PROMOTE(29, "gp2", "promote") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    GROUP_PARTICIPANT_DEMOTE(30, "gp2", "demote") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    GROUP_PARTICIPANT_INVITE(31, "gp2", "invite") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    GROUP_PARTICIPANT_LEAVE(32, "gp2", "leave") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    GROUP_PARTICIPANT_CHANGE_NUMBER(33, "gp2", "modify") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BROADCAST_CREATE(34, "broadcast_notification", "create") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BROADCAST_ADD(35, "broadcast_notification", "add") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BROADCAST_REMOVE(36, "broadcast_notification", "remove") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    GENERIC_NOTIFICATION(37, "notification", null) {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    E2E_IDENTITY_CHANGED(38, "e2e_notification", "identity") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    E2E_ENCRYPTED(39, "e2e_notification", "encrypt") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    CALL_MISSED_VOICE(40, "call_log", "miss") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    CALL_MISSED_VIDEO(41, "call_log", "miss_video") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    INDIVIDUAL_CHANGE_NUMBER(42, null, "change_number") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    GROUP_DELETE(43, "gp2", "delete") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    GROUP_ANNOUNCE_MODE_MESSAGE_BOUNCE(44, "gp2", "announce_msg_bounce") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    CALL_MISSED_GROUP_VOICE(45, "call_log", "miss_group") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    CALL_MISSED_GROUP_VIDEO(46, "call_log", "miss_group_video") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    PAYMENT_CIPHERTEXT(47, "payment", "ciphertext") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    PAYMENT_FUTUREPROOF(48, "payment", "futureproof") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    PAYMENT_TRANSACTION_STATUS_UPDATE_FAILED(49, null, "payment_transaction_status_update_failed") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    PAYMENT_TRANSACTION_STATUS_UPDATE_REFUNDED(50, null, "payment_transaction_status_update_refunded") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    PAYMENT_TRANSACTION_STATUS_UPDATE_REFUND_FAILED(51, null, "payment_transaction_status_update_refund_failed") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    PAYMENT_TRANSACTION_STATUS_RECEIVER_PENDING_SETUP(52, null, "payment_transaction_status_receiver_pending_setup") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    PAYMENT_TRANSACTION_STATUS_RECEIVER_SUCCESS_AFTER_HICCUP(53, null, "payment_transaction_status_receiver_success_after_hiccup") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    PAYMENT_ACTION_ACCOUNT_SETUP_REMINDER(54, null, "payment_action_account_setup_reminder") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    PAYMENT_ACTION_SEND_PAYMENT_REMINDER(55, null, "payment_action_send_payment_reminder") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    PAYMENT_ACTION_SEND_PAYMENT_INVITATION(56, null, "payment_action_send_payment_invitation") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    PAYMENT_ACTION_REQUEST_DECLINED(57, null, "payment_action_request_declined") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    PAYMENT_ACTION_REQUEST_EXPIRED(58, null, "payment_action_request_expired") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    PAYMENT_ACTION_REQUEST_CANCELLED(59, null, "payment_transaction_request_cancelled") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BIZ_VERIFIED_TRANSITION_TOP_TO_BOTTOM(60, null, "biz_verified_transition_top_to_bottom") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BIZ_VERIFIED_TRANSITION_BOTTOM_TO_TOP(61, null, "biz_verified_transition_bottom_to_top") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BIZ_INTRO_TOP(62, null, "biz_intro_top") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BIZ_INTRO_BOTTOM(63, null, "biz_intro_bottom") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BIZ_NAME_CHANGE(64, null, "biz_name_change") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BIZ_MOVE_TO_CONSUMER_APP(65, null, "biz_move_to_consumer_app") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BIZ_TWO_TIER_MIGRATION_TOP(66, null, "biz_two_tier_migration_top") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BIZ_TWO_TIER_MIGRATION_BOTTOM(67, null, "biz_two_tier_migration_bottom") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    OVERSIZED(68, "oversized", null) {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    GROUP_CHANGE_NO_FREQUENTLY_FORWARDED(69, "gp2", "no_frequently_forwarded") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    GROUP_V4_ADD_INVITE_SENT(70, "gp2", "v4_add_invite_sent") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    GROUP_PARTICIPANT_ADD_REQUEST_JOIN(71, "gp2", "v4_add_invite_join") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    CHANGE_EPHEMERAL_SETTING(72, "gp2", "ephemeral") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    E2E_DEVICE_CHANGED(73, "e2e_notification", "device") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    VIEWED_ONCE(74, null, null) {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    E2E_ENCRYPTED_NOW(75, "e2e_notification", "encrypt_now") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BLUE_MSG_BSP_FB_TO_BSP_PREMISE(76, null, "blue_msg_bsp_fb_to_bsp_premise") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BLUE_MSG_BSP_FB_TO_SELF_FB(77, null, "blue_msg_bsp_fb_to_self_fb") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BLUE_MSG_BSP_FB_TO_SELF_PREMISE(78, null, "blue_msg_bsp_fb_to_self_premise") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BLUE_MSG_BSP_FB_UNVERIFIED(79, null, "blue_msg_bsp_fb_unverified") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BLUE_MSG_BSP_FB_UNVERIFIED_TO_SELF_PREMISE_VERIFIED(80, null, "blue_msg_bsp_fb_unverified_to_self_premise_verified") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BLUE_MSG_BSP_FB_VERIFIED(81, null, "blue_msg_bsp_fb_verified") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BLUE_MSG_BSP_FB_VERIFIED_TO_SELF_PREMISE_UNVERIFIED(82, null, "blue_msg_bsp_fb_verified_to_self_premise_unverified") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BLUE_MSG_BSP_PREMISE_TO_SELF_PREMISE(83, null, "blue_msg_bsp_premise_to_self_premise") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BLUE_MSG_BSP_PREMISE_UNVERIFIED(84, null, "blue_msg_bsp_premise_unverified") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BLUE_MSG_BSP_PREMISE_UNVERIFIED_TO_SELF_PREMISE_VERIFIED(85, null, "blue_msg_bsp_premise_unverified_to_self_premise_verified") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BLUE_MSG_BSP_PREMISE_VERIFIED(86, null, "blue_msg_bsp_premise_verified") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BLUE_MSG_BSP_PREMISE_VERIFIED_TO_SELF_PREMISE_UNVERIFIED(87, null, "blue_msg_bsp_premise_verified_to_self_premise_unverified") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BLUE_MSG_CONSUMER_TO_BSP_FB_UNVERIFIED(88, null, "blue_msg_consumer_to_bsp_fb_unverified") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BLUE_MSG_CONSUMER_TO_BSP_PREMISE_UNVERIFIED(89, null, "blue_msg_consumer_to_bsp_premise_unverified") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BLUE_MSG_CONSUMER_TO_SELF_FB_UNVERIFIED(90, null, "blue_msg_consumer_to_self_fb_unverified") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BLUE_MSG_CONSUMER_TO_SELF_PREMISE_UNVERIFIED(91, null, "blue_msg_consumer_to_self_premise_unverified") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BLUE_MSG_SELF_FB_TO_BSP_PREMISE(92, null, "blue_msg_self_fb_to_bsp_premise") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BLUE_MSG_SELF_FB_TO_SELF_PREMISE(93, null, "blue_msg_self_fb_to_self_premise") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BLUE_MSG_SELF_FB_UNVERIFIED(94, null, "blue_msg_self_fb_unverified") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BLUE_MSG_SELF_FB_UNVERIFIED_TO_SELF_PREMISE_VERIFIED(95, null, "blue_msg_self_fb_unverified_to_self_premise_verified") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BLUE_MSG_SELF_FB_VERIFIED(96, null, "blue_msg_self_fb_verified") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BLUE_MSG_SELF_FB_VERIFIED_TO_SELF_PREMISE_UNVERIFIED(97, null, "blue_msg_self_fb_verified_to_self_premise_unverified") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BLUE_MSG_SELF_PREMISE_TO_BSP_PREMISE(98, null, "blue_msg_self_premise_to_bsp_premise") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BLUE_MSG_SELF_PREMISE_UNVERIFIED(99, null, "blue_msg_self_premise_unverified") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BLUE_MSG_SELF_PREMISE_VERIFIED(100, null, "blue_msg_self_premise_verified") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BLUE_MSG_TO_BSP_FB(101, null, "blue_msg_to_bsp_fb") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BLUE_MSG_TO_CONSUMER(102, null, "blue_msg_to_consumer") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BLUE_MSG_TO_SELF_FB(103, null, "blue_msg_to_self_fb") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BLUE_MSG_UNVERIFIED_TO_BSP_FB_VERIFIED(104, null, "blue_msg_unverified_to_bsp_fb_verified") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BLUE_MSG_UNVERIFIED_TO_BSP_PREMISE_VERIFIED(105, null, "blue_msg_unverified_to_bsp_premise_verified") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BLUE_MSG_UNVERIFIED_TO_SELF_FB_VERIFIED(106, null, "blue_msg_unverified_to_self_fb_verified") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BLUE_MSG_UNVERIFIED_TO_VERIFIED(107, null, "blue_msg_unverified_to_verified") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BLUE_MSG_VERIFIED_TO_BSP_FB_UNVERIFIED(108, null, "blue_msg_verified_to_bsp_fb_unverified") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BLUE_MSG_VERIFIED_TO_BSP_PREMISE_UNVERIFIED(109, null, "blue_msg_verified_to_bsp_premise_unverified") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BLUE_MSG_VERIFIED_TO_SELF_FB_UNVERIFIED(110, null, "blue_msg_verified_to_self_fb_unverified") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BLUE_MSG_VERIFIED_TO_UNVERIFIED(111, null, "blue_msg_verified_to_unverified") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BLUE_MSG_BSP_FB_UNVERIFIED_TO_BSP_PREMISE_VERIFIED(112, null, "blue_msg_bsp_fb_unverified_to_bsp_premise_verified") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BLUE_MSG_BSP_FB_UNVERIFIED_TO_SELF_FB_VERIFIED(113, null, "blue_msg_bsp_fb_unverified_to_self_fb_verified") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BLUE_MSG_BSP_FB_VERIFIED_TO_BSP_PREMISE_UNVERIFIED(114, null, "blue_msg_bsp_fb_verified_to_bsp_premise_unverified") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BLUE_MSG_BSP_FB_VERIFIED_TO_SELF_FB_UNVERIFIED(115, null, "blue_msg_bsp_fb_verified_to_self_fb_unverified") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BLUE_MSG_SELF_FB_UNVERIFIED_TO_BSP_PREMISE_VERIFIED(116, null, "blue_msg_self_fb_unverified_to_bsp_premise_verified") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BLUE_MSG_SELF_FB_VERIFIED_TO_BSP_PREMISE_UNVERIFIED(117, null, "blue_msg_self_fb_verified_to_bsp_premise_unverified") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    E2E_IDENTITY_UNAVAILABLE(118, "e2e_notification", "e2e_identity_unavailable") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    GROUP_CREATING(119, null, null) {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    GROUP_CREATE_FAILED(120, null, null) {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    GROUP_BOUNCED(121, null, null) {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BLOCK_CONTACT(122, null, "block_contact") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    EPHEMERAL_SETTING_NOT_APPLIED(123, null, null) {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    SYNC_FAILED(124, null, null) {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    SYNCING(125, null, null) {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BIZ_PRIVACY_MODE_INIT_FB(126, null, "biz_privacy_mode_init_fb") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BIZ_PRIVACY_MODE_INIT_BSP(127, null, "biz_privacy_mode_init_bsp") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BIZ_PRIVACY_MODE_TO_FB(128, null, "biz_privacy_mode_to_fb") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BIZ_PRIVACY_MODE_TO_BSP(129, null, "biz_privacy_mode_to_bsp") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    DISAPPEARING_MODE(130, null, "disappearing_mode") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    E2E_DEVICE_FETCH_FAILED(131, null, null) {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    ADMIN_REVOKE(132, "revoked", "admin") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    GROUP_INVITE_LINK_GROWTH_LOCKED(133, "gp2", "growth_locked") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    COMMUNITY_LINK_PARENT_GROUP(134, "gp2", "parent_group_link") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    COMMUNITY_LINK_SIBLING_GROUP(135, "gp2", "sibling_group_link") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    COMMUNITY_LINK_SUB_GROUP(136, "gp2", "sub_group_link") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    COMMUNITY_UNLINK_PARENT_GROUP(137, "gp2", "parent_group_unlink") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    COMMUNITY_UNLINK_SIBLING_GROUP(138, "gp2", "sibling_group_unlink") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    COMMUNITY_UNLINK_SUB_GROUP(139, "gp2", "sub_group_unlink") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    GROUP_PARTICIPANT_ACCEPT(140, null, null) {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    GROUP_PARTICIPANT_LINKED_GROUP_JOIN(141, "gp2", "linked_group_join") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    COMMUNITY_CREATE(142, "gp2", "community_create") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    EPHEMERAL_KEEP_IN_CHAT(143, "gp2", "ephemeral_keep_in_chat") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    GROUP_MEMBERSHIP_JOIN_APPROVAL_REQUEST(144, "gp2", "membership_approval_request") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    GROUP_MEMBERSHIP_JOIN_APPROVAL_MODE(145, "gp2", "membership_approval_mode") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    INTEGRITY_UNLINK_PARENT_GROUP(146, "gp2", "integrity_parent_group_unlink") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    COMMUNITY_PARTICIPANT_PROMOTE(147, "gp2", "linked_group_promote") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    COMMUNITY_PARTICIPANT_DEMOTE(148, "gp2", "linked_group_demote") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    COMMUNITY_PARENT_GROUP_DELETED(149, "gp2", "delete_parent_group") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    COMMUNITY_LINK_PARENT_GROUP_MEMBERSHIP_APPROVAL(150, null, null) {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    GROUP_PARTICIPANT_JOINED_GROUP_AND_PARENT_GROUP(151, "gp2", "auto_add") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    MASKED_THREAD_CREATED(152, null, "masked_thread_created") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    MASKED_THREAD_UNMASKED(153, null, null) {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BIZ_CHAT_ASSIGNMENT(154, null, "chat_assignment") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    CHAT_PSA(155, "e2e_notification", "chat_psa") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    CHAT_POLL_CREATION_MESSAGE(156, null, null) {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    CAG_MASKED_THREAD_CREATED(157, null, "cag_masked_thread_created") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    // WhatsappWeb has this on "gp2" and "subject", but it would be a duplicate of GROUP_CHANGE_SUBJECT
    COMMUNITY_PARENT_GROUP_SUBJECT_CHANGED(158, null, null) {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    CAG_INVITE_AUTO_ADD(159, "gp2", "invite_auto_add") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BIZ_CHAT_ASSIGNMENT_UNASSIGN(160, null, "chat_assignment_unassign") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    // WhatsappWeb has this on gp2 and invite_auto_add, but it woudd be a duplicate of CAG_INVITE_AUTO_ADD
    CAG_INVITE_AUTO_JOINED(161, null, null) {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    SCHEDULED_CALL_START_MESSAGE(162, null, null) {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    COMMUNITY_INVITE_RICH(163, "gp2", "community_invite_rich") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    COMMUNITY_INVITE_AUTO_ADD_RICH(164, "gp2", "community_invite_auto_add_rich") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    SUB_GROUP_INVITE_RICH(165, "gp2", "sub_group_invite_rich") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    SUB_GROUP_PARTICIPANT_ADD_RICH(166, "gp2", "sub_group_participant_add_rich") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    COMMUNITY_LINK_PARENT_GROUP_RICH(167, "gp2", "community_link_parent_group_rich") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    COMMUNITY_PARTICIPANT_ADD_RICH(168, "gp2", "community_participant_add_rich") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    SILENCED_UNKNOWN_CALLER_AUDIO(169, "call_log", "silence") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    // WhatsappWeb has this on "call_log" and silence", but it would be a duplicate of SILENCED_UNKNOWN_CALLER_AUDIO
    SILENCED_UNKNOWN_CALLER_VIDEO(170, null, null) {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    GROUP_MEMBER_ADD_MODE(171, "gp2", "member_add_mode") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    GROUP_MEMBERSHIP_JOIN_APPROVAL_REQUEST_NON_ADMIN_ADD(172, "gp2", "created_membership_requests") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    COMMUNITY_CHANGE_DESCRIPTION(173, "gp2", "parent_group_description") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    SENDER_INVITE(174, null, "sender_invite") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    RECEIVER_INVITE(175, null, "receiver_invite") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    COMMUNITY_ALLOW_MEMBER_ADDED_GROUPS(176, "gp2", "allow_non_admin_sub_group_creation") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    PINNED_MESSAGE_IN_CHAT(177, "pinned_message", null) {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    PAYMENT_INVITE_SETUP_INVITER(178, null, null) {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    PAYMENT_INVITE_SETUP_INVITEE_RECEIVE_ONLY(179, null, null) {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    PAYMENT_INVITE_SETUP_INVITEE_SEND_AND_RECEIVE(180, null, null) {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    LINKED_GROUP_CALL_START(181, null, null) {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    REPORT_TO_ADMIN_ENABLED_STATUS(182, "gp2", "allow_admin_reports") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    EMPTY_SUBGROUP_CREATE(183, "gp2", "empty_subgroup_create") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    SCHEDULED_CALL_CANCEL(184, null, null) {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    SUBGROUP_ADMIN_TRIGGERED_AUTO_ADD_RICH(185, "gp2", "subgroup_admin_triggered_auto_add") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    GROUP_CHANGE_RECENT_HISTORY_SHARING(186, null, null) {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    PAID_MESSAGE_SERVER_CAMPAIGN_ID(187, null, null) {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    GENERAL_CHAT_CREATE(188, null, null) {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    GENERAL_CHAT_ADD(189, "gp2", "general_chat_add") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    GENERAL_CHAT_AUTO_ADD_DISABLED(190, "gp2", "general_chat_auto_add_disabled") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    SUGGESTED_SUBGROUP_ANNOUNCE(191, "gp2", "created_subgroup_suggestion") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BIZ_BOT_1P_MESSAGING_ENABLED(192, "notification_template", "biz_bot_1p_disclosure") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    CHANGE_USERNAME(193, null, "change_username") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BIZ_COEX_PRIVACY_INIT_SELF(194, "notification_template", "biz_me_account_type_is_hosted") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BIZ_COEX_PRIVACY_TRANSITION_SELF(195, "notification_template", "biz_me_account_type_is_hosted_transition") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    SUPPORT_AI_EDUCATION(196, "notification_template", "saga_init") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BIZ_BOT_3P_MESSAGING_ENABLED(197, "notification_template", "biz_bot_3p_disclosure") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    REMINDER_SETUP_MESSAGE(198, null, null) {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    REMINDER_SENT_MESSAGE(199, null, null) {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    REMINDER_CANCEL_MESSAGE(200, null, null) {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BIZ_COEX_PRIVACY_INIT(201, "notification_template", "biz_account_type_is_hosted") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    BIZ_COEX_PRIVACY_TRANSITION(202, "notification_template", "biz_account_type_changed_to_hosted") {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    GROUP_DEACTIVATED(203, null, null) {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    },
    COMMUNITY_DEACTIVATE_SIBLING_GROUP(204, null, null) {
        @Override
        public List<String> getParameters(Node node) {
            return List.of();
        }
    };

    final int index;
    private final String notificationType;
    private final String bodyType;

    private static final Map<String, Map<String, ChatMessageStubType>> typedConstantsLookup = new HashMap<>();
    private static final Map<String, ChatMessageStubType> typedDefaultsLookup = new HashMap<>();
    private static final Map<String, ChatMessageStubType> anyTypeConstantsLookup = new HashMap<>();
    static {
        for (var stubType : values()) {
            if (stubType.notificationType != null) {
                if (stubType.bodyType == null) {
                    var existing = typedDefaultsLookup.put(stubType.notificationType, stubType);
                    if(existing != null) {
                        throw new IllegalStateException("Conflict between stub type " +  stubType + " and " + existing);
                    }
                } else {
                    typedConstantsLookup.compute(stubType.notificationType, (key, value) -> {
                        if (value == null) {
                            value = new HashMap<>();
                        }
                        var existing = value.put(stubType.bodyType, stubType);
                        if (existing != null) {
                            throw new IllegalStateException("Conflict between stub type " + stubType + " and " + existing);
                        }
                        return value;
                    });
                }
            } else if (stubType.bodyType != null) {
                var existing = anyTypeConstantsLookup.put(stubType.bodyType, stubType);
                if(existing != null) {
                    throw new IllegalStateException("Conflict between stub type " +  stubType + " and " + existing);
                }
            }
        }
    }

    public static ChatMessageStubType getStubType(String notificationType, String bodyType) {
        if(bodyType == null) {
            return Objects.requireNonNullElse(typedDefaultsLookup.get(notificationType), UNKNOWN);
        }

        var subTypeLookup = typedConstantsLookup.get(notificationType);
        if(subTypeLookup == null) {
            return UNKNOWN;
        }

        var subTypeResult = subTypeLookup.get(bodyType);
        if(subTypeResult != null) {
            return subTypeResult;
        }

        return anyTypeConstantsLookup.getOrDefault(bodyType, UNKNOWN);
    }

    ChatMessageStubType(@ProtobufEnumIndex int index, String notificationType, String bodyType) {
        this.index = index;
        this.notificationType = notificationType;
        this.bodyType = bodyType;
    }

    public abstract List<String> getParameters(Node node);
}