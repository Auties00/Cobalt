package com.github.auties00.cobalt.wire.linked.props;

import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * Represents an A/B testing property (AB prop) definition with its configuration code and default values.
 *
 * <p>AB props are feature flags and configuration values that WhatsApp uses to control client behavior,
 * enable/disable features, and conduct A/B testing experiments. Each prop definition consists of:
 * <ul>
 * <li>A numeric {@code code} that uniquely identifies the property
 * <li>A {@code defaultValue} string used when the server has not sent a value for this prop
 * <li>A {@code debugDefaultValue} string used in place of {@code defaultValue} when the user
 *     has joined the WhatsApp Web Beta programme
 * </ul>
 *
 * <p>Both default values are always strings, matching the format in which values are received from
 * the server. Static conversion methods are provided to parse the string into typed values
 * (boolean, int, long, double).
 *
 * <p>This record is immutable and thread-safe.
 *
 * <p>The constants in this record are generated automatically by {@code tools/web/ab-props-codegen}
 * from the {@code WAWebABPropsConfigs} module; each constant's documentation records its source
 * definition. Do not edit the constants manually.
 *
 * @param code              the unique numeric identifier for this configuration property
 * @param defaultValue      the production default value to use when the server has not provided
 *                          a value for this property, must not be {@code null}
 * @param debugDefaultValue the debug/beta default value used when the user has joined the
 *                          WhatsApp Web Beta programme, must not be {@code null}
 */
public record ABProp(int code, String defaultValue, String debugDefaultValue) {

    /**
     * The {@code status_video_max_duration} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: status_video_max_duration:[175,"int",30,30]
     */
    public static final ABProp STATUS_VIDEO_MAX_DURATION = new ABProp(175, "30", "30");

    /**
     * The {@code upload_document_thumb_mms_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: upload_document_thumb_mms_enabled:[247,"bool",!1,!0]
     */
    public static final ABProp UPLOAD_DOCUMENT_THUMB_MMS_ENABLED = new ABProp(247, "false", "true");

    /**
     * The {@code download_status_thumb_mms_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: download_status_thumb_mms_enabled:[249,"bool",!1,!1]
     */
    public static final ABProp DOWNLOAD_STATUS_THUMB_MMS_ENABLED = new ABProp(249, "false", "false");

    /**
     * The {@code download_document_thumb_mms_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: download_document_thumb_mms_enabled:[250,"bool",!1,!0]
     */
    public static final ABProp DOWNLOAD_DOCUMENT_THUMB_MMS_ENABLED = new ABProp(250, "false", "true");

    /**
     * The {@code md_icdc_hash_length} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: md_icdc_hash_length:[310,"int",10,10]
     */
    public static final ABProp MD_ICDC_HASH_LENGTH = new ABProp(310, "10", "10");

    /**
     * The {@code smb_collections_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_collections_enabled:[451,"bool",!1,!0]
     */
    public static final ABProp SMB_COLLECTIONS_ENABLED = new ABProp(451, "false", "true");

    /**
     * The {@code disappearing_mode} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: disappearing_mode:[536,"bool",!1,!1]
     */
    public static final ABProp DISAPPEARING_MODE = new ABProp(536, "false", "false");

    /**
     * The {@code group_catch_up} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: group_catch_up:[559,"bool",!1,!1]
     */
    public static final ABProp GROUP_CATCH_UP = new ABProp(559, "false", "false");

    /**
     * The {@code web_abprop_drop_full_history_sync} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_abprop_drop_full_history_sync:[600,"bool",!1,!1]
     */
    public static final ABProp WEB_ABPROP_DROP_FULL_HISTORY_SYNC = new ABProp(600, "false", "false");

    /**
     * The {@code drop_last_name} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: drop_last_name:[726,"bool",!1,!1]
     */
    public static final ABProp DROP_LAST_NAME = new ABProp(726, "false", "false");

    /**
     * The {@code num_days_key_index_list_expiration} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: num_days_key_index_list_expiration:[730,"int",35,35]
     */
    public static final ABProp NUM_DAYS_KEY_INDEX_LIST_EXPIRATION = new ABProp(730, "35", "35");

    /**
     * The {@code num_days_before_device_expiry_check} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: num_days_before_device_expiry_check:[731,"int",7,7]
     */
    public static final ABProp NUM_DAYS_BEFORE_DEVICE_EXPIRY_CHECK = new ABProp(731, "7", "7");

    /**
     * The {@code web_abprop_collections_nux_banner} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_abprop_collections_nux_banner:[741,"bool",!1,!1]
     */
    public static final ABProp WEB_ABPROP_COLLECTIONS_NUX_BANNER = new ABProp(741, "false", "false");

    /**
     * The {@code adv_v2_m4_m5} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: adv_v2_m4_m5:[753,"bool",!1,!1]
     */
    public static final ABProp ADV_V2_M4_M5 = new ABProp(753, "false", "false");

    /**
     * The {@code web_abprop_business_profile_refresh_linked_account_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_abprop_business_profile_refresh_linked_account_enabled:[764,"bool",!1,!1]
     */
    public static final ABProp WEB_ABPROP_BUSINESS_PROFILE_REFRESH_LINKED_ACCOUNT_ENABLED = new ABProp(764, "false", "false");

    /**
     * The {@code tos_3_client_gating_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: tos_3_client_gating_enabled:[791,"bool",!1,!1]
     */
    public static final ABProp TOS_3_CLIENT_GATING_ENABLED = new ABProp(791, "false", "false");

    /**
     * The {@code tctoken_duration} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: tctoken_duration:[865,"int",604800,604800]
     */
    public static final ABProp TCTOKEN_DURATION = new ABProp(865, "604800", "604800");

    /**
     * The {@code web_abprop_direct_connection_md} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_abprop_direct_connection_md:[869,"bool",!1,!0]
     */
    public static final ABProp WEB_ABPROP_DIRECT_CONNECTION_MD = new ABProp(869, "false", "true");

    /**
     * The {@code web_status_psa} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_status_psa:[873,"bool",!1,!1]
     */
    public static final ABProp WEB_STATUS_PSA = new ABProp(873, "false", "false");

    /**
     * The {@code tos_client_state_fetch_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: tos_client_state_fetch_enabled:[877,"bool",!1,!1]
     */
    public static final ABProp TOS_CLIENT_STATE_FETCH_ENABLED = new ABProp(877, "false", "false");

    /**
     * The {@code web_abprop_block_catalog_creation_ecommerce_compliance_india} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_abprop_block_catalog_creation_ecommerce_compliance_india:[894,"bool",!1,!1]
     */
    public static final ABProp WEB_ABPROP_BLOCK_CATALOG_CREATION_ECOMMERCE_COMPLIANCE_INDIA = new ABProp(894, "false", "false");

    /**
     * The {@code tos_client_state_fetch_iteration} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: tos_client_state_fetch_iteration:[908,"int",0,0]
     */
    public static final ABProp TOS_CLIENT_STATE_FETCH_ITERATION = new ABProp(908, "0", "0");

    /**
     * The {@code tctoken_num_buckets} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: tctoken_num_buckets:[909,"int",4,4]
     */
    public static final ABProp TCTOKEN_NUM_BUCKETS = new ABProp(909, "4", "4");

    /**
     * The {@code banned_shops_ux_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: banned_shops_ux_enabled:[957,"bool",!1,!0]
     */
    public static final ABProp BANNED_SHOPS_UX_ENABLED = new ABProp(957, "false", "true");

    /**
     * The {@code ctwa_tos_filtering_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ctwa_tos_filtering_enabled:[976,"bool",!1,!1]
     */
    public static final ABProp CTWA_TOS_FILTERING_ENABLED = new ABProp(976, "false", "false");

    /**
     * The {@code parent_group_view_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: parent_group_view_enabled:[982,"bool",!0,!0]
     */
    public static final ABProp PARENT_GROUP_VIEW_ENABLED = new ABProp(982, "true", "true");

    /**
     * The {@code tctoken_duration_sender} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: tctoken_duration_sender:[996,"int",604800,604800]
     */
    public static final ABProp TCTOKEN_DURATION_SENDER = new ABProp(996, "604800", "604800");

    /**
     * The {@code tctoken_num_buckets_sender} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: tctoken_num_buckets_sender:[997,"int",4,4]
     */
    public static final ABProp TCTOKEN_NUM_BUCKETS_SENDER = new ABProp(997, "4", "4");

    /**
     * The {@code smb_ecommerce_compliance_india_m4} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_ecommerce_compliance_india_m4:[1003,"bool",!1,!0]
     */
    public static final ABProp SMB_ECOMMERCE_COMPLIANCE_INDIA_M4 = new ABProp(1003, "false", "true");

    /**
     * The {@code smart_filters_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smart_filters_enabled:[1015,"bool",!1,!0]
     */
    public static final ABProp SMART_FILTERS_ENABLED = new ABProp(1015, "false", "true");

    /**
     * The {@code in_app_support_v2_number_prefixes} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: in_app_support_v2_number_prefixes:[1031,"string","15517868","15517868"]
     */
    public static final ABProp IN_APP_SUPPORT_V2_NUMBER_PREFIXES = new ABProp(1031, "15517868", "15517868");

    /**
     * The {@code system_msg_numbers_fb_branded} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: system_msg_numbers_fb_branded:[1035,"string","16325551023,16505434800,16503130062,16507885324,16508620604,16504228206,447710173736,16315551023,16505361212,16508129150,16315555102,16315558723,16505212669,16507885280,19032707825,0","16325551023,16505434800,16503130062,16507885324,16508620604,16504228206,447710173736,16315551023,16505361212,16508129150,16315555102,16315558723,16505212669,16507885280,19032707825,0"]
     */
    public static final ABProp SYSTEM_MSG_NUMBERS_FB_BRANDED = new ABProp(1035, "16325551023,16505434800,16503130062,16507885324,16508620604,16504228206,447710173736,16315551023,16505361212,16508129150,16315555102,16315558723,16505212669,16507885280,19032707825,0", "16325551023,16505434800,16503130062,16507885324,16508620604,16504228206,447710173736,16315551023,16505361212,16508129150,16315555102,16315558723,16505212669,16507885280,19032707825,0");

    /**
     * The {@code system_msg_numbers_fb_inc} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: system_msg_numbers_fb_inc:[1036,"string","",""]
     */
    public static final ABProp SYSTEM_MSG_NUMBERS_FB_INC = new ABProp(1036, "", "");

    /**
     * The {@code web_shop_storefront_message} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_shop_storefront_message:[1053,"bool",!1,!1]
     */
    public static final ABProp WEB_SHOP_STOREFRONT_MESSAGE = new ABProp(1053, "false", "false");

    /**
     * The {@code dev_prop_string} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: dev_prop_string:[1064,"string","",""]
     */
    public static final ABProp DEV_PROP_STRING = new ABProp(1064, "", "");

    /**
     * The {@code dev_prop_boolean} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: dev_prop_boolean:[1065,"bool",!1,!1]
     */
    public static final ABProp DEV_PROP_BOOLEAN = new ABProp(1065, "false", "false");

    /**
     * The {@code dev_prop_int} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: dev_prop_int:[1066,"int",0,0]
     */
    public static final ABProp DEV_PROP_INT = new ABProp(1066, "0", "0");

    /**
     * The {@code dev_prop_float} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: dev_prop_float:[1067,"float",0,0]
     */
    public static final ABProp DEV_PROP_FLOAT = new ABProp(1067, "0", "0");

    /**
     * The {@code web_send_invisible_msg_min_group_size} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_send_invisible_msg_min_group_size:[1100,"int",128,128]
     */
    public static final ABProp WEB_SEND_INVISIBLE_MSG_MIN_GROUP_SIZE = new ABProp(1100, "128", "128");

    /**
     * The {@code lthash_check_hours} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: lthash_check_hours:[1104,"int",0,0]
     */
    public static final ABProp LTHASH_CHECK_HOURS = new ABProp(1104, "0", "0");

    /**
     * The {@code country_client_gating_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: country_client_gating_enabled:[1105,"bool",!1,!1]
     */
    public static final ABProp COUNTRY_CLIENT_GATING_ENABLED = new ABProp(1105, "false", "false");

    /**
     * The {@code order_details_from_cart_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: order_details_from_cart_enabled:[1107,"bool",!1,!0]
     */
    public static final ABProp ORDER_DETAILS_FROM_CART_ENABLED = new ABProp(1107, "false", "true");

    /**
     * The {@code interactive_message_native_flow_killswitch} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: interactive_message_native_flow_killswitch:[1133,"bool",!1,!1]
     */
    public static final ABProp INTERACTIVE_MESSAGE_NATIVE_FLOW_KILLSWITCH = new ABProp(1133, "false", "false");

    /**
     * The {@code message_count_logging_md_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: message_count_logging_md_enabled:[1135,"bool",!1,!1]
     */
    public static final ABProp MESSAGE_COUNT_LOGGING_MD_ENABLED = new ABProp(1135, "false", "false");

    /**
     * The {@code web_init_chat_batch_size} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_init_chat_batch_size:[1171,"int",100,100]
     */
    public static final ABProp WEB_INIT_CHAT_BATCH_SIZE = new ABProp(1171, "100", "100");

    /**
     * The {@code web_init_chat_max_unread_message_count} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_init_chat_max_unread_message_count:[1172,"int",0,0]
     */
    public static final ABProp WEB_INIT_CHAT_MAX_UNREAD_MESSAGE_COUNT = new ABProp(1172, "0", "0");

    /**
     * The {@code order_details_custom_item_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: order_details_custom_item_enabled:[1176,"bool",!1,!0]
     */
    public static final ABProp ORDER_DETAILS_CUSTOM_ITEM_ENABLED = new ABProp(1176, "false", "true");

    /**
     * The {@code admin_revoke_receiver} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: admin_revoke_receiver:[1177,"bool",!1,!0]
     */
    public static final ABProp ADMIN_REVOKE_RECEIVER = new ABProp(1177, "false", "true");

    /**
     * The {@code order_management_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: order_management_enabled:[1188,"bool",!1,!1]
     */
    public static final ABProp ORDER_MANAGEMENT_ENABLED = new ABProp(1188, "false", "false");

    /**
     * The {@code log_clock_skew} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: log_clock_skew:[1190,"bool",!1,!1]
     */
    public static final ABProp LOG_CLOCK_SKEW = new ABProp(1190, "false", "false");

    /**
     * The {@code smb_ecommerce_compliance_india_m4_5} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_ecommerce_compliance_india_m4_5:[1192,"bool",!1,!0]
     */
    public static final ABProp SMB_ECOMMERCE_COMPLIANCE_INDIA_M4_5 = new ABProp(1192, "false", "true");

    /**
     * The {@code smb_hide_unsupported_currency_price} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_hide_unsupported_currency_price:[1203,"bool",!1,!0]
     */
    public static final ABProp SMB_HIDE_UNSUPPORTED_CURRENCY_PRICE = new ABProp(1203, "false", "true");

    /**
     * The {@code order_details_from_catalog_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: order_details_from_catalog_enabled:[1212,"bool",!1,!0]
     */
    public static final ABProp ORDER_DETAILS_FROM_CATALOG_ENABLED = new ABProp(1212, "false", "true");

    /**
     * The {@code audio_level_speaking_threshold} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: audio_level_speaking_threshold:[1213,"int",30,50]
     */
    public static final ABProp AUDIO_LEVEL_SPEAKING_THRESHOLD = new ABProp(1213, "30", "50");

    /**
     * The {@code smb_catkit_query_version} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_catkit_query_version:[1229,"int",1,1]
     */
    public static final ABProp SMB_CATKIT_QUERY_VERSION = new ABProp(1229, "1", "1");

    /**
     * The {@code parent_group_link_limit} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: parent_group_link_limit:[1238,"int",100,100]
     */
    public static final ABProp PARENT_GROUP_LINK_LIMIT = new ABProp(1238, "100", "100");

    /**
     * The {@code smart_filters_enabled_consumer} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smart_filters_enabled_consumer:[1287,"bool",!1,!0]
     */
    public static final ABProp SMART_FILTERS_ENABLED_CONSUMER = new ABProp(1287, "false", "true");

    /**
     * The {@code group_size_limit} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: group_size_limit:[1304,"int",257,257]
     */
    public static final ABProp GROUP_SIZE_LIMIT = new ABProp(1304, "257", "257");

    /**
     * The {@code commerce_sanctioned} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: commerce_sanctioned:[1319,"bool",!1,!1]
     */
    public static final ABProp COMMERCE_SANCTIONED = new ABProp(1319, "false", "false");

    /**
     * The {@code web_abprop_business_profile_refresh_linked_accounts_killswitch} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_abprop_business_profile_refresh_linked_accounts_killswitch:[1351,"bool",!1,!1]
     */
    public static final ABProp WEB_ABPROP_BUSINESS_PROFILE_REFRESH_LINKED_ACCOUNTS_KILLSWITCH = new ABProp(1351, "false", "false");

    /**
     * The {@code md_app_state_gate_D34336913} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: md_app_state_gate_D34336913:[1379,"bool",!1,!1]
     */
    public static final ABProp MD_APP_STATE_GATE_D34336913 = new ABProp(1379, "false", "false");

    /**
     * The {@code syncd_periodic_sync_days} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: syncd_periodic_sync_days:[1400,"int",0,0]
     */
    public static final ABProp SYNCD_PERIODIC_SYNC_DAYS = new ABProp(1400, "0", "0");

    /**
     * The {@code poll_name_length} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: poll_name_length:[1406,"int",255,255]
     */
    public static final ABProp POLL_NAME_LENGTH = new ABProp(1406, "255", "255");

    /**
     * The {@code poll_option_length} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: poll_option_length:[1407,"int",100,100]
     */
    public static final ABProp POLL_OPTION_LENGTH = new ABProp(1407, "100", "100");

    /**
     * The {@code poll_option_count} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: poll_option_count:[1408,"int",12,12]
     */
    public static final ABProp POLL_OPTION_COUNT = new ABProp(1408, "12", "12");

    /**
     * The {@code heartbeat_interval_s} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: heartbeat_interval_s:[1430,"int",10,5]
     */
    public static final ABProp HEARTBEAT_INTERVAL_S = new ABProp(1430, "10", "5");

    /**
     * The {@code interactive_response_message_killswitch} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: interactive_response_message_killswitch:[1435,"bool",!1,!1]
     */
    public static final ABProp INTERACTIVE_RESPONSE_MESSAGE_KILLSWITCH = new ABProp(1435, "false", "false");

    /**
     * The {@code interactive_response_message_native_flow_killswitch} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: interactive_response_message_native_flow_killswitch:[1436,"bool",!1,!1]
     */
    public static final ABProp INTERACTIVE_RESPONSE_MESSAGE_NATIVE_FLOW_KILLSWITCH = new ABProp(1436, "false", "false");

    /**
     * The {@code web_syncd_max_mutations_to_process_during_resume} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_syncd_max_mutations_to_process_during_resume:[1513,"int",1000,1000]
     */
    public static final ABProp WEB_SYNCD_MAX_MUTATIONS_TO_PROCESS_DURING_RESUME = new ABProp(1513, "1000", "1000");

    /**
     * The {@code catalog_categories_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: catalog_categories_enabled:[1514,"bool",!1,!0]
     */
    public static final ABProp CATALOG_CATEGORIES_ENABLED = new ABProp(1514, "false", "true");

    /**
     * The {@code md_offline_v2_m2_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: md_offline_v2_m2_enabled:[1517,"int",10,10]
     */
    public static final ABProp MD_OFFLINE_V2_M2_ENABLED = new ABProp(1517, "10", "10");

    /**
     * The {@code lobby_timeout_min} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: lobby_timeout_min:[1565,"int",0,1]
     */
    public static final ABProp LOBBY_TIMEOUT_MIN = new ABProp(1565, "0", "1");

    /**
     * The {@code smb_billing_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_billing_enabled:[1583,"bool",!1,!1]
     */
    public static final ABProp SMB_BILLING_ENABLED = new ABProp(1583, "false", "false");

    /**
     * The {@code order_details_quick_pay} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: order_details_quick_pay:[1600,"string","{\"allowed_product_type\":\"none\"}","{\"allowed_product_type\":\"none\"}"]
     */
    public static final ABProp ORDER_DETAILS_QUICK_PAY = new ABProp(1600, "{\"allowed_product_type\":\"none\"}", "{\"allowed_product_type\":\"none\"}");

    /**
     * The {@code reactions_chat_preview} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: reactions_chat_preview:[1605,"bool",!1,!0]
     */
    public static final ABProp REACTIONS_CHAT_PREVIEW = new ABProp(1605, "false", "true");

    /**
     * The {@code chatlist_filters_v1} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: chatlist_filters_v1:[1608,"bool",!1,!1]
     */
    public static final ABProp CHATLIST_FILTERS_V1 = new ABProp(1608, "false", "false");

    /**
     * The {@code parent_group_admins_limit} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: parent_group_admins_limit:[1655,"int",20,20]
     */
    public static final ABProp PARENT_GROUP_ADMINS_LIMIT = new ABProp(1655, "20", "20");

    /**
     * The {@code dm_updated_system_message} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: dm_updated_system_message:[1670,"bool",!1,!0]
     */
    public static final ABProp DM_UPDATED_SYSTEM_MESSAGE = new ABProp(1670, "false", "true");

    /**
     * The {@code web_abprop_screen_lock_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_abprop_screen_lock_enabled:[1680,"bool",!1,!1]
     */
    public static final ABProp WEB_ABPROP_SCREEN_LOCK_ENABLED = new ABProp(1680, "false", "false");

    /**
     * The {@code wa_ctwa_log_user_journey_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_ctwa_log_user_journey_enabled:[1681,"bool",!1,!0]
     */
    public static final ABProp WA_CTWA_LOG_USER_JOURNEY_ENABLED = new ABProp(1681, "false", "true");

    /**
     * The {@code order_details_total_maximum_value} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: order_details_total_maximum_value:[1684,"float",500000000,500000000]
     */
    public static final ABProp ORDER_DETAILS_TOTAL_MAXIMUM_VALUE = new ABProp(1684, "500000000", "500000000");

    /**
     * The {@code keep_in_chat_undo_duration_limit} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: keep_in_chat_undo_duration_limit:[1698,"int",2592000,2592000]
     */
    public static final ABProp KEEP_IN_CHAT_UNDO_DURATION_LIMIT = new ABProp(1698, "2592000", "2592000");

    /**
     * The {@code order_details_total_order_minimum_value} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: order_details_total_order_minimum_value:[1719,"float",1,1]
     */
    public static final ABProp ORDER_DETAILS_TOTAL_ORDER_MINIMUM_VALUE = new ABProp(1719, "1", "1");

    /**
     * The {@code web_group_profile_editor} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_group_profile_editor:[1745,"bool",!0,!0]
     */
    public static final ABProp WEB_GROUP_PROFILE_EDITOR = new ABProp(1745, "true", "true");

    /**
     * The {@code web_abprop_core_wam_runtime} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_abprop_core_wam_runtime:[1753,"bool",!1,!1]
     */
    public static final ABProp WEB_ABPROP_CORE_WAM_RUNTIME = new ABProp(1753, "false", "false");

    /**
     * The {@code smb_phase_out_not_a_business_V2} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_phase_out_not_a_business_V2:[1771,"bool",!1,!0]
     */
    public static final ABProp SMB_PHASE_OUT_NOT_A_BUSINESS_V2 = new ABProp(1771, "false", "true");

    /**
     * The {@code web_offline_resume_qpl_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_offline_resume_qpl_enabled:[1773,"bool",!1,!1]
     */
    public static final ABProp WEB_OFFLINE_RESUME_QPL_ENABLED = new ABProp(1773, "false", "false");

    /**
     * The {@code is_meta_employee_or_internal_tester} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: is_meta_employee_or_internal_tester:[1777,"bool",!1,!1]
     */
    public static final ABProp IS_META_EMPLOYEE_OR_INTERNAL_TESTER = new ABProp(1777, "false", "false");

    /**
     * The {@code syncd_lthash_consistency_check_on_snapshot_mac_mismatch} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: syncd_lthash_consistency_check_on_snapshot_mac_mismatch:[1783,"bool",!1,!1]
     */
    public static final ABProp SYNCD_LTHASH_CONSISTENCY_CHECK_ON_SNAPSHOT_MAC_MISMATCH = new ABProp(1783, "false", "false");

    /**
     * The {@code smb_md_agent_chat_assignment_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_md_agent_chat_assignment_enabled:[1798,"bool",!1,!0]
     */
    public static final ABProp SMB_MD_AGENT_CHAT_ASSIGNMENT_ENABLED = new ABProp(1798, "false", "true");

    /**
     * The {@code web_syncd_fatal_fields_from_L1104589PRV2} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_syncd_fatal_fields_from_L1104589PRV2:[1808,"bool",!1,!1]
     */
    public static final ABProp WEB_SYNCD_FATAL_FIELDS_FROM_L1104589PRV2 = new ABProp(1808, "false", "false");

    /**
     * The {@code report_call_replayer_id} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: report_call_replayer_id:[1834,"bool",!1,!1]
     */
    public static final ABProp REPORT_CALL_REPLAYER_ID = new ABProp(1834, "false", "false");

    /**
     * The {@code disable_auto_download} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: disable_auto_download:[1838,"bool",!1,!1]
     */
    public static final ABProp DISABLE_AUTO_DOWNLOAD = new ABProp(1838, "false", "false");

    /**
     * The {@code ctwa_data_max_length} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ctwa_data_max_length:[1841,"int",768,768]
     */
    public static final ABProp CTWA_DATA_MAX_LENGTH = new ABProp(1841, "768", "768");

    /**
     * The {@code direct_connection_business_numbers} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: direct_connection_business_numbers:[1846,"string","16005554444,918591749310,917977079770","16005554444,918591749310,917977079770"]
     */
    public static final ABProp DIRECT_CONNECTION_BUSINESS_NUMBERS = new ABProp(1846, "16005554444,918591749310,917977079770", "16005554444,918591749310,917977079770");

    /**
     * The {@code web_multi_skin_toned_emoji_picker} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_multi_skin_toned_emoji_picker:[1850,"bool",!1,!1]
     */
    public static final ABProp WEB_MULTI_SKIN_TONED_EMOJI_PICKER = new ABProp(1850, "false", "false");

    /**
     * The {@code status_reaction_emojis} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: status_reaction_emojis:[1852,"string","[128525, 128514, 128558, 128546, 128591, 128079, 127881, 128175]","[128525, 128514, 128558, 128546, 128591, 128079, 127881, 128175]"]
     */
    public static final ABProp STATUS_REACTION_EMOJIS = new ABProp(1852, "[128525, 128514, 128558, 128546, 128591, 128079, 127881, 128175]", "[128525, 128514, 128558, 128546, 128591, 128079, 127881, 128175]");

    /**
     * The {@code group_size_bypassing_sampling} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: group_size_bypassing_sampling:[1861,"int",100000,100000]
     */
    public static final ABProp GROUP_SIZE_BYPASSING_SAMPLING = new ABProp(1861, "100000", "100000");

    /**
     * The {@code community_admin_promotion_one_time_prompt} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: community_admin_promotion_one_time_prompt:[1864,"bool",!1,!1]
     */
    public static final ABProp COMMUNITY_ADMIN_PROMOTION_ONE_TIME_PROMPT = new ABProp(1864, "false", "false");

    /**
     * The {@code share_phone_number_on_cart_send_to_direct_connection_biz_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: share_phone_number_on_cart_send_to_direct_connection_biz_enabled:[1867,"bool",!0,!0]
     */
    public static final ABProp SHARE_PHONE_NUMBER_ON_CART_SEND_TO_DIRECT_CONNECTION_BIZ_ENABLED = new ABProp(1867, "true", "true");

    /**
     * The {@code smb_multi_device_agents_logging_V2_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_multi_device_agents_logging_V2_enabled:[1897,"bool",!1,!0]
     */
    public static final ABProp SMB_MULTI_DEVICE_AGENTS_LOGGING_V2_ENABLED = new ABProp(1897, "false", "true");

    /**
     * The {@code web_ptt_streamer_upload} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_ptt_streamer_upload:[1902,"bool",!1,!0]
     */
    public static final ABProp WEB_PTT_STREAMER_UPLOAD = new ABProp(1902, "false", "true");

    /**
     * The {@code smb_temp_cover_photo_privacy_messaging} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_temp_cover_photo_privacy_messaging:[1913,"bool",!1,!0]
     */
    public static final ABProp SMB_TEMP_COVER_PHOTO_PRIVACY_MESSAGING = new ABProp(1913, "false", "true");

    /**
     * The {@code web_send_invisible_msg_max_group_size} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_send_invisible_msg_max_group_size:[1945,"int",1024,1024]
     */
    public static final ABProp WEB_SEND_INVISIBLE_MSG_MAX_GROUP_SIZE = new ABProp(1945, "1024", "1024");

    /**
     * The {@code smb_multi_device_message_attribution_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_multi_device_message_attribution_enabled:[1981,"bool",!1,!0]
     */
    public static final ABProp SMB_MULTI_DEVICE_MESSAGE_ATTRIBUTION_ENABLED = new ABProp(1981, "false", "true");

    /**
     * The {@code parent_group_link_limit_community_creation} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: parent_group_link_limit_community_creation:[1990,"int",10,20]
     */
    public static final ABProp PARENT_GROUP_LINK_LIMIT_COMMUNITY_CREATION = new ABProp(1990, "10", "20");

    /**
     * The {@code graphql_locale_remapping} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: graphql_locale_remapping:[2014,"string","{}","{}"]
     */
    public static final ABProp GRAPHQL_LOCALE_REMAPPING = new ABProp(2014, "{}", "{}");

    /**
     * The {@code web_message_list_a11y_redesign} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_message_list_a11y_redesign:[2016,"bool",!0,!0]
     */
    public static final ABProp WEB_MESSAGE_LIST_A11Y_REDESIGN = new ABProp(2016, "true", "true");

    /**
     * The {@code web_enable_profile_pic_thumb_db_caching} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_enable_profile_pic_thumb_db_caching:[2018,"bool",!1,!1]
     */
    public static final ABProp WEB_ENABLE_PROFILE_PIC_THUMB_DB_CACHING = new ABProp(2018, "false", "false");

    /**
     * The {@code web_enable_biz_catalog_view_ps_logging} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_enable_biz_catalog_view_ps_logging:[2056,"bool",!0,!0]
     */
    public static final ABProp WEB_ENABLE_BIZ_CATALOG_VIEW_PS_LOGGING = new ABProp(2056, "true", "true");

    /**
     * The {@code group_suspend_appeal_include_entity_id_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: group_suspend_appeal_include_entity_id_enabled:[2057,"bool",!1,!0]
     */
    public static final ABProp GROUP_SUSPEND_APPEAL_INCLUDE_ENTITY_ID_ENABLED = new ABProp(2057, "false", "true");

    /**
     * The {@code web_abprop_media_links_docs_search} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_abprop_media_links_docs_search:[2063,"bool",!1,!1]
     */
    public static final ABProp WEB_ABPROP_MEDIA_LINKS_DOCS_SEARCH = new ABProp(2063, "false", "false");

    /**
     * The {@code mms_vcache_aggregation_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: mms_vcache_aggregation_enabled:[2134,"bool",!1,!1]
     */
    public static final ABProp MMS_VCACHE_AGGREGATION_ENABLED = new ABProp(2134, "false", "false");

    /**
     * The {@code web_link_preview_sync_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_link_preview_sync_enabled:[2156,"bool",!1,!0]
     */
    public static final ABProp WEB_LINK_PREVIEW_SYNC_ENABLED = new ABProp(2156, "false", "true");

    /**
     * The {@code smb_ctwa_billing_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_ctwa_billing_enabled:[2158,"bool",!1,!1]
     */
    public static final ABProp SMB_CTWA_BILLING_ENABLED = new ABProp(2158, "false", "false");

    /**
     * The {@code video_stream_buffering_ui_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: video_stream_buffering_ui_enabled:[2167,"bool",!1,!0]
     */
    public static final ABProp VIDEO_STREAM_BUFFERING_UI_ENABLED = new ABProp(2167, "false", "true");

    /**
     * The {@code parent_group_view_enabled_for_smb_on_web} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: parent_group_view_enabled_for_smb_on_web:[2205,"bool",!1,!0]
     */
    public static final ABProp PARENT_GROUP_VIEW_ENABLED_FOR_SMB_ON_WEB = new ABProp(2205, "false", "true");

    /**
     * The {@code smb_md_agent_chat_assignment_nux_impressions} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_md_agent_chat_assignment_nux_impressions:[2207,"int",0,3]
     */
    public static final ABProp SMB_MD_AGENT_CHAT_ASSIGNMENT_NUX_IMPRESSIONS = new ABProp(2207, "0", "3");

    /**
     * The {@code mex_phase3_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: mex_phase3_enabled:[2249,"bool",!1,!1]
     */
    public static final ABProp MEX_PHASE3_ENABLED = new ABProp(2249, "false", "false");

    /**
     * The {@code mex_phase3_status_flags} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: mex_phase3_status_flags:[2250,"int",0,0]
     */
    public static final ABProp MEX_PHASE3_STATUS_FLAGS = new ABProp(2250, "0", "0");

    /**
     * The {@code web_max_contacts_to_show_common_groups} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_max_contacts_to_show_common_groups:[2264,"int",10,10]
     */
    public static final ABProp WEB_MAX_CONTACTS_TO_SHOW_COMMON_GROUPS = new ABProp(2264, "10", "10");

    /**
     * The {@code web_max_found_common_groups_displayed} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_max_found_common_groups_displayed:[2268,"int",15,15]
     */
    public static final ABProp WEB_MAX_FOUND_COMMON_GROUPS_DISPLAYED = new ABProp(2268, "15", "15");

    /**
     * The {@code web_message_custom_aria_label} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_message_custom_aria_label:[2280,"bool",!1,!1]
     */
    public static final ABProp WEB_MESSAGE_CUSTOM_ARIA_LABEL = new ABProp(2280, "false", "false");

    /**
     * The {@code parent_group_create_privacy} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: parent_group_create_privacy:[2356,"bool",!1,!0]
     */
    public static final ABProp PARENT_GROUP_CREATE_PRIVACY = new ABProp(2356, "false", "true");

    /**
     * The {@code four_reactions_in_bubble_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: four_reactions_in_bubble_enabled:[2378,"bool",!1,!0]
     */
    public static final ABProp FOUR_REACTIONS_IN_BUBBLE_ENABLED = new ABProp(2378, "false", "true");

    /**
     * The {@code parent_group_min_participants_for_group_entry_point} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: parent_group_min_participants_for_group_entry_point:[2382,"int",20,1]
     */
    public static final ABProp PARENT_GROUP_MIN_PARTICIPANTS_FOR_GROUP_ENTRY_POINT = new ABProp(2382, "20", "1");

    /**
     * The {@code group_join_request_m2_banner_on_conversation} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: group_join_request_m2_banner_on_conversation:[2449,"bool",!1,!1]
     */
    public static final ABProp GROUP_JOIN_REQUEST_M2_BANNER_ON_CONVERSATION = new ABProp(2449, "false", "false");

    /**
     * The {@code web_non_blocking_offline_resume_max_message_count} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_non_blocking_offline_resume_max_message_count:[2508,"int",1000,1000]
     */
    public static final ABProp WEB_NON_BLOCKING_OFFLINE_RESUME_MAX_MESSAGE_COUNT = new ABProp(2508, "1000", "1000");

    /**
     * The {@code new_end_call_survey_pop_up_user_interval_s} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: new_end_call_survey_pop_up_user_interval_s:[2553,"int",-1,-1]
     */
    public static final ABProp NEW_END_CALL_SURVEY_POP_UP_USER_INTERVAL_S = new ABProp(2553, "-1", "-1");

    /**
     * The {@code out_of_sync_disappearing_messages_logging} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: out_of_sync_disappearing_messages_logging:[2561,"bool",!1,!0]
     */
    public static final ABProp OUT_OF_SYNC_DISAPPEARING_MESSAGES_LOGGING = new ABProp(2561, "false", "true");

    /**
     * The {@code link_preview_wait_time} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: link_preview_wait_time:[2566,"int",7,7]
     */
    public static final ABProp LINK_PREVIEW_WAIT_TIME = new ABProp(2566, "7", "7");

    /**
     * The {@code smb_biz_profile_custom_url} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_biz_profile_custom_url:[2582,"bool",!0,!0]
     */
    public static final ABProp SMB_BIZ_PROFILE_CUSTOM_URL = new ABProp(2582, "true", "true");

    /**
     * The {@code enable_init_bwe_for_group_call} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_init_bwe_for_group_call:[2601,"bool",!1,!1]
     */
    public static final ABProp ENABLE_INIT_BWE_FOR_GROUP_CALL = new ABProp(2601, "false", "false");

    /**
     * The {@code media_picker_select_limit} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: media_picker_select_limit:[2614,"int",30,30]
     */
    public static final ABProp MEDIA_PICKER_SELECT_LIMIT = new ABProp(2614, "30", "30");

    /**
     * The {@code web_screen_lock_max_retries} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_screen_lock_max_retries:[2622,"int",10,10]
     */
    public static final ABProp WEB_SCREEN_LOCK_MAX_RETRIES = new ABProp(2622, "10", "10");

    /**
     * The {@code placeholder_message_key_hash_logging} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: placeholder_message_key_hash_logging:[2639,"bool",!1,!0]
     */
    public static final ABProp PLACEHOLDER_MESSAGE_KEY_HASH_LOGGING = new ABProp(2639, "false", "true");

    /**
     * The {@code vid_stream_pause_resume_jb_reset_threshold_ms} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: vid_stream_pause_resume_jb_reset_threshold_ms:[2642,"int",0,0]
     */
    public static final ABProp VID_STREAM_PAUSE_RESUME_JB_RESET_THRESHOLD_MS = new ABProp(2642, "0", "0");

    /**
     * The {@code media_picker_select_limit_new} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: media_picker_select_limit_new:[2693,"int",30,30]
     */
    public static final ABProp MEDIA_PICKER_SELECT_LIMIT_NEW = new ABProp(2693, "30", "30");

    /**
     * The {@code smb_md_agent_chat_assignment_system_messages_logging_v2_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_md_agent_chat_assignment_system_messages_logging_v2_enabled:[2709,"bool",!1,!0]
     */
    public static final ABProp SMB_MD_AGENT_CHAT_ASSIGNMENT_SYSTEM_MESSAGES_LOGGING_V2_ENABLED = new ABProp(2709, "false", "true");

    /**
     * The {@code ephemeral_sync_response} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ephemeral_sync_response:[2714,"bool",!1,!1]
     */
    public static final ABProp EPHEMERAL_SYNC_RESPONSE = new ABProp(2714, "false", "false");

    /**
     * The {@code poll_receiving_cag_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: poll_receiving_cag_enabled:[2737,"bool",!1,!1]
     */
    public static final ABProp POLL_RECEIVING_CAG_ENABLED = new ABProp(2737, "false", "false");

    /**
     * The {@code poll_creation_cag_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: poll_creation_cag_enabled:[2738,"bool",!1,!1]
     */
    public static final ABProp POLL_CREATION_CAG_ENABLED = new ABProp(2738, "false", "false");

    /**
     * The {@code community_announcement_group_size_limit} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: community_announcement_group_size_limit:[2774,"int",5000,5000]
     */
    public static final ABProp COMMUNITY_ANNOUNCEMENT_GROUP_SIZE_LIMIT = new ABProp(2774, "5000", "5000");

    /**
     * The {@code fullscreen_animation_for_keyword} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: fullscreen_animation_for_keyword:[2776,"bool",!1,!1]
     */
    public static final ABProp FULLSCREEN_ANIMATION_FOR_KEYWORD = new ABProp(2776, "false", "false");

    /**
     * The {@code syncd_additional_mutations_count} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: syncd_additional_mutations_count:[2777,"int",1,1]
     */
    public static final ABProp SYNCD_ADDITIONAL_MUTATIONS_COUNT = new ABProp(2777, "1", "1");

    /**
     * The {@code smb_md_agent_chat_assignment_chats_reorder_on_chat_assignment_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_md_agent_chat_assignment_chats_reorder_on_chat_assignment_enabled:[2787,"bool",!1,!0]
     */
    public static final ABProp SMB_MD_AGENT_CHAT_ASSIGNMENT_CHATS_REORDER_ON_CHAT_ASSIGNMENT_ENABLED = new ABProp(2787, "false", "true");

    /**
     * The {@code smb_md_agent_chat_assignment_chats_reorder_on_chat_unassignment_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_md_agent_chat_assignment_chats_reorder_on_chat_unassignment_enabled:[2788,"bool",!1,!0]
     */
    public static final ABProp SMB_MD_AGENT_CHAT_ASSIGNMENT_CHATS_REORDER_ON_CHAT_UNASSIGNMENT_ENABLED = new ABProp(2788, "false", "true");

    /**
     * The {@code web_message_plugin_frontend_registration_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_message_plugin_frontend_registration_enabled:[2793,"bool",!1,!1]
     */
    public static final ABProp WEB_MESSAGE_PLUGIN_FRONTEND_REGISTRATION_ENABLED = new ABProp(2793, "false", "false");

    /**
     * The {@code enable_soox_message_sending} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_soox_message_sending:[2832,"bool",!1,!0]
     */
    public static final ABProp ENABLE_SOOX_MESSAGE_SENDING = new ABProp(2832, "false", "true");

    /**
     * The {@code supports_keep_in_chat_in_cag} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: supports_keep_in_chat_in_cag:[2844,"bool",!0,!0]
     */
    public static final ABProp SUPPORTS_KEEP_IN_CHAT_IN_CAG = new ABProp(2844, "true", "true");

    /**
     * The {@code utm_tracking_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: utm_tracking_enabled:[2895,"bool",!1,!1]
     */
    public static final ABProp UTM_TRACKING_ENABLED = new ABProp(2895, "false", "false");

    /**
     * The {@code utm_tracking_expiration_hours} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: utm_tracking_expiration_hours:[2896,"int",24,24]
     */
    public static final ABProp UTM_TRACKING_EXPIRATION_HOURS = new ABProp(2896, "24", "24");

    /**
     * The {@code wa_ctwa_web_thread_ad_attribution_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_ctwa_web_thread_ad_attribution_enabled:[2898,"bool",!1,!1]
     */
    public static final ABProp WA_CTWA_WEB_THREAD_AD_ATTRIBUTION_ENABLED = new ABProp(2898, "false", "false");

    /**
     * The {@code elevated_push_names_v2_m2_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: elevated_push_names_v2_m2_enabled:[2904,"bool",!1,!1]
     */
    public static final ABProp ELEVATED_PUSH_NAMES_V2_M2_ENABLED = new ABProp(2904, "false", "false");

    /**
     * The {@code smb_md_agent_chat_assignment_notifications_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_md_agent_chat_assignment_notifications_enabled:[2908,"bool",!1,!0]
     */
    public static final ABProp SMB_MD_AGENT_CHAT_ASSIGNMENT_NOTIFICATIONS_ENABLED = new ABProp(2908, "false", "true");

    /**
     * The {@code call_admin_version} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: call_admin_version:[2912,"int",0,0]
     */
    public static final ABProp CALL_ADMIN_VERSION = new ABProp(2912, "0", "0");

    /**
     * The {@code maximum_group_size_for_rcat} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: maximum_group_size_for_rcat:[2915,"int",100,100]
     */
    public static final ABProp MAXIMUM_GROUP_SIZE_FOR_RCAT = new ABProp(2915, "100", "100");

    /**
     * The {@code ctwa_smb_data_sharing_consent} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ctwa_smb_data_sharing_consent:[2934,"bool",!1,!0]
     */
    public static final ABProp CTWA_SMB_DATA_SHARING_CONSENT = new ABProp(2934, "false", "true");

    /**
     * The {@code is_internal_tester} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: is_internal_tester:[2945,"bool",!1,!0]
     */
    public static final ABProp IS_INTERNAL_TESTER = new ABProp(2945, "false", "true");

    /**
     * The {@code message_edit_window_duration_seconds} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: message_edit_window_duration_seconds:[2983,"int",1200,1200]
     */
    public static final ABProp MESSAGE_EDIT_WINDOW_DURATION_SECONDS = new ABProp(2983, "1200", "1200");

    /**
     * The {@code ugc_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ugc_enabled:[3011,"bool",!1,!0]
     */
    public static final ABProp UGC_ENABLED = new ABProp(3011, "false", "true");

    /**
     * The {@code web_native_fetch_media_download} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_native_fetch_media_download:[3031,"bool",!1,!1]
     */
    public static final ABProp WEB_NATIVE_FETCH_MEDIA_DOWNLOAD = new ABProp(3031, "false", "false");

    /**
     * The {@code web_image_max_edge} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_image_max_edge:[3042,"int",1600,1600]
     */
    public static final ABProp WEB_IMAGE_MAX_EDGE = new ABProp(3042, "1600", "1600");

    /**
     * The {@code payments_link_to_lite_consumer_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: payments_link_to_lite_consumer_enabled:[3051,"bool",!1,!0]
     */
    public static final ABProp PAYMENTS_LINK_TO_LITE_CONSUMER_ENABLED = new ABProp(3051, "false", "true");

    /**
     * The {@code wa_ctwa_web_entrypoint_home_header_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_ctwa_web_entrypoint_home_header_enabled:[3058,"bool",!1,!1]
     */
    public static final ABProp WA_CTWA_WEB_ENTRYPOINT_HOME_HEADER_ENABLED = new ABProp(3058, "false", "false");

    /**
     * The {@code pnh_pn_for_lid_chat_sync} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: pnh_pn_for_lid_chat_sync:[3062,"bool",!1,!0]
     */
    public static final ABProp PNH_PN_FOR_LID_CHAT_SYNC = new ABProp(3062, "false", "true");

    /**
     * The {@code original_quality_image_min_edge} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: original_quality_image_min_edge:[3068,"int",2560,2560]
     */
    public static final ABProp ORIGINAL_QUALITY_IMAGE_MIN_EDGE = new ABProp(3068, "2560", "2560");

    /**
     * The {@code send_cag_member_revokes_as_GDM} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: send_cag_member_revokes_as_GDM:[3069,"bool",!0,!0]
     */
    public static final ABProp SEND_CAG_MEMBER_REVOKES_AS_GDM = new ABProp(3069, "true", "true");

    /**
     * The {@code share_own_pn_sync} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: share_own_pn_sync:[3070,"bool",!1,!0]
     */
    public static final ABProp SHARE_OWN_PN_SYNC = new ABProp(3070, "false", "true");

    /**
     * The {@code external_beta_can_join} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: external_beta_can_join:[3081,"bool",!1,!0]
     */
    public static final ABProp EXTERNAL_BETA_CAN_JOIN = new ABProp(3081, "false", "true");

    /**
     * The {@code wa_ctwa_web_entrypoint_home_header_dropdown_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_ctwa_web_entrypoint_home_header_dropdown_enabled:[3095,"bool",!1,!1]
     */
    public static final ABProp WA_CTWA_WEB_ENTRYPOINT_HOME_HEADER_DROPDOWN_ENABLED = new ABProp(3095, "false", "false");

    /**
     * The {@code media_large_file_awareness_popup_file_size_in_MB} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: media_large_file_awareness_popup_file_size_in_MB:[3115,"int",2048,2048]
     */
    public static final ABProp MEDIA_LARGE_FILE_AWARENESS_POPUP_FILE_SIZE_IN_MB = new ABProp(3115, "2048", "2048");

    /**
     * The {@code smb_rambutan_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_rambutan_enabled:[3124,"bool",!1,!0]
     */
    public static final ABProp SMB_RAMBUTAN_ENABLED = new ABProp(3124, "false", "true");

    /**
     * The {@code web_store_quota_manager_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_store_quota_manager_enabled:[3133,"bool",!1,!1]
     */
    public static final ABProp WEB_STORE_QUOTA_MANAGER_ENABLED = new ABProp(3133, "false", "false");

    /**
     * The {@code web_browser_quota_threshold} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_browser_quota_threshold:[3134,"int",100,100]
     */
    public static final ABProp WEB_BROWSER_QUOTA_THRESHOLD = new ABProp(3134, "100", "100");

    /**
     * The {@code web_browser_min_storage_quota} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_browser_min_storage_quota:[3135,"int",5,5]
     */
    public static final ABProp WEB_BROWSER_MIN_STORAGE_QUOTA = new ABProp(3135, "5", "5");

    /**
     * The {@code web_original_photo_quality_upload_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_original_photo_quality_upload_enabled:[3136,"bool",!1,!1]
     */
    public static final ABProp WEB_ORIGINAL_PHOTO_QUALITY_UPLOAD_ENABLED = new ABProp(3136, "false", "false");

    /**
     * The {@code pinned_messages_m0} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: pinned_messages_m0:[3138,"bool",!0,!0]
     */
    public static final ABProp PINNED_MESSAGES_M0 = new ABProp(3138, "true", "true");

    /**
     * The {@code pinned_messages_m1_receiver} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: pinned_messages_m1_receiver:[3139,"bool",!0,!0]
     */
    public static final ABProp PINNED_MESSAGES_M1_RECEIVER = new ABProp(3139, "true", "true");

    /**
     * The {@code pinned_messages_m1_sender} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: pinned_messages_m1_sender:[3140,"bool",!0,!0]
     */
    public static final ABProp PINNED_MESSAGES_M1_SENDER = new ABProp(3140, "true", "true");

    /**
     * The {@code pinned_messages_m2} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: pinned_messages_m2:[3141,"bool",!1,!1]
     */
    public static final ABProp PINNED_MESSAGES_M2 = new ABProp(3141, "false", "false");

    /**
     * The {@code parent_group_subgroup_filter} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: parent_group_subgroup_filter:[3147,"bool",!1,!1]
     */
    public static final ABProp PARENT_GROUP_SUBGROUP_FILTER = new ABProp(3147, "false", "false");

    /**
     * The {@code web_deprecate_mms4_hash_based_download} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_deprecate_mms4_hash_based_download:[3152,"bool",!1,!0]
     */
    public static final ABProp WEB_DEPRECATE_MMS4_HASH_BASED_DOWNLOAD = new ABProp(3152, "false", "true");

    /**
     * The {@code group_suspend_v2_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: group_suspend_v2_enabled:[3180,"bool",!1,!0]
     */
    public static final ABProp GROUP_SUSPEND_V2_ENABLED = new ABProp(3180, "false", "true");

    /**
     * The {@code enable_chat_psa_auto_play_videos} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_chat_psa_auto_play_videos:[3182,"bool",!1,!0]
     */
    public static final ABProp ENABLE_CHAT_PSA_AUTO_PLAY_VIDEOS = new ABProp(3182, "false", "true");

    /**
     * The {@code default_video_limit_mb} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: default_video_limit_mb:[3185,"int",16,64]
     */
    public static final ABProp DEFAULT_VIDEO_LIMIT_MB = new ABProp(3185, "16", "64");

    /**
     * The {@code web_image_max_hd_edge} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_image_max_hd_edge:[3204,"int",2560,2560]
     */
    public static final ABProp WEB_IMAGE_MAX_HD_EDGE = new ABProp(3204, "2560", "2560");

    /**
     * The {@code extensions_user_report_store_max_data_exchanges_per_session} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: extensions_user_report_store_max_data_exchanges_per_session:[3211,"int",10,10]
     */
    public static final ABProp EXTENSIONS_USER_REPORT_STORE_MAX_DATA_EXCHANGES_PER_SESSION = new ABProp(3211, "10", "10");

    /**
     * The {@code extensions_user_report_store_max_data_max_sessions_per_message} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: extensions_user_report_store_max_data_max_sessions_per_message:[3212,"int",3,3]
     */
    public static final ABProp EXTENSIONS_USER_REPORT_STORE_MAX_DATA_MAX_SESSIONS_PER_MESSAGE = new ABProp(3212, "3", "3");

    /**
     * The {@code web_e2e_backfill_expire_time} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_e2e_backfill_expire_time:[3234,"int",5,60]
     */
    public static final ABProp WEB_E2E_BACKFILL_EXPIRE_TIME = new ABProp(3234, "5", "60");

    /**
     * The {@code enable_silent_offer} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_silent_offer:[3235,"bool",!1,!0]
     */
    public static final ABProp ENABLE_SILENT_OFFER = new ABProp(3235, "false", "true");

    /**
     * The {@code order_messages_ephemeral_exception_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: order_messages_ephemeral_exception_enabled:[3240,"bool",!1,!0]
     */
    public static final ABProp ORDER_MESSAGES_EPHEMERAL_EXCEPTION_ENABLED = new ABProp(3240, "false", "true");

    /**
     * The {@code message_edit_client_entry_point_limit_seconds} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: message_edit_client_entry_point_limit_seconds:[3272,"int",900,900]
     */
    public static final ABProp MESSAGE_EDIT_CLIENT_ENTRY_POINT_LIMIT_SECONDS = new ABProp(3272, "900", "900");

    /**
     * The {@code send_extended_nack_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: send_extended_nack_enabled:[3280,"bool",!1,!1]
     */
    public static final ABProp SEND_EXTENDED_NACK_ENABLED = new ABProp(3280, "false", "false");

    /**
     * The {@code smb_waldo_service_offerings_selection_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_waldo_service_offerings_selection_enabled:[3285,"bool",!1,!0]
     */
    public static final ABProp SMB_WALDO_SERVICE_OFFERINGS_SELECTION_ENABLED = new ABProp(3285, "false", "true");

    /**
     * The {@code wa_ctwa_web_fetch_linked_accounts_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_ctwa_web_fetch_linked_accounts_enabled:[3294,"bool",!1,!1]
     */
    public static final ABProp WA_CTWA_WEB_FETCH_LINKED_ACCOUNTS_ENABLED = new ABProp(3294, "false", "false");

    /**
     * The {@code dm_additional_durations} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: dm_additional_durations:[3305,"bool",!1,!1]
     */
    public static final ABProp DM_ADDITIONAL_DURATIONS = new ABProp(3305, "false", "false");

    /**
     * The {@code enable_days_since_receive_logging} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_days_since_receive_logging:[3322,"bool",!1,!0]
     */
    public static final ABProp ENABLE_DAYS_SINCE_RECEIVE_LOGGING = new ABProp(3322, "false", "true");

    /**
     * The {@code ctwa_smb_data_sharing_opt_in_cool_off_period} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ctwa_smb_data_sharing_opt_in_cool_off_period:[3331,"int",259200,259200]
     */
    public static final ABProp CTWA_SMB_DATA_SHARING_OPT_IN_COOL_OFF_PERIOD = new ABProp(3331, "259200", "259200");

    /**
     * The {@code history_sync_on_demand} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: history_sync_on_demand:[3337,"bool",!1,!1]
     */
    public static final ABProp HISTORY_SYNC_ON_DEMAND = new ABProp(3337, "false", "false");

    /**
     * The {@code wds_radius_and_casing} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wds_radius_and_casing:[3350,"bool",!1,!0]
     */
    public static final ABProp WDS_RADIUS_AND_CASING = new ABProp(3350, "false", "true");

    /**
     * The {@code ptv_max_duration_seconds} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ptv_max_duration_seconds:[3356,"int",60,60]
     */
    public static final ABProp PTV_MAX_DURATION_SECONDS = new ABProp(3356, "60", "60");

    /**
     * The {@code calling_lid_version} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: calling_lid_version:[3358,"int",0,0]
     */
    public static final ABProp CALLING_LID_VERSION = new ABProp(3358, "0", "0");

    /**
     * The {@code wa_ctwa_web_entrypoint_manage_ads_home_header_dropdown_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_ctwa_web_entrypoint_manage_ads_home_header_dropdown_enabled:[3376,"bool",!1,!1]
     */
    public static final ABProp WA_CTWA_WEB_ENTRYPOINT_MANAGE_ADS_HOME_HEADER_DROPDOWN_ENABLED = new ABProp(3376, "false", "false");

    /**
     * The {@code group_join_request_can_view_optional_message} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: group_join_request_can_view_optional_message:[3383,"bool",!1,!1]
     */
    public static final ABProp GROUP_JOIN_REQUEST_CAN_VIEW_OPTIONAL_MESSAGE = new ABProp(3383, "false", "false");

    /**
     * The {@code group_join_request_can_send_optional_message} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: group_join_request_can_send_optional_message:[3384,"bool",!1,!1]
     */
    public static final ABProp GROUP_JOIN_REQUEST_CAN_SEND_OPTIONAL_MESSAGE = new ABProp(3384, "false", "false");

    /**
     * The {@code smb_project_waldo_set_price_tier_biz_profile_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_project_waldo_set_price_tier_biz_profile_enabled:[3467,"bool",!1,!0]
     */
    public static final ABProp SMB_PROJECT_WALDO_SET_PRICE_TIER_BIZ_PROFILE_ENABLED = new ABProp(3467, "false", "true");

    /**
     * The {@code ptv_autoplay_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ptv_autoplay_enabled:[3482,"bool",!0,!0]
     */
    public static final ABProp PTV_AUTOPLAY_ENABLED = new ABProp(3482, "true", "true");

    /**
     * The {@code ptv_autoplay_loop_limit} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ptv_autoplay_loop_limit:[3483,"int",3,3]
     */
    public static final ABProp PTV_AUTOPLAY_LOOP_LIMIT = new ABProp(3483, "3", "3");

    /**
     * The {@code qp_campaign_client_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: qp_campaign_client_enabled:[3536,"bool",!1,!1]
     */
    public static final ABProp QP_CAMPAIGN_CLIENT_ENABLED = new ABProp(3536, "false", "false");

    /**
     * The {@code animated_emojis_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: animated_emojis_enabled:[3575,"bool",!1,!1]
     */
    public static final ABProp ANIMATED_EMOJIS_ENABLED = new ABProp(3575, "false", "false");

    /**
     * The {@code placeholder_message_resend} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: placeholder_message_resend:[3579,"bool",!1,!1]
     */
    public static final ABProp PLACEHOLDER_MESSAGE_RESEND = new ABProp(3579, "false", "false");

    /**
     * The {@code coupon_copy_button_url} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: coupon_copy_button_url:[3631,"string","https://www.whatsapp.com/coupon?code=","https://www.whatsapp.com/coupon?code="]
     */
    public static final ABProp COUPON_COPY_BUTTON_URL = new ABProp(3631, "https://www.whatsapp.com/coupon?code=", "https://www.whatsapp.com/coupon?code=");

    /**
     * The {@code placeholder_message_resend_maximum_days_limit} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: placeholder_message_resend_maximum_days_limit:[3639,"int",14,14]
     */
    public static final ABProp PLACEHOLDER_MESSAGE_RESEND_MAXIMUM_DAYS_LIMIT = new ABProp(3639, "14", "14");

    /**
     * The {@code default_audio_limit_mb} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: default_audio_limit_mb:[3657,"int",16,64]
     */
    public static final ABProp DEFAULT_AUDIO_LIMIT_MB = new ABProp(3657, "16", "64");

    /**
     * The {@code default_status_media_limit_mb} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: default_status_media_limit_mb:[3659,"int",16,64]
     */
    public static final ABProp DEFAULT_STATUS_MEDIA_LIMIT_MB = new ABProp(3659, "16", "64");

    /**
     * The {@code default_media_limit_mb} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: default_media_limit_mb:[3660,"int",16,64]
     */
    public static final ABProp DEFAULT_MEDIA_LIMIT_MB = new ABProp(3660, "16", "64");

    /**
     * The {@code service_improvement_opt_out_flag} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: service_improvement_opt_out_flag:[3664,"bool",!1,!1]
     */
    public static final ABProp SERVICE_IMPROVEMENT_OPT_OUT_FLAG = new ABProp(3664, "false", "false");

    /**
     * The {@code gif_min_play_loops} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: gif_min_play_loops:[3682,"int",1,1]
     */
    public static final ABProp GIF_MIN_PLAY_LOOPS = new ABProp(3682, "1", "1");

    /**
     * The {@code gif_max_play_loops} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: gif_max_play_loops:[3683,"int",3,3]
     */
    public static final ABProp GIF_MAX_PLAY_LOOPS = new ABProp(3683, "3", "3");

    /**
     * The {@code gif_max_play_duration} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: gif_max_play_duration:[3684,"int",5,5]
     */
    public static final ABProp GIF_MAX_PLAY_DURATION = new ABProp(3684, "5", "5");

    /**
     * The {@code orders_expansion_receiver_countries_allowed} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: orders_expansion_receiver_countries_allowed:[3690,"string","",""]
     */
    public static final ABProp ORDERS_EXPANSION_RECEIVER_COUNTRIES_ALLOWED = new ABProp(3690, "", "");

    /**
     * The {@code max_num_participants_for_ss} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: max_num_participants_for_ss:[3694,"int",8,8]
     */
    public static final ABProp MAX_NUM_PARTICIPANTS_FOR_SS = new ABProp(3694, "8", "8");

    /**
     * The {@code report_to_admin_kill_switch} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: report_to_admin_kill_switch:[3695,"bool",!1,!0]
     */
    public static final ABProp REPORT_TO_ADMIN_KILL_SWITCH = new ABProp(3695, "false", "true");

    /**
     * The {@code report_to_admin_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: report_to_admin_enabled:[3696,"bool",!1,!0]
     */
    public static final ABProp REPORT_TO_ADMIN_ENABLED = new ABProp(3696, "false", "true");

    /**
     * The {@code quick_promotion_banner_client_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: quick_promotion_banner_client_enabled:[3712,"bool",!1,!1]
     */
    public static final ABProp QUICK_PROMOTION_BANNER_CLIENT_ENABLED = new ABProp(3712, "false", "false");

    /**
     * The {@code web_message_processing_cache_size} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_message_processing_cache_size:[3728,"int",400,400]
     */
    public static final ABProp WEB_MESSAGE_PROCESSING_CACHE_SIZE = new ABProp(3728, "400", "400");

    /**
     * The {@code pinned_messages_m2_pin_max} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: pinned_messages_m2_pin_max:[3732,"int",1,1]
     */
    public static final ABProp PINNED_MESSAGES_M2_PIN_MAX = new ABProp(3732, "1", "1");

    /**
     * The {@code payments_merchant_global_orders_value_props_banner_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: payments_merchant_global_orders_value_props_banner_enabled:[3744,"bool",!1,!0]
     */
    public static final ABProp PAYMENTS_MERCHANT_GLOBAL_ORDERS_VALUE_PROPS_BANNER_ENABLED = new ABProp(3744, "false", "true");

    /**
     * The {@code newsletter_tos_notice_id} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: newsletter_tos_notice_id:[3810,"string","20601216","20601216"]
     */
    public static final ABProp NEWSLETTER_TOS_NOTICE_ID = new ABProp(3810, "20601216", "20601216");

    /**
     * The {@code history_sync_on_demand_message_count} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: history_sync_on_demand_message_count:[3811,"int",50,50]
     */
    public static final ABProp HISTORY_SYNC_ON_DEMAND_MESSAGE_COUNT = new ABProp(3811, "50", "50");

    /**
     * The {@code unified_otp_copy_code_url} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: unified_otp_copy_code_url:[3827,"string","https://www.whatsapp.com/otp/copy/","https://www.whatsapp.com/otp/copy/"]
     */
    public static final ABProp UNIFIED_OTP_COPY_CODE_URL = new ABProp(3827, "https://www.whatsapp.com/otp/copy/", "https://www.whatsapp.com/otp/copy/");

    /**
     * The {@code unified_otp_retriever_url} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: unified_otp_retriever_url:[3828,"string","https://www.whatsapp.com/otp/code","https://www.whatsapp.com/otp/code"]
     */
    public static final ABProp UNIFIED_OTP_RETRIEVER_URL = new ABProp(3828, "https://www.whatsapp.com/otp/code", "https://www.whatsapp.com/otp/code");

    /**
     * The {@code newsletter_creation_tos_id} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: newsletter_creation_tos_id:[3834,"string","20601217","20601217"]
     */
    public static final ABProp NEWSLETTER_CREATION_TOS_ID = new ABProp(3834, "20601217", "20601217");

    /**
     * The {@code newsletter_creation_nux_id} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: newsletter_creation_nux_id:[3835,"string","20601218","20601218"]
     */
    public static final ABProp NEWSLETTER_CREATION_NUX_ID = new ABProp(3835, "20601218", "20601218");

    /**
     * The {@code ts_session_duration_ms} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ts_session_duration_ms:[3860,"int",600000,600000]
     */
    public static final ABProp TS_SESSION_DURATION_MS = new ABProp(3860, "600000", "600000");

    /**
     * The {@code channels_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_enabled:[3877,"int",0,2]
     */
    public static final ABProp CHANNELS_ENABLED = new ABProp(3877, "0", "2");

    /**
     * The {@code channels_creation_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_creation_enabled:[3878,"int",0,2]
     */
    public static final ABProp CHANNELS_CREATION_ENABLED = new ABProp(3878, "0", "2");

    /**
     * The {@code channels_directory_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_directory_enabled:[3879,"int",0,2]
     */
    public static final ABProp CHANNELS_DIRECTORY_ENABLED = new ABProp(3879, "0", "2");

    /**
     * The {@code history_sync_on_demand_timeout_ms} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: history_sync_on_demand_timeout_ms:[3882,"int",10000,10000]
     */
    public static final ABProp HISTORY_SYNC_ON_DEMAND_TIMEOUT_MS = new ABProp(3882, "10000", "10000");

    /**
     * The {@code channel_supported_message_types} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channel_supported_message_types:[3919,"string","1, 2, 3, 5, 9, 10, 12, 15","1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15"]
     */
    public static final ABProp CHANNEL_SUPPORTED_MESSAGE_TYPES = new ABProp(3919, "1, 2, 3, 5, 9, 10, 12, 15", "1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15");

    /**
     * The {@code privacy_tips_groups_build} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: privacy_tips_groups_build:[3995,"bool",!1,!1]
     */
    public static final ABProp PRIVACY_TIPS_GROUPS_BUILD = new ABProp(3995, "false", "false");

    /**
     * The {@code privacy_tips_profile_build} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: privacy_tips_profile_build:[3998,"bool",!1,!1]
     */
    public static final ABProp PRIVACY_TIPS_PROFILE_BUILD = new ABProp(3998, "false", "false");

    /**
     * The {@code bonsai_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: bonsai_enabled:[4010,"bool",!1,!1]
     */
    public static final ABProp BONSAI_ENABLED = new ABProp(4010, "false", "false");

    /**
     * The {@code ugc_participant_limit} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ugc_participant_limit:[4118,"int",5,5]
     */
    public static final ABProp UGC_PARTICIPANT_LIMIT = new ABProp(4118, "5", "5");

    /**
     * The {@code history_sync_on_demand_with_android_beta} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: history_sync_on_demand_with_android_beta:[4135,"bool",!1,!1]
     */
    public static final ABProp HISTORY_SYNC_ON_DEMAND_WITH_ANDROID_BETA = new ABProp(4135, "false", "false");

    /**
     * The {@code hd_video_definition_min_edge} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: hd_video_definition_min_edge:[4171,"int",720,720]
     */
    public static final ABProp HD_VIDEO_DEFINITION_MIN_EDGE = new ABProp(4171, "720", "720");

    /**
     * The {@code hd_video_definition_max_edge} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: hd_video_definition_max_edge:[4172,"int",864,864]
     */
    public static final ABProp HD_VIDEO_DEFINITION_MAX_EDGE = new ABProp(4172, "864", "864");

    /**
     * The {@code hd_video_definition_min_edge_with_max_edge} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: hd_video_definition_min_edge_with_max_edge:[4175,"int",480,480]
     */
    public static final ABProp HD_VIDEO_DEFINITION_MIN_EDGE_WITH_MAX_EDGE = new ABProp(4175, "480", "480");

    /**
     * The {@code group_call_max_participants} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: group_call_max_participants:[4190,"int",32,32]
     */
    public static final ABProp GROUP_CALL_MAX_PARTICIPANTS = new ABProp(4190, "32", "32");

    /**
     * The {@code payments_br_content_optimization_variant} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: payments_br_content_optimization_variant:[4248,"int",0,0]
     */
    public static final ABProp PAYMENTS_BR_CONTENT_OPTIMIZATION_VARIANT = new ABProp(4248, "0", "0");

    /**
     * The {@code p2m_external_payments_link_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: p2m_external_payments_link_enabled:[4295,"bool",!1,!0]
     */
    public static final ABProp P2M_EXTERNAL_PAYMENTS_LINK_ENABLED = new ABProp(4295, "false", "true");

    /**
     * The {@code channel_reactions_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channel_reactions_enabled:[4306,"bool",!1,!0]
     */
    public static final ABProp CHANNEL_REACTIONS_ENABLED = new ABProp(4306, "false", "true");

    /**
     * The {@code recommended_channels_background_refresh} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: recommended_channels_background_refresh:[4309,"int",14400000,1800000]
     */
    public static final ABProp RECOMMENDED_CHANNELS_BACKGROUND_REFRESH = new ABProp(4309, "14400000", "1800000");

    /**
     * The {@code privacy_tips_killswitch} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: privacy_tips_killswitch:[4314,"bool",!1,!1]
     */
    public static final ABProp PRIVACY_TIPS_KILLSWITCH = new ABProp(4314, "false", "false");

    /**
     * The {@code channel_pull_message_updates_threshold_seconds} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channel_pull_message_updates_threshold_seconds:[4326,"int",120,120]
     */
    public static final ABProp CHANNEL_PULL_MESSAGE_UPDATES_THRESHOLD_SECONDS = new ABProp(4326, "120", "120");

    /**
     * The {@code web_otp_copy_code_disabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_otp_copy_code_disabled:[4330,"bool",!1,!1]
     */
    public static final ABProp WEB_OTP_COPY_CODE_DISABLED = new ABProp(4330, "false", "false");

    /**
     * The {@code channel_forward_to_chat_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channel_forward_to_chat_enabled:[4338,"bool",!1,!0]
     */
    public static final ABProp CHANNEL_FORWARD_TO_CHAT_ENABLED = new ABProp(4338, "false", "true");

    /**
     * The {@code enable_ml_bwe_model_download} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_ml_bwe_model_download:[4349,"bool",!1,!0]
     */
    public static final ABProp ENABLE_ML_BWE_MODEL_DOWNLOAD = new ABProp(4349, "false", "true");

    /**
     * The {@code history_sync_on_demand_failure_limit} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: history_sync_on_demand_failure_limit:[4364,"int",10,10]
     */
    public static final ABProp HISTORY_SYNC_ON_DEMAND_FAILURE_LIMIT = new ABProp(4364, "10", "10");

    /**
     * The {@code history_sync_on_demand_cooldown_sec} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: history_sync_on_demand_cooldown_sec:[4365,"int",7200,7200]
     */
    public static final ABProp HISTORY_SYNC_ON_DEMAND_COOLDOWN_SEC = new ABProp(4365, "7200", "7200");

    /**
     * The {@code flattened_reactions_collection} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: flattened_reactions_collection:[4390,"bool",!1,!1]
     */
    public static final ABProp FLATTENED_REACTIONS_COLLECTION = new ABProp(4390, "false", "false");

    /**
     * The {@code bonsai_ptt_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: bonsai_ptt_enabled:[4416,"bool",!1,!0]
     */
    public static final ABProp BONSAI_PTT_ENABLED = new ABProp(4416, "false", "true");

    /**
     * The {@code bonsai_update_interval} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: bonsai_update_interval:[4417,"int",86400,86400]
     */
    public static final ABProp BONSAI_UPDATE_INTERVAL = new ABProp(4417, "86400", "86400");

    /**
     * The {@code business_tool_enhanced_logging} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: business_tool_enhanced_logging:[4427,"bool",!1,!1]
     */
    public static final ABProp BUSINESS_TOOL_ENHANCED_LOGGING = new ABProp(4427, "false", "false");

    /**
     * The {@code pinned_messages_sender_short_expiry_durations_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: pinned_messages_sender_short_expiry_durations_enabled:[4432,"bool",!1,!1]
     */
    public static final ABProp PINNED_MESSAGES_SENDER_SHORT_EXPIRY_DURATIONS_ENABLED = new ABProp(4432, "false", "false");

    /**
     * The {@code pnh_cag_disable_reactions_group_size} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: pnh_cag_disable_reactions_group_size:[4495,"int",10000,10000]
     */
    public static final ABProp PNH_CAG_DISABLE_REACTIONS_GROUP_SIZE = new ABProp(4495, "10000", "10000");

    /**
     * The {@code bonsai_avatar_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: bonsai_avatar_enabled:[4532,"bool",!1,!1]
     */
    public static final ABProp BONSAI_AVATAR_ENABLED = new ABProp(4532, "false", "false");

    /**
     * The {@code in_app_comms_manage_ads_web_banner_campaign_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: in_app_comms_manage_ads_web_banner_campaign_enabled:[4542,"bool",!1,!0]
     */
    public static final ABProp IN_APP_COMMS_MANAGE_ADS_WEB_BANNER_CAMPAIGN_ENABLED = new ABProp(4542, "false", "true");

    /**
     * The {@code add_member_system_message} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: add_member_system_message:[4579,"bool",!0,!0]
     */
    public static final ABProp ADD_MEMBER_SYSTEM_MESSAGE = new ABProp(4579, "true", "true");

    /**
     * The {@code web_premium_messages_interactivity_rendering_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_premium_messages_interactivity_rendering_enabled:[4596,"bool",!1,!0]
     */
    public static final ABProp WEB_PREMIUM_MESSAGES_INTERACTIVITY_RENDERING_ENABLED = new ABProp(4596, "false", "true");

    /**
     * The {@code channel_views_duration_milliseconds} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channel_views_duration_milliseconds:[4648,"int",250,250]
     */
    public static final ABProp CHANNEL_VIEWS_DURATION_MILLISECONDS = new ABProp(4648, "250", "250");

    /**
     * The {@code smb_premium_messages_click_logging_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_premium_messages_click_logging_enabled:[4657,"bool",!1,!0]
     */
    public static final ABProp SMB_PREMIUM_MESSAGES_CLICK_LOGGING_ENABLED = new ABProp(4657, "false", "true");

    /**
     * The {@code enable_clear_formatted_preview} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_clear_formatted_preview:[4659,"bool",!1,!0]
     */
    public static final ABProp ENABLE_CLEAR_FORMATTED_PREVIEW = new ABProp(4659, "false", "true");

    /**
     * The {@code carousel_message_client_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: carousel_message_client_enabled:[4668,"bool",!1,!0]
     */
    public static final ABProp CAROUSEL_MESSAGE_CLIENT_ENABLED = new ABProp(4668, "false", "true");

    /**
     * The {@code web_internal_in_app_bug_reporting_enable} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_internal_in_app_bug_reporting_enable:[4681,"bool",!1,!1]
     */
    public static final ABProp WEB_INTERNAL_IN_APP_BUG_REPORTING_ENABLE = new ABProp(4681, "false", "false");

    /**
     * The {@code channel_forward_to_chat_v2_message_navigation_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channel_forward_to_chat_v2_message_navigation_enabled:[4682,"bool",!1,!0]
     */
    public static final ABProp CHANNEL_FORWARD_TO_CHAT_V2_MESSAGE_NAVIGATION_ENABLED = new ABProp(4682, "false", "true");

    /**
     * The {@code max_group_size_for_long_ringtone} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: max_group_size_for_long_ringtone:[4710,"int",0,0]
     */
    public static final ABProp MAX_GROUP_SIZE_FOR_LONG_RINGTONE = new ABProp(4710, "0", "0");

    /**
     * The {@code channel_view_counts_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channel_view_counts_enabled:[4721,"int",0,3]
     */
    public static final ABProp CHANNEL_VIEW_COUNTS_ENABLED = new ABProp(4721, "0", "3");

    /**
     * The {@code channel_playable_message_views_duration_milliseconds} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channel_playable_message_views_duration_milliseconds:[4722,"int",3000,3000]
     */
    public static final ABProp CHANNEL_PLAYABLE_MESSAGE_VIEWS_DURATION_MILLISECONDS = new ABProp(4722, "3000", "3000");

    /**
     * The {@code web_sticker_suggestions_enable} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_sticker_suggestions_enable:[4726,"bool",!1,!1]
     */
    public static final ABProp WEB_STICKER_SUGGESTIONS_ENABLE = new ABProp(4726, "false", "false");

    /**
     * The {@code bonsai_ti_timeout_duration_ms} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: bonsai_ti_timeout_duration_ms:[4736,"int",10000,10000]
     */
    public static final ABProp BONSAI_TI_TIMEOUT_DURATION_MS = new ABProp(4736, "10000", "10000");

    /**
     * The {@code username_creation} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: username_creation:[4745,"bool",!1,!1]
     */
    public static final ABProp USERNAME_CREATION = new ABProp(4745, "false", "false");

    /**
     * The {@code username_contact_display} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: username_contact_display:[4746,"bool",!1,!1]
     */
    public static final ABProp USERNAME_CONTACT_DISPLAY = new ABProp(4746, "false", "false");

    /**
     * The {@code channels_send_view_receipt_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_send_view_receipt_enabled:[4760,"bool",!1,!0]
     */
    public static final ABProp CHANNELS_SEND_VIEW_RECEIPT_ENABLED = new ABProp(4760, "false", "true");

    /**
     * The {@code in_app_support_capi_number_prefixes} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: in_app_support_capi_number_prefixes:[4799,"string","155178684","155178684"]
     */
    public static final ABProp IN_APP_SUPPORT_CAPI_NUMBER_PREFIXES = new ABProp(4799, "155178684", "155178684");

    /**
     * The {@code low_cache_hit_rate_media_types} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: low_cache_hit_rate_media_types:[4836,"string","ptt,audio,document,ppic","ptt,audio,document,ppic"]
     */
    public static final ABProp LOW_CACHE_HIT_RATE_MEDIA_TYPES = new ABProp(4836, "ptt,audio,document,ppic", "ptt,audio,document,ppic");

    /**
     * The {@code wae_metadata_integrity_timeout_minutes} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wae_metadata_integrity_timeout_minutes:[4849,"int",5,5]
     */
    public static final ABProp WAE_METADATA_INTEGRITY_TIMEOUT_MINUTES = new ABProp(4849, "5", "5");

    /**
     * The {@code wabai_message_rendering_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wabai_message_rendering_enabled:[4873,"bool",!1,!1]
     */
    public static final ABProp WABAI_MESSAGE_RENDERING_ENABLED = new ABProp(4873, "false", "false");

    /**
     * The {@code channel_reactions_settings_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channel_reactions_settings_enabled:[4887,"bool",!1,!0]
     */
    public static final ABProp CHANNEL_REACTIONS_SETTINGS_ENABLED = new ABProp(4887, "false", "true");

    /**
     * The {@code row_buyer_order_revamp_m0_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: row_buyer_order_revamp_m0_enabled:[4893,"bool",!1,!0]
     */
    public static final ABProp ROW_BUYER_ORDER_REVAMP_M0_ENABLED = new ABProp(4893, "false", "true");

    /**
     * The {@code ts_surface_killswitch} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ts_surface_killswitch:[4929,"int",0,0]
     */
    public static final ABProp TS_SURFACE_KILLSWITCH = new ABProp(4929, "0", "0");

    /**
     * The {@code bonsai_word_streaming_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: bonsai_word_streaming_enabled:[4974,"bool",!1,!1]
     */
    public static final ABProp BONSAI_WORD_STREAMING_ENABLED = new ABProp(4974, "false", "false");

    /**
     * The {@code enable_spam_report_iq_with_privacy_token} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_spam_report_iq_with_privacy_token:[4991,"bool",!1,!0]
     */
    public static final ABProp ENABLE_SPAM_REPORT_IQ_WITH_PRIVACY_TOKEN = new ABProp(4991, "false", "true");

    /**
     * The {@code enable_privacy_token_with_timestamp} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_privacy_token_with_timestamp:[4992,"bool",!1,!0]
     */
    public static final ABProp ENABLE_PRIVACY_TOKEN_WITH_TIMESTAMP = new ABProp(4992, "false", "true");

    /**
     * The {@code smb_labels_ctwa_data_sharing} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_labels_ctwa_data_sharing:[5009,"bool",!1,!0]
     */
    public static final ABProp SMB_LABELS_CTWA_DATA_SHARING = new ABProp(5009, "false", "true");

    /**
     * The {@code channels_filter_out_subscribed_in_directory_null_state} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_filter_out_subscribed_in_directory_null_state:[5015,"bool",!1,!0]
     */
    public static final ABProp CHANNELS_FILTER_OUT_SUBSCRIBED_IN_DIRECTORY_NULL_STATE = new ABProp(5015, "false", "true");

    /**
     * The {@code community_general_chat_UI_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: community_general_chat_UI_enabled:[5021,"bool",!1,!1]
     */
    public static final ABProp COMMUNITY_GENERAL_CHAT_UI_ENABLED = new ABProp(5021, "false", "false");

    /**
     * The {@code smb_premium_messages_url_cta_alert_dialog_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_premium_messages_url_cta_alert_dialog_enabled:[5044,"bool",!0,!0]
     */
    public static final ABProp SMB_PREMIUM_MESSAGES_URL_CTA_ALERT_DIALOG_ENABLED = new ABProp(5044, "true", "true");

    /**
     * The {@code pnh_cag_disable_polls_group_size} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: pnh_cag_disable_polls_group_size:[5056,"int",10000,10000]
     */
    public static final ABProp PNH_CAG_DISABLE_POLLS_GROUP_SIZE = new ABProp(5056, "10000", "10000");

    /**
     * The {@code parent_group_allow_member_suggest_existing_m3_sender} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: parent_group_allow_member_suggest_existing_m3_sender:[5077,"bool",!1,!0]
     */
    public static final ABProp PARENT_GROUP_ALLOW_MEMBER_SUGGEST_EXISTING_M3_SENDER = new ABProp(5077, "false", "true");

    /**
     * The {@code parent_group_allow_member_suggest_existing_m3_receiver} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: parent_group_allow_member_suggest_existing_m3_receiver:[5078,"bool",!1,!0]
     */
    public static final ABProp PARENT_GROUP_ALLOW_MEMBER_SUGGEST_EXISTING_M3_RECEIVER = new ABProp(5078, "false", "true");

    /**
     * The {@code web_preload_chat_messages} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_preload_chat_messages:[5079,"bool",!1,!0]
     */
    public static final ABProp WEB_PRELOAD_CHAT_MESSAGES = new ABProp(5079, "false", "true");

    /**
     * The {@code web_noncritical_history_sync_message_processing_break_iteration} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_noncritical_history_sync_message_processing_break_iteration:[5106,"int",100,100]
     */
    public static final ABProp WEB_NONCRITICAL_HISTORY_SYNC_MESSAGE_PROCESSING_BREAK_ITERATION = new ABProp(5106, "100", "100");

    /**
     * The {@code web_tc_token_db_read_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_tc_token_db_read_enabled:[5110,"bool",!1,!1]
     */
    public static final ABProp WEB_TC_TOKEN_DB_READ_ENABLED = new ABProp(5110, "false", "false");

    /**
     * The {@code buyer_initiated_order_request_variant_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: buyer_initiated_order_request_variant_enabled:[5114,"bool",!1,!1]
     */
    public static final ABProp BUYER_INITIATED_ORDER_REQUEST_VARIANT_ENABLED = new ABProp(5114, "false", "false");

    /**
     * The {@code channels_directory_v2_filter_types} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_directory_v2_filter_types:[5127,"string","","1, 2, 3, 4, 5, 6"]
     */
    public static final ABProp CHANNELS_DIRECTORY_V2_FILTER_TYPES = new ABProp(5127, "", "1, 2, 3, 4, 5, 6");

    /**
     * The {@code inbox_filters_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: inbox_filters_enabled:[5171,"bool",!1,!1]
     */
    public static final ABProp INBOX_FILTERS_ENABLED = new ABProp(5171, "false", "false");

    /**
     * The {@code channel_reactions_sender_list_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channel_reactions_sender_list_enabled:[5185,"bool",!0,!0]
     */
    public static final ABProp CHANNEL_REACTIONS_SENDER_LIST_ENABLED = new ABProp(5185, "true", "true");

    /**
     * The {@code seller_orders_management_revamp} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: seller_orders_management_revamp:[5190,"bool",!1,!1]
     */
    public static final ABProp SELLER_ORDERS_MANAGEMENT_REVAMP = new ABProp(5190, "false", "false");

    /**
     * The {@code channels_directory_search_debounce_ms} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_directory_search_debounce_ms:[5204,"int",250,250]
     */
    public static final ABProp CHANNELS_DIRECTORY_SEARCH_DEBOUNCE_MS = new ABProp(5204, "250", "250");

    /**
     * The {@code wabai_message_feedback_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wabai_message_feedback_enabled:[5215,"bool",!1,!1]
     */
    public static final ABProp WABAI_MESSAGE_FEEDBACK_ENABLED = new ABProp(5215, "false", "false");

    /**
     * The {@code channels_followers_list_cache_refresh_milliseconds} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_followers_list_cache_refresh_milliseconds:[5217,"int",60000,60000]
     */
    public static final ABProp CHANNELS_FOLLOWERS_LIST_CACHE_REFRESH_MILLISECONDS = new ABProp(5217, "60000", "60000");

    /**
     * The {@code wavoip_ml_bwe_plc_model_download_versions} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wavoip_ml_bwe_plc_model_download_versions:[5228,"string","",""]
     */
    public static final ABProp WAVOIP_ML_BWE_PLC_MODEL_DOWNLOAD_VERSIONS = new ABProp(5228, "", "");

    /**
     * The {@code wavoip_ml_bwe_undershoot_model_download_versions} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wavoip_ml_bwe_undershoot_model_download_versions:[5231,"string","",""]
     */
    public static final ABProp WAVOIP_ML_BWE_UNDERSHOOT_MODEL_DOWNLOAD_VERSIONS = new ABProp(5231, "", "");

    /**
     * The {@code web_offline_dynamic_batch_size_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_offline_dynamic_batch_size_enabled:[5271,"bool",!1,!0]
     */
    public static final ABProp WEB_OFFLINE_DYNAMIC_BATCH_SIZE_ENABLED = new ABProp(5271, "false", "true");

    /**
     * The {@code blue_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: blue_enabled:[5276,"bool",!1,!1]
     */
    public static final ABProp BLUE_ENABLED = new ABProp(5276, "false", "false");

    /**
     * The {@code bonsai_carousel_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: bonsai_carousel_enabled:[5283,"bool",!1,!0]
     */
    public static final ABProp BONSAI_CAROUSEL_ENABLED = new ABProp(5283, "false", "true");

    /**
     * The {@code channels_hide_news_url_preview} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_hide_news_url_preview:[5287,"bool",!1,!0]
     */
    public static final ABProp CHANNELS_HIDE_NEWS_URL_PREVIEW = new ABProp(5287, "false", "true");

    /**
     * The {@code blue_education_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: blue_education_enabled:[5295,"bool",!1,!1]
     */
    public static final ABProp BLUE_EDUCATION_ENABLED = new ABProp(5295, "false", "false");

    /**
     * The {@code web_offline_dynamic_batch_config} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_offline_dynamic_batch_config:[5297,"string","{\"version\": \"progressive\", \"multiplier\": 0.25}","{\"version\": \"progressive\", \"multiplier\": 0.25}"]
     */
    public static final ABProp WEB_OFFLINE_DYNAMIC_BATCH_CONFIG = new ABProp(5297, "{\"version\": \"progressive\", \"multiplier\": 0.25}", "{\"version\": \"progressive\", \"multiplier\": 0.25}");

    /**
     * The {@code channels_directory_v2_cache_refresh_interval_ms} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_directory_v2_cache_refresh_interval_ms:[5304,"int",1800000,600000]
     */
    public static final ABProp CHANNELS_DIRECTORY_V2_CACHE_REFRESH_INTERVAL_MS = new ABProp(5304, "1800000", "600000");

    /**
     * The {@code premium_blue_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: premium_blue_enabled:[5318,"bool",!1,!1]
     */
    public static final ABProp PREMIUM_BLUE_ENABLED = new ABProp(5318, "false", "false");

    /**
     * The {@code extensions_geoblocking_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: extensions_geoblocking_enabled:[5333,"bool",!1,!0]
     */
    public static final ABProp EXTENSIONS_GEOBLOCKING_ENABLED = new ABProp(5333, "false", "true");

    /**
     * The {@code web_evolve_about_send_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_evolve_about_send_enabled:[5347,"bool",!1,!1]
     */
    public static final ABProp WEB_EVOLVE_ABOUT_SEND_ENABLED = new ABProp(5347, "false", "false");

    /**
     * The {@code community_general_chat_create_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: community_general_chat_create_enabled:[5453,"bool",!1,!1]
     */
    public static final ABProp COMMUNITY_GENERAL_CHAT_CREATE_ENABLED = new ABProp(5453, "false", "false");

    /**
     * The {@code channels_share_link_logging_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_share_link_logging_enabled:[5491,"bool",!1,!0]
     */
    public static final ABProp CHANNELS_SHARE_LINK_LOGGING_ENABLED = new ABProp(5491, "false", "true");

    /**
     * The {@code channels_forward_logging_v2_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_forward_logging_v2_enabled:[5492,"bool",!1,!0]
     */
    public static final ABProp CHANNELS_FORWARD_LOGGING_V2_ENABLED = new ABProp(5492, "false", "true");

    /**
     * The {@code channels_max_messages_batch_pull} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_max_messages_batch_pull:[5494,"int",100,100]
     */
    public static final ABProp CHANNELS_MAX_MESSAGES_BATCH_PULL = new ABProp(5494, "100", "100");

    /**
     * The {@code web_resume_optimized_read_receipt_send_interval} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_resume_optimized_read_receipt_send_interval:[5502,"int",500,500]
     */
    public static final ABProp WEB_RESUME_OPTIMIZED_READ_RECEIPT_SEND_INTERVAL = new ABProp(5502, "500", "500");

    /**
     * The {@code events_create} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: events_create:[5562,"bool",!0,!0]
     */
    public static final ABProp EVENTS_CREATE = new ABProp(5562, "true", "true");

    /**
     * The {@code dm_reliability_logging} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: dm_reliability_logging:[5580,"bool",!1,!1]
     */
    public static final ABProp DM_RELIABILITY_LOGGING = new ABProp(5580, "false", "false");

    /**
     * The {@code bot_3p_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: bot_3p_enabled:[5587,"bool",!1,!0]
     */
    public static final ABProp BOT_3P_ENABLED = new ABProp(5587, "false", "true");

    /**
     * The {@code newsletter_tos_notice_id_smb_web} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: newsletter_tos_notice_id_smb_web:[5597,"string","20601216","20601216"]
     */
    public static final ABProp NEWSLETTER_TOS_NOTICE_ID_SMB_WEB = new ABProp(5597, "20601216", "20601216");

    /**
     * The {@code newsletter_creation_tos_id_smb_web} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: newsletter_creation_tos_id_smb_web:[5598,"string","20601217","20601217"]
     */
    public static final ABProp NEWSLETTER_CREATION_TOS_ID_SMB_WEB = new ABProp(5598, "20601217", "20601217");

    /**
     * The {@code ctwa_smb_data_sharing_settings_killswitch} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ctwa_smb_data_sharing_settings_killswitch:[5615,"bool",!1,!1]
     */
    public static final ABProp CTWA_SMB_DATA_SHARING_SETTINGS_KILLSWITCH = new ABProp(5615, "false", "false");

    /**
     * The {@code saga_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: saga_enabled:[5626,"bool",!0,!0]
     */
    public static final ABProp SAGA_ENABLED = new ABProp(5626, "true", "true");

    /**
     * The {@code bonsai_english_only} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: bonsai_english_only:[5637,"bool",!1,!1]
     */
    public static final ABProp BONSAI_ENGLISH_ONLY = new ABProp(5637, "false", "false");

    /**
     * The {@code channels_send_album_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_send_album_enabled:[5643,"bool",!1,!0]
     */
    public static final ABProp CHANNELS_SEND_ALBUM_ENABLED = new ABProp(5643, "false", "true");

    /**
     * The {@code rt_receive_reporting_tag} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: rt_receive_reporting_tag:[5718,"bool",!0,!0]
     */
    public static final ABProp RT_RECEIVE_REPORTING_TAG = new ABProp(5718, "true", "true");

    /**
     * The {@code wabai_consent_cooldown} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wabai_consent_cooldown:[5746,"int",-1,-1]
     */
    public static final ABProp WABAI_CONSENT_COOLDOWN = new ABProp(5746, "-1", "-1");

    /**
     * The {@code wabai_consent_required} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wabai_consent_required:[5747,"bool",!1,!1]
     */
    public static final ABProp WABAI_CONSENT_REQUIRED = new ABProp(5747, "false", "false");

    /**
     * The {@code inbox_filters_reset_timeout} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: inbox_filters_reset_timeout:[5765,"int",1800,1800]
     */
    public static final ABProp INBOX_FILTERS_RESET_TIMEOUT = new ABProp(5765, "1800", "1800");

    /**
     * The {@code order_statuses_revamp_m1_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: order_statuses_revamp_m1_enabled:[5770,"bool",!1,!0]
     */
    public static final ABProp ORDER_STATUSES_REVAMP_M1_ENABLED = new ABProp(5770, "false", "true");

    /**
     * The {@code parent_group_announcement_comments_history_sync_receiver_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: parent_group_announcement_comments_history_sync_receiver_enabled:[5813,"bool",!1,!1]
     */
    public static final ABProp PARENT_GROUP_ANNOUNCEMENT_COMMENTS_HISTORY_SYNC_RECEIVER_ENABLED = new ABProp(5813, "false", "false");

    /**
     * The {@code evolve_about_m1_receiver_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: evolve_about_m1_receiver_enabled:[5839,"bool",!1,!1]
     */
    public static final ABProp EVOLVE_ABOUT_M1_RECEIVER_ENABLED = new ABProp(5839, "false", "false");

    /**
     * The {@code blue_strings_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: blue_strings_enabled:[5846,"bool",!1,!0]
     */
    public static final ABProp BLUE_STRINGS_ENABLED = new ABProp(5846, "false", "true");

    /**
     * The {@code channels_directory_page_size} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_directory_page_size:[5853,"int",50,50]
     */
    public static final ABProp CHANNELS_DIRECTORY_PAGE_SIZE = new ABProp(5853, "50", "50");

    /**
     * The {@code mm_data_sharing_disclosure_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: mm_data_sharing_disclosure_enabled:[5869,"bool",!1,!1]
     */
    public static final ABProp MM_DATA_SHARING_DISCLOSURE_ENABLED = new ABProp(5869, "false", "false");

    /**
     * The {@code channels_proactive_message_gap_handling_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_proactive_message_gap_handling_enabled:[5871,"bool",!1,!1]
     */
    public static final ABProp CHANNELS_PROACTIVE_MESSAGE_GAP_HANDLING_ENABLED = new ABProp(5871, "false", "false");

    /**
     * The {@code channels_ptt_receiver_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_ptt_receiver_enabled:[5876,"bool",!1,!0]
     */
    public static final ABProp CHANNELS_PTT_RECEIVER_ENABLED = new ABProp(5876, "false", "true");

    /**
     * The {@code bot_3p_status} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: bot_3p_status:[5985,"int",0,0]
     */
    public static final ABProp BOT_3P_STATUS = new ABProp(5985, "0", "0");

    /**
     * The {@code data_sharing_transparency_indicator_duration} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: data_sharing_transparency_indicator_duration:[5990,"int",604800,604800]
     */
    public static final ABProp DATA_SHARING_TRANSPARENCY_INDICATOR_DURATION = new ABProp(5990, "604800", "604800");

    /**
     * The {@code unified_poll_vote_addon_infra_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: unified_poll_vote_addon_infra_enabled:[6046,"bool",!1,!1]
     */
    public static final ABProp UNIFIED_POLL_VOTE_ADDON_INFRA_ENABLED = new ABProp(6046, "false", "false");

    /**
     * The {@code inbox_filters_haptic_feedback_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: inbox_filters_haptic_feedback_enabled:[6052,"bool",!1,!1]
     */
    public static final ABProp INBOX_FILTERS_HAPTIC_FEEDBACK_ENABLED = new ABProp(6052, "false", "false");

    /**
     * The {@code fmx_ctwa_kill_switch} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: fmx_ctwa_kill_switch:[6061,"bool",!1,!1]
     */
    public static final ABProp FMX_CTWA_KILL_SWITCH = new ABProp(6061, "false", "false");

    /**
     * The {@code blue_education_v2_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: blue_education_v2_enabled:[6127,"bool",!1,!1]
     */
    public static final ABProp BLUE_EDUCATION_V2_ENABLED = new ABProp(6127, "false", "false");

    /**
     * The {@code dsa_channels_report_unlawful_content_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: dsa_channels_report_unlawful_content_enabled:[6145,"bool",!1,!0]
     */
    public static final ABProp DSA_CHANNELS_REPORT_UNLAWFUL_CONTENT_ENABLED = new ABProp(6145, "false", "true");

    /**
     * The {@code text_status_ttl_seconds_allowlist} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: text_status_ttl_seconds_allowlist:[6153,"string","1800,3600,7200,14400,28800,86400","1800,3600,7200,14400,28800,86400"]
     */
    public static final ABProp TEXT_STATUS_TTL_SECONDS_ALLOWLIST = new ABProp(6153, "1800,3600,7200,14400,28800,86400", "1800,3600,7200,14400,28800,86400");

    /**
     * The {@code evolve_about_m1_receiver_for_new_surfaces_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: evolve_about_m1_receiver_for_new_surfaces_enabled:[6172,"bool",!1,!1]
     */
    public static final ABProp EVOLVE_ABOUT_M1_RECEIVER_FOR_NEW_SURFACES_ENABLED = new ABProp(6172, "false", "false");

    /**
     * The {@code channels_poll_receive_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_poll_receive_enabled:[6191,"bool",!1,!0]
     */
    public static final ABProp CHANNELS_POLL_RECEIVE_ENABLED = new ABProp(6191, "false", "true");

    /**
     * The {@code event_name_length_limit} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: event_name_length_limit:[6207,"int",100,100]
     */
    public static final ABProp EVENT_NAME_LENGTH_LIMIT = new ABProp(6207, "100", "100");

    /**
     * The {@code event_description_length_limit} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: event_description_length_limit:[6208,"int",2048,2048]
     */
    public static final ABProp EVENT_DESCRIPTION_LENGTH_LIMIT = new ABProp(6208, "2048", "2048");

    /**
     * The {@code ctwa_entry_point_config_fetch_threshhold} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ctwa_entry_point_config_fetch_threshhold:[6214,"int",43200000,2000]
     */
    public static final ABProp CTWA_ENTRY_POINT_CONFIG_FETCH_THRESHHOLD = new ABProp(6214, "43200000", "2000");

    /**
     * The {@code kill_switch_ctwa_ml_entry_point_config} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: kill_switch_ctwa_ml_entry_point_config:[6215,"bool",!0,!1]
     */
    public static final ABProp KILL_SWITCH_CTWA_ML_ENTRY_POINT_CONFIG = new ABProp(6215, "true", "false");

    /**
     * The {@code enable_ctwa_ml_entry_point_config} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_ctwa_ml_entry_point_config:[6216,"bool",!1,!0]
     */
    public static final ABProp ENABLE_CTWA_ML_ENTRY_POINT_CONFIG = new ABProp(6216, "false", "true");

    /**
     * The {@code system_msg_text_styling} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: system_msg_text_styling:[6246,"bool",!1,!1]
     */
    public static final ABProp SYSTEM_MSG_TEXT_STYLING = new ABProp(6246, "false", "false");

    /**
     * The {@code bonsai_chat_list_entry_point_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: bonsai_chat_list_entry_point_enabled:[6251,"bool",!1,!1]
     */
    public static final ABProp BONSAI_CHAT_LIST_ENTRY_POINT_ENABLED = new ABProp(6251, "false", "false");

    /**
     * The {@code channels_ptt_logging_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_ptt_logging_enabled:[6274,"bool",!0,!0]
     */
    public static final ABProp CHANNELS_PTT_LOGGING_ENABLED = new ABProp(6274, "true", "true");

    /**
     * The {@code web_material_refresh} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_material_refresh:[6332,"bool",!1,!1]
     */
    public static final ABProp WEB_MATERIAL_REFRESH = new ABProp(6332, "false", "false");

    /**
     * The {@code blue_profile_locked_ui_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: blue_profile_locked_ui_enabled:[6337,"bool",!1,!0]
     */
    public static final ABProp BLUE_PROFILE_LOCKED_UI_ENABLED = new ABProp(6337, "false", "true");

    /**
     * The {@code channels_poll_voter_list_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_poll_voter_list_enabled:[6382,"bool",!1,!0]
     */
    public static final ABProp CHANNELS_POLL_VOTER_LIST_ENABLED = new ABProp(6382, "false", "true");

    /**
     * The {@code channels_status_updates_consumption_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_status_updates_consumption_enabled:[6444,"bool",!1,!0]
     */
    public static final ABProp CHANNELS_STATUS_UPDATES_CONSUMPTION_ENABLED = new ABProp(6444, "false", "true");

    /**
     * The {@code bonsai_carousel_reels_profile_photo_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: bonsai_carousel_reels_profile_photo_enabled:[6458,"bool",!1,!0]
     */
    public static final ABProp BONSAI_CAROUSEL_REELS_PROFILE_PHOTO_ENABLED = new ABProp(6458, "false", "true");

    /**
     * The {@code bonsai_carousel_hq_thumbnail_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: bonsai_carousel_hq_thumbnail_enabled:[6459,"bool",!1,!0]
     */
    public static final ABProp BONSAI_CAROUSEL_HQ_THUMBNAIL_ENABLED = new ABProp(6459, "false", "true");

    /**
     * The {@code channels_multi_admin_max_admin_count} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_multi_admin_max_admin_count:[6461,"int",16,16]
     */
    public static final ABProp CHANNELS_MULTI_ADMIN_MAX_ADMIN_COUNT = new ABProp(6461, "16", "16");

    /**
     * The {@code newsletter_admin_invite_tos_id} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: newsletter_admin_invite_tos_id:[6498,"string","20610101","20610101"]
     */
    public static final ABProp NEWSLETTER_ADMIN_INVITE_TOS_ID = new ABProp(6498, "20610101", "20610101");

    /**
     * The {@code channels_audio_files_sender_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_audio_files_sender_enabled:[6505,"bool",!1,!0]
     */
    public static final ABProp CHANNELS_AUDIO_FILES_SENDER_ENABLED = new ABProp(6505, "false", "true");

    /**
     * The {@code channels_audio_files_receiver_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_audio_files_receiver_enabled:[6506,"bool",!1,!0]
     */
    public static final ABProp CHANNELS_AUDIO_FILES_RECEIVER_ENABLED = new ABProp(6506, "false", "true");

    /**
     * The {@code newsletter_admin_invite_tos_id_smb_web} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: newsletter_admin_invite_tos_id_smb_web:[6536,"string","20610104","20610104"]
     */
    public static final ABProp NEWSLETTER_ADMIN_INVITE_TOS_ID_SMB_WEB = new ABProp(6536, "20610104", "20610104");

    /**
     * The {@code rt_sync_reporting_tag} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: rt_sync_reporting_tag:[6578,"bool",!0,!0]
     */
    public static final ABProp RT_SYNC_REPORTING_TAG = new ABProp(6578, "true", "true");

    /**
     * The {@code calling_audio_share_version} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: calling_audio_share_version:[6598,"int",0,0]
     */
    public static final ABProp CALLING_AUDIO_SHARE_VERSION = new ABProp(6598, "0", "0");

    /**
     * The {@code enable_syncd_debug_data_in_patch} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_syncd_debug_data_in_patch:[6614,"bool",!1,!1]
     */
    public static final ABProp ENABLE_SYNCD_DEBUG_DATA_IN_PATCH = new ABProp(6614, "false", "false");

    /**
     * The {@code web_jpeg_quality} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_jpeg_quality:[6619,"int",92,92]
     */
    public static final ABProp WEB_JPEG_QUALITY = new ABProp(6619, "92", "92");

    /**
     * The {@code web_pwa_background_sync} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_pwa_background_sync:[6656,"bool",!1,!1]
     */
    public static final ABProp WEB_PWA_BACKGROUND_SYNC = new ABProp(6656, "false", "false");

    /**
     * The {@code web_design_refresh} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_design_refresh:[6665,"bool",!1,!1]
     */
    public static final ABProp WEB_DESIGN_REFRESH = new ABProp(6665, "false", "false");

    /**
     * The {@code order_details_payment_instructions_sync_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: order_details_payment_instructions_sync_enabled:[6670,"bool",!1,!1]
     */
    public static final ABProp ORDER_DETAILS_PAYMENT_INSTRUCTIONS_SYNC_ENABLED = new ABProp(6670, "false", "false");

    /**
     * The {@code smba_premium_messages_leaving_wa_content} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smba_premium_messages_leaving_wa_content:[6693,"bool",!0,!0]
     */
    public static final ABProp SMBA_PREMIUM_MESSAGES_LEAVING_WA_CONTENT = new ABProp(6693, "true", "true");

    /**
     * The {@code web_pwa_background_sync_min_interval_hours} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_pwa_background_sync_min_interval_hours:[6706,"int",24,24]
     */
    public static final ABProp WEB_PWA_BACKGROUND_SYNC_MIN_INTERVAL_HOURS = new ABProp(6706, "24", "24");

    /**
     * The {@code rt_clean_reporting_tag} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: rt_clean_reporting_tag:[6723,"int",31,31]
     */
    public static final ABProp RT_CLEAN_REPORTING_TAG = new ABProp(6723, "31", "31");

    /**
     * The {@code gimmick_phase_two_data_suffix} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: gimmick_phase_two_data_suffix:[6785,"string","",""]
     */
    public static final ABProp GIMMICK_PHASE_TWO_DATA_SUFFIX = new ABProp(6785, "", "");

    /**
     * The {@code lid_status_send_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: lid_status_send_enabled:[6791,"bool",!1,!1]
     */
    public static final ABProp LID_STATUS_SEND_ENABLED = new ABProp(6791, "false", "false");

    /**
     * The {@code web_business_tools_drawer_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_business_tools_drawer_enabled:[6803,"bool",!1,!1]
     */
    public static final ABProp WEB_BUSINESS_TOOLS_DRAWER_ENABLED = new ABProp(6803, "false", "false");

    /**
     * The {@code is_pmx_funnel_metrics_logging_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: is_pmx_funnel_metrics_logging_enabled:[6816,"bool",!1,!0]
     */
    public static final ABProp IS_PMX_FUNNEL_METRICS_LOGGING_ENABLED = new ABProp(6816, "false", "true");

    /**
     * The {@code is_pmx_hashed_msg_key_logging_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: is_pmx_hashed_msg_key_logging_enabled:[6837,"bool",!1,!0]
     */
    public static final ABProp IS_PMX_HASHED_MSG_KEY_LOGGING_ENABLED = new ABProp(6837, "false", "true");

    /**
     * The {@code data_privacy_phase_2_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: data_privacy_phase_2_enabled:[6843,"bool",!1,!0]
     */
    public static final ABProp DATA_PRIVACY_PHASE_2_ENABLED = new ABProp(6843, "false", "true");

    /**
     * The {@code web_intern_dogfooding_upsell_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_intern_dogfooding_upsell_enabled:[6858,"bool",!1,!1]
     */
    public static final ABProp WEB_INTERN_DOGFOODING_UPSELL_ENABLED = new ABProp(6858, "false", "false");

    /**
     * The {@code web_intern_dogfooding_upsell_snooze_duration} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_intern_dogfooding_upsell_snooze_duration:[6859,"int",86400,86400]
     */
    public static final ABProp WEB_INTERN_DOGFOODING_UPSELL_SNOOZE_DURATION = new ABProp(6859, "86400", "86400");

    /**
     * The {@code web_intern_dogfooding_upsell_content} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_intern_dogfooding_upsell_content:[6860,"string","",""]
     */
    public static final ABProp WEB_INTERN_DOGFOODING_UPSELL_CONTENT = new ABProp(6860, "", "");

    /**
     * The {@code adv_accept_hosted_devices} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: adv_accept_hosted_devices:[6939,"bool",!1,!0]
     */
    public static final ABProp ADV_ACCEPT_HOSTED_DEVICES = new ABProp(6939, "false", "true");

    /**
     * The {@code channels_audio_files_sender_waveform_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_audio_files_sender_waveform_enabled:[6943,"bool",!1,!0]
     */
    public static final ABProp CHANNELS_AUDIO_FILES_SENDER_WAVEFORM_ENABLED = new ABProp(6943, "false", "true");

    /**
     * The {@code inbox_filters_read_unread_logging_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: inbox_filters_read_unread_logging_enabled:[6967,"bool",!1,!1]
     */
    public static final ABProp INBOX_FILTERS_READ_UNREAD_LOGGING_ENABLED = new ABProp(6967, "false", "false");

    /**
     * The {@code channels_audio_files_display_waveform_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_audio_files_display_waveform_enabled:[6996,"bool",!1,!0]
     */
    public static final ABProp CHANNELS_AUDIO_FILES_DISPLAY_WAVEFORM_ENABLED = new ABProp(6996, "false", "true");

    /**
     * The {@code payments_br_pix_phase_1_seller_sync_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: payments_br_pix_phase_1_seller_sync_enabled:[7024,"bool",!1,!1]
     */
    public static final ABProp PAYMENTS_BR_PIX_PHASE_1_SELLER_SYNC_ENABLED = new ABProp(7024, "false", "false");

    /**
     * The {@code saga_copy} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: saga_copy:[7044,"bool",!0,!0]
     */
    public static final ABProp SAGA_COPY = new ABProp(7044, "true", "true");

    /**
     * The {@code support_message_feedback_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: support_message_feedback_enabled:[7080,"bool",!1,!1]
     */
    public static final ABProp SUPPORT_MESSAGE_FEEDBACK_ENABLED = new ABProp(7080, "false", "false");

    /**
     * The {@code inbox_filters_smb_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: inbox_filters_smb_enabled:[7108,"bool",!1,!1]
     */
    public static final ABProp INBOX_FILTERS_SMB_ENABLED = new ABProp(7108, "false", "false");

    /**
     * The {@code data_privacy_phase_2_non_e2ee_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: data_privacy_phase_2_non_e2ee_enabled:[7131,"bool",!1,!0]
     */
    public static final ABProp DATA_PRIVACY_PHASE_2_NON_E2EE_ENABLED = new ABProp(7131, "false", "true");

    /**
     * The {@code dm_initiator_trigger_groups} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: dm_initiator_trigger_groups:[7141,"bool",!1,!0]
     */
    public static final ABProp DM_INITIATOR_TRIGGER_GROUPS = new ABProp(7141, "false", "true");

    /**
     * The {@code enable_product_carousel_message} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_product_carousel_message:[7177,"bool",!1,!1]
     */
    public static final ABProp ENABLE_PRODUCT_CAROUSEL_MESSAGE = new ABProp(7177, "false", "false");

    /**
     * The {@code channels_admin_reply_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_admin_reply_enabled:[7211,"bool",!1,!1]
     */
    public static final ABProp CHANNELS_ADMIN_REPLY_ENABLED = new ABProp(7211, "false", "false");

    /**
     * The {@code channels_quick_forwarding_button_mode} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_quick_forwarding_button_mode:[7234,"int",0,0]
     */
    public static final ABProp CHANNELS_QUICK_FORWARDING_BUTTON_MODE = new ABProp(7234, "0", "0");

    /**
     * The {@code channels_admin_reply_receiver_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_admin_reply_receiver_enabled:[7237,"bool",!1,!1]
     */
    public static final ABProp CHANNELS_ADMIN_REPLY_RECEIVER_ENABLED = new ABProp(7237, "false", "false");

    /**
     * The {@code favorites_limit} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: favorites_limit:[7267,"int",100,100]
     */
    public static final ABProp FAVORITES_LIMIT = new ABProp(7267, "100", "100");

    /**
     * The {@code web_disable_sw_on_safari_pwa} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_disable_sw_on_safari_pwa:[7281,"bool",!1,!1]
     */
    public static final ABProp WEB_DISABLE_SW_ON_SAFARI_PWA = new ABProp(7281, "false", "false");

    /**
     * The {@code visible_message_drop_placeholder_enabled_internal_only} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: visible_message_drop_placeholder_enabled_internal_only:[7287,"bool",!1,!0]
     */
    public static final ABProp VISIBLE_MESSAGE_DROP_PLACEHOLDER_ENABLED_INTERNAL_ONLY = new ABProp(7287, "false", "true");

    /**
     * The {@code native_contact_companion_change_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: native_contact_companion_change_enabled:[7301,"bool",!1,!0]
     */
    public static final ABProp NATIVE_CONTACT_COMPANION_CHANGE_ENABLED = new ABProp(7301, "false", "true");

    /**
     * The {@code smb_catalog_graphql_create_product_catalog} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_catalog_graphql_create_product_catalog:[7320,"bool",!1,!1]
     */
    public static final ABProp SMB_CATALOG_GRAPHQL_CREATE_PRODUCT_CATALOG = new ABProp(7320, "false", "false");

    /**
     * The {@code smb_catalog_graphql_update_collection_list} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_catalog_graphql_update_collection_list:[7328,"bool",!1,!1]
     */
    public static final ABProp SMB_CATALOG_GRAPHQL_UPDATE_COLLECTION_LIST = new ABProp(7328, "false", "false");

    /**
     * The {@code smb_catalog_graphql_delete_collection} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_catalog_graphql_delete_collection:[7329,"bool",!1,!1]
     */
    public static final ABProp SMB_CATALOG_GRAPHQL_DELETE_COLLECTION = new ABProp(7329, "false", "false");

    /**
     * The {@code web_recent_sync_chunk_download_optimization} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_recent_sync_chunk_download_optimization:[7356,"bool",!1,!1]
     */
    public static final ABProp WEB_RECENT_SYNC_CHUNK_DOWNLOAD_OPTIMIZATION = new ABProp(7356, "false", "false");

    /**
     * The {@code events_edit_receive} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: events_edit_receive:[7358,"bool",!1,!1]
     */
    public static final ABProp EVENTS_EDIT_RECEIVE = new ABProp(7358, "false", "false");

    /**
     * The {@code dm_initiator_trigger_daily_logs} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: dm_initiator_trigger_daily_logs:[7402,"bool",!1,!0]
     */
    public static final ABProp DM_INITIATOR_TRIGGER_DAILY_LOGS = new ABProp(7402, "false", "true");

    /**
     * The {@code web_autodownload_stickers} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_autodownload_stickers:[7422,"bool",!1,!0]
     */
    public static final ABProp WEB_AUTODOWNLOAD_STICKERS = new ABProp(7422, "false", "true");

    /**
     * The {@code custom_racing_emoji} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: custom_racing_emoji:[7463,"bool",!1,!1]
     */
    public static final ABProp CUSTOM_RACING_EMOJI = new ABProp(7463, "false", "false");

    /**
     * The {@code pinned_messages_m2_image_thumbnail} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: pinned_messages_m2_image_thumbnail:[7467,"bool",!1,!1]
     */
    public static final ABProp PINNED_MESSAGES_M2_IMAGE_THUMBNAIL = new ABProp(7467, "false", "false");

    /**
     * The {@code username_security_code_generation} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: username_security_code_generation:[7468,"bool",!1,!0]
     */
    public static final ABProp USERNAME_SECURITY_CODE_GENERATION = new ABProp(7468, "false", "true");

    /**
     * The {@code similar_channels_in_thread_on_follow_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: similar_channels_in_thread_on_follow_enabled:[7472,"bool",!1,!0]
     */
    public static final ABProp SIMILAR_CHANNELS_IN_THREAD_ON_FOLLOW_ENABLED = new ABProp(7472, "false", "true");

    /**
     * The {@code similar_channels_in_channel_details_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: similar_channels_in_channel_details_enabled:[7473,"bool",!1,!0]
     */
    public static final ABProp SIMILAR_CHANNELS_IN_CHANNEL_DETAILS_ENABLED = new ABProp(7473, "false", "true");

    /**
     * The {@code events_m3_cover_image_send} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: events_m3_cover_image_send:[7510,"bool",!1,!1]
     */
    public static final ABProp EVENTS_M3_COVER_IMAGE_SEND = new ABProp(7510, "false", "false");

    /**
     * The {@code events_m3_cover_image_receive} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: events_m3_cover_image_receive:[7511,"bool",!1,!1]
     */
    public static final ABProp EVENTS_M3_COVER_IMAGE_RECEIVE = new ABProp(7511, "false", "false");

    /**
     * The {@code similar_channels_max_limit} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: similar_channels_max_limit:[7559,"int",10,10]
     */
    public static final ABProp SIMILAR_CHANNELS_MAX_LIMIT = new ABProp(7559, "10", "10");

    /**
     * The {@code similar_channels_min_limit} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: similar_channels_min_limit:[7560,"int",4,4]
     */
    public static final ABProp SIMILAR_CHANNELS_MIN_LIMIT = new ABProp(7560, "4", "4");

    /**
     * The {@code addon_infra_enable_perf_logging} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: addon_infra_enable_perf_logging:[7567,"bool",!1,!0]
     */
    public static final ABProp ADDON_INFRA_ENABLE_PERF_LOGGING = new ABProp(7567, "false", "true");

    /**
     * The {@code dsa_information_for_eu_only_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: dsa_information_for_eu_only_enabled:[7592,"bool",!1,!0]
     */
    public static final ABProp DSA_INFORMATION_FOR_EU_ONLY_ENABLED = new ABProp(7592, "false", "true");

    /**
     * The {@code profile_picture_deeplink_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: profile_picture_deeplink_enabled:[7634,"bool",!1,!1]
     */
    public static final ABProp PROFILE_PICTURE_DEEPLINK_ENABLED = new ABProp(7634, "false", "false");

    /**
     * The {@code inbox_filters_custom_smb_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: inbox_filters_custom_smb_enabled:[7637,"bool",!1,!1]
     */
    public static final ABProp INBOX_FILTERS_CUSTOM_SMB_ENABLED = new ABProp(7637, "false", "false");

    /**
     * The {@code smb_graphql_to_fetch_qp_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_graphql_to_fetch_qp_enabled:[7645,"bool",!1,!1]
     */
    public static final ABProp SMB_GRAPHQL_TO_FETCH_QP_ENABLED = new ABProp(7645, "false", "false");

    /**
     * The {@code smb_graphql_to_fetch_qp_frequency_mins} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_graphql_to_fetch_qp_frequency_mins:[7646,"int",1320,5]
     */
    public static final ABProp SMB_GRAPHQL_TO_FETCH_QP_FREQUENCY_MINS = new ABProp(7646, "1320", "5");

    /**
     * The {@code smb_graphql_to_fetch_qp_surface_ids} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_graphql_to_fetch_qp_surface_ids:[7647,"string","",""]
     */
    public static final ABProp SMB_GRAPHQL_TO_FETCH_QP_SURFACE_IDS = new ABProp(7647, "", "");

    /**
     * The {@code channels_qpl_logging} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_qpl_logging:[7677,"bool",!1,!1]
     */
    public static final ABProp CHANNELS_QPL_LOGGING = new ABProp(7677, "false", "false");

    /**
     * The {@code channels_reactions_bottomsheet_tap_to_react_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_reactions_bottomsheet_tap_to_react_enabled:[7682,"bool",!1,!0]
     */
    public static final ABProp CHANNELS_REACTIONS_BOTTOMSHEET_TAP_TO_REACT_ENABLED = new ABProp(7682, "false", "true");

    /**
     * The {@code channels_directory_categories_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_directory_categories_enabled:[7685,"bool",!1,!1]
     */
    public static final ABProp CHANNELS_DIRECTORY_CATEGORIES_ENABLED = new ABProp(7685, "false", "false");

    /**
     * The {@code smb_notes_v1_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_notes_v1_enabled:[7710,"bool",!1,!1]
     */
    public static final ABProp SMB_NOTES_V1_ENABLED = new ABProp(7710, "false", "false");

    /**
     * The {@code channels_directory_category_types} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_directory_category_types:[7734,"string","3,7,6,4,1,5,2","3,7,6,4,1,5,2"]
     */
    public static final ABProp CHANNELS_DIRECTORY_CATEGORY_TYPES = new ABProp(7734, "3,7,6,4,1,5,2", "3,7,6,4,1,5,2");

    /**
     * The {@code inbox_filters_suppress_contact_filter} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: inbox_filters_suppress_contact_filter:[7769,"bool",!1,!1]
     */
    public static final ABProp INBOX_FILTERS_SUPPRESS_CONTACT_FILTER = new ABProp(7769, "false", "false");

    /**
     * The {@code single_e2ee_session_migration_state_outgoing} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: single_e2ee_session_migration_state_outgoing:[7820,"int",2,2]
     */
    public static final ABProp SINGLE_E2EE_SESSION_MIGRATION_STATE_OUTGOING = new ABProp(7820, "2", "2");

    /**
     * The {@code single_e2ee_session_migration_state_incoming} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: single_e2ee_session_migration_state_incoming:[7821,"int",2,2]
     */
    public static final ABProp SINGLE_E2EE_SESSION_MIGRATION_STATE_INCOMING = new ABProp(7821, "2", "2");

    /**
     * The {@code bonsai_supported_languages} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: bonsai_supported_languages:[7848,"string","en","en"]
     */
    public static final ABProp BONSAI_SUPPORTED_LANGUAGES = new ABProp(7848, "en", "en");

    /**
     * The {@code web_comms_socket_reconnect_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_comms_socket_reconnect_enabled:[7854,"bool",!1,!1]
     */
    public static final ABProp WEB_COMMS_SOCKET_RECONNECT_ENABLED = new ABProp(7854, "false", "false");

    /**
     * The {@code payments_br_pix_quick_reply_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: payments_br_pix_quick_reply_enabled:[7857,"bool",!1,!1]
     */
    public static final ABProp PAYMENTS_BR_PIX_QUICK_REPLY_ENABLED = new ABProp(7857, "false", "false");

    /**
     * The {@code status_mentions_receiver} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: status_mentions_receiver:[7869,"bool",!1,!1]
     */
    public static final ABProp STATUS_MENTIONS_RECEIVER = new ABProp(7869, "false", "false");

    /**
     * The {@code enable_sticker_verification_for_gimmick} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_sticker_verification_for_gimmick:[7886,"bool",!0,!0]
     */
    public static final ABProp ENABLE_STICKER_VERIFICATION_FOR_GIMMICK = new ABProp(7886, "true", "true");

    /**
     * The {@code channels_poll_voters_summary_cache_ttl_ms} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_poll_voters_summary_cache_ttl_ms:[7919,"int",120000,120000]
     */
    public static final ABProp CHANNELS_POLL_VOTERS_SUMMARY_CACHE_TTL_MS = new ABProp(7919, "120000", "120000");

    /**
     * The {@code channels_poll_voters_details_cache_ttl_ms} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_poll_voters_details_cache_ttl_ms:[7920,"int",300000,300000]
     */
    public static final ABProp CHANNELS_POLL_VOTERS_DETAILS_CACHE_TTL_MS = new ABProp(7920, "300000", "300000");

    /**
     * The {@code meta_verified_badge_education_vai_content} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: meta_verified_badge_education_vai_content:[7976,"bool",!1,!0]
     */
    public static final ABProp META_VERIFIED_BADGE_EDUCATION_VAI_CONTENT = new ABProp(7976, "false", "true");

    /**
     * The {@code directory_categories_newsletters_per_category_limit} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: directory_categories_newsletters_per_category_limit:[7986,"int",10,10]
     */
    public static final ABProp DIRECTORY_CATEGORIES_NEWSLETTERS_PER_CATEGORY_LIMIT = new ABProp(7986, "10", "10");

    /**
     * The {@code bonsai_meta_ai_shortcut_tos_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: bonsai_meta_ai_shortcut_tos_enabled:[8004,"bool",!1,!0]
     */
    public static final ABProp BONSAI_META_AI_SHORTCUT_TOS_ENABLED = new ABProp(8004, "false", "true");

    /**
     * The {@code ctwa_long_term_holdout_content_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ctwa_long_term_holdout_content_enabled:[8015,"bool",!1,!0]
     */
    public static final ABProp CTWA_LONG_TERM_HOLDOUT_CONTENT_ENABLED = new ABProp(8015, "false", "true");

    /**
     * The {@code web_socket_parallel_connection_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_socket_parallel_connection_enabled:[8019,"bool",!1,!1]
     */
    public static final ABProp WEB_SOCKET_PARALLEL_CONNECTION_ENABLED = new ABProp(8019, "false", "false");

    /**
     * The {@code ai_search_null_state_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_search_null_state_enabled:[8026,"bool",!1,!1]
     */
    public static final ABProp AI_SEARCH_NULL_STATE_ENABLED = new ABProp(8026, "false", "false");

    /**
     * The {@code channels_verified_badge_in_compact_inbox_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_verified_badge_in_compact_inbox_enabled:[8059,"bool",!1,!1]
     */
    public static final ABProp CHANNELS_VERIFIED_BADGE_IN_COMPACT_INBOX_ENABLED = new ABProp(8059, "false", "false");

    /**
     * The {@code ai_search_max_num_suggestions} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_search_max_num_suggestions:[8076,"int",5,5]
     */
    public static final ABProp AI_SEARCH_MAX_NUM_SUGGESTIONS = new ABProp(8076, "5", "5");

    /**
     * The {@code xplat_attachment_format_check_v2} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: xplat_attachment_format_check_v2:[8082,"bool",!1,!1]
     */
    public static final ABProp XPLAT_ATTACHMENT_FORMAT_CHECK_V2 = new ABProp(8082, "false", "false");

    /**
     * The {@code ai_search_null_state_update_interval} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_search_null_state_update_interval:[8100,"int",86400,86400]
     */
    public static final ABProp AI_SEARCH_NULL_STATE_UPDATE_INTERVAL = new ABProp(8100, "86400", "86400");

    /**
     * The {@code web_sticky_hd_photo_setting_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_sticky_hd_photo_setting_enabled:[8115,"bool",!1,!1]
     */
    public static final ABProp WEB_STICKY_HD_PHOTO_SETTING_ENABLED = new ABProp(8115, "false", "false");

    /**
     * The {@code app_exit_reason_version} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: app_exit_reason_version:[8147,"int",0,0]
     */
    public static final ABProp APP_EXIT_REASON_VERSION = new ABProp(8147, "0", "0");

    /**
     * The {@code channels_directory_categories_cache_refresh_interval_ms} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_directory_categories_cache_refresh_interval_ms:[8151,"int",86400000,600000]
     */
    public static final ABProp CHANNELS_DIRECTORY_CATEGORIES_CACHE_REFRESH_INTERVAL_MS = new ABProp(8151, "86400000", "600000");

    /**
     * The {@code br_enable_payment_logos_on_bubble} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: br_enable_payment_logos_on_bubble:[8160,"bool",!1,!1]
     */
    public static final ABProp BR_ENABLE_PAYMENT_LOGOS_ON_BUBBLE = new ABProp(8160, "false", "false");

    /**
     * The {@code ctwa_ad_account_token_storage_kill_switch_web} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ctwa_ad_account_token_storage_kill_switch_web:[8166,"bool",!0,!1]
     */
    public static final ABProp CTWA_AD_ACCOUNT_TOKEN_STORAGE_KILL_SWITCH_WEB = new ABProp(8166, "true", "false");

    /**
     * The {@code channels_recommended_v3_ui_limit} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_recommended_v3_ui_limit:[8167,"int",5,5]
     */
    public static final ABProp CHANNELS_RECOMMENDED_V3_UI_LIMIT = new ABProp(8167, "5", "5");

    /**
     * The {@code search_the_web_dialog_redesign} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: search_the_web_dialog_redesign:[8171,"bool",!1,!0]
     */
    public static final ABProp SEARCH_THE_WEB_DIALOG_REDESIGN = new ABProp(8171, "false", "true");

    /**
     * The {@code web_larger_link_previews} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_larger_link_previews:[8172,"bool",!1,!1]
     */
    public static final ABProp WEB_LARGER_LINK_PREVIEWS = new ABProp(8172, "false", "false");

    /**
     * The {@code channel_to_channel_forwarding_logging_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channel_to_channel_forwarding_logging_enabled:[8227,"bool",!1,!1]
     */
    public static final ABProp CHANNEL_TO_CHANNEL_FORWARDING_LOGGING_ENABLED = new ABProp(8227, "false", "false");

    /**
     * The {@code smb_meta_verified_context_card} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_meta_verified_context_card:[8313,"bool",!1,!0]
     */
    public static final ABProp SMB_META_VERIFIED_CONTEXT_CARD = new ABProp(8313, "false", "true");

    /**
     * The {@code report_block_improvements_for_groups_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: report_block_improvements_for_groups_enabled:[8327,"bool",!1,!0]
     */
    public static final ABProp REPORT_BLOCK_IMPROVEMENTS_FOR_GROUPS_ENABLED = new ABProp(8327, "false", "true");

    /**
     * The {@code web_pending_message_cache_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_pending_message_cache_enabled:[8353,"bool",!1,!0]
     */
    public static final ABProp WEB_PENDING_MESSAGE_CACHE_ENABLED = new ABProp(8353, "false", "true");

    /**
     * The {@code unified_pin_addon_table_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: unified_pin_addon_table_enabled:[8356,"bool",!1,!1]
     */
    public static final ABProp UNIFIED_PIN_ADDON_TABLE_ENABLED = new ABProp(8356, "false", "false");

    /**
     * The {@code web_offline_message_processor_timeout_seconds} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_offline_message_processor_timeout_seconds:[8406,"int",0,0]
     */
    public static final ABProp WEB_OFFLINE_MESSAGE_PROCESSOR_TIMEOUT_SECONDS = new ABProp(8406, "0", "0");

    /**
     * The {@code ai_search_null_state_row_count} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_search_null_state_row_count:[8407,"int",3,3]
     */
    public static final ABProp AI_SEARCH_NULL_STATE_ROW_COUNT = new ABProp(8407, "3", "3");

    /**
     * The {@code mex_usync_username_query} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: mex_usync_username_query:[8421,"bool",!1,!0]
     */
    public static final ABProp MEX_USYNC_USERNAME_QUERY = new ABProp(8421, "false", "true");

    /**
     * The {@code webc_page_load_early_commit_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: webc_page_load_early_commit_enabled:[8458,"bool",!1,!1]
     */
    public static final ABProp WEBC_PAGE_LOAD_EARLY_COMMIT_ENABLED = new ABProp(8458, "false", "false");

    /**
     * The {@code search_the_web_url_offer} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: search_the_web_url_offer:[8473,"bool",!1,!0]
     */
    public static final ABProp SEARCH_THE_WEB_URL_OFFER = new ABProp(8473, "false", "true");

    /**
     * The {@code biz_ai_smb_agents_automatic_reply_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: biz_ai_smb_agents_automatic_reply_enabled:[8505,"bool",!1,!1]
     */
    public static final ABProp BIZ_AI_SMB_AGENTS_AUTOMATIC_REPLY_ENABLED = new ABProp(8505, "false", "false");

    /**
     * The {@code album_v2_receiving_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: album_v2_receiving_enabled:[8528,"bool",!1,!0]
     */
    public static final ABProp ALBUM_V2_RECEIVING_ENABLED = new ABProp(8528, "false", "true");

    /**
     * The {@code album_v2_sender_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: album_v2_sender_enabled:[8529,"bool",!1,!1]
     */
    public static final ABProp ALBUM_V2_SENDER_ENABLED = new ABProp(8529, "false", "false");

    /**
     * The {@code improve_subgroup_activation_subgroup_poll_interval} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: improve_subgroup_activation_subgroup_poll_interval:[8542,"int",43200,43200]
     */
    public static final ABProp IMPROVE_SUBGROUP_ACTIVATION_SUBGROUP_POLL_INTERVAL = new ABProp(8542, "43200", "43200");

    /**
     * The {@code web_communities_general_chat_v_2} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_communities_general_chat_v_2:[8580,"bool",!1,!0]
     */
    public static final ABProp WEB_COMMUNITIES_GENERAL_CHAT_V_2 = new ABProp(8580, "false", "true");

    /**
     * The {@code text_user_journey_logging_wam_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: text_user_journey_logging_wam_enabled:[8627,"bool",!1,!0]
     */
    public static final ABProp TEXT_USER_JOURNEY_LOGGING_WAM_ENABLED = new ABProp(8627, "false", "true");

    /**
     * The {@code ptt_user_journey_logging_wam_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ptt_user_journey_logging_wam_enabled:[8630,"bool",!1,!0]
     */
    public static final ABProp PTT_USER_JOURNEY_LOGGING_WAM_ENABLED = new ABProp(8630, "false", "true");

    /**
     * The {@code channels_updates_tab_swipe_actions_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_updates_tab_swipe_actions_enabled:[8653,"bool",!1,!0]
     */
    public static final ABProp CHANNELS_UPDATES_TAB_SWIPE_ACTIONS_ENABLED = new ABProp(8653, "false", "true");

    /**
     * The {@code ctwa_ad_account_nonce_retries_max_web} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ctwa_ad_account_nonce_retries_max_web:[8663,"int",0,0]
     */
    public static final ABProp CTWA_AD_ACCOUNT_NONCE_RETRIES_MAX_WEB = new ABProp(8663, "0", "0");

    /**
     * The {@code ctwa_ad_account_nonce_push_wait_timeout_web} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ctwa_ad_account_nonce_push_wait_timeout_web:[8664,"int",20,20]
     */
    public static final ABProp CTWA_AD_ACCOUNT_NONCE_PUSH_WAIT_TIMEOUT_WEB = new ABProp(8664, "20", "20");

    /**
     * The {@code ai_metabot_send_image_limit} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_metabot_send_image_limit:[8685,"int",1,1]
     */
    public static final ABProp AI_METABOT_SEND_IMAGE_LIMIT = new ABProp(8685, "1", "1");

    /**
     * The {@code status_poster_side_gating_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: status_poster_side_gating_enabled:[8742,"bool",!1,!0]
     */
    public static final ABProp STATUS_POSTER_SIDE_GATING_ENABLED = new ABProp(8742, "false", "true");

    /**
     * The {@code username_mex_account_sync_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: username_mex_account_sync_enabled:[8763,"bool",!1,!1]
     */
    public static final ABProp USERNAME_MEX_ACCOUNT_SYNC_ENABLED = new ABProp(8763, "false", "false");

    /**
     * The {@code web_background_sync_v2} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_background_sync_v2:[8782,"bool",!1,!1]
     */
    public static final ABProp WEB_BACKGROUND_SYNC_V2 = new ABProp(8782, "false", "false");

    /**
     * The {@code message_association_infra_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: message_association_infra_enabled:[8783,"bool",!0,!0]
     */
    public static final ABProp MESSAGE_ASSOCIATION_INFRA_ENABLED = new ABProp(8783, "true", "true");

    /**
     * The {@code lid_migration_notifications_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: lid_migration_notifications_enabled:[8785,"bool",!1,!1]
     */
    public static final ABProp LID_MIGRATION_NOTIFICATIONS_ENABLED = new ABProp(8785, "false", "false");

    /**
     * The {@code graphql_get_product_list} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: graphql_get_product_list:[8800,"bool",!1,!1]
     */
    public static final ABProp GRAPHQL_GET_PRODUCT_LIST = new ABProp(8800, "false", "false");

    /**
     * The {@code rt_sender_reporting_token_version} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: rt_sender_reporting_token_version:[8860,"int",2,2]
     */
    public static final ABProp RT_SENDER_REPORTING_TOKEN_VERSION = new ABProp(8860, "2", "2");

    /**
     * The {@code enable_minimize_individual_mutation_write} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_minimize_individual_mutation_write:[8910,"bool",!1,!0]
     */
    public static final ABProp ENABLE_MINIMIZE_INDIVIDUAL_MUTATION_WRITE = new ABProp(8910, "false", "true");

    /**
     * The {@code payments_br_force_copy_pix_cta_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: payments_br_force_copy_pix_cta_enabled:[8953,"bool",!1,!1]
     */
    public static final ABProp PAYMENTS_BR_FORCE_COPY_PIX_CTA_ENABLED = new ABProp(8953, "false", "false");

    /**
     * The {@code channels_producer_insights_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_producer_insights_enabled:[8960,"bool",!1,!0]
     */
    public static final ABProp CHANNELS_PRODUCER_INSIGHTS_ENABLED = new ABProp(8960, "false", "true");

    /**
     * The {@code smb_payment_links_url_regex_list} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_payment_links_url_regex_list:[8969,"string","{}","{}"]
     */
    public static final ABProp SMB_PAYMENT_LINKS_URL_REGEX_LIST = new ABProp(8969, "{}", "{}");

    /**
     * The {@code payments_br_copy_pix_code_api_merchant_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: payments_br_copy_pix_code_api_merchant_enabled:[9017,"bool",!1,!1]
     */
    public static final ABProp PAYMENTS_BR_COPY_PIX_CODE_API_MERCHANT_ENABLED = new ABProp(9017, "false", "false");

    /**
     * The {@code smb_orders_graphql_get_order_info} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_orders_graphql_get_order_info:[9030,"bool",!1,!1]
     */
    public static final ABProp SMB_ORDERS_GRAPHQL_GET_ORDER_INFO = new ABProp(9030, "false", "false");

    /**
     * The {@code smb_orders_graphql_refresh_cart} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_orders_graphql_refresh_cart:[9032,"bool",!1,!1]
     */
    public static final ABProp SMB_ORDERS_GRAPHQL_REFRESH_CART = new ABProp(9032, "false", "false");

    /**
     * The {@code enable_chat_list_sticker_emojis} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_chat_list_sticker_emojis:[9069,"bool",!1,!1]
     */
    public static final ABProp ENABLE_CHAT_LIST_STICKER_EMOJIS = new ABProp(9069, "false", "false");

    /**
     * The {@code payments_br_merchant_psp_account_status_sync} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: payments_br_merchant_psp_account_status_sync:[9076,"bool",!1,!1]
     */
    public static final ABProp PAYMENTS_BR_MERCHANT_PSP_ACCOUNT_STATUS_SYNC = new ABProp(9076, "false", "false");

    /**
     * The {@code lazy_system_message_insertion_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: lazy_system_message_insertion_enabled:[9077,"bool",!1,!0]
     */
    public static final ABProp LAZY_SYSTEM_MESSAGE_INSERTION_ENABLED = new ABProp(9077, "false", "true");

    /**
     * The {@code flows_termination_message_v2_sending_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: flows_termination_message_v2_sending_enabled:[9157,"bool",!1,!1]
     */
    public static final ABProp FLOWS_TERMINATION_MESSAGE_V2_SENDING_ENABLED = new ABProp(9157, "false", "false");

    /**
     * The {@code ai_metabot_image_input_languages} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_metabot_image_input_languages:[9163,"string"," ","en"]
     */
    public static final ABProp AI_METABOT_IMAGE_INPUT_LANGUAGES = new ABProp(9163, " ", "en");

    /**
     * The {@code send_invalid_protobuf_nack_failure_reason} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: send_invalid_protobuf_nack_failure_reason:[9174,"bool",!1,!1]
     */
    public static final ABProp SEND_INVALID_PROTOBUF_NACK_FAILURE_REASON = new ABProp(9174, "false", "false");

    /**
     * The {@code smb_graphql_token_recovery_during_account_recovery_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_graphql_token_recovery_during_account_recovery_enabled:[9197,"bool",!1,!1]
     */
    public static final ABProp SMB_GRAPHQL_TOKEN_RECOVERY_DURING_ACCOUNT_RECOVERY_ENABLED = new ABProp(9197, "false", "false");

    /**
     * The {@code hash_identity_keys_for_qr_code_device_verification} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: hash_identity_keys_for_qr_code_device_verification:[9211,"bool",!1,!0]
     */
    public static final ABProp HASH_IDENTITY_KEYS_FOR_QR_CODE_DEVICE_VERIFICATION = new ABProp(9211, "false", "true");

    /**
     * The {@code smb_payment_links_logging_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_payment_links_logging_enabled:[9213,"bool",!1,!0]
     */
    public static final ABProp SMB_PAYMENT_LINKS_LOGGING_ENABLED = new ABProp(9213, "false", "true");

    /**
     * The {@code verified_badge_in_chats_list_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: verified_badge_in_chats_list_enabled:[9292,"bool",!1,!1]
     */
    public static final ABProp VERIFIED_BADGE_IN_CHATS_LIST_ENABLED = new ABProp(9292, "false", "false");

    /**
     * The {@code directory_categories_display_newsletters_per_category_limit} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: directory_categories_display_newsletters_per_category_limit:[9312,"int",4,4]
     */
    public static final ABProp DIRECTORY_CATEGORIES_DISPLAY_NEWSLETTERS_PER_CATEGORY_LIMIT = new ABProp(9312, "4", "4");

    /**
     * The {@code optimized_delivery_signal_collection_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: optimized_delivery_signal_collection_enabled:[9348,"bool",!1,!0]
     */
    public static final ABProp OPTIMIZED_DELIVERY_SIGNAL_COLLECTION_ENABLED = new ABProp(9348, "false", "true");

    /**
     * The {@code channel_forward_bottom_button_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channel_forward_bottom_button_enabled:[9422,"bool",!1,!0]
     */
    public static final ABProp CHANNEL_FORWARD_BOTTOM_BUTTON_ENABLED = new ABProp(9422, "false", "true");

    /**
     * The {@code lid_one_on_one_migration_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: lid_one_on_one_migration_enabled:[9435,"bool",!1,!1]
     */
    public static final ABProp LID_ONE_ON_ONE_MIGRATION_ENABLED = new ABProp(9435, "false", "false");

    /**
     * The {@code channels_producer_insights_min_followers} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_producer_insights_min_followers:[9447,"int",100,100]
     */
    public static final ABProp CHANNELS_PRODUCER_INSIGHTS_MIN_FOLLOWERS = new ABProp(9447, "100", "100");

    /**
     * The {@code status_ranking_poster_side_gating_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: status_ranking_poster_side_gating_enabled:[9453,"bool",!1,!1]
     */
    public static final ABProp STATUS_RANKING_POSTER_SIDE_GATING_ENABLED = new ABProp(9453, "false", "false");

    /**
     * The {@code ai_pdfn_tos_shortcut_notice_id} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_pdfn_tos_shortcut_notice_id:[9482,"string"," "," "]
     */
    public static final ABProp AI_PDFN_TOS_SHORTCUT_NOTICE_ID = new ABProp(9482, " ", " ");

    /**
     * The {@code ai_pdfn_tos_invoke_notice_id} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_pdfn_tos_invoke_notice_id:[9483,"string"," "," "]
     */
    public static final ABProp AI_PDFN_TOS_INVOKE_NOTICE_ID = new ABProp(9483, " ", " ");

    /**
     * The {@code web_wam_max_buffer_upload_size_bytes} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_wam_max_buffer_upload_size_bytes:[9501,"int",64000,64000]
     */
    public static final ABProp WEB_WAM_MAX_BUFFER_UPLOAD_SIZE_BYTES = new ABProp(9501, "64000", "64000");

    /**
     * The {@code status_future_proofing} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: status_future_proofing:[9522,"bool",!1,!1]
     */
    public static final ABProp STATUS_FUTURE_PROOFING = new ABProp(9522, "false", "false");

    /**
     * The {@code mex_usync_about_status} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: mex_usync_about_status:[9524,"bool",!1,!0]
     */
    public static final ABProp MEX_USYNC_ABOUT_STATUS = new ABProp(9524, "false", "true");

    /**
     * The {@code bonsai_fp_ugc_sender} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: bonsai_fp_ugc_sender:[9541,"bool",!1,!1]
     */
    public static final ABProp BONSAI_FP_UGC_SENDER = new ABProp(9541, "false", "false");

    /**
     * The {@code search_the_web_image_search} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: search_the_web_image_search:[9547,"bool",!1,!0]
     */
    public static final ABProp SEARCH_THE_WEB_IMAGE_SEARCH = new ABProp(9547, "false", "true");

    /**
     * The {@code search_the_web_text_search} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: search_the_web_text_search:[9548,"bool",!1,!0]
     */
    public static final ABProp SEARCH_THE_WEB_TEXT_SEARCH = new ABProp(9548, "false", "true");

    /**
     * The {@code rt_clean_reporting_token} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: rt_clean_reporting_token:[9567,"int",31,31]
     */
    public static final ABProp RT_CLEAN_REPORTING_TOKEN = new ABProp(9567, "31", "31");

    /**
     * The {@code ctwa_ad_creation_entry_point_catalog_web} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ctwa_ad_creation_entry_point_catalog_web:[9596,"bool",!1,!0]
     */
    public static final ABProp CTWA_AD_CREATION_ENTRY_POINT_CATALOG_WEB = new ABProp(9596, "false", "true");

    /**
     * The {@code ai_experiment_graphql_config} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_experiment_graphql_config:[9601,"string"," "," "]
     */
    public static final ABProp AI_EXPERIMENT_GRAPHQL_CONFIG = new ABProp(9601, " ", " ");

    /**
     * The {@code channels_admin_insights_gizmos_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_admin_insights_gizmos_enabled:[9641,"bool",!1,!1]
     */
    public static final ABProp CHANNELS_ADMIN_INSIGHTS_GIZMOS_ENABLED = new ABProp(9641, "false", "false");

    /**
     * The {@code profile_scraping_privacy_token_in_about_iq} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: profile_scraping_privacy_token_in_about_iq:[9668,"bool",!1,!0]
     */
    public static final ABProp PROFILE_SCRAPING_PRIVACY_TOKEN_IN_ABOUT_IQ = new ABProp(9668, "false", "true");

    /**
     * The {@code single_emoji_logging_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: single_emoji_logging_enabled:[9669,"bool",!1,!1]
     */
    public static final ABProp SINGLE_EMOJI_LOGGING_ENABLED = new ABProp(9669, "false", "false");

    /**
     * The {@code enable_busy_reason_fs} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_busy_reason_fs:[9674,"bool",!1,!1]
     */
    public static final ABProp ENABLE_BUSY_REASON_FS = new ABProp(9674, "false", "false");

    /**
     * The {@code ctwa_ad_creation_entry_point_catalog_product_web} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ctwa_ad_creation_entry_point_catalog_product_web:[9677,"bool",!1,!0]
     */
    public static final ABProp CTWA_AD_CREATION_ENTRY_POINT_CATALOG_PRODUCT_WEB = new ABProp(9677, "false", "true");

    /**
     * The {@code ai_ptt_main_gate_supported_languages} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_ptt_main_gate_supported_languages:[9694,"string"," ","en"]
     */
    public static final ABProp AI_PTT_MAIN_GATE_SUPPORTED_LANGUAGES = new ABProp(9694, " ", "en");

    /**
     * The {@code wa_ctwa_web_hide_ad_context_if_soft_dismissed_in_primary} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_ctwa_web_hide_ad_context_if_soft_dismissed_in_primary:[9729,"bool",!1,!1]
     */
    public static final ABProp WA_CTWA_WEB_HIDE_AD_CONTEXT_IF_SOFT_DISMISSED_IN_PRIMARY = new ABProp(9729, "false", "false");

    /**
     * The {@code use_per_chat_wallpaper} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: use_per_chat_wallpaper:[9756,"bool",!1,!1]
     */
    public static final ABProp USE_PER_CHAT_WALLPAPER = new ABProp(9756, "false", "false");

    /**
     * The {@code animated_emoji_final_set_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: animated_emoji_final_set_enabled:[9757,"bool",!1,!1]
     */
    public static final ABProp ANIMATED_EMOJI_FINAL_SET_ENABLED = new ABProp(9757, "false", "false");

    /**
     * The {@code animated_emoji_set_1_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: animated_emoji_set_1_enabled:[9758,"bool",!1,!1]
     */
    public static final ABProp ANIMATED_EMOJI_SET_1_ENABLED = new ABProp(9758, "false", "false");

    /**
     * The {@code channels_producer_insights_hide_deltas} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_producer_insights_hide_deltas:[9792,"bool",!0,!0]
     */
    public static final ABProp CHANNELS_PRODUCER_INSIGHTS_HIDE_DELTAS = new ABProp(9792, "true", "true");

    /**
     * The {@code rt_report_token_from_inclusion_list} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: rt_report_token_from_inclusion_list:[9818,"bool",!1,!1]
     */
    public static final ABProp RT_REPORT_TOKEN_FROM_INCLUSION_LIST = new ABProp(9818, "false", "false");

    /**
     * The {@code whatsapp_vpv_logging_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: whatsapp_vpv_logging_enabled:[9833,"bool",!0,!0]
     */
    public static final ABProp WHATSAPP_VPV_LOGGING_ENABLED = new ABProp(9833, "true", "true");

    /**
     * The {@code channels_vpv_logging_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_vpv_logging_enabled:[9834,"bool",!1,!0]
     */
    public static final ABProp CHANNELS_VPV_LOGGING_ENABLED = new ABProp(9834, "false", "true");

    /**
     * The {@code brigading_privacy_setting_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: brigading_privacy_setting_enabled:[9876,"bool",!1,!0]
     */
    public static final ABProp BRIGADING_PRIVACY_SETTING_ENABLED = new ABProp(9876, "false", "true");

    /**
     * The {@code saga_v1_reengagement_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: saga_v1_reengagement_enabled:[9924,"bool",!0,!0]
     */
    public static final ABProp SAGA_V1_REENGAGEMENT_ENABLED = new ABProp(9924, "true", "true");

    /**
     * The {@code events_create_cag_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: events_create_cag_enabled:[9932,"bool",!1,!0]
     */
    public static final ABProp EVENTS_CREATE_CAG_ENABLED = new ABProp(9932, "false", "true");

    /**
     * The {@code saga_v1_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: saga_v1_enabled:[9942,"bool",!0,!0]
     */
    public static final ABProp SAGA_V1_ENABLED = new ABProp(9942, "true", "true");

    /**
     * The {@code saga_v1_nux_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: saga_v1_nux_enabled:[9944,"bool",!0,!0]
     */
    public static final ABProp SAGA_V1_NUX_ENABLED = new ABProp(9944, "true", "true");

    /**
     * The {@code mm_message_level_feedback_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: mm_message_level_feedback_enabled:[10011,"bool",!1,!1]
     */
    public static final ABProp MM_MESSAGE_LEVEL_FEEDBACK_ENABLED = new ABProp(10011, "false", "false");

    /**
     * The {@code wa_web_calling_deep_link_error} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_calling_deep_link_error:[10051,"bool",!0,!0]
     */
    public static final ABProp WA_WEB_CALLING_DEEP_LINK_ERROR = new ABProp(10051, "true", "true");

    /**
     * The {@code enable_ring_for_gc_on_offer_expire} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_ring_for_gc_on_offer_expire:[10103,"bool",!1,!1]
     */
    public static final ABProp ENABLE_RING_FOR_GC_ON_OFFER_EXPIRE = new ABProp(10103, "false", "false");

    /**
     * The {@code channels_directory_categories_logging_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_directory_categories_logging_enabled:[10188,"bool",!1,!0]
     */
    public static final ABProp CHANNELS_DIRECTORY_CATEGORIES_LOGGING_ENABLED = new ABProp(10188, "false", "true");

    /**
     * The {@code enable_wefr_client_expo_pulse} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_wefr_client_expo_pulse:[10230,"bool",!1,!0]
     */
    public static final ABProp ENABLE_WEFR_CLIENT_EXPO_PULSE = new ABProp(10230, "false", "true");

    /**
     * The {@code smb_notes_content_max_limit} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_notes_content_max_limit:[10272,"int",5000,5000]
     */
    public static final ABProp SMB_NOTES_CONTENT_MAX_LIMIT = new ABProp(10272, "5000", "5000");

    /**
     * The {@code ignore_one_to_one_terminate_in_group_call} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ignore_one_to_one_terminate_in_group_call:[10273,"bool",!1,!1]
     */
    public static final ABProp IGNORE_ONE_TO_ONE_TERMINATE_IN_GROUP_CALL = new ABProp(10273, "false", "false");

    /**
     * The {@code optimized_delivery_signal_collection_config} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: optimized_delivery_signal_collection_config:[10302,"string","{}","{}"]
     */
    public static final ABProp OPTIMIZED_DELIVERY_SIGNAL_COLLECTION_CONFIG = new ABProp(10302, "{}", "{}");

    /**
     * The {@code optimized_delivery_tokens_storage_config} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: optimized_delivery_tokens_storage_config:[10303,"string","{}","{}"]
     */
    public static final ABProp OPTIMIZED_DELIVERY_TOKENS_STORAGE_CONFIG = new ABProp(10303, "{}", "{}");

    /**
     * The {@code channels_fetch_and_log_capabilities} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_fetch_and_log_capabilities:[10325,"bool",!0,!0]
     */
    public static final ABProp CHANNELS_FETCH_AND_LOG_CAPABILITIES = new ABProp(10325, "true", "true");

    /**
     * The {@code channels_capabilities_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_capabilities_enabled:[10328,"bool",!0,!0]
     */
    public static final ABProp CHANNELS_CAPABILITIES_ENABLED = new ABProp(10328, "true", "true");

    /**
     * The {@code ctwa_mm_biz_ai_disclosure_update_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ctwa_mm_biz_ai_disclosure_update_enabled:[10379,"bool",!1,!0]
     */
    public static final ABProp CTWA_MM_BIZ_AI_DISCLOSURE_UPDATE_ENABLED = new ABProp(10379, "false", "true");

    /**
     * The {@code smb_payment_links_seller_logging_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_payment_links_seller_logging_enabled:[10389,"bool",!1,!1]
     */
    public static final ABProp SMB_PAYMENT_LINKS_SELLER_LOGGING_ENABLED = new ABProp(10389, "false", "false");

    /**
     * The {@code channel_poll_forwarding_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channel_poll_forwarding_enabled:[10412,"bool",!1,!0]
     */
    public static final ABProp CHANNEL_POLL_FORWARDING_ENABLED = new ABProp(10412, "false", "true");

    /**
     * The {@code reaction_user_journey_logging_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: reaction_user_journey_logging_enabled:[10438,"bool",!1,!0]
     */
    public static final ABProp REACTION_USER_JOURNEY_LOGGING_ENABLED = new ABProp(10438, "false", "true");

    /**
     * The {@code smb_agent_chat_list_indicator_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_agent_chat_list_indicator_enabled:[10455,"bool",!1,!1]
     */
    public static final ABProp SMB_AGENT_CHAT_LIST_INDICATOR_ENABLED = new ABProp(10455, "false", "false");

    /**
     * The {@code smb_agent_thread_control_notification_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_agent_thread_control_notification_enabled:[10456,"bool",!1,!1]
     */
    public static final ABProp SMB_AGENT_THREAD_CONTROL_NOTIFICATION_ENABLED = new ABProp(10456, "false", "false");

    /**
     * The {@code privacy_token_sending_on_all_1_on_1_messages} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: privacy_token_sending_on_all_1_on_1_messages:[10518,"bool",!1,!1]
     */
    public static final ABProp PRIVACY_TOKEN_SENDING_ON_ALL_1_ON_1_MESSAGES = new ABProp(10518, "false", "false");

    /**
     * The {@code enable_community_suspend_and_appeals} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_community_suspend_and_appeals:[10539,"bool",!1,!0]
     */
    public static final ABProp ENABLE_COMMUNITY_SUSPEND_AND_APPEALS = new ABProp(10539, "false", "true");

    /**
     * The {@code enable_call_result_fix_for_404_accept_nack} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_call_result_fix_for_404_accept_nack:[10565,"bool",!1,!1]
     */
    public static final ABProp ENABLE_CALL_RESULT_FIX_FOR_404_ACCEPT_NACK = new ABProp(10565, "false", "false");

    /**
     * The {@code saga_v1_carousel} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: saga_v1_carousel:[10609,"bool",!0,!0]
     */
    public static final ABProp SAGA_V1_CAROUSEL = new ABProp(10609, "true", "true");

    /**
     * The {@code mm_message_level_feedback_not_interested_menu_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: mm_message_level_feedback_not_interested_menu_enabled:[10668,"bool",!1,!1]
     */
    public static final ABProp MM_MESSAGE_LEVEL_FEEDBACK_NOT_INTERESTED_MENU_ENABLED = new ABProp(10668, "false", "false");

    /**
     * The {@code album_v2_forward_as_album_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: album_v2_forward_as_album_enabled:[10725,"bool",!1,!0]
     */
    public static final ABProp ALBUM_V2_FORWARD_AS_ALBUM_ENABLED = new ABProp(10725, "false", "true");

    /**
     * The {@code web_history_sync_allow_duplicate_in_bulk_error} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_history_sync_allow_duplicate_in_bulk_error:[10842,"bool",!1,!0]
     */
    public static final ABProp WEB_HISTORY_SYNC_ALLOW_DUPLICATE_IN_BULK_ERROR = new ABProp(10842, "false", "true");

    /**
     * The {@code album_v2_min_items_to_send_as_album_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: album_v2_min_items_to_send_as_album_enabled:[10848,"int",4,4]
     */
    public static final ABProp ALBUM_V2_MIN_ITEMS_TO_SEND_AS_ALBUM_ENABLED = new ABProp(10848, "4", "4");

    /**
     * The {@code web_view_mode_usage_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_view_mode_usage_enabled:[10856,"bool",!1,!1]
     */
    public static final ABProp WEB_VIEW_MODE_USAGE_ENABLED = new ABProp(10856, "false", "false");

    /**
     * The {@code wabba_receiver_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wabba_receiver_enabled:[10970,"bool",!1,!1]
     */
    public static final ABProp WABBA_RECEIVER_ENABLED = new ABProp(10970, "false", "false");

    /**
     * The {@code music_ohai_proxy_url} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: music_ohai_proxy_url:[10975,"string","https://meta-ohttp-relay-prod.fastly-edge.com/","https://meta-ohttp-relay-prod.fastly-edge.com/"]
     */
    public static final ABProp MUSIC_OHAI_PROXY_URL = new ABProp(10975, "https://meta-ohttp-relay-prod.fastly-edge.com/", "https://meta-ohttp-relay-prod.fastly-edge.com/");

    /**
     * The {@code ctwa_long_term_holdout_client_side_check} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ctwa_long_term_holdout_client_side_check:[11000,"bool",!1,!1]
     */
    public static final ABProp CTWA_LONG_TERM_HOLDOUT_CLIENT_SIDE_CHECK = new ABProp(11000, "false", "false");

    /**
     * The {@code web_adv_logout_on_self_device_list_expired} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_adv_logout_on_self_device_list_expired:[11011,"bool",!1,!1]
     */
    public static final ABProp WEB_ADV_LOGOUT_ON_SELF_DEVICE_LIST_EXPIRED = new ABProp(11011, "false", "false");

    /**
     * The {@code wamo_sub_logging_enabled_v2} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wamo_sub_logging_enabled_v2:[11017,"bool",!1,!0]
     */
    public static final ABProp WAMO_SUB_LOGGING_ENABLED_V2 = new ABProp(11017, "false", "true");

    /**
     * The {@code wamo_sub_admin_enabled_v2} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wamo_sub_admin_enabled_v2:[11020,"bool",!1,!1]
     */
    public static final ABProp WAMO_SUB_ADMIN_ENABLED_V2 = new ABProp(11020, "false", "false");

    /**
     * The {@code wamo_sub_consumer_enabled_v2} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wamo_sub_consumer_enabled_v2:[11021,"bool",!1,!1]
     */
    public static final ABProp WAMO_SUB_CONSUMER_ENABLED_V2 = new ABProp(11021, "false", "false");

    /**
     * The {@code meta_catalog_linking_m2_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: meta_catalog_linking_m2_enabled:[11029,"bool",!0,!0]
     */
    public static final ABProp META_CATALOG_LINKING_M2_ENABLED = new ABProp(11029, "true", "true");

    /**
     * The {@code lid_migration_for_vname_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: lid_migration_for_vname_enabled:[11049,"bool",!1,!1]
     */
    public static final ABProp LID_MIGRATION_FOR_VNAME_ENABLED = new ABProp(11049, "false", "false");

    /**
     * The {@code lid_one_on_one_migration_log_out_on_mismatch} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: lid_one_on_one_migration_log_out_on_mismatch:[11050,"bool",!0,!0]
     */
    public static final ABProp LID_ONE_ON_ONE_MIGRATION_LOG_OUT_ON_MISMATCH = new ABProp(11050, "true", "true");

    /**
     * The {@code wamo_sub_messages_supported} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wamo_sub_messages_supported:[11062,"bool",!1,!1]
     */
    public static final ABProp WAMO_SUB_MESSAGES_SUPPORTED = new ABProp(11062, "false", "false");

    /**
     * The {@code default_endpoint_thread_poll_timeout} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: default_endpoint_thread_poll_timeout:[11129,"int",0,0]
     */
    public static final ABProp DEFAULT_ENDPOINT_THREAD_POLL_TIMEOUT = new ABProp(11129, "0", "0");

    /**
     * The {@code ai_home_bot_profile_sync_interval_sec} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_home_bot_profile_sync_interval_sec:[11168,"int",86400,86400]
     */
    public static final ABProp AI_HOME_BOT_PROFILE_SYNC_INTERVAL_SEC = new ABProp(11168, "86400", "86400");

    /**
     * The {@code enable_channel_video_server_thumbnail} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_channel_video_server_thumbnail:[11192,"bool",!1,!1]
     */
    public static final ABProp ENABLE_CHANNEL_VIDEO_SERVER_THUMBNAIL = new ABProp(11192, "false", "false");

    /**
     * The {@code ctwa_custom_label_signals_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ctwa_custom_label_signals_enabled:[11205,"bool",!1,!1]
     */
    public static final ABProp CTWA_CUSTOM_LABEL_SIGNALS_ENABLED = new ABProp(11205, "false", "false");

    /**
     * The {@code mm_opt_out_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: mm_opt_out_enabled:[11241,"bool",!1,!1]
     */
    public static final ABProp MM_OPT_OUT_ENABLED = new ABProp(11241, "false", "false");

    /**
     * The {@code privacy_token_sending_on_group_create} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: privacy_token_sending_on_group_create:[11261,"bool",!1,!1]
     */
    public static final ABProp PRIVACY_TOKEN_SENDING_ON_GROUP_CREATE = new ABProp(11261, "false", "false");

    /**
     * The {@code privacy_token_sending_on_group_participant_add} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: privacy_token_sending_on_group_participant_add:[11262,"bool",!1,!1]
     */
    public static final ABProp PRIVACY_TOKEN_SENDING_ON_GROUP_PARTICIPANT_ADD = new ABProp(11262, "false", "false");

    /**
     * The {@code ml_model_download_skip_hash_check} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ml_model_download_skip_hash_check:[11454,"bool",!0,!0]
     */
    public static final ABProp ML_MODEL_DOWNLOAD_SKIP_HASH_CHECK = new ABProp(11454, "true", "true");

    /**
     * The {@code ignore_joinable_terminate_on_expired_offer} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ignore_joinable_terminate_on_expired_offer:[11519,"bool",!1,!1]
     */
    public static final ABProp IGNORE_JOINABLE_TERMINATE_ON_EXPIRED_OFFER = new ABProp(11519, "false", "false");

    /**
     * The {@code smb_catalog_graphql_verify_postcode} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_catalog_graphql_verify_postcode:[11624,"bool",!1,!1]
     */
    public static final ABProp SMB_CATALOG_GRAPHQL_VERIFY_POSTCODE = new ABProp(11624, "false", "false");

    /**
     * The {@code native_contact_companion_nux_learn_more_article_id} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: native_contact_companion_nux_learn_more_article_id:[11644,"string","1191526044909364","1191526044909364"]
     */
    public static final ABProp NATIVE_CONTACT_COMPANION_NUX_LEARN_MORE_ARTICLE_ID = new ABProp(11644, "1191526044909364", "1191526044909364");

    /**
     * The {@code smb_catalog_graphql_update_product_visibility} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_catalog_graphql_update_product_visibility:[11651,"bool",!1,!1]
     */
    public static final ABProp SMB_CATALOG_GRAPHQL_UPDATE_PRODUCT_VISIBILITY = new ABProp(11651, "false", "false");

    /**
     * The {@code external_ctx_authorise_wa_chat} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: external_ctx_authorise_wa_chat:[11655,"bool",!1,!1]
     */
    public static final ABProp EXTERNAL_CTX_AUTHORISE_WA_CHAT = new ABProp(11655, "false", "false");

    /**
     * The {@code ai_fbid_migration_receive_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_fbid_migration_receive_enabled:[11660,"bool",!1,!1]
     */
    public static final ABProp AI_FBID_MIGRATION_RECEIVE_ENABLED = new ABProp(11660, "false", "false");

    /**
     * The {@code payments_br_p2m_boleto_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: payments_br_p2m_boleto_enabled:[11671,"bool",!1,!1]
     */
    public static final ABProp PAYMENTS_BR_P2M_BOLETO_ENABLED = new ABProp(11671, "false", "false");

    /**
     * The {@code smb_catalog_graphql_get_public_key} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_catalog_graphql_get_public_key:[11690,"bool",!1,!1]
     */
    public static final ABProp SMB_CATALOG_GRAPHQL_GET_PUBLIC_KEY = new ABProp(11690, "false", "false");

    /**
     * The {@code add_to_call_in_chat_thread} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: add_to_call_in_chat_thread:[11700,"int",0,0]
     */
    public static final ABProp ADD_TO_CALL_IN_CHAT_THREAD = new ABProp(11700, "0", "0");

    /**
     * The {@code saga_protobuf_ai_stardust_web} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: saga_protobuf_ai_stardust_web:[11756,"bool",!1,!0]
     */
    public static final ABProp SAGA_PROTOBUF_AI_STARDUST_WEB = new ABProp(11756, "false", "true");

    /**
     * The {@code username_engagement_network_impact_logging} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: username_engagement_network_impact_logging:[11794,"bool",!1,!0]
     */
    public static final ABProp USERNAME_ENGAGEMENT_NETWORK_IMPACT_LOGGING = new ABProp(11794, "false", "true");

    /**
     * The {@code syncd_mutation_and_bundle_logging} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: syncd_mutation_and_bundle_logging:[11821,"string","{\"allowlist\": []}","{\"allowlist\": []}"]
     */
    public static final ABProp SYNCD_MUTATION_AND_BUNDLE_LOGGING = new ABProp(11821, "{\"allowlist\": []}", "{\"allowlist\": []}");

    /**
     * The {@code saga_protobuf_show_sysmsg_web} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: saga_protobuf_show_sysmsg_web:[11832,"bool",!1,!0]
     */
    public static final ABProp SAGA_PROTOBUF_SHOW_SYSMSG_WEB = new ABProp(11832, "false", "true");

    /**
     * The {@code notification_highlight_group_size_threshold} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: notification_highlight_group_size_threshold:[11891,"int",130,130]
     */
    public static final ABProp NOTIFICATION_HIGHLIGHT_GROUP_SIZE_THRESHOLD = new ABProp(11891, "130", "130");

    /**
     * The {@code album_v2_item_with_caption_in_album_receiver_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: album_v2_item_with_caption_in_album_receiver_enabled:[11943,"bool",!0,!0]
     */
    public static final ABProp ALBUM_V2_ITEM_WITH_CAPTION_IN_ALBUM_RECEIVER_ENABLED = new ABProp(11943, "true", "true");

    /**
     * The {@code futureproof_associated_child_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: futureproof_associated_child_enabled:[11976,"bool",!1,!0]
     */
    public static final ABProp FUTUREPROOF_ASSOCIATED_CHILD_ENABLED = new ABProp(11976, "false", "true");

    /**
     * The {@code use_signed_shimmed_url_link} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: use_signed_shimmed_url_link:[11977,"bool",!1,!1]
     */
    public static final ABProp USE_SIGNED_SHIMMED_URL_LINK = new ABProp(11977, "false", "false");

    /**
     * The {@code channel_photo_poll_receiver_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channel_photo_poll_receiver_enabled:[11980,"bool",!1,!0]
     */
    public static final ABProp CHANNEL_PHOTO_POLL_RECEIVER_ENABLED = new ABProp(11980, "false", "true");

    /**
     * The {@code channel_photo_poll_sender_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channel_photo_poll_sender_enabled:[11989,"bool",!1,!1]
     */
    public static final ABProp CHANNEL_PHOTO_POLL_SENDER_ENABLED = new ABProp(11989, "false", "false");

    /**
     * The {@code lid_migration_for_biz_profile_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: lid_migration_for_biz_profile_enabled:[12000,"bool",!1,!1]
     */
    public static final ABProp LID_MIGRATION_FOR_BIZ_PROFILE_ENABLED = new ABProp(12000, "false", "false");

    /**
     * The {@code enable_group_create_or_add_rate_limiting_error_ux} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_group_create_or_add_rate_limiting_error_ux:[12020,"bool",!1,!0]
     */
    public static final ABProp ENABLE_GROUP_CREATE_OR_ADD_RATE_LIMITING_ERROR_UX = new ABProp(12020, "false", "true");

    /**
     * The {@code smb_catalog_graphql_commerce_settings} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_catalog_graphql_commerce_settings:[12099,"bool",!1,!1]
     */
    public static final ABProp SMB_CATALOG_GRAPHQL_COMMERCE_SETTINGS = new ABProp(12099, "false", "false");

    /**
     * The {@code mm_opt_out_fmx_stop_for_high_trust} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: mm_opt_out_fmx_stop_for_high_trust:[12172,"bool",!1,!1]
     */
    public static final ABProp MM_OPT_OUT_FMX_STOP_FOR_HIGH_TRUST = new ABProp(12172, "false", "false");

    /**
     * The {@code ohai_request_kb_size} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ohai_request_kb_size:[12248,"float",20,20]
     */
    public static final ABProp OHAI_REQUEST_KB_SIZE = new ABProp(12248, "20", "20");

    /**
     * The {@code status_mentions_group_mention_receiver} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: status_mentions_group_mention_receiver:[12254,"bool",!1,!0]
     */
    public static final ABProp STATUS_MENTIONS_GROUP_MENTION_RECEIVER = new ABProp(12254, "false", "true");

    /**
     * The {@code poll_result_snapshot_polltype_envelope_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: poll_result_snapshot_polltype_envelope_enabled:[12258,"bool",!1,!0]
     */
    public static final ABProp POLL_RESULT_SNAPSHOT_POLLTYPE_ENVELOPE_ENABLED = new ABProp(12258, "false", "true");

    /**
     * The {@code web_new_chat_flow_refresh_variant} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_new_chat_flow_refresh_variant:[12276,"int",0,0]
     */
    public static final ABProp WEB_NEW_CHAT_FLOW_REFRESH_VARIANT = new ABProp(12276, "0", "0");

    /**
     * The {@code channels_view_counts_vpv_logging_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_view_counts_vpv_logging_enabled:[12295,"bool",!1,!0]
     */
    public static final ABProp CHANNELS_VIEW_COUNTS_VPV_LOGGING_ENABLED = new ABProp(12295, "false", "true");

    /**
     * The {@code wam_disable_abkey_attribute} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wam_disable_abkey_attribute:[12390,"bool",!1,!1]
     */
    public static final ABProp WAM_DISABLE_ABKEY_ATTRIBUTE = new ABProp(12390, "false", "false");

    /**
     * The {@code wam_disable_expokey_attribute} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wam_disable_expokey_attribute:[12391,"bool",!1,!1]
     */
    public static final ABProp WAM_DISABLE_EXPOKEY_ATTRIBUTE = new ABProp(12391, "false", "false");

    /**
     * The {@code web_signal_future_messages_max} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_signal_future_messages_max:[12509,"int",20000,20000]
     */
    public static final ABProp WEB_SIGNAL_FUTURE_MESSAGES_MAX = new ABProp(12509, "20000", "20000");

    /**
     * The {@code flows_wa_web} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: flows_wa_web:[12520,"bool",!1,!0]
     */
    public static final ABProp FLOWS_WA_WEB = new ABProp(12520, "false", "true");

    /**
     * The {@code album_v2_min_items_to_send_album_with_caption} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: album_v2_min_items_to_send_album_with_caption:[12538,"int",2,2]
     */
    public static final ABProp ALBUM_V2_MIN_ITEMS_TO_SEND_ALBUM_WITH_CAPTION = new ABProp(12538, "2", "2");

    /**
     * The {@code ai_rich_response_main_gate_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_rich_response_main_gate_enabled:[12539,"bool",!0,!0]
     */
    public static final ABProp AI_RICH_RESPONSE_MAIN_GATE_ENABLED = new ABProp(12539, "true", "true");

    /**
     * The {@code smb_catalog_graphql_delete_product} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_catalog_graphql_delete_product:[12543,"bool",!1,!1]
     */
    public static final ABProp SMB_CATALOG_GRAPHQL_DELETE_PRODUCT = new ABProp(12543, "false", "false");

    /**
     * The {@code otp_lid_migration_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: otp_lid_migration_enabled:[12553,"bool",!1,!0]
     */
    public static final ABProp OTP_LID_MIGRATION_ENABLED = new ABProp(12553, "false", "true");

    /**
     * The {@code rt_sender_dual_encrypted_msg_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: rt_sender_dual_encrypted_msg_enabled:[12623,"bool",!0,!0]
     */
    public static final ABProp RT_SENDER_DUAL_ENCRYPTED_MSG_ENABLED = new ABProp(12623, "true", "true");

    /**
     * The {@code ai_voice_multimodal_composer_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_voice_multimodal_composer_enabled:[12692,"bool",!1,!1]
     */
    public static final ABProp AI_VOICE_MULTIMODAL_COMPOSER_ENABLED = new ABProp(12692, "false", "false");

    /**
     * The {@code wamo_sub_process_message_kill_switch} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wamo_sub_process_message_kill_switch:[12722,"bool",!0,!0]
     */
    public static final ABProp WAMO_SUB_PROCESS_MESSAGE_KILL_SWITCH = new ABProp(12722, "true", "true");

    /**
     * The {@code external_ctx_url_param_names} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: external_ctx_url_param_names:[12726,"string","partnertoken","partnertoken"]
     */
    public static final ABProp EXTERNAL_CTX_URL_PARAM_NAMES = new ABProp(12726, "partnertoken", "partnertoken");

    /**
     * The {@code external_ctx_authorise_existing_chats} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: external_ctx_authorise_existing_chats:[12761,"int",0,0]
     */
    public static final ABProp EXTERNAL_CTX_AUTHORISE_EXISTING_CHATS = new ABProp(12761, "0", "0");

    /**
     * The {@code ai_fbid_migration_invoke_receive_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_fbid_migration_invoke_receive_enabled:[12795,"bool",!1,!1]
     */
    public static final ABProp AI_FBID_MIGRATION_INVOKE_RECEIVE_ENABLED = new ABProp(12795, "false", "false");

    /**
     * The {@code media_viewer_accelerated_playback_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: media_viewer_accelerated_playback_enabled:[12813,"bool",!1,!0]
     */
    public static final ABProp MEDIA_VIEWER_ACCELERATED_PLAYBACK_ENABLED = new ABProp(12813, "false", "true");

    /**
     * The {@code web_dexie_hooks_support_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_dexie_hooks_support_enabled:[12831,"bool",!1,!0]
     */
    public static final ABProp WEB_DEXIE_HOOKS_SUPPORT_ENABLED = new ABProp(12831, "false", "true");

    /**
     * The {@code reuse_cached_certs_for_data_channel} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: reuse_cached_certs_for_data_channel:[12913,"bool",!1,!0]
     */
    public static final ABProp REUSE_CACHED_CERTS_FOR_DATA_CHANNEL = new ABProp(12913, "false", "true");

    /**
     * The {@code lid_group_creation_addressing_mode_override} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: lid_group_creation_addressing_mode_override:[12985,"bool",!1,!1]
     */
    public static final ABProp LID_GROUP_CREATION_ADDRESSING_MODE_OVERRIDE = new ABProp(12985, "false", "false");

    /**
     * The {@code channel_osa_reporting_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channel_osa_reporting_enabled:[12987,"bool",!1,!0]
     */
    public static final ABProp CHANNEL_OSA_REPORTING_ENABLED = new ABProp(12987, "false", "true");

    /**
     * The {@code private_osa_reporting_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: private_osa_reporting_enabled:[12990,"bool",!1,!0]
     */
    public static final ABProp PRIVATE_OSA_REPORTING_ENABLED = new ABProp(12990, "false", "true");

    /**
     * The {@code web_ui_refresh_m1} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_ui_refresh_m1:[12993,"bool",!1,!1]
     */
    public static final ABProp WEB_UI_REFRESH_M1 = new ABProp(12993, "false", "false");

    /**
     * The {@code disclosure_for_the_marketing_message_body_links_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: disclosure_for_the_marketing_message_body_links_enabled:[12994,"bool",!1,!1]
     */
    public static final ABProp DISCLOSURE_FOR_THE_MARKETING_MESSAGE_BODY_LINKS_ENABLED = new ABProp(12994, "false", "false");

    /**
     * The {@code ft_validation_failure_drop_placeholder} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ft_validation_failure_drop_placeholder:[13063,"bool",!1,!0]
     */
    public static final ABProp FT_VALIDATION_FAILURE_DROP_PLACEHOLDER = new ABProp(13063, "false", "true");

    /**
     * The {@code wabba_save_to_camera_roll_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wabba_save_to_camera_roll_enabled:[13114,"bool",!1,!0]
     */
    public static final ABProp WABBA_SAVE_TO_CAMERA_ROLL_ENABLED = new ABProp(13114, "false", "true");

    /**
     * The {@code lid_one_on_one_migration_compatible} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: lid_one_on_one_migration_compatible:[13161,"bool",!0,!0]
     */
    public static final ABProp LID_ONE_ON_ONE_MIGRATION_COMPATIBLE = new ABProp(13161, "true", "true");

    /**
     * The {@code early_audio_driver_capture_at_native} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: early_audio_driver_capture_at_native:[13166,"bool",!1,!1]
     */
    public static final ABProp EARLY_AUDIO_DRIVER_CAPTURE_AT_NATIVE = new ABProp(13166, "false", "false");

    /**
     * The {@code early_audio_driver_pre_buffering} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: early_audio_driver_pre_buffering:[13168,"bool",!1,!1]
     */
    public static final ABProp EARLY_AUDIO_DRIVER_PRE_BUFFERING = new ABProp(13168, "false", "false");

    /**
     * The {@code channel_album_v2_receiving_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channel_album_v2_receiving_enabled:[13219,"bool",!1,!1]
     */
    public static final ABProp CHANNEL_ALBUM_V2_RECEIVING_ENABLED = new ABProp(13219, "false", "false");

    /**
     * The {@code channel_album_v2_sender_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channel_album_v2_sender_enabled:[13220,"bool",!1,!1]
     */
    public static final ABProp CHANNEL_ALBUM_V2_SENDER_ENABLED = new ABProp(13220, "false", "false");

    /**
     * The {@code enable_audio_device_async_start} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_audio_device_async_start:[13231,"bool",!1,!1]
     */
    public static final ABProp ENABLE_AUDIO_DEVICE_ASYNC_START = new ABProp(13231, "false", "false");

    /**
     * The {@code ctwa_enable_biz_data_sharing_after_nux_dismiss} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ctwa_enable_biz_data_sharing_after_nux_dismiss:[13240,"bool",!1,!0]
     */
    public static final ABProp CTWA_ENABLE_BIZ_DATA_SHARING_AFTER_NUX_DISMISS = new ABProp(13240, "false", "true");

    /**
     * The {@code ai_voice_entry_point_logging_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_voice_entry_point_logging_enabled:[13247,"bool",!1,!1]
     */
    public static final ABProp AI_VOICE_ENTRY_POINT_LOGGING_ENABLED = new ABProp(13247, "false", "false");

    /**
     * The {@code anyone_can_link_to_groups} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: anyone_can_link_to_groups:[13268,"bool",!1,!1]
     */
    public static final ABProp ANYONE_CAN_LINK_TO_GROUPS = new ABProp(13268, "false", "false");

    /**
     * The {@code status_save_to_camera_roll_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: status_save_to_camera_roll_enabled:[13280,"bool",!1,!0]
     */
    public static final ABProp STATUS_SAVE_TO_CAMERA_ROLL_ENABLED = new ABProp(13280, "false", "true");

    /**
     * The {@code custom_racing_emoji_feb2025} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: custom_racing_emoji_feb2025:[13322,"bool",!1,!1]
     */
    public static final ABProp CUSTOM_RACING_EMOJI_FEB2025 = new ABProp(13322, "false", "false");

    /**
     * The {@code emoji_search_cldr} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: emoji_search_cldr:[13323,"bool",!1,!1]
     */
    public static final ABProp EMOJI_SEARCH_CLDR = new ABProp(13323, "false", "false");

    /**
     * The {@code enable_calling_username} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_calling_username:[13359,"bool",!1,!1]
     */
    public static final ABProp ENABLE_CALLING_USERNAME = new ABProp(13359, "false", "false");

    /**
     * The {@code per_customer_data_sharing_controls_eligible} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: per_customer_data_sharing_controls_eligible:[13383,"bool",!1,!1]
     */
    public static final ABProp PER_CUSTOMER_DATA_SHARING_CONTROLS_ELIGIBLE = new ABProp(13383, "false", "false");

    /**
     * The {@code ctwa_download_3pd_signals} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ctwa_download_3pd_signals:[13385,"bool",!1,!0]
     */
    public static final ABProp CTWA_DOWNLOAD_3PD_SIGNALS = new ABProp(13385, "false", "true");

    /**
     * The {@code smb_ai_agents_web_chat_assignment_interop_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_ai_agents_web_chat_assignment_interop_enabled:[13387,"bool",!1,!0]
     */
    public static final ABProp SMB_AI_AGENTS_WEB_CHAT_ASSIGNMENT_INTEROP_ENABLED = new ABProp(13387, "false", "true");

    /**
     * The {@code smb_product_country_of_origin_m1} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_product_country_of_origin_m1:[13415,"bool",!1,!1]
     */
    public static final ABProp SMB_PRODUCT_COUNTRY_OF_ORIGIN_M1 = new ABProp(13415, "false", "false");

    /**
     * The {@code use_cached_app_settings_from_global_ctx} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: use_cached_app_settings_from_global_ctx:[13428,"bool",!0,!0]
     */
    public static final ABProp USE_CACHED_APP_SETTINGS_FROM_GLOBAL_CTX = new ABProp(13428, "true", "true");

    /**
     * The {@code rasterize_text_status_pixel_width} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: rasterize_text_status_pixel_width:[13460,"int",1080,1080]
     */
    public static final ABProp RASTERIZE_TEXT_STATUS_PIXEL_WIDTH = new ABProp(13460, "1080", "1080");

    /**
     * The {@code biz_ai_auto_save_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: biz_ai_auto_save_enabled:[13464,"bool",!1,!1]
     */
    public static final ABProp BIZ_AI_AUTO_SAVE_ENABLED = new ABProp(13464, "false", "false");

    /**
     * The {@code biz_ai_coaching_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: biz_ai_coaching_enabled:[13465,"bool",!1,!1]
     */
    public static final ABProp BIZ_AI_COACHING_ENABLED = new ABProp(13465, "false", "false");

    /**
     * The {@code ctwa_3pd_data_sharing_on_thread_entry} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ctwa_3pd_data_sharing_on_thread_entry:[13485,"bool",!1,!1]
     */
    public static final ABProp CTWA_3PD_DATA_SHARING_ON_THREAD_ENTRY = new ABProp(13485, "false", "false");

    /**
     * The {@code animated_race_mercedes_car_emoji_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: animated_race_mercedes_car_emoji_enabled:[13490,"bool",!1,!1]
     */
    public static final ABProp ANIMATED_RACE_MERCEDES_CAR_EMOJI_ENABLED = new ABProp(13490, "false", "false");

    /**
     * The {@code enable_unified_call_buttons_in_chat} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_unified_call_buttons_in_chat:[13497,"bool",!1,!1]
     */
    public static final ABProp ENABLE_UNIFIED_CALL_BUTTONS_IN_CHAT = new ABProp(13497, "false", "false");

    /**
     * The {@code mm_user_controls_exposure} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: mm_user_controls_exposure:[13510,"bool",!1,!1]
     */
    public static final ABProp MM_USER_CONTROLS_EXPOSURE = new ABProp(13510, "false", "false");

    /**
     * The {@code member_name_tag_receiver_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: member_name_tag_receiver_enabled:[13523,"bool",!1,!0]
     */
    public static final ABProp MEMBER_NAME_TAG_RECEIVER_ENABLED = new ABProp(13523, "false", "true");

    /**
     * The {@code channels_ptv_receiving_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_ptv_receiving_enabled:[13559,"bool",!1,!1]
     */
    public static final ABProp CHANNELS_PTV_RECEIVING_ENABLED = new ABProp(13559, "false", "false");

    /**
     * The {@code external_ctx_foa_logging} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: external_ctx_foa_logging:[13565,"int",1,1]
     */
    public static final ABProp EXTERNAL_CTX_FOA_LOGGING = new ABProp(13565, "1", "1");

    /**
     * The {@code ai_rich_response_grid_image_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_rich_response_grid_image_enabled:[13578,"bool",!1,!1]
     */
    public static final ABProp AI_RICH_RESPONSE_GRID_IMAGE_ENABLED = new ABProp(13578, "false", "false");

    /**
     * The {@code ctwa_show_ads_data_sharing_after_message} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ctwa_show_ads_data_sharing_after_message:[13579,"bool",!1,!0]
     */
    public static final ABProp CTWA_SHOW_ADS_DATA_SHARING_AFTER_MESSAGE = new ABProp(13579, "false", "true");

    /**
     * The {@code wa_web_fmx_agm_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_fmx_agm_enabled:[13597,"bool",!1,!0]
     */
    public static final ABProp WA_WEB_FMX_AGM_ENABLED = new ABProp(13597, "false", "true");

    /**
     * The {@code sticky_chat_profile_picture_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: sticky_chat_profile_picture_enabled:[13692,"bool",!1,!1]
     */
    public static final ABProp STICKY_CHAT_PROFILE_PICTURE_ENABLED = new ABProp(13692, "false", "false");

    /**
     * The {@code channels_ptv_forwarding_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_ptv_forwarding_enabled:[13776,"bool",!1,!0]
     */
    public static final ABProp CHANNELS_PTV_FORWARDING_ENABLED = new ABProp(13776, "false", "true");

    /**
     * The {@code enable_early_audio_driver_start} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_early_audio_driver_start:[13807,"bool",!1,!1]
     */
    public static final ABProp ENABLE_EARLY_AUDIO_DRIVER_START = new ABProp(13807, "false", "false");

    /**
     * The {@code premium_broadcast_smb_capping_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: premium_broadcast_smb_capping_enabled:[13808,"bool",!1,!0]
     */
    public static final ABProp PREMIUM_BROADCAST_SMB_CAPPING_ENABLED = new ABProp(13808, "false", "true");

    /**
     * The {@code defense_mode_available} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: defense_mode_available:[13874,"int",0,1]
     */
    public static final ABProp DEFENSE_MODE_AVAILABLE = new ABProp(13874, "0", "1");

    /**
     * The {@code ai_forward_flow_surface_meta_ai_as_contact_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_forward_flow_surface_meta_ai_as_contact_enabled:[13879,"bool",!1,!1]
     */
    public static final ABProp AI_FORWARD_FLOW_SURFACE_META_AI_AS_CONTACT_ENABLED = new ABProp(13879, "false", "false");

    /**
     * The {@code lid_one_on_one_migration_peer_sync_timeout_in_seconds} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: lid_one_on_one_migration_peer_sync_timeout_in_seconds:[13936,"int",300,300]
     */
    public static final ABProp LID_ONE_ON_ONE_MIGRATION_PEER_SYNC_TIMEOUT_IN_SECONDS = new ABProp(13936, "300", "300");

    /**
     * The {@code newsletters_video_playback_wabba_logging_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: newsletters_video_playback_wabba_logging_enabled:[13954,"bool",!1,!0]
     */
    public static final ABProp NEWSLETTERS_VIDEO_PLAYBACK_WABBA_LOGGING_ENABLED = new ABProp(13954, "false", "true");

    /**
     * The {@code group_status_receiver_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: group_status_receiver_enabled:[13956,"bool",!1,!0]
     */
    public static final ABProp GROUP_STATUS_RECEIVER_ENABLED = new ABProp(13956, "false", "true");

    /**
     * The {@code ai_pdfn_tos_inline_notices} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_pdfn_tos_inline_notices:[13970,"string"," "," "]
     */
    public static final ABProp AI_PDFN_TOS_INLINE_NOTICES = new ABProp(13970, " ", " ");

    /**
     * The {@code updates_quick_promotion_banner_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: updates_quick_promotion_banner_enabled:[13997,"bool",!1,!0]
     */
    public static final ABProp UPDATES_QUICK_PROMOTION_BANNER_ENABLED = new ABProp(13997, "false", "true");

    /**
     * The {@code mm_user_controls_exception_number_prefixes} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: mm_user_controls_exception_number_prefixes:[13999,"string","",""]
     */
    public static final ABProp MM_USER_CONTROLS_EXCEPTION_NUMBER_PREFIXES = new ABProp(13999, "", "");

    /**
     * The {@code snapl_newsletter_logging_media_id_placeholder_string} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: snapl_newsletter_logging_media_id_placeholder_string:[14064,"string","-1","-1"]
     */
    public static final ABProp SNAPL_NEWSLETTER_LOGGING_MEDIA_ID_PLACEHOLDER_STRING = new ABProp(14064, "-1", "-1");

    /**
     * The {@code ai_rich_response_web_structured_response_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_rich_response_web_structured_response_enabled:[14141,"bool",!1,!1]
     */
    public static final ABProp AI_RICH_RESPONSE_WEB_STRUCTURED_RESPONSE_ENABLED = new ABProp(14141, "false", "false");

    /**
     * The {@code view_replies_infra_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: view_replies_infra_enabled:[14199,"bool",!1,!1]
     */
    public static final ABProp VIEW_REPLIES_INFRA_ENABLED = new ABProp(14199, "false", "false");

    /**
     * The {@code ai_rewrite_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_rewrite_enabled:[14219,"bool",!1,!1]
     */
    public static final ABProp AI_REWRITE_ENABLED = new ABProp(14219, "false", "false");

    /**
     * The {@code ai_rewrite_supported_languages} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_rewrite_supported_languages:[14220,"string"," ","en"]
     */
    public static final ABProp AI_REWRITE_SUPPORTED_LANGUAGES = new ABProp(14220, " ", "en");

    /**
     * The {@code channels_uk_osa_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_uk_osa_enabled:[14249,"bool",!1,!0]
     */
    public static final ABProp CHANNELS_UK_OSA_ENABLED = new ABProp(14249, "false", "true");

    /**
     * The {@code private_messaging_uk_osa_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: private_messaging_uk_osa_enabled:[14250,"bool",!1,!0]
     */
    public static final ABProp PRIVATE_MESSAGING_UK_OSA_ENABLED = new ABProp(14250, "false", "true");

    /**
     * The {@code wa_web_favicons_update_m1} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_favicons_update_m1:[14260,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_FAVICONS_UPDATE_M1 = new ABProp(14260, "false", "false");

    /**
     * The {@code is_part_of_gsc_experiment} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: is_part_of_gsc_experiment:[14279,"bool",!1,!1]
     */
    public static final ABProp IS_PART_OF_GSC_EXPERIMENT = new ABProp(14279, "false", "false");

    /**
     * The {@code username_numeric_code_v4} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: username_numeric_code_v4:[14286,"int",0,0]
     */
    public static final ABProp USERNAME_NUMERIC_CODE_V4 = new ABProp(14286, "0", "0");

    /**
     * The {@code web_catalog_recovery_flow_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_catalog_recovery_flow_enabled:[14294,"bool",!1,!1]
     */
    public static final ABProp WEB_CATALOG_RECOVERY_FLOW_ENABLED = new ABProp(14294, "false", "false");

    /**
     * The {@code web_waffle} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_waffle:[14300,"bool",!1,!1]
     */
    public static final ABProp WEB_WAFFLE = new ABProp(14300, "false", "false");

    /**
     * The {@code lid_trusted_token_issue_to_lid} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: lid_trusted_token_issue_to_lid:[14303,"bool",!1,!1]
     */
    public static final ABProp LID_TRUSTED_TOKEN_ISSUE_TO_LID = new ABProp(14303, "false", "false");

    /**
     * The {@code support_lids} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: support_lids:[14317,"string","4200746488034,30563255730192,70334669676777,19349129719984,66065505775654,133814269518032,243799792062487,7323238039569,269290422947912,261718412386336,4351103873168,12391299473616,92410801582180,277730033709185,36090878648473,79882365190287,94274800595104,117794058317863,115784047153172,179250745360524,7301780005088,166653589463190,94249030815912,198964645236955,198427807899653,23656948363422,255735573270728,106670109786240,130932396826763,18855208456329","4200746488034,30563255730192,70334669676777,19349129719984,66065505775654,133814269518032,243799792062487,7323238039569,269290422947912,261718412386336,4351103873168,12391299473616,92410801582180,277730033709185,36090878648473,79882365190287,94274800595104,117794058317863,115784047153172,179250745360524,7301780005088,166653589463190,94249030815912,198964645236955,198427807899653,23656948363422,255735573270728,106670109786240,130932396826763,18855208456329"]
     */
    public static final ABProp SUPPORT_LIDS = new ABProp(14317, "4200746488034,30563255730192,70334669676777,19349129719984,66065505775654,133814269518032,243799792062487,7323238039569,269290422947912,261718412386336,4351103873168,12391299473616,92410801582180,277730033709185,36090878648473,79882365190287,94274800595104,117794058317863,115784047153172,179250745360524,7301780005088,166653589463190,94249030815912,198964645236955,198427807899653,23656948363422,255735573270728,106670109786240,130932396826763,18855208456329", "4200746488034,30563255730192,70334669676777,19349129719984,66065505775654,133814269518032,243799792062487,7323238039569,269290422947912,261718412386336,4351103873168,12391299473616,92410801582180,277730033709185,36090878648473,79882365190287,94274800595104,117794058317863,115784047153172,179250745360524,7301780005088,166653589463190,94249030815912,198964645236955,198427807899653,23656948363422,255735573270728,106670109786240,130932396826763,18855208456329");

    /**
     * The {@code payment_support_lids} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: payment_support_lids:[14333,"string","116664750354676,128385682505839,46635358933114,26521959944357,200206125658243,179985503506636,187797998674170,228746200088715,117914552262794,10158134550607","116664750354676,128385682505839,46635358933114,26521959944357,200206125658243,179985503506636,187797998674170,228746200088715,117914552262794,10158134550607"]
     */
    public static final ABProp PAYMENT_SUPPORT_LIDS = new ABProp(14333, "116664750354676,128385682505839,46635358933114,26521959944357,200206125658243,179985503506636,187797998674170,228746200088715,117914552262794,10158134550607", "116664750354676,128385682505839,46635358933114,26521959944357,200206125658243,179985503506636,187797998674170,228746200088715,117914552262794,10158134550607");

    /**
     * The {@code gif_provider} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: gif_provider:[14343,"int",1,1]
     */
    public static final ABProp GIF_PROVIDER = new ABProp(14343, "1", "1");

    /**
     * The {@code payment_br_holdout} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: payment_br_holdout:[14358,"bool",!1,!1]
     */
    public static final ABProp PAYMENT_BR_HOLDOUT = new ABProp(14358, "false", "false");

    /**
     * The {@code updates_privacy_notice_rollout_date} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: updates_privacy_notice_rollout_date:[14387,"int",1742310000,1742310000]
     */
    public static final ABProp UPDATES_PRIVACY_NOTICE_ROLLOUT_DATE = new ABProp(14387, "1742310000", "1742310000");

    /**
     * The {@code render_updated_disclosure} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: render_updated_disclosure:[14407,"bool",!1,!1]
     */
    public static final ABProp RENDER_UPDATED_DISCLOSURE = new ABProp(14407, "false", "false");

    /**
     * The {@code syncd_sentinel_timeout_seconds} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: syncd_sentinel_timeout_seconds:[14485,"int",3,3]
     */
    public static final ABProp SYNCD_SENTINEL_TIMEOUT_SECONDS = new ABProp(14485, "3", "3");

    /**
     * The {@code syncd_key_max_use_days} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: syncd_key_max_use_days:[14488,"int",30,30]
     */
    public static final ABProp SYNCD_KEY_MAX_USE_DAYS = new ABProp(14488, "30", "30");

    /**
     * The {@code syncd_wait_for_key_timeout_days} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: syncd_wait_for_key_timeout_days:[14492,"int",7,7]
     */
    public static final ABProp SYNCD_WAIT_FOR_KEY_TIMEOUT_DAYS = new ABProp(14492, "7", "7");

    /**
     * The {@code syncd_inline_mutations_max_count} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: syncd_inline_mutations_max_count:[14494,"int",100,100]
     */
    public static final ABProp SYNCD_INLINE_MUTATIONS_MAX_COUNT = new ABProp(14494, "100", "100");

    /**
     * The {@code syncd_patch_protobuf_max_size} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: syncd_patch_protobuf_max_size:[14495,"int",10,10]
     */
    public static final ABProp SYNCD_PATCH_PROTOBUF_MAX_SIZE = new ABProp(14495, "10", "10");

    /**
     * The {@code username_contact_usync_lid_based} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: username_contact_usync_lid_based:[14565,"bool",!1,!1]
     */
    public static final ABProp USERNAME_CONTACT_USYNC_LID_BASED = new ABProp(14565, "false", "false");

    /**
     * The {@code optimized_delivery_multiple_collection_windows_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: optimized_delivery_multiple_collection_windows_enabled:[14588,"bool",!1,!0]
     */
    public static final ABProp OPTIMIZED_DELIVERY_MULTIPLE_COLLECTION_WINDOWS_ENABLED = new ABProp(14588, "false", "true");

    /**
     * The {@code enable_ugc_voice_fs_logging} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_ugc_voice_fs_logging:[14641,"bool",!1,!1]
     */
    public static final ABProp ENABLE_UGC_VOICE_FS_LOGGING = new ABProp(14641, "false", "false");

    /**
     * The {@code hybrid_educational_dialogs_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: hybrid_educational_dialogs_enabled:[14674,"bool",!1,!1]
     */
    public static final ABProp HYBRID_EDUCATIONAL_DIALOGS_ENABLED = new ABProp(14674, "false", "false");

    /**
     * The {@code educational_dialogs_button_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: educational_dialogs_button_enabled:[14676,"bool",!1,!1]
     */
    public static final ABProp EDUCATIONAL_DIALOGS_BUTTON_ENABLED = new ABProp(14676, "false", "false");

    /**
     * The {@code search_user_journey_logging_wam_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: search_user_journey_logging_wam_enabled:[14682,"bool",!1,!0]
     */
    public static final ABProp SEARCH_USER_JOURNEY_LOGGING_WAM_ENABLED = new ABProp(14682, "false", "true");

    /**
     * The {@code ai_rewrite_tone_modifiers} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_rewrite_tone_modifiers:[14743,"string","rephrase,professional,funny,supportive","rephrase,professional,funny,supportive"]
     */
    public static final ABProp AI_REWRITE_TONE_MODIFIERS = new ABProp(14743, "rephrase,professional,funny,supportive", "rephrase,professional,funny,supportive");

    /**
     * The {@code group_description_length} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: group_description_length:[14778,"int",2048,2048]
     */
    public static final ABProp GROUP_DESCRIPTION_LENGTH = new ABProp(14778, "2048", "2048");

    /**
     * The {@code group_max_subject} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: group_max_subject:[14801,"int",100,100]
     */
    public static final ABProp GROUP_MAX_SUBJECT = new ABProp(14801, "100", "100");

    /**
     * The {@code web_biz_profile_options} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_biz_profile_options:[14881,"int",116,116]
     */
    public static final ABProp WEB_BIZ_PROFILE_OPTIONS = new ABProp(14881, "116", "116");

    /**
     * The {@code ctwa_custom_label_algorithm} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ctwa_custom_label_algorithm:[14887,"int",0,0]
     */
    public static final ABProp CTWA_CUSTOM_LABEL_ALGORITHM = new ABProp(14887, "0", "0");

    /**
     * The {@code ai_rewrite_entry_point_min_words} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_rewrite_entry_point_min_words:[14923,"int",4,4]
     */
    public static final ABProp AI_REWRITE_ENTRY_POINT_MIN_WORDS = new ABProp(14923, "4", "4");

    /**
     * The {@code ai_rewrite_num_suggestions} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_rewrite_num_suggestions:[14924,"int",3,3]
     */
    public static final ABProp AI_REWRITE_NUM_SUGGESTIONS = new ABProp(14924, "3", "3");

    /**
     * The {@code smb_payment_links_cta_variant} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_payment_links_cta_variant:[14957,"int",2,2]
     */
    public static final ABProp SMB_PAYMENT_LINKS_CTA_VARIANT = new ABProp(14957, "2", "2");

    /**
     * The {@code smb_payment_links_cta_button_kill_switch} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_payment_links_cta_button_kill_switch:[14967,"bool",!1,!1]
     */
    public static final ABProp SMB_PAYMENT_LINKS_CTA_BUTTON_KILL_SWITCH = new ABProp(14967, "false", "false");

    /**
     * The {@code wamo_privacy_tos_linked_highlighted_notice_id} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wamo_privacy_tos_linked_highlighted_notice_id:[14985,"string","20610204","20610204"]
     */
    public static final ABProp WAMO_PRIVACY_TOS_LINKED_HIGHLIGHTED_NOTICE_ID = new ABProp(14985, "20610204", "20610204");

    /**
     * The {@code wamo_privacy_tos_unlinked_highlighted_notice_id} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wamo_privacy_tos_unlinked_highlighted_notice_id:[14987,"string","20610203","20610203"]
     */
    public static final ABProp WAMO_PRIVACY_TOS_UNLINKED_HIGHLIGHTED_NOTICE_ID = new ABProp(14987, "20610203", "20610203");

    /**
     * The {@code smb_payment_links_cta_psp_list} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_payment_links_cta_psp_list:[14998,"string","{}","{}"]
     */
    public static final ABProp SMB_PAYMENT_LINKS_CTA_PSP_LIST = new ABProp(14998, "{}", "{}");

    /**
     * The {@code rt_edit_receive} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: rt_edit_receive:[15016,"bool",!0,!0]
     */
    public static final ABProp RT_EDIT_RECEIVE = new ABProp(15016, "true", "true");

    /**
     * The {@code harmful_file_dialog_logging} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: harmful_file_dialog_logging:[15020,"bool",!1,!0]
     */
    public static final ABProp HARMFUL_FILE_DIALOG_LOGGING = new ABProp(15020, "false", "true");

    /**
     * The {@code updated_harmful_document_dialog} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: updated_harmful_document_dialog:[15022,"bool",!1,!0]
     */
    public static final ABProp UPDATED_HARMFUL_DOCUMENT_DIALOG = new ABProp(15022, "false", "true");

    /**
     * The {@code limit_sharing_enabled_for_1on1_chat} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: limit_sharing_enabled_for_1on1_chat:[15127,"bool",!1,!1]
     */
    public static final ABProp LIMIT_SHARING_ENABLED_FOR_1ON1_CHAT = new ABProp(15127, "false", "false");

    /**
     * The {@code limit_sharing_protocol_message_receiver_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: limit_sharing_protocol_message_receiver_enabled:[15129,"bool",!1,!1]
     */
    public static final ABProp LIMIT_SHARING_PROTOCOL_MESSAGE_RECEIVER_ENABLED = new ABProp(15129, "false", "false");

    /**
     * The {@code rt_web_delay_processing} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: rt_web_delay_processing:[15181,"bool",!1,!0]
     */
    public static final ABProp RT_WEB_DELAY_PROCESSING = new ABProp(15181, "false", "true");

    /**
     * The {@code channels_question_receiver_message_types_m1_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_question_receiver_message_types_m1_enabled:[15246,"string",""," 22"]
     */
    public static final ABProp CHANNELS_QUESTION_RECEIVER_MESSAGE_TYPES_M1_ENABLED = new ABProp(15246, "", " 22");

    /**
     * The {@code wamo_privacy_tos_show_channels_nux_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wamo_privacy_tos_show_channels_nux_enabled:[15254,"bool",!0,!0]
     */
    public static final ABProp WAMO_PRIVACY_TOS_SHOW_CHANNELS_NUX_ENABLED = new ABProp(15254, "true", "true");

    /**
     * The {@code newsletter_nux_notice_id} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: newsletter_nux_notice_id:[15255,"string","20610210","20610210"]
     */
    public static final ABProp NEWSLETTER_NUX_NOTICE_ID = new ABProp(15255, "20610210", "20610210");

    /**
     * The {@code newsletter_admin_invite_nux_id} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: newsletter_admin_invite_nux_id:[15256,"string","20610220","20610220"]
     */
    public static final ABProp NEWSLETTER_ADMIN_INVITE_NUX_ID = new ABProp(15256, "20610220", "20610220");

    /**
     * The {@code rt_receiver_dual_encrypted_msg_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: rt_receiver_dual_encrypted_msg_enabled:[15258,"bool",!0,!0]
     */
    public static final ABProp RT_RECEIVER_DUAL_ENCRYPTED_MSG_ENABLED = new ABProp(15258, "true", "true");

    /**
     * The {@code ctwa_important_label_sends_signals} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ctwa_important_label_sends_signals:[15271,"bool",!1,!0]
     */
    public static final ABProp CTWA_IMPORTANT_LABEL_SENDS_SIGNALS = new ABProp(15271, "false", "true");

    /**
     * The {@code ai_pdfn_tos_non_blocking_notices} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_pdfn_tos_non_blocking_notices:[15280,"string","",""]
     */
    public static final ABProp AI_PDFN_TOS_NON_BLOCKING_NOTICES = new ABProp(15280, "", "");

    /**
     * The {@code ai_pdfn_tos_master_notice_id} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_pdfn_tos_master_notice_id:[15295,"string"," "," "]
     */
    public static final ABProp AI_PDFN_TOS_MASTER_NOTICE_ID = new ABProp(15295, " ", " ");

    /**
     * The {@code ctwa_smb_detected_outcome_labels_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ctwa_smb_detected_outcome_labels_enabled:[15307,"bool",!1,!1]
     */
    public static final ABProp CTWA_SMB_DETECTED_OUTCOME_LABELS_ENABLED = new ABProp(15307, "false", "false");

    /**
     * The {@code ctwa_smb_detected_outcome_labels_merger_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ctwa_smb_detected_outcome_labels_merger_enabled:[15308,"bool",!1,!1]
     */
    public static final ABProp CTWA_SMB_DETECTED_OUTCOME_LABELS_MERGER_ENABLED = new ABProp(15308, "false", "false");

    /**
     * The {@code group_history_receive} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: group_history_receive:[15311,"bool",!1,!1]
     */
    public static final ABProp GROUP_HISTORY_RECEIVE = new ABProp(15311, "false", "false");

    /**
     * The {@code group_history_send} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: group_history_send:[15313,"bool",!1,!1]
     */
    public static final ABProp GROUP_HISTORY_SEND = new ABProp(15313, "false", "false");

    /**
     * The {@code channels_question_sender_message_types_m1_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_question_sender_message_types_m1_enabled:[15418,"string"," "," "]
     */
    public static final ABProp CHANNELS_QUESTION_SENDER_MESSAGE_TYPES_M1_ENABLED = new ABProp(15418, " ", " ");

    /**
     * The {@code search_the_web_design_experiment_v1} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: search_the_web_design_experiment_v1:[15423,"bool",!1,!0]
     */
    public static final ABProp SEARCH_THE_WEB_DESIGN_EXPERIMENT_V1 = new ABProp(15423, "false", "true");

    /**
     * The {@code enable_web_calling} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_web_calling:[15461,"bool",!1,!1]
     */
    public static final ABProp ENABLE_WEB_CALLING = new ABProp(15461, "false", "false");

    /**
     * The {@code username_adoption_and_engagement_monitoring_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: username_adoption_and_engagement_monitoring_enabled:[15493,"bool",!1,!0]
     */
    public static final ABProp USERNAME_ADOPTION_AND_ENGAGEMENT_MONITORING_ENABLED = new ABProp(15493, "false", "true");

    /**
     * The {@code enable_upcoming_schedule_call_events_in_calls_tab} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_upcoming_schedule_call_events_in_calls_tab:[15514,"bool",!1,!1]
     */
    public static final ABProp ENABLE_UPCOMING_SCHEDULE_CALL_EVENTS_IN_CALLS_TAB = new ABProp(15514, "false", "false");

    /**
     * The {@code biz_ai_in_thread_unmute_v2} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: biz_ai_in_thread_unmute_v2:[15523,"bool",!1,!0]
     */
    public static final ABProp BIZ_AI_IN_THREAD_UNMUTE_V2 = new ABProp(15523, "false", "true");

    /**
     * The {@code web_catalog_viewing_variants_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_catalog_viewing_variants_enabled:[15534,"bool",!1,!1]
     */
    public static final ABProp WEB_CATALOG_VIEWING_VARIANTS_ENABLED = new ABProp(15534, "false", "false");

    /**
     * The {@code wa_web_growth_empty_state_upsell_variant_m1} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_growth_empty_state_upsell_variant_m1:[15557,"int",1,1]
     */
    public static final ABProp WA_WEB_GROWTH_EMPTY_STATE_UPSELL_VARIANT_M1 = new ABProp(15557, "1", "1");

    /**
     * The {@code ai_rich_response_reasoning_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_rich_response_reasoning_enabled:[15589,"bool",!1,!1]
     */
    public static final ABProp AI_RICH_RESPONSE_REASONING_ENABLED = new ABProp(15589, "false", "false");

    /**
     * The {@code phone_number_sharing_flow} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: phone_number_sharing_flow:[15653,"bool",!1,!0]
     */
    public static final ABProp PHONE_NUMBER_SHARING_FLOW = new ABProp(15653, "false", "true");

    /**
     * The {@code ctwa_3pd_data_sharing_cooldown_max_times_shown_for_opted_out} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ctwa_3pd_data_sharing_cooldown_max_times_shown_for_opted_out:[15686,"int",0,0]
     */
    public static final ABProp CTWA_3PD_DATA_SHARING_COOLDOWN_MAX_TIMES_SHOWN_FOR_OPTED_OUT = new ABProp(15686, "0", "0");

    /**
     * The {@code quoted_message_user_journey_logging_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: quoted_message_user_journey_logging_enabled:[15694,"bool",!1,!1]
     */
    public static final ABProp QUOTED_MESSAGE_USER_JOURNEY_LOGGING_ENABLED = new ABProp(15694, "false", "false");

    /**
     * The {@code wamo_agm_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wamo_agm_enabled:[15714,"bool",!1,!1]
     */
    public static final ABProp WAMO_AGM_ENABLED = new ABProp(15714, "false", "false");

    /**
     * The {@code group_history_notice_receive} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: group_history_notice_receive:[15722,"bool",!1,!1]
     */
    public static final ABProp GROUP_HISTORY_NOTICE_RECEIVE = new ABProp(15722, "false", "false");

    /**
     * The {@code channels_open_qpl_improvements_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_open_qpl_improvements_enabled:[15754,"bool",!1,!0]
     */
    public static final ABProp CHANNELS_OPEN_QPL_IMPROVEMENTS_ENABLED = new ABProp(15754, "false", "true");

    /**
     * The {@code create_group_and_add_member_overflow} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: create_group_and_add_member_overflow:[15772,"bool",!1,!0]
     */
    public static final ABProp CREATE_GROUP_AND_ADD_MEMBER_OVERFLOW = new ABProp(15772, "false", "true");

    /**
     * The {@code lid_one_to_one_migration_event_response_force_pn_jid} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: lid_one_to_one_migration_event_response_force_pn_jid:[15791,"bool",!1,!1]
     */
    public static final ABProp LID_ONE_TO_ONE_MIGRATION_EVENT_RESPONSE_FORCE_PN_JID = new ABProp(15791, "false", "false");

    /**
     * The {@code kmp_syncd_engine_crypto_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: kmp_syncd_engine_crypto_enabled:[15909,"bool",!1,!1]
     */
    public static final ABProp KMP_SYNCD_ENGINE_CRYPTO_ENABLED = new ABProp(15909, "false", "false");

    /**
     * The {@code username_search} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: username_search:[15956,"bool",!1,!1]
     */
    public static final ABProp USERNAME_SEARCH = new ABProp(15956, "false", "false");

    /**
     * The {@code enable_pre_warm_audio_component} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_pre_warm_audio_component:[15994,"bool",!1,!1]
     */
    public static final ABProp ENABLE_PRE_WARM_AUDIO_COMPONENT = new ABProp(15994, "false", "false");

    /**
     * The {@code forwarded_message_user_journey_logging_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: forwarded_message_user_journey_logging_enabled:[16055,"bool",!1,!1]
     */
    public static final ABProp FORWARDED_MESSAGE_USER_JOURNEY_LOGGING_ENABLED = new ABProp(16055, "false", "false");

    /**
     * The {@code message_edit_to_message_secret_sender_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: message_edit_to_message_secret_sender_enabled:[16057,"bool",!1,!1]
     */
    public static final ABProp MESSAGE_EDIT_TO_MESSAGE_SECRET_SENDER_ENABLED = new ABProp(16057, "false", "false");

    /**
     * The {@code ai_all_languages_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_all_languages_enabled:[16091,"bool",!1,!1]
     */
    public static final ABProp AI_ALL_LANGUAGES_ENABLED = new ABProp(16091, "false", "false");

    /**
     * The {@code lid_group_migration_non_member_iq} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: lid_group_migration_non_member_iq:[16104,"bool",!1,!0]
     */
    public static final ABProp LID_GROUP_MIGRATION_NON_MEMBER_IQ = new ABProp(16104, "false", "true");

    /**
     * The {@code wa_web_debug_color_code_retry_messages} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_debug_color_code_retry_messages:[16138,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_DEBUG_COLOR_CODE_RETRY_MESSAGES = new ABProp(16138, "false", "false");

    /**
     * The {@code username_group_mutation_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: username_group_mutation_enabled:[16148,"bool",!1,!1]
     */
    public static final ABProp USERNAME_GROUP_MUTATION_ENABLED = new ABProp(16148, "false", "false");

    /**
     * The {@code payments_br_pix_on_web} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: payments_br_pix_on_web:[16156,"bool",!1,!1]
     */
    public static final ABProp PAYMENTS_BR_PIX_ON_WEB = new ABProp(16156, "false", "false");

    /**
     * The {@code privacy_settings_profile_lid_migration_enable} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: privacy_settings_profile_lid_migration_enable:[16161,"bool",!1,!1]
     */
    public static final ABProp PRIVACY_SETTINGS_PROFILE_LID_MIGRATION_ENABLE = new ABProp(16161, "false", "false");

    /**
     * The {@code channels_is_multi_admin_lid_migration_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_is_multi_admin_lid_migration_enabled:[16193,"bool",!1,!0]
     */
    public static final ABProp CHANNELS_IS_MULTI_ADMIN_LID_MIGRATION_ENABLED = new ABProp(16193, "false", "true");

    /**
     * The {@code privacy_settings_about_lid_migration_enable} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: privacy_settings_about_lid_migration_enable:[16195,"bool",!1,!1]
     */
    public static final ABProp PRIVACY_SETTINGS_ABOUT_LID_MIGRATION_ENABLE = new ABProp(16195, "false", "false");

    /**
     * The {@code attach_transport_rtx} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: attach_transport_rtx:[16201,"bool",!1,!0]
     */
    public static final ABProp ATTACH_TRANSPORT_RTX = new ABProp(16201, "false", "true");

    /**
     * The {@code ai_search_bar_2025_redesign_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_search_bar_2025_redesign_enabled:[16208,"bool",!1,!1]
     */
    public static final ABProp AI_SEARCH_BAR_2025_REDESIGN_ENABLED = new ABProp(16208, "false", "false");

    /**
     * The {@code web_mac_beta_upsell} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_mac_beta_upsell:[16223,"bool",!1,!0]
     */
    public static final ABProp WEB_MAC_BETA_UPSELL = new ABProp(16223, "false", "true");

    /**
     * The {@code schedule_call_show_join_button_time_interval_mins} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: schedule_call_show_join_button_time_interval_mins:[16253,"int",5,5]
     */
    public static final ABProp SCHEDULE_CALL_SHOW_JOIN_BUTTON_TIME_INTERVAL_MINS = new ABProp(16253, "5", "5");

    /**
     * The {@code schedule_call_show_upcoming_banner_time_interval_mins} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: schedule_call_show_upcoming_banner_time_interval_mins:[16254,"int",1440,1440]
     */
    public static final ABProp SCHEDULE_CALL_SHOW_UPCOMING_BANNER_TIME_INTERVAL_MINS = new ABProp(16254, "1440", "1440");

    /**
     * The {@code privacy_settings_group_add_lid_migration_enable} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: privacy_settings_group_add_lid_migration_enable:[16274,"bool",!1,!1]
     */
    public static final ABProp PRIVACY_SETTINGS_GROUP_ADD_LID_MIGRATION_ENABLE = new ABProp(16274, "false", "false");

    /**
     * The {@code privacy_settings_presence_lid_migration_enable} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: privacy_settings_presence_lid_migration_enable:[16275,"bool",!1,!1]
     */
    public static final ABProp PRIVACY_SETTINGS_PRESENCE_LID_MIGRATION_ENABLE = new ABProp(16275, "false", "false");

    /**
     * The {@code enable_peer_snapshot_recovery} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_peer_snapshot_recovery:[16329,"bool",!1,!1]
     */
    public static final ABProp ENABLE_PEER_SNAPSHOT_RECOVERY = new ABProp(16329, "false", "false");

    /**
     * The {@code group_history_bump_message_id} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: group_history_bump_message_id:[16346,"int",200,200]
     */
    public static final ABProp GROUP_HISTORY_BUMP_MESSAGE_ID = new ABProp(16346, "200", "200");

    /**
     * The {@code limit_sharing_update_enabled_web} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: limit_sharing_update_enabled_web:[16376,"bool",!1,!1]
     */
    public static final ABProp LIMIT_SHARING_UPDATE_ENABLED_WEB = new ABProp(16376, "false", "false");

    /**
     * The {@code biz_ai_priority_list_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: biz_ai_priority_list_enabled:[16420,"bool",!1,!0]
     */
    public static final ABProp BIZ_AI_PRIORITY_LIST_ENABLED = new ABProp(16420, "false", "true");

    /**
     * The {@code voip_stack_incoming_message_ownership_transfer} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: voip_stack_incoming_message_ownership_transfer:[16481,"bool",!1,!1]
     */
    public static final ABProp VOIP_STACK_INCOMING_MESSAGE_OWNERSHIP_TRANSFER = new ABProp(16481, "false", "false");

    /**
     * The {@code channels_video_play_logging_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_video_play_logging_enabled:[16491,"bool",!1,!0]
     */
    public static final ABProp CHANNELS_VIDEO_PLAY_LOGGING_ENABLED = new ABProp(16491, "false", "true");

    /**
     * The {@code ai_rewrite_in_expression_tray_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_rewrite_in_expression_tray_enabled:[16510,"bool",!1,!1]
     */
    public static final ABProp AI_REWRITE_IN_EXPRESSION_TRAY_ENABLED = new ABProp(16510, "false", "false");

    /**
     * The {@code rich_order_status_wa_web} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: rich_order_status_wa_web:[16534,"bool",!1,!1]
     */
    public static final ABProp RICH_ORDER_STATUS_WA_WEB = new ABProp(16534, "false", "false");

    /**
     * The {@code member_name_tag_db_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: member_name_tag_db_enabled:[16551,"bool",!0,!0]
     */
    public static final ABProp MEMBER_NAME_TAG_DB_ENABLED = new ABProp(16551, "true", "true");

    /**
     * The {@code pnh_thread_promotion_to_general_lid} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: pnh_thread_promotion_to_general_lid:[16632,"bool",!1,!1]
     */
    public static final ABProp PNH_THREAD_PROMOTION_TO_GENERAL_LID = new ABProp(16632, "false", "false");

    /**
     * The {@code ai_rich_response_forward_sending_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_rich_response_forward_sending_enabled:[16681,"bool",!1,!0]
     */
    public static final ABProp AI_RICH_RESPONSE_FORWARD_SENDING_ENABLED = new ABProp(16681, "false", "true");

    /**
     * The {@code ai_rich_response_forward_receiving_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_rich_response_forward_receiving_enabled:[16682,"bool",!1,!0]
     */
    public static final ABProp AI_RICH_RESPONSE_FORWARD_RECEIVING_ENABLED = new ABProp(16682, "false", "true");

    /**
     * The {@code mm_signal_sharing_verification_system_lid_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: mm_signal_sharing_verification_system_lid_enabled:[16727,"bool",!0,!0]
     */
    public static final ABProp MM_SIGNAL_SHARING_VERIFICATION_SYSTEM_LID_ENABLED = new ABProp(16727, "true", "true");

    /**
     * The {@code channels_invite_contacts_to_follow_producer_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_invite_contacts_to_follow_producer_enabled:[16789,"bool",!1,!1]
     */
    public static final ABProp CHANNELS_INVITE_CONTACTS_TO_FOLLOW_PRODUCER_ENABLED = new ABProp(16789, "false", "false");

    /**
     * The {@code channels_invite_contacts_to_follow_consumer_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_invite_contacts_to_follow_consumer_enabled:[16790,"bool",!1,!1]
     */
    public static final ABProp CHANNELS_INVITE_CONTACTS_TO_FOLLOW_CONSUMER_ENABLED = new ABProp(16790, "false", "false");

    /**
     * The {@code reactions_alignment_for_transparent_messages_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: reactions_alignment_for_transparent_messages_enabled:[16792,"bool",!1,!0]
     */
    public static final ABProp REACTIONS_ALIGNMENT_FOR_TRANSPARENT_MESSAGES_ENABLED = new ABProp(16792, "false", "true");

    /**
     * The {@code wa_web_console_log_level} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_console_log_level:[16806,"int",3,1]
     */
    public static final ABProp WA_WEB_CONSOLE_LOG_LEVEL = new ABProp(16806, "3", "1");

    /**
     * The {@code web_pdf_thumbnail_size_in_bytes} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_pdf_thumbnail_size_in_bytes:[16834,"int",1300,1300]
     */
    public static final ABProp WEB_PDF_THUMBNAIL_SIZE_IN_BYTES = new ABProp(16834, "1300", "1300");

    /**
     * The {@code channels_sticker_forwarded_attribution_ui_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_sticker_forwarded_attribution_ui_enabled:[16856,"bool",!1,!0]
     */
    public static final ABProp CHANNELS_STICKER_FORWARDED_ATTRIBUTION_UI_ENABLED = new ABProp(16856, "false", "true");

    /**
     * The {@code channels_sticker_pack_forwarded_attribution_ui_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_sticker_pack_forwarded_attribution_ui_enabled:[16858,"bool",!1,!0]
     */
    public static final ABProp CHANNELS_STICKER_PACK_FORWARDED_ATTRIBUTION_UI_ENABLED = new ABProp(16858, "false", "true");

    /**
     * The {@code payment_links_trust_signals_metatag_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: payment_links_trust_signals_metatag_enabled:[16866,"bool",!1,!1]
     */
    public static final ABProp PAYMENT_LINKS_TRUST_SIGNALS_METATAG_ENABLED = new ABProp(16866, "false", "false");

    /**
     * The {@code ai_rewrite_stack_undo_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_rewrite_stack_undo_enabled:[16943,"bool",!1,!1]
     */
    public static final ABProp AI_REWRITE_STACK_UNDO_ENABLED = new ABProp(16943, "false", "false");

    /**
     * The {@code mm_opt_out_lid_migration_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: mm_opt_out_lid_migration_enabled:[16952,"bool",!1,!1]
     */
    public static final ABProp MM_OPT_OUT_LID_MIGRATION_ENABLED = new ABProp(16952, "false", "false");

    /**
     * The {@code web_offline_resume_wait_for_ping_timeout_seconds} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_offline_resume_wait_for_ping_timeout_seconds:[16956,"int",10,10]
     */
    public static final ABProp WEB_OFFLINE_RESUME_WAIT_FOR_PING_TIMEOUT_SECONDS = new ABProp(16956, "10", "10");

    /**
     * The {@code view_replies_with_threadid_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: view_replies_with_threadid_enabled:[16998,"bool",!1,!1]
     */
    public static final ABProp VIEW_REPLIES_WITH_THREADID_ENABLED = new ABProp(16998, "false", "false");

    /**
     * The {@code status_allow_forwarding_to_status_on_web} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: status_allow_forwarding_to_status_on_web:[17071,"bool",!1,!1]
     */
    public static final ABProp STATUS_ALLOW_FORWARDING_TO_STATUS_ON_WEB = new ABProp(17071, "false", "false");

    /**
     * The {@code channels_emoji_forwarded_attribution_ui_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_emoji_forwarded_attribution_ui_enabled:[17081,"bool",!1,!1]
     */
    public static final ABProp CHANNELS_EMOJI_FORWARDED_ATTRIBUTION_UI_ENABLED = new ABProp(17081, "false", "false");

    /**
     * The {@code biz_ai_web_ai_hub_tap_cta_show_alert} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: biz_ai_web_ai_hub_tap_cta_show_alert:[17093,"bool",!1,!1]
     */
    public static final ABProp BIZ_AI_WEB_AI_HUB_TAP_CTA_SHOW_ALERT = new ABProp(17093, "false", "false");

    /**
     * The {@code payment_links_trust_signals_metatag_psp_list} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: payment_links_trust_signals_metatag_psp_list:[17162,"string","{\"psp\":[\"mercadopago\"]} ","{\"psp\":[\"mercadopago\"]} "]
     */
    public static final ABProp PAYMENT_LINKS_TRUST_SIGNALS_METATAG_PSP_LIST = new ABProp(17162, "{\"psp\":[\"mercadopago\"]} ", "{\"psp\":[\"mercadopago\"]} ");

    /**
     * The {@code history_sync_on_demand_companion} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: history_sync_on_demand_companion:[17198,"bool",!1,!0]
     */
    public static final ABProp HISTORY_SYNC_ON_DEMAND_COMPANION = new ABProp(17198, "false", "true");

    /**
     * The {@code ai_ugc_not_an_expert_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_ugc_not_an_expert_enabled:[17285,"bool",!1,!1]
     */
    public static final ABProp AI_UGC_NOT_AN_EXPERT_ENABLED = new ABProp(17285, "false", "false");

    /**
     * The {@code payment_links_trust_signals_other_metatags_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: payment_links_trust_signals_other_metatags_enabled:[17355,"bool",!1,!1]
     */
    public static final ABProp PAYMENT_LINKS_TRUST_SIGNALS_OTHER_METATAGS_ENABLED = new ABProp(17355, "false", "false");

    /**
     * The {@code ai_rich_response_side_by_side_survey_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_rich_response_side_by_side_survey_enabled:[17408,"bool",!1,!1]
     */
    public static final ABProp AI_RICH_RESPONSE_SIDE_BY_SIDE_SURVEY_ENABLED = new ABProp(17408, "false", "false");

    /**
     * The {@code channels_question_follower_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_question_follower_enabled:[17425,"bool",!1,!0]
     */
    public static final ABProp CHANNELS_QUESTION_FOLLOWER_ENABLED = new ABProp(17425, "false", "true");

    /**
     * The {@code channels_question_admin_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_question_admin_enabled:[17426,"bool",!1,!0]
     */
    public static final ABProp CHANNELS_QUESTION_ADMIN_ENABLED = new ABProp(17426, "false", "true");

    /**
     * The {@code smb_business_broadcast_import_contact} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_business_broadcast_import_contact:[17433,"bool",!1,!1]
     */
    public static final ABProp SMB_BUSINESS_BROADCAST_IMPORT_CONTACT = new ABProp(17433, "false", "false");

    /**
     * The {@code wa_web_hybrid_simple_chat_conversation_context_menu_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_hybrid_simple_chat_conversation_context_menu_enabled:[17479,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_HYBRID_SIMPLE_CHAT_CONVERSATION_CONTEXT_MENU_ENABLED = new ABProp(17479, "false", "false");

    /**
     * The {@code web_rating_and_review_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_rating_and_review_enabled:[17540,"bool",!1,!1]
     */
    public static final ABProp WEB_RATING_AND_REVIEW_ENABLED = new ABProp(17540, "false", "false");

    /**
     * The {@code ctwa_suppress_message_via_ad_spam_web} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ctwa_suppress_message_via_ad_spam_web:[17580,"bool",!1,!1]
     */
    public static final ABProp CTWA_SUPPRESS_MESSAGE_VIA_AD_SPAM_WEB = new ABProp(17580, "false", "false");

    /**
     * The {@code channels_questions_integrity_m1_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_questions_integrity_m1_enabled:[17600,"bool",!1,!0]
     */
    public static final ABProp CHANNELS_QUESTIONS_INTEGRITY_M1_ENABLED = new ABProp(17600, "false", "true");

    /**
     * The {@code username_contact_syncd_support_enable} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: username_contact_syncd_support_enable:[17614,"bool",!1,!1]
     */
    public static final ABProp USERNAME_CONTACT_SYNCD_SUPPORT_ENABLE = new ABProp(17614, "false", "false");

    /**
     * The {@code mm_data_sharing_disclosure_on_chat_open_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: mm_data_sharing_disclosure_on_chat_open_enabled:[17630,"bool",!1,!1]
     */
    public static final ABProp MM_DATA_SHARING_DISCLOSURE_ON_CHAT_OPEN_ENABLED = new ABProp(17630, "false", "false");

    /**
     * The {@code hybrid_font_size_dropdown} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: hybrid_font_size_dropdown:[17637,"bool",!1,!0]
     */
    public static final ABProp HYBRID_FONT_SIZE_DROPDOWN = new ABProp(17637, "false", "true");

    /**
     * The {@code wa_web_hybrid_context_menu_reactions_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_hybrid_context_menu_reactions_enabled:[17650,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_HYBRID_CONTEXT_MENU_REACTIONS_ENABLED = new ABProp(17650, "false", "false");

    /**
     * The {@code calls_tab_username_global_search_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: calls_tab_username_global_search_enabled:[17698,"bool",!1,!1]
     */
    public static final ABProp CALLS_TAB_USERNAME_GLOBAL_SEARCH_ENABLED = new ABProp(17698, "false", "false");

    /**
     * The {@code channels_open_qpl_user_rid_logging_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_open_qpl_user_rid_logging_enabled:[17712,"bool",!1,!1]
     */
    public static final ABProp CHANNELS_OPEN_QPL_USER_RID_LOGGING_ENABLED = new ABProp(17712, "false", "false");

    /**
     * The {@code hybrid_nux_beta_50_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: hybrid_nux_beta_50_enabled:[17717,"bool",!1,!1]
     */
    public static final ABProp HYBRID_NUX_BETA_50_ENABLED = new ABProp(17717, "false", "false");

    /**
     * The {@code web_calls_tab_empty_state_buttons} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_calls_tab_empty_state_buttons:[17724,"bool",!1,!0]
     */
    public static final ABProp WEB_CALLS_TAB_EMPTY_STATE_BUTTONS = new ABProp(17724, "false", "true");

    /**
     * The {@code enable_calling_phone_number_privacy} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_calling_phone_number_privacy:[17731,"bool",!1,!1]
     */
    public static final ABProp ENABLE_CALLING_PHONE_NUMBER_PRIVACY = new ABProp(17731, "false", "false");

    /**
     * The {@code smba_business_broadcast_genai_text} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smba_business_broadcast_genai_text:[17743,"bool",!1,!1]
     */
    public static final ABProp SMBA_BUSINESS_BROADCAST_GENAI_TEXT = new ABProp(17743, "false", "false");

    /**
     * The {@code message_edit_to_message_secret_receiver_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: message_edit_to_message_secret_receiver_enabled:[17811,"bool",!1,!0]
     */
    public static final ABProp MESSAGE_EDIT_TO_MESSAGE_SECRET_RECEIVER_ENABLED = new ABProp(17811, "false", "true");

    /**
     * The {@code wa_individual_new_chat_msg_capping_limit} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_individual_new_chat_msg_capping_limit:[17845,"int",0,0]
     */
    public static final ABProp WA_INDIVIDUAL_NEW_CHAT_MSG_CAPPING_LIMIT = new ABProp(17845, "0", "0");

    /**
     * The {@code smba_business_broadcast_recipient_limit} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smba_business_broadcast_recipient_limit:[17937,"int",-1,-1]
     */
    public static final ABProp SMBA_BUSINESS_BROADCAST_RECIPIENT_LIMIT = new ABProp(17937, "-1", "-1");

    /**
     * The {@code smoothie_performance_msg_send} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smoothie_performance_msg_send:[17942,"bool",!0,!0]
     */
    public static final ABProp SMOOTHIE_PERFORMANCE_MSG_SEND = new ABProp(17942, "true", "true");

    /**
     * The {@code calling_rust_migration_bitmap} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: calling_rust_migration_bitmap:[17954,"int",0,0]
     */
    public static final ABProp CALLING_RUST_MIGRATION_BITMAP = new ABProp(17954, "0", "0");

    /**
     * The {@code meta_ai_in_app_survey_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: meta_ai_in_app_survey_enabled:[17956,"bool",!1,!0]
     */
    public static final ABProp META_AI_IN_APP_SURVEY_ENABLED = new ABProp(17956, "false", "true");

    /**
     * The {@code ai_metabot_document_upload_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_metabot_document_upload_enabled:[17957,"string","",""]
     */
    public static final ABProp AI_METABOT_DOCUMENT_UPLOAD_ENABLED = new ABProp(17957, "", "");

    /**
     * The {@code advanced_chat_privacy_content_update_july_25} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: advanced_chat_privacy_content_update_july_25:[18025,"bool",!1,!0]
     */
    public static final ABProp ADVANCED_CHAT_PRIVACY_CONTENT_UPDATE_JULY_25 = new ABProp(18025, "false", "true");

    /**
     * The {@code coex_calling_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: coex_calling_enabled:[18047,"bool",!1,!0]
     */
    public static final ABProp COEX_CALLING_ENABLED = new ABProp(18047, "false", "true");

    /**
     * The {@code hybrid_incremental_zooming_simple_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: hybrid_incremental_zooming_simple_enabled:[18080,"bool",!1,!1]
     */
    public static final ABProp HYBRID_INCREMENTAL_ZOOMING_SIMPLE_ENABLED = new ABProp(18080, "false", "false");

    /**
     * The {@code enable_avatars_on_web_companion} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_avatars_on_web_companion:[18081,"bool",!1,!1]
     */
    public static final ABProp ENABLE_AVATARS_ON_WEB_COMPANION = new ABProp(18081, "false", "false");

    /**
     * The {@code pushname_blocklist_starting_with_at} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: pushname_blocklist_starting_with_at:[18097,"bool",!1,!1]
     */
    public static final ABProp PUSHNAME_BLOCKLIST_STARTING_WITH_AT = new ABProp(18097, "false", "false");

    /**
     * The {@code internal_group_indicator} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: internal_group_indicator:[18109,"bool",!1,!0]
     */
    public static final ABProp INTERNAL_GROUP_INDICATOR = new ABProp(18109, "false", "true");

    /**
     * The {@code enable_av_downgrade_1on1} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_av_downgrade_1on1:[18165,"bool",!1,!1]
     */
    public static final ABProp ENABLE_AV_DOWNGRADE_1ON1 = new ABProp(18165, "false", "false");

    /**
     * The {@code username_contact_ui_vcard} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: username_contact_ui_vcard:[18204,"bool",!1,!1]
     */
    public static final ABProp USERNAME_CONTACT_UI_VCARD = new ABProp(18204, "false", "false");

    /**
     * The {@code lists_smb_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: lists_smb_enabled:[18229,"bool",!1,!0]
     */
    public static final ABProp LISTS_SMB_ENABLED = new ABProp(18229, "false", "true");

    /**
     * The {@code kmp_syncd_engine_outgoing_processor_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: kmp_syncd_engine_outgoing_processor_enabled:[18234,"bool",!1,!1]
     */
    public static final ABProp KMP_SYNCD_ENGINE_OUTGOING_PROCESSOR_ENABLED = new ABProp(18234, "false", "false");

    /**
     * The {@code username_global_search_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: username_global_search_enabled:[18251,"bool",!1,!1]
     */
    public static final ABProp USERNAME_GLOBAL_SEARCH_ENABLED = new ABProp(18251, "false", "false");

    /**
     * The {@code ai_forward_attribution_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_forward_attribution_enabled:[18286,"bool",!1,!0]
     */
    public static final ABProp AI_FORWARD_ATTRIBUTION_ENABLED = new ABProp(18286, "false", "true");

    /**
     * The {@code status_pog_id_rotation_window_days} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: status_pog_id_rotation_window_days:[18297,"int",-1,-1]
     */
    public static final ABProp STATUS_POG_ID_ROTATION_WINDOW_DAYS = new ABProp(18297, "-1", "-1");

    /**
     * The {@code ai_rewrite_non_streaming_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_rewrite_non_streaming_enabled:[18316,"bool",!1,!1]
     */
    public static final ABProp AI_REWRITE_NON_STREAMING_ENABLED = new ABProp(18316, "false", "false");

    /**
     * The {@code history_sync_on_demand_time_boundary_days_desktops} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: history_sync_on_demand_time_boundary_days_desktops:[18391,"int",1095,1095]
     */
    public static final ABProp HISTORY_SYNC_ON_DEMAND_TIME_BOUNDARY_DAYS_DESKTOPS = new ABProp(18391, "1095", "1095");

    /**
     * The {@code channels_question_reply_receiver_message_types_m1_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_question_reply_receiver_message_types_m1_enabled:[18393,"string","","25"]
     */
    public static final ABProp CHANNELS_QUESTION_REPLY_RECEIVER_MESSAGE_TYPES_M1_ENABLED = new ABProp(18393, "", "25");

    /**
     * The {@code channels_question_reply_sender_message_types_m1_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_question_reply_sender_message_types_m1_enabled:[18394,"string","","22"]
     */
    public static final ABProp CHANNELS_QUESTION_REPLY_SENDER_MESSAGE_TYPES_M1_ENABLED = new ABProp(18394, "", "22");

    /**
     * The {@code group_history_message_count_limit} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: group_history_message_count_limit:[18405,"int",100,100]
     */
    public static final ABProp GROUP_HISTORY_MESSAGE_COUNT_LIMIT = new ABProp(18405, "100", "100");

    /**
     * The {@code group_history_messages_time_limit_secs} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: group_history_messages_time_limit_secs:[18406,"int",1209600,1209600]
     */
    public static final ABProp GROUP_HISTORY_MESSAGES_TIME_LIMIT_SECS = new ABProp(18406, "1209600", "1209600");

    /**
     * The {@code enable_new_user_action_stanza_for_raise_hand_sender} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_new_user_action_stanza_for_raise_hand_sender:[18489,"bool",!1,!1]
     */
    public static final ABProp ENABLE_NEW_USER_ACTION_STANZA_FOR_RAISE_HAND_SENDER = new ABProp(18489, "false", "false");

    /**
     * The {@code channels_admin_notifications_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_admin_notifications_enabled:[18560,"bool",!1,!1]
     */
    public static final ABProp CHANNELS_ADMIN_NOTIFICATIONS_ENABLED = new ABProp(18560, "false", "false");

    /**
     * The {@code channels_creation_entrypoint_in_directory_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_creation_entrypoint_in_directory_enabled:[18613,"int",0,0]
     */
    public static final ABProp CHANNELS_CREATION_ENTRYPOINT_IN_DIRECTORY_ENABLED = new ABProp(18613, "0", "0");

    /**
     * The {@code web_disable_logs_low_end_device} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_disable_logs_low_end_device:[18660,"bool",!1,!1]
     */
    public static final ABProp WEB_DISABLE_LOGS_LOW_END_DEVICE = new ABProp(18660, "false", "false");

    /**
     * The {@code dialer_pad_for_new_chats} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: dialer_pad_for_new_chats:[18688,"bool",!1,!1]
     */
    public static final ABProp DIALER_PAD_FOR_NEW_CHATS = new ABProp(18688, "false", "false");

    /**
     * The {@code rnr_min_days_user_active} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: rnr_min_days_user_active:[18702,"int",2,2]
     */
    public static final ABProp RNR_MIN_DAYS_USER_ACTIVE = new ABProp(18702, "2", "2");

    /**
     * The {@code rnr_days_cooldown} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: rnr_days_cooldown:[18703,"int",100000,100000]
     */
    public static final ABProp RNR_DAYS_COOLDOWN = new ABProp(18703, "100000", "100000");

    /**
     * The {@code optimized_delivery_block_and_report_entry_points_allowlist_web} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: optimized_delivery_block_and_report_entry_points_allowlist_web:[18736,"string","4,10,12,13,14,15,17,18,24,31,32,33,34,35,36,39,40,45","4,10,12,13,14,15,17,18,24,31,32,33,34,35,36,39,40,45"]
     */
    public static final ABProp OPTIMIZED_DELIVERY_BLOCK_AND_REPORT_ENTRY_POINTS_ALLOWLIST_WEB = new ABProp(18736, "4,10,12,13,14,15,17,18,24,31,32,33,34,35,36,39,40,45", "4,10,12,13,14,15,17,18,24,31,32,33,34,35,36,39,40,45");

    /**
     * The {@code web_rating_and_review_contextual_prompt_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_rating_and_review_contextual_prompt_enabled:[18737,"bool",!1,!1]
     */
    public static final ABProp WEB_RATING_AND_REVIEW_CONTEXTUAL_PROMPT_ENABLED = new ABProp(18737, "false", "false");

    /**
     * The {@code ai_search_experience_web_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_search_experience_web_enabled:[18740,"bool",!1,!0]
     */
    public static final ABProp AI_SEARCH_EXPERIENCE_WEB_ENABLED = new ABProp(18740, "false", "true");

    /**
     * The {@code ai_rich_response_ur_media_grid_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_rich_response_ur_media_grid_enabled:[18746,"bool",!1,!1]
     */
    public static final ABProp AI_RICH_RESPONSE_UR_MEDIA_GRID_ENABLED = new ABProp(18746, "false", "false");

    /**
     * The {@code web_low_end_device_level} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_low_end_device_level:[18747,"int",0,0]
     */
    public static final ABProp WEB_LOW_END_DEVICE_LEVEL = new ABProp(18747, "0", "0");

    /**
     * The {@code snapshot_recovery_max_mutations_count_allowed} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: snapshot_recovery_max_mutations_count_allowed:[18786,"int",2000,2000]
     */
    public static final ABProp SNAPSHOT_RECOVERY_MAX_MUTATIONS_COUNT_ALLOWED = new ABProp(18786, "2000", "2000");

    /**
     * The {@code wa_web_falco_clear_local_storage_queue_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_falco_clear_local_storage_queue_enabled:[18835,"bool",!1,!0]
     */
    public static final ABProp WA_WEB_FALCO_CLEAR_LOCAL_STORAGE_QUEUE_ENABLED = new ABProp(18835, "false", "true");

    /**
     * The {@code ai_migrate_away_from_inline_tos_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_migrate_away_from_inline_tos_enabled:[18843,"bool",!1,!1]
     */
    public static final ABProp AI_MIGRATE_AWAY_FROM_INLINE_TOS_ENABLED = new ABProp(18843, "false", "false");

    /**
     * The {@code ctwa_native_ads_creation_web_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ctwa_native_ads_creation_web_enabled:[18857,"bool",!1,!1]
     */
    public static final ABProp CTWA_NATIVE_ADS_CREATION_WEB_ENABLED = new ABProp(18857, "false", "false");

    /**
     * The {@code custom_notification_tones} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: custom_notification_tones:[18884,"bool",!1,!1]
     */
    public static final ABProp CUSTOM_NOTIFICATION_TONES = new ABProp(18884, "false", "false");

    /**
     * The {@code channels_creation_entrypoint_in_updates_tab_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_creation_entrypoint_in_updates_tab_enabled:[18925,"int",0,1]
     */
    public static final ABProp CHANNELS_CREATION_ENTRYPOINT_IN_UPDATES_TAB_ENABLED = new ABProp(18925, "0", "1");

    /**
     * The {@code username_check_debounce_in_ms} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: username_check_debounce_in_ms:[18975,"int",600,600]
     */
    public static final ABProp USERNAME_CHECK_DEBOUNCE_IN_MS = new ABProp(18975, "600", "600");

    /**
     * The {@code channels_question_fetch_responses_page_size} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_question_fetch_responses_page_size:[18984,"int",30,30]
     */
    public static final ABProp CHANNELS_QUESTION_FETCH_RESPONSES_PAGE_SIZE = new ABProp(18984, "30", "30");

    /**
     * The {@code channels_question_forward_message_types_chat_m1_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_question_forward_message_types_chat_m1_enabled:[18988,"string","","22"]
     */
    public static final ABProp CHANNELS_QUESTION_FORWARD_MESSAGE_TYPES_CHAT_M1_ENABLED = new ABProp(18988, "", "22");

    /**
     * The {@code smoothie_performance_resize_followup} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smoothie_performance_resize_followup:[18992,"bool",!1,!1]
     */
    public static final ABProp SMOOTHIE_PERFORMANCE_RESIZE_FOLLOWUP = new ABProp(18992, "false", "false");

    /**
     * The {@code coex_edit_msg_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: coex_edit_msg_enabled:[19039,"bool",!1,!0]
     */
    public static final ABProp COEX_EDIT_MSG_ENABLED = new ABProp(19039, "false", "true");

    /**
     * The {@code channels_reply_forward_message_types_chat_m1_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_reply_forward_message_types_chat_m1_enabled:[19053,"string","","25"]
     */
    public static final ABProp CHANNELS_REPLY_FORWARD_MESSAGE_TYPES_CHAT_M1_ENABLED = new ABProp(19053, "", "25");

    /**
     * The {@code utility_order_status_logging_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: utility_order_status_logging_enabled:[19059,"bool",!1,!1]
     */
    public static final ABProp UTILITY_ORDER_STATUS_LOGGING_ENABLED = new ABProp(19059, "false", "false");

    /**
     * The {@code wa_web_history_sync_dynamic_throttling} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_history_sync_dynamic_throttling:[19110,"bool",!0,!0]
     */
    public static final ABProp WA_WEB_HISTORY_SYNC_DYNAMIC_THROTTLING = new ABProp(19110, "true", "true");

    /**
     * The {@code public_bug_reporting_sidebar} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: public_bug_reporting_sidebar:[19124,"bool",!1,!1]
     */
    public static final ABProp PUBLIC_BUG_REPORTING_SIDEBAR = new ABProp(19124, "false", "false");

    /**
     * The {@code web_notifications_banner_variant} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_notifications_banner_variant:[19168,"int",0,0]
     */
    public static final ABProp WEB_NOTIFICATIONS_BANNER_VARIANT = new ABProp(19168, "0", "0");

    /**
     * The {@code dm_receiver_allowed_values} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: dm_receiver_allowed_values:[19232,"string","{\"timers\": [0, 86400, 604800, 7776000]}","{\"timers\": [0, 86400, 604800, 7776000]}"]
     */
    public static final ABProp DM_RECEIVER_ALLOWED_VALUES = new ABProp(19232, "{\"timers\": [0, 86400, 604800, 7776000]}", "{\"timers\": [0, 86400, 604800, 7776000]}");

    /**
     * The {@code payments_br_pix_web_attachment_tray} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: payments_br_pix_web_attachment_tray:[19276,"bool",!1,!1]
     */
    public static final ABProp PAYMENTS_BR_PIX_WEB_ATTACHMENT_TRAY = new ABProp(19276, "false", "false");

    /**
     * The {@code coex_revoke_message_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: coex_revoke_message_enabled:[19285,"bool",!1,!0]
     */
    public static final ABProp COEX_REVOKE_MESSAGE_ENABLED = new ABProp(19285, "false", "true");

    /**
     * The {@code chatlist_show_draft_for_empty_chat} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: chatlist_show_draft_for_empty_chat:[19287,"bool",!1,!1]
     */
    public static final ABProp CHATLIST_SHOW_DRAFT_FOR_EMPTY_CHAT = new ABProp(19287, "false", "false");

    /**
     * The {@code web_anr_throttle_history_sync_db_writes} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_anr_throttle_history_sync_db_writes:[19298,"bool",!1,!1]
     */
    public static final ABProp WEB_ANR_THROTTLE_HISTORY_SYNC_DB_WRITES = new ABProp(19298, "false", "false");

    /**
     * The {@code newsletter_rcat_field_generating_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: newsletter_rcat_field_generating_enabled:[19303,"bool",!1,!0]
     */
    public static final ABProp NEWSLETTER_RCAT_FIELD_GENERATING_ENABLED = new ABProp(19303, "false", "true");

    /**
     * The {@code web_notifications_banner_new_logic_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_notifications_banner_new_logic_enabled:[19399,"bool",!1,!1]
     */
    public static final ABProp WEB_NOTIFICATIONS_BANNER_NEW_LOGIC_ENABLED = new ABProp(19399, "false", "false");

    /**
     * The {@code payment_link_trace_id_logging_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: payment_link_trace_id_logging_enabled:[19440,"bool",!1,!0]
     */
    public static final ABProp PAYMENT_LINK_TRACE_ID_LOGGING_ENABLED = new ABProp(19440, "false", "true");

    /**
     * The {@code hybrid_flytrap_feedback_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: hybrid_flytrap_feedback_enabled:[19495,"bool",!1,!1]
     */
    public static final ABProp HYBRID_FLYTRAP_FEEDBACK_ENABLED = new ABProp(19495, "false", "false");

    /**
     * The {@code desktop_upsell_intro_panel_illustration_variant} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: desktop_upsell_intro_panel_illustration_variant:[19518,"int",0,0]
     */
    public static final ABProp DESKTOP_UPSELL_INTRO_PANEL_ILLUSTRATION_VARIANT = new ABProp(19518, "0", "0");

    /**
     * The {@code channels_qpl_improvements_supported_types} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_qpl_improvements_supported_types:[19589,"string","","1,2"]
     */
    public static final ABProp CHANNELS_QPL_IMPROVEMENTS_SUPPORTED_TYPES = new ABProp(19589, "", "1,2");

    /**
     * The {@code ai_rich_response_forwarding_verification_enabled_v1} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_rich_response_forwarding_verification_enabled_v1:[19590,"string","\"none\"","\"none\""]
     */
    public static final ABProp AI_RICH_RESPONSE_FORWARDING_VERIFICATION_ENABLED_V1 = new ABProp(19590, "\"none\"", "\"none\"");

    /**
     * The {@code voice_ai_conversation_starter_latency_tracking} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: voice_ai_conversation_starter_latency_tracking:[19624,"bool",!1,!1]
     */
    public static final ABProp VOICE_AI_CONVERSATION_STARTER_LATENCY_TRACKING = new ABProp(19624, "false", "false");

    /**
     * The {@code ai_web_forward_flow_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_web_forward_flow_enabled:[19676,"bool",!1,!1]
     */
    public static final ABProp AI_WEB_FORWARD_FLOW_ENABLED = new ABProp(19676, "false", "false");

    /**
     * The {@code lid_status_non_soaked_client_support_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: lid_status_non_soaked_client_support_enabled:[19696,"bool",!0,!0]
     */
    public static final ABProp LID_STATUS_NON_SOAKED_CLIENT_SUPPORT_ENABLED = new ABProp(19696, "true", "true");

    /**
     * The {@code web_hybrid_getters_cache_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_hybrid_getters_cache_enabled:[19700,"bool",!1,!1]
     */
    public static final ABProp WEB_HYBRID_GETTERS_CACHE_ENABLED = new ABProp(19700, "false", "false");

    /**
     * The {@code ctwa_per_customer_data_sharing_controls_do_not_show_msg_until_chosen} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ctwa_per_customer_data_sharing_controls_do_not_show_msg_until_chosen:[19763,"bool",!1,!0]
     */
    public static final ABProp CTWA_PER_CUSTOMER_DATA_SHARING_CONTROLS_DO_NOT_SHOW_MSG_UNTIL_CHOSEN = new ABProp(19763, "false", "true");

    /**
     * The {@code channels_quiz_sending_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_quiz_sending_enabled:[19777,"bool",!1,!1]
     */
    public static final ABProp CHANNELS_QUIZ_SENDING_ENABLED = new ABProp(19777, "false", "false");

    /**
     * The {@code channels_quiz_receiving_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_quiz_receiving_enabled:[19778,"bool",!1,!1]
     */
    public static final ABProp CHANNELS_QUIZ_RECEIVING_ENABLED = new ABProp(19778, "false", "false");

    /**
     * The {@code ai_metabot_document_upload_size_limit_mb} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_metabot_document_upload_size_limit_mb:[19823,"int",40,40]
     */
    public static final ABProp AI_METABOT_DOCUMENT_UPLOAD_SIZE_LIMIT_MB = new ABProp(19823, "40", "40");

    /**
     * The {@code web_enable_improved_bulk_merge} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_enable_improved_bulk_merge:[19854,"bool",!1,!0]
     */
    public static final ABProp WEB_ENABLE_IMPROVED_BULK_MERGE = new ABProp(19854, "false", "true");

    /**
     * The {@code view_replies_entry_point} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: view_replies_entry_point:[19860,"int",0,0]
     */
    public static final ABProp VIEW_REPLIES_ENTRY_POINT = new ABProp(19860, "0", "0");

    /**
     * The {@code newsletter_forward_counter_ui_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: newsletter_forward_counter_ui_enabled:[19888,"int",0,0]
     */
    public static final ABProp NEWSLETTER_FORWARD_COUNTER_UI_ENABLED = new ABProp(19888, "0", "0");

    /**
     * The {@code newsletter_forward_counter_infra_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: newsletter_forward_counter_infra_enabled:[19889,"bool",!1,!1]
     */
    public static final ABProp NEWSLETTER_FORWARD_COUNTER_INFRA_ENABLED = new ABProp(19889, "false", "false");

    /**
     * The {@code enable_fmx_logging} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_fmx_logging:[19893,"bool",!1,!0]
     */
    public static final ABProp ENABLE_FMX_LOGGING = new ABProp(19893, "false", "true");

    /**
     * The {@code enable_rate_app_prompt} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_rate_app_prompt:[19894,"bool",!1,!1]
     */
    public static final ABProp ENABLE_RATE_APP_PROMPT = new ABProp(19894, "false", "false");

    /**
     * The {@code enable_hybrid_video_transcoding} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_hybrid_video_transcoding:[19895,"bool",!1,!1]
     */
    public static final ABProp ENABLE_HYBRID_VIDEO_TRANSCODING = new ABProp(19895, "false", "false");

    /**
     * The {@code web_channel_video_server_transcode_upload} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_channel_video_server_transcode_upload:[19920,"bool",!1,!1]
     */
    public static final ABProp WEB_CHANNEL_VIDEO_SERVER_TRANSCODE_UPLOAD = new ABProp(19920, "false", "false");

    /**
     * The {@code smb_core_biz_profile_ux_refreshed} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_core_biz_profile_ux_refreshed:[19929,"bool",!1,!1]
     */
    public static final ABProp SMB_CORE_BIZ_PROFILE_UX_REFRESHED = new ABProp(19929, "false", "false");

    /**
     * The {@code ctwa_web_custom_label_signals_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ctwa_web_custom_label_signals_enabled:[19985,"bool",!1,!1]
     */
    public static final ABProp CTWA_WEB_CUSTOM_LABEL_SIGNALS_ENABLED = new ABProp(19985, "false", "false");

    /**
     * The {@code ai_metabot_document_upload_page_count_limit} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_metabot_document_upload_page_count_limit:[19987,"int",100000,100000]
     */
    public static final ABProp AI_METABOT_DOCUMENT_UPLOAD_PAGE_COUNT_LIMIT = new ABProp(19987, "100000", "100000");

    /**
     * The {@code channels_question_response_rate_limit_max_count_in_client_ui} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_question_response_rate_limit_max_count_in_client_ui:[19989,"int",5,5]
     */
    public static final ABProp CHANNELS_QUESTION_RESPONSE_RATE_LIMIT_MAX_COUNT_IN_CLIENT_UI = new ABProp(19989, "5", "5");

    /**
     * The {@code web_fix_duplicated_lids_history_sync} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_fix_duplicated_lids_history_sync:[19994,"bool",!1,!1]
     */
    public static final ABProp WEB_FIX_DUPLICATED_LIDS_HISTORY_SYNC = new ABProp(19994, "false", "false");

    /**
     * The {@code ai_ugc_hide_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_ugc_hide_enabled:[20041,"bool",!1,!1]
     */
    public static final ABProp AI_UGC_HIDE_ENABLED = new ABProp(20041, "false", "false");

    /**
     * The {@code wa_web_app_lock_upsell} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_app_lock_upsell:[20064,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_APP_LOCK_UPSELL = new ABProp(20064, "false", "false");

    /**
     * The {@code enable_hybrid_video_transcoding_for_valid_mp4} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_hybrid_video_transcoding_for_valid_mp4:[20070,"bool",!1,!1]
     */
    public static final ABProp ENABLE_HYBRID_VIDEO_TRANSCODING_FOR_VALID_MP4 = new ABProp(20070, "false", "false");

    /**
     * The {@code biz_ai_agent_thread_status_history_sync_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: biz_ai_agent_thread_status_history_sync_enabled:[20099,"bool",!1,!0]
     */
    public static final ABProp BIZ_AI_AGENT_THREAD_STATUS_HISTORY_SYNC_ENABLED = new ABProp(20099, "false", "true");

    /**
     * The {@code channels_sticker_pack_rendering} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_sticker_pack_rendering:[20182,"bool",!1,!0]
     */
    public static final ABProp CHANNELS_STICKER_PACK_RENDERING = new ABProp(20182, "false", "true");

    /**
     * The {@code channel_sticker_pack_forwarding} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channel_sticker_pack_forwarding:[20212,"bool",!1,!1]
     */
    public static final ABProp CHANNEL_STICKER_PACK_FORWARDING = new ABProp(20212, "false", "false");

    /**
     * The {@code ctwa_smb_detected_outcome_lists_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ctwa_smb_detected_outcome_lists_enabled:[20220,"bool",!1,!0]
     */
    public static final ABProp CTWA_SMB_DETECTED_OUTCOME_LISTS_ENABLED = new ABProp(20220, "false", "true");

    /**
     * The {@code channels_music_receiver_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_music_receiver_enabled:[20266,"bool",!1,!0]
     */
    public static final ABProp CHANNELS_MUSIC_RECEIVER_ENABLED = new ABProp(20266, "false", "true");

    /**
     * The {@code admin_only_mention_everyone_group_size} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: admin_only_mention_everyone_group_size:[20354,"int",33,33]
     */
    public static final ABProp ADMIN_ONLY_MENTION_EVERYONE_GROUP_SIZE = new ABProp(20354, "33", "33");

    /**
     * The {@code web_use_kaleidoscope_media_check_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_use_kaleidoscope_media_check_enabled:[20375,"bool",!1,!1]
     */
    public static final ABProp WEB_USE_KALEIDOSCOPE_MEDIA_CHECK_ENABLED = new ABProp(20375, "false", "false");

    /**
     * The {@code mm_user_controls_entry_points_update_m1_icon} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: mm_user_controls_entry_points_update_m1_icon:[20388,"bool",!1,!0]
     */
    public static final ABProp MM_USER_CONTROLS_ENTRY_POINTS_UPDATE_M1_ICON = new ABProp(20388, "false", "true");

    /**
     * The {@code ctwa_native_ads_creation_web_hawk_tool_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ctwa_native_ads_creation_web_hawk_tool_enabled:[20442,"bool",!1,!1]
     */
    public static final ABProp CTWA_NATIVE_ADS_CREATION_WEB_HAWK_TOOL_ENABLED = new ABProp(20442, "false", "false");

    /**
     * The {@code smba_business_broadcast_genai_custom_user_prompt_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smba_business_broadcast_genai_custom_user_prompt_enabled:[20464,"bool",!1,!0]
     */
    public static final ABProp SMBA_BUSINESS_BROADCAST_GENAI_CUSTOM_USER_PROMPT_ENABLED = new ABProp(20464, "false", "true");

    /**
     * The {@code ai_web_meta_ai_image_input_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_web_meta_ai_image_input_enabled:[20522,"bool",!1,!1]
     */
    public static final ABProp AI_WEB_META_AI_IMAGE_INPUT_ENABLED = new ABProp(20522, "false", "false");

    /**
     * The {@code pending_group_requests_persistent_banner} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: pending_group_requests_persistent_banner:[20545,"bool",!1,!0]
     */
    public static final ABProp PENDING_GROUP_REQUESTS_PERSISTENT_BANNER = new ABProp(20545, "false", "true");

    /**
     * The {@code channel_enforcement_logging_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channel_enforcement_logging_enabled:[20549,"bool",!1,!0]
     */
    public static final ABProp CHANNEL_ENFORCEMENT_LOGGING_ENABLED = new ABProp(20549, "false", "true");

    /**
     * The {@code channels_pinning_nudge_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_pinning_nudge_enabled:[20551,"bool",!1,!1]
     */
    public static final ABProp CHANNELS_PINNING_NUDGE_ENABLED = new ABProp(20551, "false", "false");

    /**
     * The {@code wa_webtp_use_thumbnail_renderer} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_webtp_use_thumbnail_renderer:[20555,"bool",!1,!1]
     */
    public static final ABProp WA_WEBTP_USE_THUMBNAIL_RENDERER = new ABProp(20555, "false", "false");

    /**
     * The {@code web_voip_video_renderer} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_voip_video_renderer:[20573,"int",0,0]
     */
    public static final ABProp WEB_VOIP_VIDEO_RENDERER = new ABProp(20573, "0", "0");

    /**
     * The {@code ai_web_meta_ai_pdf_document_input_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_web_meta_ai_pdf_document_input_enabled:[20581,"bool",!1,!1]
     */
    public static final ABProp AI_WEB_META_AI_PDF_DOCUMENT_INPUT_ENABLED = new ABProp(20581, "false", "false");

    /**
     * The {@code wa_individual_new_chat_msg_latest_rampup_date} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_individual_new_chat_msg_latest_rampup_date:[20601,"int",0,0]
     */
    public static final ABProp WA_INDIVIDUAL_NEW_CHAT_MSG_LATEST_RAMPUP_DATE = new ABProp(20601, "0", "0");

    /**
     * The {@code wa_webtp_use_pdf_renderer} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_webtp_use_pdf_renderer:[20607,"bool",!1,!1]
     */
    public static final ABProp WA_WEBTP_USE_PDF_RENDERER = new ABProp(20607, "false", "false");

    /**
     * The {@code wa_web_clear_selected_chats_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_clear_selected_chats_enabled:[20626,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_CLEAR_SELECTED_CHATS_ENABLED = new ABProp(20626, "false", "false");

    /**
     * The {@code ai_chat_threads_infra_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_chat_threads_infra_enabled:[20652,"bool",!1,!1]
     */
    public static final ABProp AI_CHAT_THREADS_INFRA_ENABLED = new ABProp(20652, "false", "false");

    /**
     * The {@code ctwa_native_ads_creation_web_targeting_modal_hawk_tool_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ctwa_native_ads_creation_web_targeting_modal_hawk_tool_enabled:[20731,"bool",!1,!1]
     */
    public static final ABProp CTWA_NATIVE_ADS_CREATION_WEB_TARGETING_MODAL_HAWK_TOOL_ENABLED = new ABProp(20731, "false", "false");

    /**
     * The {@code profile_scraping_privacy_token_in_about_usync} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: profile_scraping_privacy_token_in_about_usync:[20798,"bool",!1,!0]
     */
    public static final ABProp PROFILE_SCRAPING_PRIVACY_TOKEN_IN_ABOUT_USYNC = new ABProp(20798, "false", "true");

    /**
     * The {@code favorite_sticker_sync_after_pairing_enabled_web} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: favorite_sticker_sync_after_pairing_enabled_web:[20815,"bool",!1,!1]
     */
    public static final ABProp FAVORITE_STICKER_SYNC_AFTER_PAIRING_ENABLED_WEB = new ABProp(20815, "false", "false");

    /**
     * The {@code view_replies_is_composer_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: view_replies_is_composer_enabled:[20817,"bool",!0,!0]
     */
    public static final ABProp VIEW_REPLIES_IS_COMPOSER_ENABLED = new ABProp(20817, "true", "true");

    /**
     * The {@code biz_ai_tos_variant} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: biz_ai_tos_variant:[20833,"int",0,0]
     */
    public static final ABProp BIZ_AI_TOS_VARIANT = new ABProp(20833, "0", "0");

    /**
     * The {@code channels_invite_contacts_to_follow_receiver_logging_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_invite_contacts_to_follow_receiver_logging_enabled:[20836,"bool",!1,!1]
     */
    public static final ABProp CHANNELS_INVITE_CONTACTS_TO_FOLLOW_RECEIVER_LOGGING_ENABLED = new ABProp(20836, "false", "false");

    /**
     * The {@code channels_invite_contacts_to_follow_sender_logging_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_invite_contacts_to_follow_sender_logging_enabled:[20837,"bool",!1,!1]
     */
    public static final ABProp CHANNELS_INVITE_CONTACTS_TO_FOLLOW_SENDER_LOGGING_ENABLED = new ABProp(20837, "false", "false");

    /**
     * The {@code hide_auto_quotes_on_web} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: hide_auto_quotes_on_web:[20892,"bool",!1,!1]
     */
    public static final ABProp HIDE_AUTO_QUOTES_ON_WEB = new ABProp(20892, "false", "false");

    /**
     * The {@code ai_rewrite_load_more_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_rewrite_load_more_enabled:[20918,"bool",!1,!1]
     */
    public static final ABProp AI_REWRITE_LOAD_MORE_ENABLED = new ABProp(20918, "false", "false");

    /**
     * The {@code enable_web_group_calling} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_web_group_calling:[20924,"bool",!1,!1]
     */
    public static final ABProp ENABLE_WEB_GROUP_CALLING = new ABProp(20924, "false", "false");

    /**
     * The {@code smba_business_broadcast_genai_share_message_history} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smba_business_broadcast_genai_share_message_history:[20926,"bool",!1,!1]
     */
    public static final ABProp SMBA_BUSINESS_BROADCAST_GENAI_SHARE_MESSAGE_HISTORY = new ABProp(20926, "false", "false");

    /**
     * The {@code smba_business_broadcast_genai_text_model} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smba_business_broadcast_genai_text_model:[20929,"string","LLAMA","LLAMA"]
     */
    public static final ABProp SMBA_BUSINESS_BROADCAST_GENAI_TEXT_MODEL = new ABProp(20929, "LLAMA", "LLAMA");

    /**
     * The {@code smba_business_broadcast_genai_text_max_tries} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smba_business_broadcast_genai_text_max_tries:[20946,"int",30,30]
     */
    public static final ABProp SMBA_BUSINESS_BROADCAST_GENAI_TEXT_MAX_TRIES = new ABProp(20946, "30", "30");

    /**
     * The {@code wds_web_chip} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wds_web_chip:[20970,"bool",!1,!0]
     */
    public static final ABProp WDS_WEB_CHIP = new ABProp(20970, "false", "true");

    /**
     * The {@code username_contact_privacy_setting_allow_uncontact_set_enable} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: username_contact_privacy_setting_allow_uncontact_set_enable:[20993,"bool",!1,!1]
     */
    public static final ABProp USERNAME_CONTACT_PRIVACY_SETTING_ALLOW_UNCONTACT_SET_ENABLE = new ABProp(20993, "false", "false");

    /**
     * The {@code web_threads_infra_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_threads_infra_enabled:[21062,"bool",!0,!0]
     */
    public static final ABProp WEB_THREADS_INFRA_ENABLED = new ABProp(21062, "true", "true");

    /**
     * The {@code dsa_21_channel_reporting_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: dsa_21_channel_reporting_enabled:[21073,"bool",!1,!0]
     */
    public static final ABProp DSA_21_CHANNEL_REPORTING_ENABLED = new ABProp(21073, "false", "true");

    /**
     * The {@code ai_rewrite_languages_and_tones_config} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_rewrite_languages_and_tones_config:[21139,"string","{}","{\"en\": \"rephrase,professional,funny,supportive,proofread\"}"]
     */
    public static final ABProp AI_REWRITE_LANGUAGES_AND_TONES_CONFIG = new ABProp(21139, "{}", "{\"en\": \"rephrase,professional,funny,supportive,proofread\"}");

    /**
     * The {@code chatlist_prevent_autoread} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: chatlist_prevent_autoread:[21156,"bool",!1,!1]
     */
    public static final ABProp CHATLIST_PREVENT_AUTOREAD = new ABProp(21156, "false", "false");

    /**
     * The {@code enable_sharing_files_from_web_windows_hybrid} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_sharing_files_from_web_windows_hybrid:[21184,"bool",!1,!0]
     */
    public static final ABProp ENABLE_SHARING_FILES_FROM_WEB_WINDOWS_HYBRID = new ABProp(21184, "false", "true");

    /**
     * The {@code paa_support_for_disabled_epehemerality} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: paa_support_for_disabled_epehemerality:[21235,"bool",!1,!0]
     */
    public static final ABProp PAA_SUPPORT_FOR_DISABLED_EPEHEMERALITY = new ABProp(21235, "false", "true");

    /**
     * The {@code web_navigation_bar_updates_tab} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_navigation_bar_updates_tab:[21250,"bool",!1,!1]
     */
    public static final ABProp WEB_NAVIGATION_BAR_UPDATES_TAB = new ABProp(21250, "false", "false");

    /**
     * The {@code group_history_settings} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: group_history_settings:[21261,"bool",!1,!1]
     */
    public static final ABProp GROUP_HISTORY_SETTINGS = new ABProp(21261, "false", "false");

    /**
     * The {@code mm_data_sharing_disclosure_enabled_companion_history_sync} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: mm_data_sharing_disclosure_enabled_companion_history_sync:[21288,"bool",!1,!0]
     */
    public static final ABProp MM_DATA_SHARING_DISCLOSURE_ENABLED_COMPANION_HISTORY_SYNC = new ABProp(21288, "false", "true");

    /**
     * The {@code group_history_messages_time_limit_receiver_enforcement_secs} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: group_history_messages_time_limit_receiver_enforcement_secs:[21313,"int",1209600,1209600]
     */
    public static final ABProp GROUP_HISTORY_MESSAGES_TIME_LIMIT_RECEIVER_ENFORCEMENT_SECS = new ABProp(21313, "1209600", "1209600");

    /**
     * The {@code web_voip_video_capture_impl} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_voip_video_capture_impl:[21350,"int",0,0]
     */
    public static final ABProp WEB_VOIP_VIDEO_CAPTURE_IMPL = new ABProp(21350, "0", "0");

    /**
     * The {@code smba_bb_genai_composer_min_words} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smba_bb_genai_composer_min_words:[21447,"int",4,4]
     */
    public static final ABProp SMBA_BB_GENAI_COMPOSER_MIN_WORDS = new ABProp(21447, "4", "4");

    /**
     * The {@code group_history_settings_toggle_ui} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: group_history_settings_toggle_ui:[21481,"bool",!1,!1]
     */
    public static final ABProp GROUP_HISTORY_SETTINGS_TOGGLE_UI = new ABProp(21481, "false", "false");

    /**
     * The {@code web_status_crossposting_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_status_crossposting_enabled:[21501,"bool",!1,!1]
     */
    public static final ABProp WEB_STATUS_CROSSPOSTING_ENABLED = new ABProp(21501, "false", "false");

    /**
     * The {@code smb_business_broadcast_send_web} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_business_broadcast_send_web:[21508,"bool",!1,!1]
     */
    public static final ABProp SMB_BUSINESS_BROADCAST_SEND_WEB = new ABProp(21508, "false", "false");

    /**
     * The {@code ai_continuous_session_transparency_notice_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_continuous_session_transparency_notice_enabled:[21510,"bool",!1,!1]
     */
    public static final ABProp AI_CONTINUOUS_SESSION_TRANSPARENCY_NOTICE_ENABLED = new ABProp(21510, "false", "false");

    /**
     * The {@code unified_calling_entry_point_desktop_type} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: unified_calling_entry_point_desktop_type:[21591,"int",0,0]
     */
    public static final ABProp UNIFIED_CALLING_ENTRY_POINT_DESKTOP_TYPE = new ABProp(21591, "0", "0");

    /**
     * The {@code enable_waiting_room_admin_ui} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_waiting_room_admin_ui:[21676,"bool",!1,!1]
     */
    public static final ABProp ENABLE_WAITING_ROOM_ADMIN_UI = new ABProp(21676, "false", "false");

    /**
     * The {@code web_voip_audio_capture_impl} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_voip_audio_capture_impl:[21688,"int",0,0]
     */
    public static final ABProp WEB_VOIP_AUDIO_CAPTURE_IMPL = new ABProp(21688, "0", "0");

    /**
     * The {@code web_voip_audio_playback_impl} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_voip_audio_playback_impl:[21689,"int",0,0]
     */
    public static final ABProp WEB_VOIP_AUDIO_PLAYBACK_IMPL = new ABProp(21689, "0", "0");

    /**
     * The {@code rt_swapped_fallback_validation} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: rt_swapped_fallback_validation:[21718,"bool",!0,!0]
     */
    public static final ABProp RT_SWAPPED_FALLBACK_VALIDATION = new ABProp(21718, "true", "true");

    /**
     * The {@code wavoip_ml_media_automos_model_download_versions} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wavoip_ml_media_automos_model_download_versions:[21731,"string","",""]
     */
    public static final ABProp WAVOIP_ML_MEDIA_AUTOMOS_MODEL_DOWNLOAD_VERSIONS = new ABProp(21731, "", "");

    /**
     * The {@code wavoip_ml_bwe_cong_model_download_versions} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wavoip_ml_bwe_cong_model_download_versions:[21732,"string","",""]
     */
    public static final ABProp WAVOIP_ML_BWE_CONG_MODEL_DOWNLOAD_VERSIONS = new ABProp(21732, "", "");

    /**
     * The {@code wavoip_ml_bwe_rl_model_download_versions} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wavoip_ml_bwe_rl_model_download_versions:[21733,"string","",""]
     */
    public static final ABProp WAVOIP_ML_BWE_RL_MODEL_DOWNLOAD_VERSIONS = new ABProp(21733, "", "");

    /**
     * The {@code wavoip_ml_bwe_tr_model_download_versions} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wavoip_ml_bwe_tr_model_download_versions:[21734,"string","",""]
     */
    public static final ABProp WAVOIP_ML_BWE_TR_MODEL_DOWNLOAD_VERSIONS = new ABProp(21734, "", "");

    /**
     * The {@code wavoip_ml_media_vsr_model_download_versions} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wavoip_ml_media_vsr_model_download_versions:[21735,"string","",""]
     */
    public static final ABProp WAVOIP_ML_MEDIA_VSR_MODEL_DOWNLOAD_VERSIONS = new ABProp(21735, "", "");

    /**
     * The {@code wavoip_ml_media_vmos_model_download_versions} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wavoip_ml_media_vmos_model_download_versions:[21736,"string","",""]
     */
    public static final ABProp WAVOIP_ML_MEDIA_VMOS_MODEL_DOWNLOAD_VERSIONS = new ABProp(21736, "", "");

    /**
     * The {@code wavoip_ml_media_ns_model_download_versions} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wavoip_ml_media_ns_model_download_versions:[21737,"string","",""]
     */
    public static final ABProp WAVOIP_ML_MEDIA_NS_MODEL_DOWNLOAD_VERSIONS = new ABProp(21737, "", "");

    /**
     * The {@code wavoip_ml_bwe_hd_target_model_download_versions} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wavoip_ml_bwe_hd_target_model_download_versions:[21738,"string","",""]
     */
    public static final ABProp WAVOIP_ML_BWE_HD_TARGET_MODEL_DOWNLOAD_VERSIONS = new ABProp(21738, "", "");

    /**
     * The {@code br_payments_pix_groups_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: br_payments_pix_groups_enabled:[21741,"bool",!1,!1]
     */
    public static final ABProp BR_PAYMENTS_PIX_GROUPS_ENABLED = new ABProp(21741, "false", "false");

    /**
     * The {@code wa_web_wae_qpl_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_wae_qpl_enabled:[21742,"bool",!0,!0]
     */
    public static final ABProp WA_WEB_WAE_QPL_ENABLED = new ABProp(21742, "true", "true");

    /**
     * The {@code mm_optimized_delivery_replacing_shimmed_links_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: mm_optimized_delivery_replacing_shimmed_links_enabled:[21782,"bool",!1,!1]
     */
    public static final ABProp MM_OPTIMIZED_DELIVERY_REPLACING_SHIMMED_LINKS_ENABLED = new ABProp(21782, "false", "false");

    /**
     * The {@code functional_chatlist_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: functional_chatlist_enabled:[21799,"bool",!1,!0]
     */
    public static final ABProp FUNCTIONAL_CHATLIST_ENABLED = new ABProp(21799, "false", "true");

    /**
     * The {@code wavoip_ml_temp_model_download_versions} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wavoip_ml_temp_model_download_versions:[21815,"string","",""]
     */
    public static final ABProp WAVOIP_ML_TEMP_MODEL_DOWNLOAD_VERSIONS = new ABProp(21815, "", "");

    /**
     * The {@code ctwa_suppress_message_with_external_ad_reply_consumer_db_level_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ctwa_suppress_message_with_external_ad_reply_consumer_db_level_enabled:[21819,"bool",!1,!1]
     */
    public static final ABProp CTWA_SUPPRESS_MESSAGE_WITH_EXTERNAL_AD_REPLY_CONSUMER_DB_LEVEL_ENABLED = new ABProp(21819, "false", "false");

    /**
     * The {@code wavoip_ml_bwe_gc_undershoot_model_download_versions} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wavoip_ml_bwe_gc_undershoot_model_download_versions:[21821,"string","",""]
     */
    public static final ABProp WAVOIP_ML_BWE_GC_UNDERSHOOT_MODEL_DOWNLOAD_VERSIONS = new ABProp(21821, "", "");

    /**
     * The {@code wavoip_ml_bwe_gc_hd_target_model_download_versions} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wavoip_ml_bwe_gc_hd_target_model_download_versions:[21822,"string","",""]
     */
    public static final ABProp WAVOIP_ML_BWE_GC_HD_TARGET_MODEL_DOWNLOAD_VERSIONS = new ABProp(21822, "", "");

    /**
     * The {@code wa_web_enable_granular_notifications} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_enable_granular_notifications:[21909,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_ENABLE_GRANULAR_NOTIFICATIONS = new ABProp(21909, "false", "false");

    /**
     * The {@code wa_web_disable_prefetch_loadables} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_disable_prefetch_loadables:[21917,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_DISABLE_PREFETCH_LOADABLES = new ABProp(21917, "false", "false");

    /**
     * The {@code username_suggestions_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: username_suggestions_enabled:[21984,"bool",!1,!1]
     */
    public static final ABProp USERNAME_SUGGESTIONS_ENABLED = new ABProp(21984, "false", "false");

    /**
     * The {@code enable_agm_flow_cta} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_agm_flow_cta:[22006,"bool",!1,!1]
     */
    public static final ABProp ENABLE_AGM_FLOW_CTA = new ABProp(22006, "false", "false");

    /**
     * The {@code ai_reply_message_context_max_count} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_reply_message_context_max_count:[22024,"int",20,20]
     */
    public static final ABProp AI_REPLY_MESSAGE_CONTEXT_MAX_COUNT = new ABProp(22024, "20", "20");

    /**
     * The {@code ai_reply_message_context_trigger_min_count} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_reply_message_context_trigger_min_count:[22025,"int",10,10]
     */
    public static final ABProp AI_REPLY_MESSAGE_CONTEXT_TRIGGER_MIN_COUNT = new ABProp(22025, "10", "10");

    /**
     * The {@code ai_chat_thread_capability_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_chat_thread_capability_enabled:[22038,"bool",!1,!1]
     */
    public static final ABProp AI_CHAT_THREAD_CAPABILITY_ENABLED = new ABProp(22038, "false", "false");

    /**
     * The {@code ai_chat_threads_historical_messages_migration_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_chat_threads_historical_messages_migration_enabled:[22070,"bool",!1,!1]
     */
    public static final ABProp AI_CHAT_THREADS_HISTORICAL_MESSAGES_MIGRATION_ENABLED = new ABProp(22070, "false", "false");

    /**
     * The {@code wa_web_lists_m2_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_lists_m2_enabled:[22086,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_LISTS_M2_ENABLED = new ABProp(22086, "false", "false");

    /**
     * The {@code channels_music_forwarding_disabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_music_forwarding_disabled:[22089,"bool",!1,!1]
     */
    public static final ABProp CHANNELS_MUSIC_FORWARDING_DISABLED = new ABProp(22089, "false", "false");

    /**
     * The {@code wa_web_lists_m1_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_lists_m1_enabled:[22090,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_LISTS_M1_ENABLED = new ABProp(22090, "false", "false");

    /**
     * The {@code web_cache_open_failed_reload_flow_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_cache_open_failed_reload_flow_enabled:[22155,"bool",!1,!1]
     */
    public static final ABProp WEB_CACHE_OPEN_FAILED_RELOAD_FLOW_ENABLED = new ABProp(22155, "false", "false");

    /**
     * The {@code ai_group_participation_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_group_participation_enabled:[22171,"bool",!1,!1]
     */
    public static final ABProp AI_GROUP_PARTICIPATION_ENABLED = new ABProp(22171, "false", "false");

    /**
     * The {@code ai_group_participation_send_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_group_participation_send_enabled:[22184,"bool",!1,!1]
     */
    public static final ABProp AI_GROUP_PARTICIPATION_SEND_ENABLED = new ABProp(22184, "false", "false");

    /**
     * The {@code web_calling_perf_optimizations_bitmask} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_calling_perf_optimizations_bitmask:[22186,"int",1,1]
     */
    public static final ABProp WEB_CALLING_PERF_OPTIMIZATIONS_BITMASK = new ABProp(22186, "1", "1");

    /**
     * The {@code channels_invite_link_preview_improvement_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_invite_link_preview_improvement_enabled:[22196,"bool",!1,!1]
     */
    public static final ABProp CHANNELS_INVITE_LINK_PREVIEW_IMPROVEMENT_ENABLED = new ABProp(22196, "false", "false");

    /**
     * The {@code newsletter_forward_counter_bump_own_channel_updates_fowards} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: newsletter_forward_counter_bump_own_channel_updates_fowards:[22203,"bool",!0,!0]
     */
    public static final ABProp NEWSLETTER_FORWARD_COUNTER_BUMP_OWN_CHANNEL_UPDATES_FOWARDS = new ABProp(22203, "true", "true");

    /**
     * The {@code newsletter_forward_counter_bump_forwards_to_self} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: newsletter_forward_counter_bump_forwards_to_self:[22204,"bool",!0,!0]
     */
    public static final ABProp NEWSLETTER_FORWARD_COUNTER_BUMP_FORWARDS_TO_SELF = new ABProp(22204, "true", "true");

    /**
     * The {@code newsletter_forward_counter_bump_second_order_forwards} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: newsletter_forward_counter_bump_second_order_forwards:[22205,"bool",!1,!0]
     */
    public static final ABProp NEWSLETTER_FORWARD_COUNTER_BUMP_SECOND_ORDER_FORWARDS = new ABProp(22205, "false", "true");

    /**
     * The {@code newsletter_forward_counter_max_send_after_random_time} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: newsletter_forward_counter_max_send_after_random_time:[22206,"int",3600,60]
     */
    public static final ABProp NEWSLETTER_FORWARD_COUNTER_MAX_SEND_AFTER_RANDOM_TIME = new ABProp(22206, "3600", "60");

    /**
     * The {@code is_spoiler_rich_format_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: is_spoiler_rich_format_enabled:[22221,"bool",!1,!1]
     */
    public static final ABProp IS_SPOILER_RICH_FORMAT_ENABLED = new ABProp(22221, "false", "false");

    /**
     * The {@code ai_group_participation_add_tee_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_group_participation_add_tee_enabled:[22236,"bool",!1,!1]
     */
    public static final ABProp AI_GROUP_PARTICIPATION_ADD_TEE_ENABLED = new ABProp(22236, "false", "false");

    /**
     * The {@code br_payments_home_duration_rule_for_pux_banner} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: br_payments_home_duration_rule_for_pux_banner:[22249,"int",604800,604800]
     */
    public static final ABProp BR_PAYMENTS_HOME_DURATION_RULE_FOR_PUX_BANNER = new ABProp(22249, "604800", "604800");

    /**
     * The {@code channels_invite_contacts_to_follow_receiver_invalid_message_drop_endabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_invite_contacts_to_follow_receiver_invalid_message_drop_endabled:[22280,"bool",!0,!0]
     */
    public static final ABProp CHANNELS_INVITE_CONTACTS_TO_FOLLOW_RECEIVER_INVALID_MESSAGE_DROP_ENDABLED = new ABProp(22280, "true", "true");

    /**
     * The {@code ai_metabot_document_ocr_image_conversion_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_metabot_document_ocr_image_conversion_enabled:[22301,"bool",!1,!1]
     */
    public static final ABProp AI_METABOT_DOCUMENT_OCR_IMAGE_CONVERSION_ENABLED = new ABProp(22301, "false", "false");

    /**
     * The {@code enable_futureproof_galaxy_flow_message_for_business_numbers} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_futureproof_galaxy_flow_message_for_business_numbers:[22311,"string","",""]
     */
    public static final ABProp ENABLE_FUTUREPROOF_GALAXY_FLOW_MESSAGE_FOR_BUSINESS_NUMBERS = new ABProp(22311, "", "");

    /**
     * The {@code channels_admin_profiles_sender_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_admin_profiles_sender_enabled:[22316,"bool",!1,!0]
     */
    public static final ABProp CHANNELS_ADMIN_PROFILES_SENDER_ENABLED = new ABProp(22316, "false", "true");

    /**
     * The {@code channels_admin_profiles_receiver_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_admin_profiles_receiver_enabled:[22318,"bool",!1,!0]
     */
    public static final ABProp CHANNELS_ADMIN_PROFILES_RECEIVER_ENABLED = new ABProp(22318, "false", "true");

    /**
     * The {@code smba_business_broadcast_genai_master_abprop} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smba_business_broadcast_genai_master_abprop:[22384,"bool",!1,!1]
     */
    public static final ABProp SMBA_BUSINESS_BROADCAST_GENAI_MASTER_ABPROP = new ABProp(22384, "false", "false");

    /**
     * The {@code utility_payment_reminder_m1_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: utility_payment_reminder_m1_enabled:[22434,"bool",!1,!1]
     */
    public static final ABProp UTILITY_PAYMENT_REMINDER_M1_ENABLED = new ABProp(22434, "false", "false");

    /**
     * The {@code wa_web_reactions_2} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_reactions_2:[22469,"bool",!1,!0]
     */
    public static final ABProp WA_WEB_REACTIONS_2 = new ABProp(22469, "false", "true");

    /**
     * The {@code ai_contextual_writing_help_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_contextual_writing_help_enabled:[22488,"bool",!1,!1]
     */
    public static final ABProp AI_CONTEXTUAL_WRITING_HELP_ENABLED = new ABProp(22488, "false", "false");

    /**
     * The {@code dsa_26_receiver_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: dsa_26_receiver_enabled:[22515,"bool",!1,!0]
     */
    public static final ABProp DSA_26_RECEIVER_ENABLED = new ABProp(22515, "false", "true");

    /**
     * The {@code dsa_26_sender_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: dsa_26_sender_enabled:[22516,"bool",!1,!0]
     */
    public static final ABProp DSA_26_SENDER_ENABLED = new ABProp(22516, "false", "true");

    /**
     * The {@code media_hub_history_max_days} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: media_hub_history_max_days:[22518,"int",14,14]
     */
    public static final ABProp MEDIA_HUB_HISTORY_MAX_DAYS = new ABProp(22518, "14", "14");

    /**
     * The {@code vid_port_frm_buf_mutex_fixes} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: vid_port_frm_buf_mutex_fixes:[22525,"bool",!1,!1]
     */
    public static final ABProp VID_PORT_FRM_BUF_MUTEX_FIXES = new ABProp(22525, "false", "false");

    /**
     * The {@code smb_core_biz_profile_ux_refreshed_v2} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_core_biz_profile_ux_refreshed_v2:[22561,"bool",!1,!1]
     */
    public static final ABProp SMB_CORE_BIZ_PROFILE_UX_REFRESHED_V2 = new ABProp(22561, "false", "false");

    /**
     * The {@code wa_web_show_status_ring_for_no_unread} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_show_status_ring_for_no_unread:[22567,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_SHOW_STATUS_RING_FOR_NO_UNREAD = new ABProp(22567, "false", "false");

    /**
     * The {@code web_phone_number_global_search} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_phone_number_global_search:[22603,"bool",!1,!1]
     */
    public static final ABProp WEB_PHONE_NUMBER_GLOBAL_SEARCH = new ABProp(22603, "false", "false");

    /**
     * The {@code wa_web_create_group_in_filter} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_create_group_in_filter:[22617,"bool",!1,!0]
     */
    public static final ABProp WA_WEB_CREATE_GROUP_IN_FILTER = new ABProp(22617, "false", "true");

    /**
     * The {@code member_name_tag_web_sender_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: member_name_tag_web_sender_enabled:[22654,"bool",!1,!0]
     */
    public static final ABProp MEMBER_NAME_TAG_WEB_SENDER_ENABLED = new ABProp(22654, "false", "true");

    /**
     * The {@code member_name_tag_web_receiver_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: member_name_tag_web_receiver_enabled:[22655,"bool",!1,!0]
     */
    public static final ABProp MEMBER_NAME_TAG_WEB_RECEIVER_ENABLED = new ABProp(22655, "false", "true");

    /**
     * The {@code ai_rich_response_post_citations_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_rich_response_post_citations_enabled:[22672,"bool",!1,!1]
     */
    public static final ABProp AI_RICH_RESPONSE_POST_CITATIONS_ENABLED = new ABProp(22672, "false", "false");

    /**
     * The {@code settings_sync_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: settings_sync_enabled:[22692,"bool",!1,!1]
     */
    public static final ABProp SETTINGS_SYNC_ENABLED = new ABProp(22692, "false", "false");

    /**
     * The {@code ai_rich_response_zeitgeist_carousel_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_rich_response_zeitgeist_carousel_enabled:[22750,"bool",!1,!1]
     */
    public static final ABProp AI_RICH_RESPONSE_ZEITGEIST_CAROUSEL_ENABLED = new ABProp(22750, "false", "false");

    /**
     * The {@code ai_contextual_writing_help_num_suggestions} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_contextual_writing_help_num_suggestions:[22759,"int",4,4]
     */
    public static final ABProp AI_CONTEXTUAL_WRITING_HELP_NUM_SUGGESTIONS = new ABProp(22759, "4", "4");

    /**
     * The {@code mm_optimized_delivery_app_cta_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: mm_optimized_delivery_app_cta_enabled:[22776,"bool",!1,!1]
     */
    public static final ABProp MM_OPTIMIZED_DELIVERY_APP_CTA_ENABLED = new ABProp(22776, "false", "false");

    /**
     * The {@code wa_media_image_upload_cache} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_media_image_upload_cache:[22784,"bool",!1,!1]
     */
    public static final ABProp WA_MEDIA_IMAGE_UPLOAD_CACHE = new ABProp(22784, "false", "false");

    /**
     * The {@code ai_imagine_loading_indicator_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_imagine_loading_indicator_enabled:[22795,"bool",!1,!1]
     */
    public static final ABProp AI_IMAGINE_LOADING_INDICATOR_ENABLED = new ABProp(22795, "false", "false");

    /**
     * The {@code ai_contextual_writing_help_languages_and_tones_config} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_contextual_writing_help_languages_and_tones_config:[22797,"string","{}","{\"en\": \"auto,professional,funny,supportive\"}"]
     */
    public static final ABProp AI_CONTEXTUAL_WRITING_HELP_LANGUAGES_AND_TONES_CONFIG = new ABProp(22797, "{}", "{\"en\": \"auto,professional,funny,supportive\"}");

    /**
     * The {@code wa_web_share_content_uj} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_share_content_uj:[22813,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_SHARE_CONTENT_UJ = new ABProp(22813, "false", "false");

    /**
     * The {@code message_keys_async_chunk_size} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: message_keys_async_chunk_size:[22815,"int",50,50]
     */
    public static final ABProp MESSAGE_KEYS_ASYNC_CHUNK_SIZE = new ABProp(22815, "50", "50");

    /**
     * The {@code synced_message_keys_processing_type} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: synced_message_keys_processing_type:[22825,"string","control","control"]
     */
    public static final ABProp SYNCED_MESSAGE_KEYS_PROCESSING_TYPE = new ABProp(22825, "control", "control");

    /**
     * The {@code wa_web_favicon_badging_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_favicon_badging_enabled:[22924,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_FAVICON_BADGING_ENABLED = new ABProp(22924, "false", "false");

    /**
     * The {@code web_anr_file_size_threshold_to_use_worker_mb} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_anr_file_size_threshold_to_use_worker_mb:[22930,"int",0,0]
     */
    public static final ABProp WEB_ANR_FILE_SIZE_THRESHOLD_TO_USE_WORKER_MB = new ABProp(22930, "0", "0");

    /**
     * The {@code web_anr_media_chunk_enc_delay_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_anr_media_chunk_enc_delay_enabled:[22931,"bool",!1,!1]
     */
    public static final ABProp WEB_ANR_MEDIA_CHUNK_ENC_DELAY_ENABLED = new ABProp(22931, "false", "false");

    /**
     * The {@code empty_unread_filter_cta_variant} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: empty_unread_filter_cta_variant:[22962,"int",0,0]
     */
    public static final ABProp EMPTY_UNREAD_FILTER_CTA_VARIANT = new ABProp(22962, "0", "0");

    /**
     * The {@code waweb_chatinfo_refresh} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: waweb_chatinfo_refresh:[23018,"bool",!1,!0]
     */
    public static final ABProp WAWEB_CHATINFO_REFRESH = new ABProp(23018, "false", "true");

    /**
     * The {@code smb_graphql_merchant_info_set_compliance} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_graphql_merchant_info_set_compliance:[23026,"bool",!1,!1]
     */
    public static final ABProp SMB_GRAPHQL_MERCHANT_INFO_SET_COMPLIANCE = new ABProp(23026, "false", "false");

    /**
     * The {@code smb_graphql_merchant_info_get_compliance} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_graphql_merchant_info_get_compliance:[23027,"bool",!1,!1]
     */
    public static final ABProp SMB_GRAPHQL_MERCHANT_INFO_GET_COMPLIANCE = new ABProp(23027, "false", "false");

    /**
     * The {@code br_smb_paymentshome_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: br_smb_paymentshome_enabled:[23042,"bool",!1,!1]
     */
    public static final ABProp BR_SMB_PAYMENTSHOME_ENABLED = new ABProp(23042, "false", "false");

    /**
     * The {@code web_voip_load_wasm_variant} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_voip_load_wasm_variant:[23045,"string","prod-nonlab","prod-nonlab"]
     */
    public static final ABProp WEB_VOIP_LOAD_WASM_VARIANT = new ABProp(23045, "prod-nonlab", "prod-nonlab");

    /**
     * The {@code ai_chat_threads_web_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_chat_threads_web_enabled:[23169,"bool",!1,!0]
     */
    public static final ABProp AI_CHAT_THREADS_WEB_ENABLED = new ABProp(23169, "false", "true");

    /**
     * The {@code channels_admin_profiles_forwarding_to_chats_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_admin_profiles_forwarding_to_chats_enabled:[23170,"bool",!1,!1]
     */
    public static final ABProp CHANNELS_ADMIN_PROFILES_FORWARDING_TO_CHATS_ENABLED = new ABProp(23170, "false", "false");

    /**
     * The {@code channels_admin_profiles_list_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_admin_profiles_list_enabled:[23174,"bool",!1,!1]
     */
    public static final ABProp CHANNELS_ADMIN_PROFILES_LIST_ENABLED = new ABProp(23174, "false", "false");

    /**
     * The {@code ai_session_transparency_meta_ai_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_session_transparency_meta_ai_enabled:[23188,"bool",!1,!1]
     */
    public static final ABProp AI_SESSION_TRANSPARENCY_META_AI_ENABLED = new ABProp(23188, "false", "false");

    /**
     * The {@code web_anr_async_media_decryption_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_anr_async_media_decryption_enabled:[23200,"bool",!1,!1]
     */
    public static final ABProp WEB_ANR_ASYNC_MEDIA_DECRYPTION_ENABLED = new ABProp(23200, "false", "false");

    /**
     * The {@code aura_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: aura_enabled:[23270,"bool",!1,!1]
     */
    public static final ABProp AURA_ENABLED = new ABProp(23270, "false", "false");

    /**
     * The {@code aura_app_themes_benefit_active} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: aura_app_themes_benefit_active:[23273,"bool",!1,!1]
     */
    public static final ABProp AURA_APP_THEMES_BENEFIT_ACTIVE = new ABProp(23273, "false", "false");

    /**
     * The {@code aura_app_themes_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: aura_app_themes_enabled:[23274,"bool",!1,!1]
     */
    public static final ABProp AURA_APP_THEMES_ENABLED = new ABProp(23274, "false", "false");

    /**
     * The {@code aura_pinned_chats_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: aura_pinned_chats_enabled:[23277,"bool",!1,!1]
     */
    public static final ABProp AURA_PINNED_CHATS_ENABLED = new ABProp(23277, "false", "false");

    /**
     * The {@code aura_pinned_chats_benefit_active} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: aura_pinned_chats_benefit_active:[23278,"bool",!1,!1]
     */
    public static final ABProp AURA_PINNED_CHATS_BENEFIT_ACTIVE = new ABProp(23278, "false", "false");

    /**
     * The {@code imp_send_signal_post_connect_webc_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: imp_send_signal_post_connect_webc_enabled:[23322,"bool",!1,!0]
     */
    public static final ABProp IMP_SEND_SIGNAL_POST_CONNECT_WEBC_ENABLED = new ABProp(23322, "false", "true");

    /**
     * The {@code imp_send_signal_post_connect_delay} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: imp_send_signal_post_connect_delay:[23323,"int",500,500]
     */
    public static final ABProp IMP_SEND_SIGNAL_POST_CONNECT_DELAY = new ABProp(23323, "500", "500");

    /**
     * The {@code saga_message_feedback_using_canonical_ent} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: saga_message_feedback_using_canonical_ent:[23328,"bool",!1,!1]
     */
    public static final ABProp SAGA_MESSAGE_FEEDBACK_USING_CANONICAL_ENT = new ABProp(23328, "false", "false");

    /**
     * The {@code ai_unified_response_sender_web_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_unified_response_sender_web_enabled:[23347,"bool",!1,!1]
     */
    public static final ABProp AI_UNIFIED_RESPONSE_SENDER_WEB_ENABLED = new ABProp(23347, "false", "false");

    /**
     * The {@code ai_unified_response_receiver_web_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_unified_response_receiver_web_enabled:[23348,"bool",!1,!1]
     */
    public static final ABProp AI_UNIFIED_RESPONSE_RECEIVER_WEB_ENABLED = new ABProp(23348, "false", "false");

    /**
     * The {@code mex_get_privacy_settings_mode} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: mex_get_privacy_settings_mode:[23463,"int",0,0]
     */
    public static final ABProp MEX_GET_PRIVACY_SETTINGS_MODE = new ABProp(23463, "0", "0");

    /**
     * The {@code coex_calling_permissions_3p_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: coex_calling_permissions_3p_enabled:[23464,"bool",!1,!0]
     */
    public static final ABProp COEX_CALLING_PERMISSIONS_3P_ENABLED = new ABProp(23464, "false", "true");

    /**
     * The {@code wds_web_toast} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wds_web_toast:[23486,"bool",!1,!0]
     */
    public static final ABProp WDS_WEB_TOAST = new ABProp(23486, "false", "true");

    /**
     * The {@code wa_webtp_use_pdf_editor} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_webtp_use_pdf_editor:[23498,"bool",!1,!1]
     */
    public static final ABProp WA_WEBTP_USE_PDF_EDITOR = new ABProp(23498, "false", "false");

    /**
     * The {@code web_ai_group_open_support} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_ai_group_open_support:[23530,"bool",!1,!1]
     */
    public static final ABProp WEB_AI_GROUP_OPEN_SUPPORT = new ABProp(23530, "false", "false");

    /**
     * The {@code booking_confirmation_enabled_wa_web} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: booking_confirmation_enabled_wa_web:[23559,"bool",!1,!0]
     */
    public static final ABProp BOOKING_CONFIRMATION_ENABLED_WA_WEB = new ABProp(23559, "false", "true");

    /**
     * The {@code web_hybrid_apply_latest_db_schema_optimization_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_hybrid_apply_latest_db_schema_optimization_enabled:[23595,"bool",!1,!0]
     */
    public static final ABProp WEB_HYBRID_APPLY_LATEST_DB_SCHEMA_OPTIMIZATION_ENABLED = new ABProp(23595, "false", "true");

    /**
     * The {@code channel_views_vpv_definition_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channel_views_vpv_definition_enabled:[23616,"bool",!1,!1]
     */
    public static final ABProp CHANNEL_VIEWS_VPV_DEFINITION_ENABLED = new ABProp(23616, "false", "false");

    /**
     * The {@code aigc_version} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: aigc_version:[23692,"int",1,1]
     */
    public static final ABProp AIGC_VERSION = new ABProp(23692, "1", "1");

    /**
     * The {@code ai_chat_threads_web_msgs_load_limit} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_chat_threads_web_msgs_load_limit:[23694,"int",50,50]
     */
    public static final ABProp AI_CHAT_THREADS_WEB_MSGS_LOAD_LIMIT = new ABProp(23694, "50", "50");

    /**
     * The {@code web_chatpsa_forwarding} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_chatpsa_forwarding:[23695,"bool",!1,!1]
     */
    public static final ABProp WEB_CHATPSA_FORWARDING = new ABProp(23695, "false", "false");

    /**
     * The {@code ai_web_ask_meta_ai_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_web_ask_meta_ai_enabled:[23725,"bool",!1,!1]
     */
    public static final ABProp AI_WEB_ASK_META_AI_ENABLED = new ABProp(23725, "false", "false");

    /**
     * The {@code channel_enforcement_policy_education_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channel_enforcement_policy_education_enabled:[23745,"bool",!1,!1]
     */
    public static final ABProp CHANNEL_ENFORCEMENT_POLICY_EDUCATION_ENABLED = new ABProp(23745, "false", "false");

    /**
     * The {@code web_voip_dynamic_thread_preallocate_count} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_voip_dynamic_thread_preallocate_count:[23789,"int",0,0]
     */
    public static final ABProp WEB_VOIP_DYNAMIC_THREAD_PREALLOCATE_COUNT = new ABProp(23789, "0", "0");

    /**
     * The {@code username_channels_pn_privacy_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: username_channels_pn_privacy_enabled:[23795,"bool",!1,!1]
     */
    public static final ABProp USERNAME_CHANNELS_PN_PRIVACY_ENABLED = new ABProp(23795, "false", "false");

    /**
     * The {@code top_level_message_secret_check} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: top_level_message_secret_check:[23796,"bool",!1,!0]
     */
    public static final ABProp TOP_LEVEL_MESSAGE_SECRET_CHECK = new ABProp(23796, "false", "true");

    /**
     * The {@code channels_album_receiver_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_album_receiver_enabled:[23809,"bool",!1,!1]
     */
    public static final ABProp CHANNELS_ALBUM_RECEIVER_ENABLED = new ABProp(23809, "false", "false");

    /**
     * The {@code username_enabled_on_companion} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: username_enabled_on_companion:[23817,"bool",!1,!1]
     */
    public static final ABProp USERNAME_ENABLED_ON_COMPANION = new ABProp(23817, "false", "false");

    /**
     * The {@code ai_rich_response_inline_links_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_rich_response_inline_links_enabled:[23819,"bool",!1,!1]
     */
    public static final ABProp AI_RICH_RESPONSE_INLINE_LINKS_ENABLED = new ABProp(23819, "false", "false");

    /**
     * The {@code disable_libaom_registration} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: disable_libaom_registration:[23836,"bool",!1,!1]
     */
    public static final ABProp DISABLE_LIBAOM_REGISTRATION = new ABProp(23836, "false", "false");

    /**
     * The {@code scheduled_messages_sender_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: scheduled_messages_sender_enabled:[23845,"bool",!1,!1]
     */
    public static final ABProp SCHEDULED_MESSAGES_SENDER_ENABLED = new ABProp(23845, "false", "false");

    /**
     * The {@code smbi_premium_broadcast_max_recipient_limit} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smbi_premium_broadcast_max_recipient_limit:[23857,"int",256,500]
     */
    public static final ABProp SMBI_PREMIUM_BROADCAST_MAX_RECIPIENT_LIMIT = new ABProp(23857, "256", "500");

    /**
     * The {@code channels_album_sender_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_album_sender_enabled:[23859,"bool",!1,!1]
     */
    public static final ABProp CHANNELS_ALBUM_SENDER_ENABLED = new ABProp(23859, "false", "false");

    /**
     * The {@code mex_get_privacy_contact_list_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: mex_get_privacy_contact_list_enabled:[23874,"bool",!1,!1]
     */
    public static final ABProp MEX_GET_PRIVACY_CONTACT_LIST_ENABLED = new ABProp(23874, "false", "false");

    /**
     * The {@code biz_ai_consumer_tos_update_web} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: biz_ai_consumer_tos_update_web:[23880,"bool",!1,!1]
     */
    public static final ABProp BIZ_AI_CONSUMER_TOS_UPDATE_WEB = new ABProp(23880, "false", "false");

    /**
     * The {@code ai_mode_selector_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_mode_selector_enabled:[23885,"bool",!1,!1]
     */
    public static final ABProp AI_MODE_SELECTOR_ENABLED = new ABProp(23885, "false", "false");

    /**
     * The {@code pix_onboarding_new_content_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: pix_onboarding_new_content_enabled:[23953,"bool",!1,!1]
     */
    public static final ABProp PIX_ONBOARDING_NEW_CONTENT_ENABLED = new ABProp(23953, "false", "false");

    /**
     * The {@code channel_status_creation} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channel_status_creation:[23994,"bool",!1,!1]
     */
    public static final ABProp CHANNEL_STATUS_CREATION = new ABProp(23994, "false", "false");

    /**
     * The {@code channel_status_consumption} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channel_status_consumption:[23995,"bool",!1,!1]
     */
    public static final ABProp CHANNEL_STATUS_CONSUMPTION = new ABProp(23995, "false", "false");

    /**
     * The {@code channels_questions_search_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_questions_search_enabled:[24004,"bool",!1,!0]
     */
    public static final ABProp CHANNELS_QUESTIONS_SEARCH_ENABLED = new ABProp(24004, "false", "true");

    /**
     * The {@code group_from_group} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: group_from_group:[24024,"bool",!1,!0]
     */
    public static final ABProp GROUP_FROM_GROUP = new ABProp(24024, "false", "true");

    /**
     * The {@code aura_ringtones_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: aura_ringtones_enabled:[24047,"bool",!1,!1]
     */
    public static final ABProp AURA_RINGTONES_ENABLED = new ABProp(24047, "false", "false");

    /**
     * The {@code aura_ringtones_benefit_active} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: aura_ringtones_benefit_active:[24050,"bool",!1,!1]
     */
    public static final ABProp AURA_RINGTONES_BENEFIT_ACTIVE = new ABProp(24050, "false", "false");

    /**
     * The {@code ai_unified_response_imagine_receiver_web_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_unified_response_imagine_receiver_web_enabled:[24109,"bool",!1,!1]
     */
    public static final ABProp AI_UNIFIED_RESPONSE_IMAGINE_RECEIVER_WEB_ENABLED = new ABProp(24109, "false", "false");

    /**
     * The {@code lists_chat_list_row_pill_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: lists_chat_list_row_pill_enabled:[24133,"bool",!1,!1]
     */
    public static final ABProp LISTS_CHAT_LIST_ROW_PILL_ENABLED = new ABProp(24133, "false", "false");

    /**
     * The {@code web_history_sync_worker_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_history_sync_worker_enabled:[24147,"bool",!1,!1]
     */
    public static final ABProp WEB_HISTORY_SYNC_WORKER_ENABLED = new ABProp(24147, "false", "false");

    /**
     * The {@code bug_reporting_using_graphql} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: bug_reporting_using_graphql:[24161,"bool",!1,!1]
     */
    public static final ABProp BUG_REPORTING_USING_GRAPHQL = new ABProp(24161, "false", "false");

    /**
     * The {@code wavoip_ml_transport_download_versions} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wavoip_ml_transport_download_versions:[24173,"string","",""]
     */
    public static final ABProp WAVOIP_ML_TRANSPORT_DOWNLOAD_VERSIONS = new ABProp(24173, "", "");

    /**
     * The {@code wavoip_ml_nadl_model_download_versions} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wavoip_ml_nadl_model_download_versions:[24174,"string","",""]
     */
    public static final ABProp WAVOIP_ML_NADL_MODEL_DOWNLOAD_VERSIONS = new ABProp(24174, "", "");

    /**
     * The {@code is_spoiler_rich_format_sender_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: is_spoiler_rich_format_sender_enabled:[24210,"bool",!1,!0]
     */
    public static final ABProp IS_SPOILER_RICH_FORMAT_SENDER_ENABLED = new ABProp(24210, "false", "true");

    /**
     * The {@code flows_wa_web_agm_cta} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: flows_wa_web_agm_cta:[24215,"bool",!1,!0]
     */
    public static final ABProp FLOWS_WA_WEB_AGM_CTA = new ABProp(24215, "false", "true");

    /**
     * The {@code flows_wa_web_responses_download} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: flows_wa_web_responses_download:[24216,"bool",!1,!0]
     */
    public static final ABProp FLOWS_WA_WEB_RESPONSES_DOWNLOAD = new ABProp(24216, "false", "true");

    /**
     * The {@code enable_mention_everyone_syncd_sender} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_mention_everyone_syncd_sender:[24244,"bool",!1,!1]
     */
    public static final ABProp ENABLE_MENTION_EVERYONE_SYNCD_SENDER = new ABProp(24244, "false", "false");

    /**
     * The {@code web_display_lid_contacts} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_display_lid_contacts:[24280,"bool",!1,!1]
     */
    public static final ABProp WEB_DISPLAY_LID_CONTACTS = new ABProp(24280, "false", "false");

    /**
     * The {@code smb_label_sync_critical_event_logging} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_label_sync_critical_event_logging:[24311,"bool",!1,!0]
     */
    public static final ABProp SMB_LABEL_SYNC_CRITICAL_EVENT_LOGGING = new ABProp(24311, "false", "true");

    /**
     * The {@code web_force_lid_chats_in_history} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_force_lid_chats_in_history:[24343,"bool",!0,!0]
     */
    public static final ABProp WEB_FORCE_LID_CHATS_IN_HISTORY = new ABProp(24343, "true", "true");

    /**
     * The {@code ai_group_send_mentioned_pushname_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_group_send_mentioned_pushname_enabled:[24361,"bool",!1,!1]
     */
    public static final ABProp AI_GROUP_SEND_MENTIONED_PUSHNAME_ENABLED = new ABProp(24361, "false", "false");

    /**
     * The {@code web_log_capacity_override} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_log_capacity_override:[24363,"int",0,0]
     */
    public static final ABProp WEB_LOG_CAPACITY_OVERRIDE = new ABProp(24363, "0", "0");

    /**
     * The {@code wa_consumer_entry_point_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_consumer_entry_point_enabled:[24380,"bool",!1,!1]
     */
    public static final ABProp WA_CONSUMER_ENTRY_POINT_ENABLED = new ABProp(24380, "false", "false");

    /**
     * The {@code web_attach_menu_add_drawing_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_attach_menu_add_drawing_enabled:[24384,"bool",!1,!1]
     */
    public static final ABProp WEB_ATTACH_MENU_ADD_DRAWING_ENABLED = new ABProp(24384, "false", "false");

    /**
     * The {@code br_smb_pix_payment_request_variant} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: br_smb_pix_payment_request_variant:[24388,"int",0,0]
     */
    public static final ABProp BR_SMB_PIX_PAYMENT_REQUEST_VARIANT = new ABProp(24388, "0", "0");

    /**
     * The {@code wa_web_horizontal_link_previews} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_horizontal_link_previews:[24425,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_HORIZONTAL_LINK_PREVIEWS = new ABProp(24425, "false", "false");

    /**
     * The {@code wa_web_enable_follow_up_reply_icon} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_enable_follow_up_reply_icon:[24429,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_ENABLE_FOLLOW_UP_REPLY_ICON = new ABProp(24429, "false", "false");

    /**
     * The {@code wa_web_anyone_can_link_m2} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_anyone_can_link_m2:[24432,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_ANYONE_CAN_LINK_M2 = new ABProp(24432, "false", "false");

    /**
     * The {@code ai_unified_response_qpl_logging} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_unified_response_qpl_logging:[24484,"bool",!1,!1]
     */
    public static final ABProp AI_UNIFIED_RESPONSE_QPL_LOGGING = new ABProp(24484, "false", "false");

    /**
     * The {@code is_ai_mode_selector_visible} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: is_ai_mode_selector_visible:[24489,"bool",!1,!1]
     */
    public static final ABProp IS_AI_MODE_SELECTOR_VISIBLE = new ABProp(24489, "false", "false");

    /**
     * The {@code wa_consumer_nova_entry_point_settings_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_consumer_nova_entry_point_settings_enabled:[24495,"bool",!1,!1]
     */
    public static final ABProp WA_CONSUMER_NOVA_ENTRY_POINT_SETTINGS_ENABLED = new ABProp(24495, "false", "false");

    /**
     * The {@code enable_web_calling_nux} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_web_calling_nux:[24504,"bool",!1,!1]
     */
    public static final ABProp ENABLE_WEB_CALLING_NUX = new ABProp(24504, "false", "false");

    /**
     * The {@code wa_web_reduce_forced_layout_chat_open} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_reduce_forced_layout_chat_open:[24526,"bool",!1,!0]
     */
    public static final ABProp WA_WEB_REDUCE_FORCED_LAYOUT_CHAT_OPEN = new ABProp(24526, "false", "true");

    /**
     * The {@code wa_web_channels_comet_video_player_enabled_v2} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_channels_comet_video_player_enabled_v2:[24541,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_CHANNELS_COMET_VIDEO_PLAYER_ENABLED_V2 = new ABProp(24541, "false", "false");

    /**
     * The {@code wa_web_global_search_prefix_based} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_global_search_prefix_based:[24559,"bool",!1,!0]
     */
    public static final ABProp WA_WEB_GLOBAL_SEARCH_PREFIX_BASED = new ABProp(24559, "false", "true");

    /**
     * The {@code wa_web_multi_ppl_typing_indicator_for_chatlist_groups_variant} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_multi_ppl_typing_indicator_for_chatlist_groups_variant:[24560,"int",0,0]
     */
    public static final ABProp WA_WEB_MULTI_PPL_TYPING_INDICATOR_FOR_CHATLIST_GROUPS_VARIANT = new ABProp(24560, "0", "0");

    /**
     * The {@code group_member_updates_hide_in_thread_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: group_member_updates_hide_in_thread_enabled:[24584,"bool",!1,!1]
     */
    public static final ABProp GROUP_MEMBER_UPDATES_HIDE_IN_THREAD_ENABLED = new ABProp(24584, "false", "false");

    /**
     * The {@code calling_av_sync_webrtc} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: calling_av_sync_webrtc:[24599,"bool",!1,!1]
     */
    public static final ABProp CALLING_AV_SYNC_WEBRTC = new ABProp(24599, "false", "false");

    /**
     * The {@code scheduled_messages_receiver_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: scheduled_messages_receiver_enabled:[24610,"bool",!1,!1]
     */
    public static final ABProp SCHEDULED_MESSAGES_RECEIVER_ENABLED = new ABProp(24610, "false", "false");

    /**
     * The {@code group_member_updates_usernames_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: group_member_updates_usernames_enabled:[24617,"bool",!1,!1]
     */
    public static final ABProp GROUP_MEMBER_UPDATES_USERNAMES_ENABLED = new ABProp(24617, "false", "false");

    /**
     * The {@code ai_group_call_max_version_by_platform} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_group_call_max_version_by_platform:[24655,"int",0,0]
     */
    public static final ABProp AI_GROUP_CALL_MAX_VERSION_BY_PLATFORM = new ABProp(24655, "0", "0");

    /**
     * The {@code ai_group_call_max_version_by_country} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_group_call_max_version_by_country:[24656,"int",0,0]
     */
    public static final ABProp AI_GROUP_CALL_MAX_VERSION_BY_COUNTRY = new ABProp(24656, "0", "0");

    /**
     * The {@code payment_links_trust_signals_other_metatag_kill_switch_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: payment_links_trust_signals_other_metatag_kill_switch_enabled:[24662,"bool",!1,!1]
     */
    public static final ABProp PAYMENT_LINKS_TRUST_SIGNALS_OTHER_METATAG_KILL_SWITCH_ENABLED = new ABProp(24662, "false", "false");

    /**
     * The {@code ctwa_web_native_ads_mvp_qe1_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ctwa_web_native_ads_mvp_qe1_enabled:[24668,"bool",!1,!1]
     */
    public static final ABProp CTWA_WEB_NATIVE_ADS_MVP_QE1_ENABLED = new ABProp(24668, "false", "false");

    /**
     * The {@code ctwa_web_native_ads_mvp_qe2_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ctwa_web_native_ads_mvp_qe2_enabled:[24669,"bool",!1,!1]
     */
    public static final ABProp CTWA_WEB_NATIVE_ADS_MVP_QE2_ENABLED = new ABProp(24669, "false", "false");

    /**
     * The {@code lists_smb_web_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: lists_smb_web_enabled:[24732,"bool",!1,!1]
     */
    public static final ABProp LISTS_SMB_WEB_ENABLED = new ABProp(24732, "false", "false");

    /**
     * The {@code rt_ghs_sender_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: rt_ghs_sender_enabled:[24741,"bool",!1,!0]
     */
    public static final ABProp RT_GHS_SENDER_ENABLED = new ABProp(24741, "false", "true");

    /**
     * The {@code rt_ghs_receiver_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: rt_ghs_receiver_enabled:[24742,"bool",!1,!0]
     */
    public static final ABProp RT_GHS_RECEIVER_ENABLED = new ABProp(24742, "false", "true");

    /**
     * The {@code biz_ai_consumer_tos_notice_iq_web} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: biz_ai_consumer_tos_notice_iq_web:[24754,"bool",!1,!1]
     */
    public static final ABProp BIZ_AI_CONSUMER_TOS_NOTICE_IQ_WEB = new ABProp(24754, "false", "false");

    /**
     * The {@code ctwa_web_native_ads_mvp_qe1_enabled_no_exposure} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ctwa_web_native_ads_mvp_qe1_enabled_no_exposure:[24761,"bool",!1,!1]
     */
    public static final ABProp CTWA_WEB_NATIVE_ADS_MVP_QE1_ENABLED_NO_EXPOSURE = new ABProp(24761, "false", "false");

    /**
     * The {@code wa_web_contact_search_tokenized_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_contact_search_tokenized_enabled:[24773,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_CONTACT_SEARCH_TOKENIZED_ENABLED = new ABProp(24773, "false", "false");

    /**
     * The {@code enable_windows_xdr_chat_handoff} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_windows_xdr_chat_handoff:[24783,"bool",!1,!0]
     */
    public static final ABProp ENABLE_WINDOWS_XDR_CHAT_HANDOFF = new ABProp(24783, "false", "true");

    /**
     * The {@code wa_web_status_comet_video_player_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_status_comet_video_player_enabled:[24791,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_STATUS_COMET_VIDEO_PLAYER_ENABLED = new ABProp(24791, "false", "false");

    /**
     * The {@code aura_stickers_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: aura_stickers_enabled:[24800,"bool",!1,!1]
     */
    public static final ABProp AURA_STICKERS_ENABLED = new ABProp(24800, "false", "false");

    /**
     * The {@code aura_stickers_benefit_active} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: aura_stickers_benefit_active:[24801,"bool",!1,!1]
     */
    public static final ABProp AURA_STICKERS_BENEFIT_ACTIVE = new ABProp(24801, "false", "false");

    /**
     * The {@code enable_web_calling_beta_upsell} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_web_calling_beta_upsell:[24812,"bool",!1,!1]
     */
    public static final ABProp ENABLE_WEB_CALLING_BETA_UPSELL = new ABProp(24812, "false", "false");

    /**
     * The {@code web_request_missing_keys_for_removes} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_request_missing_keys_for_removes:[24838,"bool",!1,!1]
     */
    public static final ABProp WEB_REQUEST_MISSING_KEYS_FOR_REMOVES = new ABProp(24838, "false", "false");

    /**
     * The {@code enable_mention_everyone_receiver_web} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_mention_everyone_receiver_web:[24843,"bool",!1,!1]
     */
    public static final ABProp ENABLE_MENTION_EVERYONE_RECEIVER_WEB = new ABProp(24843, "false", "false");

    /**
     * The {@code enable_mention_everyone_sender_web} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_mention_everyone_sender_web:[24844,"bool",!1,!1]
     */
    public static final ABProp ENABLE_MENTION_EVERYONE_SENDER_WEB = new ABProp(24844, "false", "false");

    /**
     * The {@code enhanced_mention_suggestions_non_group_members_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enhanced_mention_suggestions_non_group_members_enabled:[24852,"bool",!1,!1]
     */
    public static final ABProp ENHANCED_MENTION_SUGGESTIONS_NON_GROUP_MEMBERS_ENABLED = new ABProp(24852, "false", "false");

    /**
     * The {@code cci_compliance_mm} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: cci_compliance_mm:[24853,"bool",!1,!1]
     */
    public static final ABProp CCI_COMPLIANCE_MM = new ABProp(24853, "false", "false");

    /**
     * The {@code web_bulk_add_contacts_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_bulk_add_contacts_enabled:[24875,"bool",!1,!1]
     */
    public static final ABProp WEB_BULK_ADD_CONTACTS_ENABLED = new ABProp(24875, "false", "false");

    /**
     * The {@code poll_end_time_receiving_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: poll_end_time_receiving_enabled:[24884,"bool",!1,!1]
     */
    public static final ABProp POLL_END_TIME_RECEIVING_ENABLED = new ABProp(24884, "false", "false");

    /**
     * The {@code poll_hide_voters_receiving_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: poll_hide_voters_receiving_enabled:[24885,"int",0,0]
     */
    public static final ABProp POLL_HIDE_VOTERS_RECEIVING_ENABLED = new ABProp(24885, "0", "0");

    /**
     * The {@code poll_creator_edit_receiving_version} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: poll_creator_edit_receiving_version:[24886,"int",0,0]
     */
    public static final ABProp POLL_CREATOR_EDIT_RECEIVING_VERSION = new ABProp(24886, "0", "0");

    /**
     * The {@code wa_web_video_comet_video_player_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_video_comet_video_player_enabled:[24905,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_VIDEO_COMET_VIDEO_PLAYER_ENABLED = new ABProp(24905, "false", "false");

    /**
     * The {@code wa_nct_token_salt_creation_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_nct_token_salt_creation_enabled:[24915,"bool",!1,!1]
     */
    public static final ABProp WA_NCT_TOKEN_SALT_CREATION_ENABLED = new ABProp(24915, "false", "false");

    /**
     * The {@code web_worker_adv_processing_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_worker_adv_processing_enabled:[24924,"bool",!1,!1]
     */
    public static final ABProp WEB_WORKER_ADV_PROCESSING_ENABLED = new ABProp(24924, "false", "false");

    /**
     * The {@code wa_nct_token_send_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_nct_token_send_enabled:[24941,"bool",!1,!1]
     */
    public static final ABProp WA_NCT_TOKEN_SEND_ENABLED = new ABProp(24941, "false", "false");

    /**
     * The {@code wa_web_me_tab} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_me_tab:[24944,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_ME_TAB = new ABProp(24944, "false", "false");

    /**
     * The {@code wa_web_self_profile_photo_fix_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_self_profile_photo_fix_enabled:[24945,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_SELF_PROFILE_PHOTO_FIX_ENABLED = new ABProp(24945, "false", "false");

    /**
     * The {@code wa_consumer_nova_settings_green_dot_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_consumer_nova_settings_green_dot_enabled:[24955,"bool",!1,!1]
     */
    public static final ABProp WA_CONSUMER_NOVA_SETTINGS_GREEN_DOT_ENABLED = new ABProp(24955, "false", "false");

    /**
     * The {@code defense_mode_quarantine} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: defense_mode_quarantine:[24959,"bool",!1,!0]
     */
    public static final ABProp DEFENSE_MODE_QUARANTINE = new ABProp(24959, "false", "true");

    /**
     * The {@code cci_compliance_ctwa} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: cci_compliance_ctwa:[24983,"bool",!1,!1]
     */
    public static final ABProp CCI_COMPLIANCE_CTWA = new ABProp(24983, "false", "false");

    /**
     * The {@code ctwa_3pd_opt_out_counter_optimization_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ctwa_3pd_opt_out_counter_optimization_enabled:[24984,"bool",!1,!1]
     */
    public static final ABProp CTWA_3PD_OPT_OUT_COUNTER_OPTIMIZATION_ENABLED = new ABProp(24984, "false", "false");

    /**
     * The {@code enable_waiting_room_logging} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_waiting_room_logging:[24991,"bool",!1,!1]
     */
    public static final ABProp ENABLE_WAITING_ROOM_LOGGING = new ABProp(24991, "false", "false");

    /**
     * The {@code wa_web_reduce_cascading_updates_chat_open} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_reduce_cascading_updates_chat_open:[25006,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_REDUCE_CASCADING_UPDATES_CHAT_OPEN = new ABProp(25006, "false", "false");

    /**
     * The {@code wa_web_anyone_can_link_m2_flood_limit} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_anyone_can_link_m2_flood_limit:[25009,"int",10,10]
     */
    public static final ABProp WA_WEB_ANYONE_CAN_LINK_M2_FLOOD_LIMIT = new ABProp(25009, "10", "10");

    /**
     * The {@code wa_web_status_first_upload_fix_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_status_first_upload_fix_enabled:[25015,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_STATUS_FIRST_UPLOAD_FIX_ENABLED = new ABProp(25015, "false", "false");

    /**
     * The {@code mm_disclosure_learn_more_article_id} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: mm_disclosure_learn_more_article_id:[25021,"string","263784176043634","263784176043634"]
     */
    public static final ABProp MM_DISCLOSURE_LEARN_MORE_ARTICLE_ID = new ABProp(25021, "263784176043634", "263784176043634");

    /**
     * The {@code status_infra_1_1_session_split} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: status_infra_1_1_session_split:[25034,"int",0,0]
     */
    public static final ABProp STATUS_INFRA_1_1_SESSION_SPLIT = new ABProp(25034, "0", "0");

    /**
     * The {@code wa_web_comet_video_player_snapl} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_comet_video_player_snapl:[25065,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_COMET_VIDEO_PLAYER_SNAPL = new ABProp(25065, "false", "false");

    /**
     * The {@code im_bloks_widget_enable} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: im_bloks_widget_enable:[25071,"bool",!1,!0]
     */
    public static final ABProp IM_BLOKS_WIDGET_ENABLE = new ABProp(25071, "false", "true");

    /**
     * The {@code channels_t_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_t_enabled:[25078,"bool",!1,!1]
     */
    public static final ABProp CHANNELS_T_ENABLED = new ABProp(25078, "false", "false");

    /**
     * The {@code wa_web_enable_status_hq_thumbnail} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_enable_status_hq_thumbnail:[25079,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_ENABLE_STATUS_HQ_THUMBNAIL = new ABProp(25079, "false", "false");

    /**
     * The {@code ai_file_upload_supported_file_types} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_file_upload_supported_file_types:[25090,"string","",""]
     */
    public static final ABProp AI_FILE_UPLOAD_SUPPORTED_FILE_TYPES = new ABProp(25090, "", "");

    /**
     * The {@code ai_file_upload_count_limit} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_file_upload_count_limit:[25093,"int",0,1]
     */
    public static final ABProp AI_FILE_UPLOAD_COUNT_LIMIT = new ABProp(25093, "0", "1");

    /**
     * The {@code ai_bot_integration_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_bot_integration_enabled:[25119,"bool",!1,!1]
     */
    public static final ABProp AI_BOT_INTEGRATION_ENABLED = new ABProp(25119, "false", "false");

    /**
     * The {@code wds_web_expressions_panel} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wds_web_expressions_panel:[25144,"bool",!1,!1]
     */
    public static final ABProp WDS_WEB_EXPRESSIONS_PANEL = new ABProp(25144, "false", "false");

    /**
     * The {@code enable_logging_qbm_incoming_message} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_logging_qbm_incoming_message:[25149,"bool",!1,!0]
     */
    public static final ABProp ENABLE_LOGGING_QBM_INCOMING_MESSAGE = new ABProp(25149, "false", "true");

    /**
     * The {@code wa_web_status_viewer_side_poster_identifiers_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_status_viewer_side_poster_identifiers_enabled:[25151,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_STATUS_VIEWER_SIDE_POSTER_IDENTIFIERS_ENABLED = new ABProp(25151, "false", "false");

    /**
     * The {@code enable_web_voip_platform_av_sync} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_web_voip_platform_av_sync:[25177,"bool",!1,!1]
     */
    public static final ABProp ENABLE_WEB_VOIP_PLATFORM_AV_SYNC = new ABProp(25177, "false", "false");

    /**
     * The {@code ctwa_smb_label_chat_header_enabled_web} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ctwa_smb_label_chat_header_enabled_web:[25180,"bool",!1,!1]
     */
    public static final ABProp CTWA_SMB_LABEL_CHAT_HEADER_ENABLED_WEB = new ABProp(25180, "false", "false");

    /**
     * The {@code wa_nct_token_history_sync_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_nct_token_history_sync_enabled:[25189,"bool",!1,!1]
     */
    public static final ABProp WA_NCT_TOKEN_HISTORY_SYNC_ENABLED = new ABProp(25189, "false", "false");

    /**
     * The {@code smb_business_broadcast_multi_audience_send_web} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_business_broadcast_multi_audience_send_web:[25206,"bool",!1,!1]
     */
    public static final ABProp SMB_BUSINESS_BROADCAST_MULTI_AUDIENCE_SEND_WEB = new ABProp(25206, "false", "false");

    /**
     * The {@code aura_stickers_overlay_animation_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: aura_stickers_overlay_animation_enabled:[25210,"bool",!1,!1]
     */
    public static final ABProp AURA_STICKERS_OVERLAY_ANIMATION_ENABLED = new ABProp(25210, "false", "false");

    /**
     * The {@code wa_nct_token_syncd_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_nct_token_syncd_enabled:[25253,"bool",!1,!1]
     */
    public static final ABProp WA_NCT_TOKEN_SYNCD_ENABLED = new ABProp(25253, "false", "false");

    /**
     * The {@code acp_removal} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: acp_removal:[25255,"bool",!1,!1]
     */
    public static final ABProp ACP_REMOVAL = new ABProp(25255, "false", "false");

    /**
     * The {@code ai_bot_integration_bot_profile} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_bot_integration_bot_profile:[25268,"string","",""]
     */
    public static final ABProp AI_BOT_INTEGRATION_BOT_PROFILE = new ABProp(25268, "", "");

    /**
     * The {@code ai_dynamic_mode_selector_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_dynamic_mode_selector_enabled:[25287,"bool",!1,!1]
     */
    public static final ABProp AI_DYNAMIC_MODE_SELECTOR_ENABLED = new ABProp(25287, "false", "false");

    /**
     * The {@code wa_web_group_info_notification_row} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_group_info_notification_row:[25292,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_GROUP_INFO_NOTIFICATION_ROW = new ABProp(25292, "false", "false");

    /**
     * The {@code wa_web_wam_falco_mode} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_wam_falco_mode:[25306,"int",0,0]
     */
    public static final ABProp WA_WEB_WAM_FALCO_MODE = new ABProp(25306, "0", "0");

    /**
     * The {@code wa_web_wam_falco_shadow_event_ids} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_wam_falco_shadow_event_ids:[25309,"string","",""]
     */
    public static final ABProp WA_WEB_WAM_FALCO_SHADOW_EVENT_IDS = new ABProp(25309, "", "");

    /**
     * The {@code wa_web_search_empty_state_m1} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_search_empty_state_m1:[25310,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_SEARCH_EMPTY_STATE_M1 = new ABProp(25310, "false", "false");

    /**
     * The {@code wa_web_ur_imagine_video_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_ur_imagine_video_enabled:[25329,"bool",!1,!0]
     */
    public static final ABProp WA_WEB_UR_IMAGINE_VIDEO_ENABLED = new ABProp(25329, "false", "true");

    /**
     * The {@code wa_web_imagine_ur_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_imagine_ur_enabled:[25331,"bool",!1,!0]
     */
    public static final ABProp WA_WEB_IMAGINE_UR_ENABLED = new ABProp(25331, "false", "true");

    /**
     * The {@code wa_web_ur_bloks_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_ur_bloks_enabled:[25332,"bool",!1,!0]
     */
    public static final ABProp WA_WEB_UR_BLOKS_ENABLED = new ABProp(25332, "false", "true");

    /**
     * The {@code wds_web_submenus} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wds_web_submenus:[25351,"bool",!1,!0]
     */
    public static final ABProp WDS_WEB_SUBMENUS = new ABProp(25351, "false", "true");

    /**
     * The {@code username_exposed_logging_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: username_exposed_logging_enabled:[25353,"bool",!1,!1]
     */
    public static final ABProp USERNAME_EXPOSED_LOGGING_ENABLED = new ABProp(25353, "false", "false");

    /**
     * The {@code cci_compliance_ctwa_learn_more_hyperlink} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: cci_compliance_ctwa_learn_more_hyperlink:[25366,"string","https://faq.whatsapp.com/785493319976156/","https://faq.whatsapp.com/785493319976156/"]
     */
    public static final ABProp CCI_COMPLIANCE_CTWA_LEARN_MORE_HYPERLINK = new ABProp(25366, "https://faq.whatsapp.com/785493319976156/", "https://faq.whatsapp.com/785493319976156/");

    /**
     * The {@code wa_consumer_nova_eligibility_subscription_status_check_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_consumer_nova_eligibility_subscription_status_check_enabled:[25388,"bool",!1,!1]
     */
    public static final ABProp WA_CONSUMER_NOVA_ELIGIBILITY_SUBSCRIPTION_STATUS_CHECK_ENABLED = new ABProp(25388, "false", "false");

    /**
     * The {@code enable_web_voip_dynamic_fps_throttle} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_web_voip_dynamic_fps_throttle:[25394,"bool",!0,!0]
     */
    public static final ABProp ENABLE_WEB_VOIP_DYNAMIC_FPS_THROTTLE = new ABProp(25394, "true", "true");

    /**
     * The {@code wa_web_highlight_me_mention} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_highlight_me_mention:[25408,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_HIGHLIGHT_ME_MENTION = new ABProp(25408, "false", "false");

    /**
     * The {@code web_anr_batch_and_queue_bulk_contacts_db_writes_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_anr_batch_and_queue_bulk_contacts_db_writes_enabled:[25413,"bool",!1,!1]
     */
    public static final ABProp WEB_ANR_BATCH_AND_QUEUE_BULK_CONTACTS_DB_WRITES_ENABLED = new ABProp(25413, "false", "false");

    /**
     * The {@code web_group_experimentation_enable} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_group_experimentation_enable:[25414,"bool",!1,!1]
     */
    public static final ABProp WEB_GROUP_EXPERIMENTATION_ENABLE = new ABProp(25414, "false", "false");

    /**
     * The {@code mm_data_sharing_disclosure_enabled_additional_transparency_large_screens} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: mm_data_sharing_disclosure_enabled_additional_transparency_large_screens:[25421,"bool",!1,!1]
     */
    public static final ABProp MM_DATA_SHARING_DISCLOSURE_ENABLED_ADDITIONAL_TRANSPARENCY_LARGE_SCREENS = new ABProp(25421, "false", "false");

    /**
     * The {@code wa_web_default_profile_pics} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_default_profile_pics:[25455,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_DEFAULT_PROFILE_PICS = new ABProp(25455, "false", "false");

    /**
     * The {@code biz_vpv_impression_logging_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: biz_vpv_impression_logging_enabled:[25465,"bool",!1,!1]
     */
    public static final ABProp BIZ_VPV_IMPRESSION_LOGGING_ENABLED = new ABProp(25465, "false", "false");

    /**
     * The {@code ai_chat_threads_pin_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_chat_threads_pin_enabled:[25517,"bool",!1,!1]
     */
    public static final ABProp AI_CHAT_THREADS_PIN_ENABLED = new ABProp(25517, "false", "false");

    /**
     * The {@code ai_chat_threads_pin_max_count} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_chat_threads_pin_max_count:[25520,"int",3,3]
     */
    public static final ABProp AI_CHAT_THREADS_PIN_MAX_COUNT = new ABProp(25520, "3", "3");

    /**
     * The {@code ai_file_upload_size_limit_mb} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_file_upload_size_limit_mb:[25524,"int",40,40]
     */
    public static final ABProp AI_FILE_UPLOAD_SIZE_LIMIT_MB = new ABProp(25524, "40", "40");

    /**
     * The {@code wa_web_notify_for} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_notify_for:[25544,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_NOTIFY_FOR = new ABProp(25544, "false", "false");

    /**
     * The {@code br_payments_payment_request_cta} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: br_payments_payment_request_cta:[25599,"bool",!1,!1]
     */
    public static final ABProp BR_PAYMENTS_PAYMENT_REQUEST_CTA = new ABProp(25599, "false", "false");

    /**
     * The {@code wa_web_chat_search_entrypoint} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_chat_search_entrypoint:[25609,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_CHAT_SEARCH_ENTRYPOINT = new ABProp(25609, "false", "false");

    /**
     * The {@code enable_web_voip_p2p} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_web_voip_p2p:[25621,"bool",!1,!1]
     */
    public static final ABProp ENABLE_WEB_VOIP_P2P = new ABProp(25621, "false", "false");

    /**
     * The {@code sticker_store_testing_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: sticker_store_testing_enabled:[25639,"bool",!1,!1]
     */
    public static final ABProp STICKER_STORE_TESTING_ENABLED = new ABProp(25639, "false", "false");

    /**
     * The {@code web_media_compute_in_worker_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_media_compute_in_worker_enabled:[25641,"bool",!1,!1]
     */
    public static final ABProp WEB_MEDIA_COMPUTE_IN_WORKER_ENABLED = new ABProp(25641, "false", "false");

    /**
     * The {@code after_read_sending_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: after_read_sending_enabled:[25648,"bool",!1,!1]
     */
    public static final ABProp AFTER_READ_SENDING_ENABLED = new ABProp(25648, "false", "false");

    /**
     * The {@code after_read_receiver_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: after_read_receiver_enabled:[25649,"bool",!1,!1]
     */
    public static final ABProp AFTER_READ_RECEIVER_ENABLED = new ABProp(25649, "false", "false");

    /**
     * The {@code wa_web_base_video_comet_video_player_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_base_video_comet_video_player_enabled:[25660,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_BASE_VIDEO_COMET_VIDEO_PLAYER_ENABLED = new ABProp(25660, "false", "false");

    /**
     * The {@code wa_web_group_discard_dialog_contact_threshold} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_group_discard_dialog_contact_threshold:[25682,"int",-1,2]
     */
    public static final ABProp WA_WEB_GROUP_DISCARD_DIALOG_CONTACT_THRESHOLD = new ABProp(25682, "-1", "2");

    /**
     * The {@code poll_add_option_receiving_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: poll_add_option_receiving_enabled:[25758,"int",0,0]
     */
    public static final ABProp POLL_ADD_OPTION_RECEIVING_ENABLED = new ABProp(25758, "0", "0");

    /**
     * The {@code username_key_upsell_max_numbers} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: username_key_upsell_max_numbers:[25789,"int",1,1]
     */
    public static final ABProp USERNAME_KEY_UPSELL_MAX_NUMBERS = new ABProp(25789, "1", "1");

    /**
     * The {@code username_key_upsell_max_characters} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: username_key_upsell_max_characters:[25790,"int",8,8]
     */
    public static final ABProp USERNAME_KEY_UPSELL_MAX_CHARACTERS = new ABProp(25790, "8", "8");

    /**
     * The {@code ai_dynamic_mode_selector_ttl_seconds} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_dynamic_mode_selector_ttl_seconds:[25797,"int",86400,86400]
     */
    public static final ABProp AI_DYNAMIC_MODE_SELECTOR_TTL_SECONDS = new ABProp(25797, "86400", "86400");

    /**
     * The {@code wa_web_lists_full_width_filters} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_lists_full_width_filters:[25805,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_LISTS_FULL_WIDTH_FILTERS = new ABProp(25805, "false", "false");

    /**
     * The {@code wa_web_groups_in_common_multi_contact} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_groups_in_common_multi_contact:[25808,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_GROUPS_IN_COMMON_MULTI_CONTACT = new ABProp(25808, "false", "false");

    /**
     * The {@code web_date_marker_calendar_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_date_marker_calendar_enabled:[25811,"bool",!1,!0]
     */
    public static final ABProp WEB_DATE_MARKER_CALENDAR_ENABLED = new ABProp(25811, "false", "true");

    /**
     * The {@code contacts_from_common_groups_section_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: contacts_from_common_groups_section_enabled:[25817,"bool",!1,!0]
     */
    public static final ABProp CONTACTS_FROM_COMMON_GROUPS_SECTION_ENABLED = new ABProp(25817, "false", "true");

    /**
     * The {@code channel_us_ncii_reporting_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channel_us_ncii_reporting_enabled:[25818,"bool",!1,!0]
     */
    public static final ABProp CHANNEL_US_NCII_REPORTING_ENABLED = new ABProp(25818, "false", "true");

    /**
     * The {@code wa_web_copy_link_url_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_copy_link_url_enabled:[25820,"bool",!1,!0]
     */
    public static final ABProp WA_WEB_COPY_LINK_URL_ENABLED = new ABProp(25820, "false", "true");

    /**
     * The {@code web_calendar_message_density_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_calendar_message_density_enabled:[25823,"bool",!1,!0]
     */
    public static final ABProp WEB_CALENDAR_MESSAGE_DENSITY_ENABLED = new ABProp(25823, "false", "true");

    /**
     * The {@code wa_web_highlight_me_mention_groupsize_threshold} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_highlight_me_mention_groupsize_threshold:[25836,"int",130,130]
     */
    public static final ABProp WA_WEB_HIGHLIGHT_ME_MENTION_GROUPSIZE_THRESHOLD = new ABProp(25836, "130", "130");

    /**
     * The {@code wa_web_biz_profile_graphql_migration} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_biz_profile_graphql_migration:[25846,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_BIZ_PROFILE_GRAPHQL_MIGRATION = new ABProp(25846, "false", "false");

    /**
     * The {@code enable_web_voip_video_resolution_cap} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_web_voip_video_resolution_cap:[25899,"bool",!0,!0]
     */
    public static final ABProp ENABLE_WEB_VOIP_VIDEO_RESOLUTION_CAP = new ABProp(25899, "true", "true");

    /**
     * The {@code group_history_bundle_time_limit_receiver_enforcement_secs} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: group_history_bundle_time_limit_receiver_enforcement_secs:[25910,"int",1209600,1209600]
     */
    public static final ABProp GROUP_HISTORY_BUNDLE_TIME_LIMIT_RECEIVER_ENFORCEMENT_SECS = new ABProp(25910, "1209600", "1209600");

    /**
     * The {@code web_anr_noop_gc_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_anr_noop_gc_enabled:[25915,"bool",!1,!1]
     */
    public static final ABProp WEB_ANR_NOOP_GC_ENABLED = new ABProp(25915, "false", "false");

    /**
     * The {@code ai_unified_response_receiver_web_enabled_v2} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_unified_response_receiver_web_enabled_v2:[25929,"bool",!1,!1]
     */
    public static final ABProp AI_UNIFIED_RESPONSE_RECEIVER_WEB_ENABLED_V2 = new ABProp(25929, "false", "false");

    /**
     * The {@code ai_unified_response_receiver_web_timestamp_v2} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_unified_response_receiver_web_timestamp_v2:[25930,"int",1772082000,1772082000]
     */
    public static final ABProp AI_UNIFIED_RESPONSE_RECEIVER_WEB_TIMESTAMP_V2 = new ABProp(25930, "1772082000", "1772082000");

    /**
     * The {@code wa_web_preload_conversation_chat_open} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_preload_conversation_chat_open:[25937,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_PRELOAD_CONVERSATION_CHAT_OPEN = new ABProp(25937, "false", "false");

    /**
     * The {@code enhanced_mention_limit} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enhanced_mention_limit:[25951,"int",5,5]
     */
    public static final ABProp ENHANCED_MENTION_LIMIT = new ABProp(25951, "5", "5");

    /**
     * The {@code acp_removal_epoch_time} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: acp_removal_epoch_time:[25993,"int",1782518400,1782518400]
     */
    public static final ABProp ACP_REMOVAL_EPOCH_TIME = new ABProp(25993, "1782518400", "1782518400");

    /**
     * The {@code support_contact_form_using_graphql} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: support_contact_form_using_graphql:[26001,"bool",!1,!1]
     */
    public static final ABProp SUPPORT_CONTACT_FORM_USING_GRAPHQL = new ABProp(26001, "false", "false");

    /**
     * The {@code enable_web_voip_proxy_and_sctp_workers} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_web_voip_proxy_and_sctp_workers:[26012,"bool",!0,!0]
     */
    public static final ABProp ENABLE_WEB_VOIP_PROXY_AND_SCTP_WORKERS = new ABProp(26012, "true", "true");

    /**
     * The {@code transcode_and_repair_videos} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: transcode_and_repair_videos:[26027,"bool",!1,!1]
     */
    public static final ABProp TRANSCODE_AND_REPAIR_VIDEOS = new ABProp(26027, "false", "false");

    /**
     * The {@code br_pix_key_bubble_content_update} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: br_pix_key_bubble_content_update:[26033,"bool",!1,!1]
     */
    public static final ABProp BR_PIX_KEY_BUBBLE_CONTENT_UPDATE = new ABProp(26033, "false", "false");

    /**
     * The {@code group_history_out_of_window_pin_sender} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: group_history_out_of_window_pin_sender:[26037,"bool",!1,!1]
     */
    public static final ABProp GROUP_HISTORY_OUT_OF_WINDOW_PIN_SENDER = new ABProp(26037, "false", "false");

    /**
     * The {@code group_history_out_of_window_pins_receiver} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: group_history_out_of_window_pins_receiver:[26039,"bool",!1,!1]
     */
    public static final ABProp GROUP_HISTORY_OUT_OF_WINDOW_PINS_RECEIVER = new ABProp(26039, "false", "false");

    /**
     * The {@code tappable_links_in_poll_option_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: tappable_links_in_poll_option_enabled:[26062,"bool",!1,!1]
     */
    public static final ABProp TAPPABLE_LINKS_IN_POLL_OPTION_ENABLED = new ABProp(26062, "false", "false");

    /**
     * The {@code enable_webcodec_video_encode} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_webcodec_video_encode:[26079,"bool",!1,!1]
     */
    public static final ABProp ENABLE_WEBCODEC_VIDEO_ENCODE = new ABProp(26079, "false", "false");

    /**
     * The {@code aura_subscription_simulation_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: aura_subscription_simulation_enabled:[26086,"bool",!1,!1]
     */
    public static final ABProp AURA_SUBSCRIPTION_SIMULATION_ENABLED = new ABProp(26086, "false", "false");

    /**
     * The {@code wa_web_reactions_motion_v2_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_reactions_motion_v2_enabled:[26102,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_REACTIONS_MOTION_V2_ENABLED = new ABProp(26102, "false", "false");

    /**
     * The {@code improve_group_reporting} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: improve_group_reporting:[26114,"bool",!1,!0]
     */
    public static final ABProp IMPROVE_GROUP_REPORTING = new ABProp(26114, "false", "true");

    /**
     * The {@code channels_follower_invite_creation_modal_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_follower_invite_creation_modal_enabled:[26120,"bool",!1,!0]
     */
    public static final ABProp CHANNELS_FOLLOWER_INVITE_CREATION_MODAL_ENABLED = new ABProp(26120, "false", "true");

    /**
     * The {@code web_worker_prekey_processing_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_worker_prekey_processing_enabled:[26133,"bool",!1,!1]
     */
    public static final ABProp WEB_WORKER_PREKEY_PROCESSING_ENABLED = new ABProp(26133, "false", "false");

    /**
     * The {@code waweb_crossposting_attributions} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: waweb_crossposting_attributions:[26138,"bool",!1,!1]
     */
    public static final ABProp WAWEB_CROSSPOSTING_ATTRIBUTIONS = new ABProp(26138, "false", "false");

    /**
     * The {@code channels_forward_counter_on_status_card_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_forward_counter_on_status_card_enabled:[26148,"bool",!1,!1]
     */
    public static final ABProp CHANNELS_FORWARD_COUNTER_ON_STATUS_CARD_ENABLED = new ABProp(26148, "false", "false");

    /**
     * The {@code smb_web_customer_management_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_web_customer_management_enabled:[26165,"bool",!1,!1]
     */
    public static final ABProp SMB_WEB_CUSTOMER_MANAGEMENT_ENABLED = new ABProp(26165, "false", "false");

    /**
     * The {@code ai_pdfn_nux_ai_group_tee_discover_notice_id} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_pdfn_nux_ai_group_tee_discover_notice_id:[26171,"string","20260212","20260212"]
     */
    public static final ABProp AI_PDFN_NUX_AI_GROUP_TEE_DISCOVER_NOTICE_ID = new ABProp(26171, "20260212", "20260212");

    /**
     * The {@code gc_device_switching_killswitch} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: gc_device_switching_killswitch:[26182,"bool",!1,!1]
     */
    public static final ABProp GC_DEVICE_SWITCHING_KILLSWITCH = new ABProp(26182, "false", "false");

    /**
     * The {@code ai_hatch_integration_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_hatch_integration_enabled:[26189,"bool",!1,!1]
     */
    public static final ABProp AI_HATCH_INTEGRATION_ENABLED = new ABProp(26189, "false", "false");

    /**
     * The {@code ai_hatch_integration_bot_profile} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_hatch_integration_bot_profile:[26190,"string","",""]
     */
    public static final ABProp AI_HATCH_INTEGRATION_BOT_PROFILE = new ABProp(26190, "", "");

    /**
     * The {@code is_individual_suspicious_fmx_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: is_individual_suspicious_fmx_enabled:[26191,"bool",!1,!0]
     */
    public static final ABProp IS_INDIVIDUAL_SUSPICIOUS_FMX_ENABLED = new ABProp(26191, "false", "true");

    /**
     * The {@code web_thread_loading_infra_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_thread_loading_infra_enabled:[26192,"bool",!1,!0]
     */
    public static final ABProp WEB_THREAD_LOADING_INFRA_ENABLED = new ABProp(26192, "false", "true");

    /**
     * The {@code wa_web_wam_falco_logging_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_wam_falco_logging_enabled:[26200,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_WAM_FALCO_LOGGING_ENABLED = new ABProp(26200, "false", "false");

    /**
     * The {@code wa_web_export_chat} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_export_chat:[26201,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_EXPORT_CHAT = new ABProp(26201, "false", "false");

    /**
     * The {@code suggested_audiences_wa_web} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: suggested_audiences_wa_web:[26207,"bool",!1,!1]
     */
    public static final ABProp SUGGESTED_AUDIENCES_WA_WEB = new ABProp(26207, "false", "false");

    /**
     * The {@code web_pnless_stanzas} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_pnless_stanzas:[26211,"bool",!1,!1]
     */
    public static final ABProp WEB_PNLESS_STANZAS = new ABProp(26211, "false", "false");

    /**
     * The {@code newsletter_status_receiver_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: newsletter_status_receiver_enabled:[26217,"bool",!1,!0]
     */
    public static final ABProp NEWSLETTER_STATUS_RECEIVER_ENABLED = new ABProp(26217, "false", "true");

    /**
     * The {@code dm_receiver_after_read_allow_values} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: dm_receiver_after_read_allow_values:[26218,"string","{\"timers\": [0, 900]}","{\"timers\": [0, 900]}"]
     */
    public static final ABProp DM_RECEIVER_AFTER_READ_ALLOW_VALUES = new ABProp(26218, "{\"timers\": [0, 900]}", "{\"timers\": [0, 900]}");

    /**
     * The {@code username_key_upsell_mode} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: username_key_upsell_mode:[26220,"int",0,1]
     */
    public static final ABProp USERNAME_KEY_UPSELL_MODE = new ABProp(26220, "0", "1");

    /**
     * The {@code after_read_fallback_duration} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: after_read_fallback_duration:[26225,"int",86400,86400]
     */
    public static final ABProp AFTER_READ_FALLBACK_DURATION = new ABProp(26225, "86400", "86400");

    /**
     * The {@code wa_asteria_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_asteria_enabled:[26234,"bool",!1,!1]
     */
    public static final ABProp WA_ASTERIA_ENABLED = new ABProp(26234, "false", "false");

    /**
     * The {@code web_calling_enable_on_windows} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_calling_enable_on_windows:[26259,"bool",!1,!1]
     */
    public static final ABProp WEB_CALLING_ENABLE_ON_WINDOWS = new ABProp(26259, "false", "false");

    /**
     * The {@code group_suspension_appeals_redesign_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: group_suspension_appeals_redesign_enabled:[26276,"bool",!1,!1]
     */
    public static final ABProp GROUP_SUSPENSION_APPEALS_REDESIGN_ENABLED = new ABProp(26276, "false", "false");

    /**
     * The {@code wa_webtp_edit_pdf_in_whatsapp_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_webtp_edit_pdf_in_whatsapp_enabled:[26279,"bool",!1,!1]
     */
    public static final ABProp WA_WEBTP_EDIT_PDF_IN_WHATSAPP_ENABLED = new ABProp(26279, "false", "false");

    /**
     * The {@code mm_1pd_post_dc_new_schema_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: mm_1pd_post_dc_new_schema_enabled:[26280,"bool",!1,!1]
     */
    public static final ABProp MM_1PD_POST_DC_NEW_SCHEMA_ENABLED = new ABProp(26280, "false", "false");

    /**
     * The {@code mm_1pd_post_dc_depth_limit} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: mm_1pd_post_dc_depth_limit:[26281,"int",0,0]
     */
    public static final ABProp MM_1PD_POST_DC_DEPTH_LIMIT = new ABProp(26281, "0", "0");

    /**
     * The {@code mm_1pd_post_dc_old_schema_disabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: mm_1pd_post_dc_old_schema_disabled:[26282,"bool",!1,!1]
     */
    public static final ABProp MM_1PD_POST_DC_OLD_SCHEMA_DISABLED = new ABProp(26282, "false", "false");

    /**
     * The {@code web_crosspost_settings_sync} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_crosspost_settings_sync:[26296,"int",0,0]
     */
    public static final ABProp WEB_CROSSPOST_SETTINGS_SYNC = new ABProp(26296, "0", "0");

    /**
     * The {@code ctwa_block_ib_ar_for_wabai} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ctwa_block_ib_ar_for_wabai:[26302,"bool",!1,!0]
     */
    public static final ABProp CTWA_BLOCK_IB_AR_FOR_WABAI = new ABProp(26302, "false", "true");

    /**
     * The {@code bug_reporting_attach_view_dump_pre_bug_creation} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: bug_reporting_attach_view_dump_pre_bug_creation:[26307,"bool",!0,!1]
     */
    public static final ABProp BUG_REPORTING_ATTACH_VIEW_DUMP_PRE_BUG_CREATION = new ABProp(26307, "true", "false");

    /**
     * The {@code bug_reporting_attach_pathfinder_pre_bug_creation} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: bug_reporting_attach_pathfinder_pre_bug_creation:[26311,"bool",!0,!1]
     */
    public static final ABProp BUG_REPORTING_ATTACH_PATHFINDER_PRE_BUG_CREATION = new ABProp(26311, "true", "false");

    /**
     * The {@code smb_qp_conversion_tracking_infra} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_qp_conversion_tracking_infra:[26331,"bool",!1,!1]
     */
    public static final ABProp SMB_QP_CONVERSION_TRACKING_INFRA = new ABProp(26331, "false", "false");

    /**
     * The {@code calling_rust_migration_incoming_stanza_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: calling_rust_migration_incoming_stanza_enabled:[26338,"string","",""]
     */
    public static final ABProp CALLING_RUST_MIGRATION_INCOMING_STANZA_ENABLED = new ABProp(26338, "", "");

    /**
     * The {@code aura_status_search_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: aura_status_search_enabled:[26346,"bool",!1,!1]
     */
    public static final ABProp AURA_STATUS_SEARCH_ENABLED = new ABProp(26346, "false", "false");

    /**
     * The {@code scheduled_messages_window_duration_max_seconds} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: scheduled_messages_window_duration_max_seconds:[26347,"int",1209600,1209600]
     */
    public static final ABProp SCHEDULED_MESSAGES_WINDOW_DURATION_MAX_SECONDS = new ABProp(26347, "1209600", "1209600");

    /**
     * The {@code scheduled_messages_window_duration_min_seconds} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: scheduled_messages_window_duration_min_seconds:[26348,"int",600,600]
     */
    public static final ABProp SCHEDULED_MESSAGES_WINDOW_DURATION_MIN_SECONDS = new ABProp(26348, "600", "600");

    /**
     * The {@code wa_web_attach_icon_variant} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_attach_icon_variant:[26386,"int",0,0]
     */
    public static final ABProp WA_WEB_ATTACH_ICON_VARIANT = new ABProp(26386, "0", "0");

    /**
     * The {@code inapp_signup_confirmation_message_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: inapp_signup_confirmation_message_enabled:[26390,"bool",!1,!0]
     */
    public static final ABProp INAPP_SIGNUP_CONFIRMATION_MESSAGE_ENABLED = new ABProp(26390, "false", "true");

    /**
     * The {@code channels_photo_polls_genai_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_photo_polls_genai_enabled:[26392,"bool",!1,!1]
     */
    public static final ABProp CHANNELS_PHOTO_POLLS_GENAI_ENABLED = new ABProp(26392, "false", "false");

    /**
     * The {@code wa_asteria_eligibility_subscription_status_check_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_asteria_eligibility_subscription_status_check_enabled:[26399,"bool",!1,!1]
     */
    public static final ABProp WA_ASTERIA_ELIGIBILITY_SUBSCRIPTION_STATUS_CHECK_ENABLED = new ABProp(26399, "false", "false");

    /**
     * The {@code calling_e2e_keygen_via_self_lid} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: calling_e2e_keygen_via_self_lid:[26411,"int",0,0]
     */
    public static final ABProp CALLING_E2E_KEYGEN_VIA_SELF_LID = new ABProp(26411, "0", "0");

    /**
     * The {@code native_lib_sandboxing_enable_libwebp} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: native_lib_sandboxing_enable_libwebp:[26414,"bool",!1,!0]
     */
    public static final ABProp NATIVE_LIB_SANDBOXING_ENABLE_LIBWEBP = new ABProp(26414, "false", "true");

    /**
     * The {@code business_broadcast_campaign_syncd_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: business_broadcast_campaign_syncd_enabled:[26426,"bool",!0,!0]
     */
    public static final ABProp BUSINESS_BROADCAST_CAMPAIGN_SYNCD_ENABLED = new ABProp(26426, "true", "true");

    /**
     * The {@code enable_offer_v2_upgrade} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_offer_v2_upgrade:[26435,"bool",!1,!1]
     */
    public static final ABProp ENABLE_OFFER_V2_UPGRADE = new ABProp(26435, "false", "false");

    /**
     * The {@code smb_core_biz_profile_preview} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_core_biz_profile_preview:[26441,"bool",!1,!1]
     */
    public static final ABProp SMB_CORE_BIZ_PROFILE_PREVIEW = new ABProp(26441, "false", "false");

    /**
     * The {@code ai_hatch_integration_history_sync_pre_chatd_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_hatch_integration_history_sync_pre_chatd_enabled:[26445,"bool",!1,!1]
     */
    public static final ABProp AI_HATCH_INTEGRATION_HISTORY_SYNC_PRE_CHATD_ENABLED = new ABProp(26445, "false", "false");

    /**
     * The {@code group_history_send_after_join} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: group_history_send_after_join:[26451,"bool",!1,!1]
     */
    public static final ABProp GROUP_HISTORY_SEND_AFTER_JOIN = new ABProp(26451, "false", "false");

    /**
     * The {@code stickers_emoji_tagging_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: stickers_emoji_tagging_enabled:[26465,"bool",!1,!1]
     */
    public static final ABProp STICKERS_EMOJI_TAGGING_ENABLED = new ABProp(26465, "false", "false");

    /**
     * The {@code wa_web_agm_signup_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_agm_signup_enabled:[26467,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_AGM_SIGNUP_ENABLED = new ABProp(26467, "false", "false");

    /**
     * The {@code web_status_likes_send_v2_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_status_likes_send_v2_enabled:[26470,"bool",!1,!0]
     */
    public static final ABProp WEB_STATUS_LIKES_SEND_V2_ENABLED = new ABProp(26470, "false", "true");

    /**
     * The {@code wa_web_feature_parity_small_wins} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_feature_parity_small_wins:[26481,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_FEATURE_PARITY_SMALL_WINS = new ABProp(26481, "false", "false");

    /**
     * The {@code auth_agents_consumer_exp_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: auth_agents_consumer_exp_enabled:[26492,"bool",!1,!0]
     */
    public static final ABProp AUTH_AGENTS_CONSUMER_EXP_ENABLED = new ABProp(26492, "false", "true");

    /**
     * The {@code ai_hatch_integration_history_sync_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_hatch_integration_history_sync_enabled:[26517,"bool",!1,!1]
     */
    public static final ABProp AI_HATCH_INTEGRATION_HISTORY_SYNC_ENABLED = new ABProp(26517, "false", "false");

    /**
     * The {@code ctwa_lead_taxonomy} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ctwa_lead_taxonomy:[26531,"bool",!1,!0]
     */
    public static final ABProp CTWA_LEAD_TAXONOMY = new ABProp(26531, "false", "true");

    /**
     * The {@code aura_status_search_max_viewers} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: aura_status_search_max_viewers:[26545,"int",1000,1000]
     */
    public static final ABProp AURA_STATUS_SEARCH_MAX_VIEWERS = new ABProp(26545, "1000", "1000");

    /**
     * The {@code aura_status_search_timeout_threshold} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: aura_status_search_timeout_threshold:[26546,"int",5,5]
     */
    public static final ABProp AURA_STATUS_SEARCH_TIMEOUT_THRESHOLD = new ABProp(26546, "5", "5");

    /**
     * The {@code is_expand_fmx_account_age_ui_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: is_expand_fmx_account_age_ui_enabled:[26548,"bool",!1,!0]
     */
    public static final ABProp IS_EXPAND_FMX_ACCOUNT_AGE_UI_ENABLED = new ABProp(26548, "false", "true");

    /**
     * The {@code is_expand_fmx_account_age_bolded_non_auto_expose} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: is_expand_fmx_account_age_bolded_non_auto_expose:[26549,"bool",!1,!0]
     */
    public static final ABProp IS_EXPAND_FMX_ACCOUNT_AGE_BOLDED_NON_AUTO_EXPOSE = new ABProp(26549, "false", "true");

    /**
     * The {@code is_expand_fmx_mex_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: is_expand_fmx_mex_enabled:[26550,"bool",!1,!0]
     */
    public static final ABProp IS_EXPAND_FMX_MEX_ENABLED = new ABProp(26550, "false", "true");

    /**
     * The {@code is_expand_fmx_enabled_non_auto_expose} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: is_expand_fmx_enabled_non_auto_expose:[26551,"bool",!1,!0]
     */
    public static final ABProp IS_EXPAND_FMX_ENABLED_NON_AUTO_EXPOSE = new ABProp(26551, "false", "true");

    /**
     * The {@code wa_web_pre_chat_device_id_test} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_pre_chat_device_id_test:[26553,"bool",!1,!0]
     */
    public static final ABProp WA_WEB_PRE_CHAT_DEVICE_ID_TEST = new ABProp(26553, "false", "true");

    /**
     * The {@code wa_web_download_mimetype_check_block_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_download_mimetype_check_block_enabled:[26555,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_DOWNLOAD_MIMETYPE_CHECK_BLOCK_ENABLED = new ABProp(26555, "false", "false");

    /**
     * The {@code aura_stickers_preview_max_animation_count} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: aura_stickers_preview_max_animation_count:[26602,"int",5,5]
     */
    public static final ABProp AURA_STICKERS_PREVIEW_MAX_ANIMATION_COUNT = new ABProp(26602, "5", "5");

    /**
     * The {@code wa_web_show_hd_photo} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_show_hd_photo:[26610,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_SHOW_HD_PHOTO = new ABProp(26610, "false", "false");

    /**
     * The {@code ai_bizai_2way_integration_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_bizai_2way_integration_enabled:[26613,"bool",!1,!1]
     */
    public static final ABProp AI_BIZAI_2WAY_INTEGRATION_ENABLED = new ABProp(26613, "false", "false");

    /**
     * The {@code ai_bizai_2way_integration_history_sync_pre_chatd_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_bizai_2way_integration_history_sync_pre_chatd_enabled:[26614,"bool",!1,!1]
     */
    public static final ABProp AI_BIZAI_2WAY_INTEGRATION_HISTORY_SYNC_PRE_CHATD_ENABLED = new ABProp(26614, "false", "false");

    /**
     * The {@code wa_web_chat_themes} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_chat_themes:[26629,"bool",!1,!0]
     */
    public static final ABProp WA_WEB_CHAT_THEMES = new ABProp(26629, "false", "true");

    /**
     * The {@code waweb_status_close_friends_viewer_side_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: waweb_status_close_friends_viewer_side_enabled:[26659,"bool",!1,!1]
     */
    public static final ABProp WAWEB_STATUS_CLOSE_FRIENDS_VIEWER_SIDE_ENABLED = new ABProp(26659, "false", "false");

    /**
     * The {@code newsletter_status_creation_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: newsletter_status_creation_enabled:[26669,"bool",!1,!0]
     */
    public static final ABProp NEWSLETTER_STATUS_CREATION_ENABLED = new ABProp(26669, "false", "true");

    /**
     * The {@code biz_ai_responding_list_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: biz_ai_responding_list_enabled:[26670,"bool",!1,!0]
     */
    public static final ABProp BIZ_AI_RESPONDING_LIST_ENABLED = new ABProp(26670, "false", "true");

    /**
     * The {@code interactive_bloks_widget_web_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: interactive_bloks_widget_web_enabled:[26685,"bool",!1,!0]
     */
    public static final ABProp INTERACTIVE_BLOKS_WIDGET_WEB_ENABLED = new ABProp(26685, "false", "true");

    /**
     * The {@code ctwa_smb_multiselect_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ctwa_smb_multiselect_enabled:[26719,"bool",!1,!1]
     */
    public static final ABProp CTWA_SMB_MULTISELECT_ENABLED = new ABProp(26719, "false", "false");

    /**
     * The {@code wa_web_contact_and_chat_fuzzy_search_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_contact_and_chat_fuzzy_search_enabled:[26728,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_CONTACT_AND_CHAT_FUZZY_SEARCH_ENABLED = new ABProp(26728, "false", "false");

    /**
     * The {@code wa_web_contact_and_chat_fuzzy_search_similarity_optimization_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_contact_and_chat_fuzzy_search_similarity_optimization_enabled:[26729,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_CONTACT_AND_CHAT_FUZZY_SEARCH_SIMILARITY_OPTIMIZATION_ENABLED = new ABProp(26729, "false", "false");

    /**
     * The {@code wa_web_contact_and_chat_fuzzy_search_distance_threshold} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_contact_and_chat_fuzzy_search_distance_threshold:[26731,"float",0.30000001192092896,0.30000001192092896]
     */
    public static final ABProp WA_WEB_CONTACT_AND_CHAT_FUZZY_SEARCH_DISTANCE_THRESHOLD = new ABProp(26731, "0.30000001192092896", "0.30000001192092896");

    /**
     * The {@code wa_web_contact_and_chat_fuzzy_search_timeout_threshold} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_contact_and_chat_fuzzy_search_timeout_threshold:[26733,"float",5,5]
     */
    public static final ABProp WA_WEB_CONTACT_AND_CHAT_FUZZY_SEARCH_TIMEOUT_THRESHOLD = new ABProp(26733, "5", "5");

    /**
     * The {@code voip_enable_webrtc_stats_polling} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: voip_enable_webrtc_stats_polling:[26744,"bool",!0,!0]
     */
    public static final ABProp VOIP_ENABLE_WEBRTC_STATS_POLLING = new ABProp(26744, "true", "true");

    /**
     * The {@code ai_learning_clear_chat_disable_empty_chats} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_learning_clear_chat_disable_empty_chats:[26745,"bool",!1,!1]
     */
    public static final ABProp AI_LEARNING_CLEAR_CHAT_DISABLE_EMPTY_CHATS = new ABProp(26745, "false", "false");

    /**
     * The {@code parse_encrypted_dsm_msg_fix} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: parse_encrypted_dsm_msg_fix:[26772,"bool",!1,!0]
     */
    public static final ABProp PARSE_ENCRYPTED_DSM_MSG_FIX = new ABProp(26772, "false", "true");

    /**
     * The {@code wds_web_composer_toolbar_v2} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wds_web_composer_toolbar_v2:[26773,"bool",!1,!0]
     */
    public static final ABProp WDS_WEB_COMPOSER_TOOLBAR_V2 = new ABProp(26773, "false", "true");

    /**
     * The {@code ai_chat_threads_infra_web_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_chat_threads_infra_web_enabled:[26776,"bool",!1,!0]
     */
    public static final ABProp AI_CHAT_THREADS_INFRA_WEB_ENABLED = new ABProp(26776, "false", "true");

    /**
     * The {@code mm_signal_sharing_verification_new_signal_type_origin} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: mm_signal_sharing_verification_new_signal_type_origin:[26784,"bool",!1,!0]
     */
    public static final ABProp MM_SIGNAL_SHARING_VERIFICATION_NEW_SIGNAL_TYPE_ORIGIN = new ABProp(26784, "false", "true");

    /**
     * The {@code large_screens_new_chat_button_variants} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: large_screens_new_chat_button_variants:[26788,"int",0,0]
     */
    public static final ABProp LARGE_SCREENS_NEW_CHAT_BUTTON_VARIANTS = new ABProp(26788, "0", "0");

    /**
     * The {@code ai_chat_threads_web_killswitch_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_chat_threads_web_killswitch_enabled:[26806,"bool",!1,!0]
     */
    public static final ABProp AI_CHAT_THREADS_WEB_KILLSWITCH_ENABLED = new ABProp(26806, "false", "true");

    /**
     * The {@code wa_web_discuss_privately} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_discuss_privately:[26815,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_DISCUSS_PRIVATELY = new ABProp(26815, "false", "false");

    /**
     * The {@code enable_web_voip_virtual_video_capture_driver} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_web_voip_virtual_video_capture_driver:[26817,"bool",!1,!1]
     */
    public static final ABProp ENABLE_WEB_VOIP_VIRTUAL_VIDEO_CAPTURE_DRIVER = new ABProp(26817, "false", "false");

    /**
     * The {@code privacy_screen_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: privacy_screen_enabled:[26820,"bool",!1,!1]
     */
    public static final ABProp PRIVACY_SCREEN_ENABLED = new ABProp(26820, "false", "false");

    /**
     * The {@code feature_key_store_infra_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: feature_key_store_infra_enabled:[26829,"bool",!1,!0]
     */
    public static final ABProp FEATURE_KEY_STORE_INFRA_ENABLED = new ABProp(26829, "false", "true");

    /**
     * The {@code enable_web_voip_virtual_audio_capture_driver} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_web_voip_virtual_audio_capture_driver:[26838,"bool",!1,!1]
     */
    public static final ABProp ENABLE_WEB_VOIP_VIRTUAL_AUDIO_CAPTURE_DRIVER = new ABProp(26838, "false", "false");

    /**
     * The {@code payments_br_p2p_pix_copy_key_buyer_logging} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: payments_br_p2p_pix_copy_key_buyer_logging:[26847,"bool",!1,!0]
     */
    public static final ABProp PAYMENTS_BR_P2P_PIX_COPY_KEY_BUYER_LOGGING = new ABProp(26847, "false", "true");

    /**
     * The {@code web_menu_share_group} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_menu_share_group:[26850,"bool",!1,!1]
     */
    public static final ABProp WEB_MENU_SHARE_GROUP = new ABProp(26850, "false", "false");

    /**
     * The {@code calling_rust_migration_incoming_stanza_bitmap} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: calling_rust_migration_incoming_stanza_bitmap:[26876,"int",0,0]
     */
    public static final ABProp CALLING_RUST_MIGRATION_INCOMING_STANZA_BITMAP = new ABProp(26876, "0", "0");

    /**
     * The {@code remove_pn_dependencies} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: remove_pn_dependencies:[26888,"bool",!1,!0]
     */
    public static final ABProp REMOVE_PN_DEPENDENCIES = new ABProp(26888, "false", "true");

    /**
     * The {@code web_add_contact} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_add_contact:[26892,"string","",""]
     */
    public static final ABProp WEB_ADD_CONTACT = new ABProp(26892, "", "");

    /**
     * The {@code smb_bb_web_audience_expression_sync_read} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_bb_web_audience_expression_sync_read:[26894,"bool",!0,!0]
     */
    public static final ABProp SMB_BB_WEB_AUDIENCE_EXPRESSION_SYNC_READ = new ABProp(26894, "true", "true");

    /**
     * The {@code channels_question_admin_m2_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_question_admin_m2_enabled:[26910,"bool",!1,!1]
     */
    public static final ABProp CHANNELS_QUESTION_ADMIN_M2_ENABLED = new ABProp(26910, "false", "false");

    /**
     * The {@code channels_question_follower_m2_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_question_follower_m2_enabled:[26911,"bool",!1,!1]
     */
    public static final ABProp CHANNELS_QUESTION_FOLLOWER_M2_ENABLED = new ABProp(26911, "false", "false");

    /**
     * The {@code channels_reply_forward_message_types_status_m2_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_reply_forward_message_types_status_m2_enabled:[26924,"string","",""]
     */
    public static final ABProp CHANNELS_REPLY_FORWARD_MESSAGE_TYPES_STATUS_M2_ENABLED = new ABProp(26924, "", "");

    /**
     * The {@code channels_question_forward_message_types_chat_m2_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_question_forward_message_types_chat_m2_enabled:[26925,"string","",""]
     */
    public static final ABProp CHANNELS_QUESTION_FORWARD_MESSAGE_TYPES_CHAT_M2_ENABLED = new ABProp(26925, "", "");

    /**
     * The {@code channels_question_forward_message_types_status_m2_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_question_forward_message_types_status_m2_enabled:[26926,"string","",""]
     */
    public static final ABProp CHANNELS_QUESTION_FORWARD_MESSAGE_TYPES_STATUS_M2_ENABLED = new ABProp(26926, "", "");

    /**
     * The {@code channels_reply_forward_message_types_chat_m2_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_reply_forward_message_types_chat_m2_enabled:[26927,"string","",""]
     */
    public static final ABProp CHANNELS_REPLY_FORWARD_MESSAGE_TYPES_CHAT_M2_ENABLED = new ABProp(26927, "", "");

    /**
     * The {@code channels_question_sender_message_types_m2_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_question_sender_message_types_m2_enabled:[26930,"string","",""]
     */
    public static final ABProp CHANNELS_QUESTION_SENDER_MESSAGE_TYPES_M2_ENABLED = new ABProp(26930, "", "");

    /**
     * The {@code channels_question_reply_sender_message_types_m2_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_question_reply_sender_message_types_m2_enabled:[26931,"string","",""]
     */
    public static final ABProp CHANNELS_QUESTION_REPLY_SENDER_MESSAGE_TYPES_M2_ENABLED = new ABProp(26931, "", "");

    /**
     * The {@code channels_question_receiver_message_types_m2_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_question_receiver_message_types_m2_enabled:[26932,"string","",""]
     */
    public static final ABProp CHANNELS_QUESTION_RECEIVER_MESSAGE_TYPES_M2_ENABLED = new ABProp(26932, "", "");

    /**
     * The {@code channels_question_reply_receiver_message_types_m2_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_question_reply_receiver_message_types_m2_enabled:[26933,"string","",""]
     */
    public static final ABProp CHANNELS_QUESTION_REPLY_RECEIVER_MESSAGE_TYPES_M2_ENABLED = new ABProp(26933, "", "");

    /**
     * The {@code wavoip_enable_ml_namespace_v2} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wavoip_enable_ml_namespace_v2:[26947,"bool",!1,!1]
     */
    public static final ABProp WAVOIP_ENABLE_ML_NAMESPACE_V2 = new ABProp(26947, "false", "false");

    /**
     * The {@code integrity_checkpoints_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: integrity_checkpoints_enabled:[26961,"bool",!1,!0]
     */
    public static final ABProp INTEGRITY_CHECKPOINTS_ENABLED = new ABProp(26961, "false", "true");

    /**
     * The {@code ks_use_component_model} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ks_use_component_model:[26966,"bool",!1,!1]
     */
    public static final ABProp KS_USE_COMPONENT_MODEL = new ABProp(26966, "false", "false");

    /**
     * The {@code enable_wds_calling_dropdown} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_wds_calling_dropdown:[26974,"bool",!1,!0]
     */
    public static final ABProp ENABLE_WDS_CALLING_DROPDOWN = new ABProp(26974, "false", "true");

    /**
     * The {@code wa_asteria_rollout_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_asteria_rollout_enabled:[26996,"bool",!1,!1]
     */
    public static final ABProp WA_ASTERIA_ROLLOUT_ENABLED = new ABProp(26996, "false", "false");

    /**
     * The {@code pix_payment_request_update_status_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: pix_payment_request_update_status_enabled:[27006,"bool",!1,!1]
     */
    public static final ABProp PIX_PAYMENT_REQUEST_UPDATE_STATUS_ENABLED = new ABProp(27006, "false", "false");

    /**
     * The {@code payments_br_p2m_order_details_buyer_logging} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: payments_br_p2m_order_details_buyer_logging:[27008,"bool",!1,!0]
     */
    public static final ABProp PAYMENTS_BR_P2M_ORDER_DETAILS_BUYER_LOGGING = new ABProp(27008, "false", "true");

    /**
     * The {@code payments_br_p2m_pix_copy_key_buyer_logging} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: payments_br_p2m_pix_copy_key_buyer_logging:[27026,"bool",!1,!0]
     */
    public static final ABProp PAYMENTS_BR_P2M_PIX_COPY_KEY_BUYER_LOGGING = new ABProp(27026, "false", "true");

    /**
     * The {@code payments_br_payment_links_buyer_logging} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: payments_br_payment_links_buyer_logging:[27027,"bool",!1,!0]
     */
    public static final ABProp PAYMENTS_BR_PAYMENT_LINKS_BUYER_LOGGING = new ABProp(27027, "false", "true");

    /**
     * The {@code payments_br_p2m_pix_copy_code_buyer_logging} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: payments_br_p2m_pix_copy_code_buyer_logging:[27028,"bool",!1,!0]
     */
    public static final ABProp PAYMENTS_BR_P2M_PIX_COPY_CODE_BUYER_LOGGING = new ABProp(27028, "false", "true");

    /**
     * The {@code payments_br_p2m_pix_in_groups_buyer_logging} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: payments_br_p2m_pix_in_groups_buyer_logging:[27029,"bool",!1,!0]
     */
    public static final ABProp PAYMENTS_BR_P2M_PIX_IN_GROUPS_BUYER_LOGGING = new ABProp(27029, "false", "true");

    /**
     * The {@code status_likes_fifa_lottie_full_screen_animation_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: status_likes_fifa_lottie_full_screen_animation_enabled:[27054,"bool",!1,!1]
     */
    public static final ABProp STATUS_LIKES_FIFA_LOTTIE_FULL_SCREEN_ANIMATION_ENABLED = new ABProp(27054, "false", "false");

    /**
     * The {@code wa_consumer_nova_subscription_notifications_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_consumer_nova_subscription_notifications_enabled:[27068,"bool",!1,!0]
     */
    public static final ABProp WA_CONSUMER_NOVA_SUBSCRIPTION_NOTIFICATIONS_ENABLED = new ABProp(27068, "false", "true");

    /**
     * The {@code wa_web_enable_syncd_key_persistence_only_after_server_ack} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_enable_syncd_key_persistence_only_after_server_ack:[27069,"bool",!1,!0]
     */
    public static final ABProp WA_WEB_ENABLE_SYNCD_KEY_PERSISTENCE_ONLY_AFTER_SERVER_ACK = new ABProp(27069, "false", "true");

    /**
     * The {@code smb_payment_request_status_update} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_payment_request_status_update:[27077,"bool",!1,!1]
     */
    public static final ABProp SMB_PAYMENT_REQUEST_STATUS_UPDATE = new ABProp(27077, "false", "false");

    /**
     * The {@code business_broadcast_insights_sync_past_x_days} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: business_broadcast_insights_sync_past_x_days:[27082,"int",30,30]
     */
    public static final ABProp BUSINESS_BROADCAST_INSIGHTS_SYNC_PAST_X_DAYS = new ABProp(27082, "30", "30");

    /**
     * The {@code ai_maiba_wass_migration_receiving} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_maiba_wass_migration_receiving:[27083,"bool",!1,!1]
     */
    public static final ABProp AI_MAIBA_WASS_MIGRATION_RECEIVING = new ABProp(27083, "false", "false");

    /**
     * The {@code ai_maiba_wass_migration_sending} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_maiba_wass_migration_sending:[27084,"bool",!1,!1]
     */
    public static final ABProp AI_MAIBA_WASS_MIGRATION_SENDING = new ABProp(27084, "false", "false");

    /**
     * The {@code payments_br_p2m_pay_now_buyer_logging} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: payments_br_p2m_pay_now_buyer_logging:[27092,"bool",!1,!0]
     */
    public static final ABProp PAYMENTS_BR_P2M_PAY_NOW_BUYER_LOGGING = new ABProp(27092, "false", "true");

    /**
     * The {@code payments_br_p2m_view_order_buyer_logging} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: payments_br_p2m_view_order_buyer_logging:[27093,"bool",!1,!0]
     */
    public static final ABProp PAYMENTS_BR_P2M_VIEW_ORDER_BUYER_LOGGING = new ABProp(27093, "false", "true");

    /**
     * The {@code payments_br_p2m_pix_more_ways_to_pay_buyer_logging} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: payments_br_p2m_pix_more_ways_to_pay_buyer_logging:[27094,"bool",!1,!0]
     */
    public static final ABProp PAYMENTS_BR_P2M_PIX_MORE_WAYS_TO_PAY_BUYER_LOGGING = new ABProp(27094, "false", "true");

    /**
     * The {@code payments_br_p2m_completed_payment_intent_buyer_logging} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: payments_br_p2m_completed_payment_intent_buyer_logging:[27095,"bool",!1,!0]
     */
    public static final ABProp PAYMENTS_BR_P2M_COMPLETED_PAYMENT_INTENT_BUYER_LOGGING = new ABProp(27095, "false", "true");

    /**
     * The {@code payments_br_p2m_copy_boleto_code_buyer_logging} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: payments_br_p2m_copy_boleto_code_buyer_logging:[27096,"bool",!1,!0]
     */
    public static final ABProp PAYMENTS_BR_P2M_COPY_BOLETO_CODE_BUYER_LOGGING = new ABProp(27096, "false", "true");

    /**
     * The {@code wavoip_ml_bwe_quickhd_model_download_versions} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wavoip_ml_bwe_quickhd_model_download_versions:[27109,"string","",""]
     */
    public static final ABProp WAVOIP_ML_BWE_QUICKHD_MODEL_DOWNLOAD_VERSIONS = new ABProp(27109, "", "");

    /**
     * The {@code payments_br_p2p_pix_copy_code_buyer_logging} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: payments_br_p2p_pix_copy_code_buyer_logging:[27114,"bool",!1,!0]
     */
    public static final ABProp PAYMENTS_BR_P2P_PIX_COPY_CODE_BUYER_LOGGING = new ABProp(27114, "false", "true");

    /**
     * The {@code smb_qp_emergency_force_fetch_nonce} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_qp_emergency_force_fetch_nonce:[27115,"string","",""]
     */
    public static final ABProp SMB_QP_EMERGENCY_FORCE_FETCH_NONCE = new ABProp(27115, "", "");

    /**
     * The {@code wa_asteria_meta_ai_settings_tab_entrypoint_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_asteria_meta_ai_settings_tab_entrypoint_enabled:[27118,"bool",!1,!1]
     */
    public static final ABProp WA_ASTERIA_META_AI_SETTINGS_TAB_ENTRYPOINT_ENABLED = new ABProp(27118, "false", "false");

    /**
     * The {@code wa_web_change_list_wds_submenu} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_change_list_wds_submenu:[27123,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_CHANGE_LIST_WDS_SUBMENU = new ABProp(27123, "false", "false");

    /**
     * The {@code md_syncd_mutation_logging} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: md_syncd_mutation_logging:[27124,"string","{\"allowlist\": []}","{\"allowlist\": []}"]
     */
    public static final ABProp MD_SYNCD_MUTATION_LOGGING = new ABProp(27124, "{\"allowlist\": []}", "{\"allowlist\": []}");

    /**
     * The {@code md_syncd_mutation_summary_logging} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: md_syncd_mutation_summary_logging:[27125,"string","{\"allowlist\": []}","{\"allowlist\": []}"]
     */
    public static final ABProp MD_SYNCD_MUTATION_SUMMARY_LOGGING = new ABProp(27125, "{\"allowlist\": []}", "{\"allowlist\": []}");

    /**
     * The {@code md_syncd_bundle_logging} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: md_syncd_bundle_logging:[27126,"string","{\"allowlist\": []}","{\"allowlist\": []}"]
     */
    public static final ABProp MD_SYNCD_BUNDLE_LOGGING = new ABProp(27126, "{\"allowlist\": []}", "{\"allowlist\": []}");

    /**
     * The {@code aura_pinned_chats_targeted_nux_force} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: aura_pinned_chats_targeted_nux_force:[27135,"bool",!1,!1]
     */
    public static final ABProp AURA_PINNED_CHATS_TARGETED_NUX_FORCE = new ABProp(27135, "false", "false");

    /**
     * The {@code wa_webtp_thumbnail_renderer_timeout_ms} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_webtp_thumbnail_renderer_timeout_ms:[27148,"int",3000,3000]
     */
    public static final ABProp WA_WEBTP_THUMBNAIL_RENDERER_TIMEOUT_MS = new ABProp(27148, "3000", "3000");

    /**
     * The {@code wa_web_forward_to_small_groups} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_forward_to_small_groups:[27157,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_FORWARD_TO_SMALL_GROUPS = new ABProp(27157, "false", "false");

    /**
     * The {@code wa_payments_smb_labels_convention_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_payments_smb_labels_convention_enabled:[27172,"bool",!1,!0]
     */
    public static final ABProp WA_PAYMENTS_SMB_LABELS_CONVENTION_ENABLED = new ABProp(27172, "false", "true");

    /**
     * The {@code wa_payments_smb_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_payments_smb_enabled:[27173,"bool",!1,!0]
     */
    public static final ABProp WA_PAYMENTS_SMB_ENABLED = new ABProp(27173, "false", "true");

    /**
     * The {@code disable_raise_hand_1on1} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: disable_raise_hand_1on1:[27177,"bool",!1,!1]
     */
    public static final ABProp DISABLE_RAISE_HAND_1ON1 = new ABProp(27177, "false", "false");

    /**
     * The {@code ai_chat_threads_fuzzy_search_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_chat_threads_fuzzy_search_enabled:[27199,"bool",!1,!1]
     */
    public static final ABProp AI_CHAT_THREADS_FUZZY_SEARCH_ENABLED = new ABProp(27199, "false", "false");

    /**
     * The {@code aura_settings_row_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: aura_settings_row_enabled:[27210,"bool",!1,!1]
     */
    public static final ABProp AURA_SETTINGS_ROW_ENABLED = new ABProp(27210, "false", "false");

    /**
     * The {@code non_wa_contact_invite_cta_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: non_wa_contact_invite_cta_enabled:[27217,"int",-1,-1]
     */
    public static final ABProp NON_WA_CONTACT_INVITE_CTA_ENABLED = new ABProp(27217, "-1", "-1");

    /**
     * The {@code business_broadcast_insights_campaign_ttl_days} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: business_broadcast_insights_campaign_ttl_days:[27218,"int",30,30]
     */
    public static final ABProp BUSINESS_BROADCAST_INSIGHTS_CAMPAIGN_TTL_DAYS = new ABProp(27218, "30", "30");

    /**
     * The {@code acs_use_graphql_issuance} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: acs_use_graphql_issuance:[27219,"bool",!1,!1]
     */
    public static final ABProp ACS_USE_GRAPHQL_ISSUANCE = new ABProp(27219, "false", "false");

    /**
     * The {@code wmi_worker_scheduler_web} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wmi_worker_scheduler_web:[27237,"bool",!1,!1]
     */
    public static final ABProp WMI_WORKER_SCHEDULER_WEB = new ABProp(27237, "false", "false");

    /**
     * The {@code waweb_enable_legacy_image_zoom} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: waweb_enable_legacy_image_zoom:[27239,"bool",!1,!1]
     */
    public static final ABProp WAWEB_ENABLE_LEGACY_IMAGE_ZOOM = new ABProp(27239, "false", "false");

    /**
     * The {@code channels_status_consumption_entrypoints} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_status_consumption_entrypoints:[27240,"int",0,3]
     */
    public static final ABProp CHANNELS_STATUS_CONSUMPTION_ENTRYPOINTS = new ABProp(27240, "0", "3");

    /**
     * The {@code web_anr_async_msg_send_handler} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_anr_async_msg_send_handler:[27249,"bool",!1,!1]
     */
    public static final ABProp WEB_ANR_ASYNC_MSG_SEND_HANDLER = new ABProp(27249, "false", "false");

    /**
     * The {@code wds_web_rich_text_field} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wds_web_rich_text_field:[27264,"bool",!1,!1]
     */
    public static final ABProp WDS_WEB_RICH_TEXT_FIELD = new ABProp(27264, "false", "false");

    /**
     * The {@code enable_web_voip_anr_optimizations} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_web_voip_anr_optimizations:[27268,"bool",!1,!0]
     */
    public static final ABProp ENABLE_WEB_VOIP_ANR_OPTIMIZATIONS = new ABProp(27268, "false", "true");

    /**
     * The {@code web_test_abprop_delete_me} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_test_abprop_delete_me:[27274,"bool",!1,!1]
     */
    public static final ABProp WEB_TEST_ABPROP_DELETE_ME = new ABProp(27274, "false", "false");

    /**
     * The {@code opus_time} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: opus_time:[27277,"int",1784516400,1784516400]
     */
    public static final ABProp OPUS_TIME = new ABProp(27277, "1784516400", "1784516400");

    /**
     * The {@code opus_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: opus_enabled:[27278,"bool",!1,!1]
     */
    public static final ABProp OPUS_ENABLED = new ABProp(27278, "false", "false");

    /**
     * The {@code br_payments_payment_detection_enhancement} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: br_payments_payment_detection_enhancement:[27309,"bool",!1,!1]
     */
    public static final ABProp BR_PAYMENTS_PAYMENT_DETECTION_ENHANCEMENT = new ABProp(27309, "false", "false");

    /**
     * The {@code ai_chat_threads_history_icon_variant} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_chat_threads_history_icon_variant:[27316,"int",0,0]
     */
    public static final ABProp AI_CHAT_THREADS_HISTORY_ICON_VARIANT = new ABProp(27316, "0", "0");

    /**
     * The {@code wa_web_starred_msgs_search} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_starred_msgs_search:[27353,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_STARRED_MSGS_SEARCH = new ABProp(27353, "false", "false");

    /**
     * The {@code ai_rich_response_unknown_sender_preview_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_rich_response_unknown_sender_preview_enabled:[27355,"bool",!1,!1]
     */
    public static final ABProp AI_RICH_RESPONSE_UNKNOWN_SENDER_PREVIEW_ENABLED = new ABProp(27355, "false", "false");

    /**
     * The {@code ai_hatch_integration_tab_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_hatch_integration_tab_enabled:[27356,"bool",!1,!1]
     */
    public static final ABProp AI_HATCH_INTEGRATION_TAB_ENABLED = new ABProp(27356, "false", "false");

    /**
     * The {@code username_1on1_sys_msg_creation_upsell_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: username_1on1_sys_msg_creation_upsell_enabled:[27359,"bool",!0,!0]
     */
    public static final ABProp USERNAME_1ON1_SYS_MSG_CREATION_UPSELL_ENABLED = new ABProp(27359, "true", "true");

    /**
     * The {@code wa_web_composer_height_increase_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_composer_height_increase_enabled:[27441,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_COMPOSER_HEIGHT_INCREASE_ENABLED = new ABProp(27441, "false", "false");

    /**
     * The {@code channels_context_card_invite_followers_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_context_card_invite_followers_enabled:[27449,"bool",!1,!0]
     */
    public static final ABProp CHANNELS_CONTEXT_CARD_INVITE_FOLLOWERS_ENABLED = new ABProp(27449, "false", "true");

    /**
     * The {@code web_msg_infra_remove_devices_on_406_error_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_msg_infra_remove_devices_on_406_error_enabled:[27463,"bool",!1,!1]
     */
    public static final ABProp WEB_MSG_INFRA_REMOVE_DEVICES_ON_406_ERROR_ENABLED = new ABProp(27463, "false", "false");

    /**
     * The {@code ai_hatch_video_upload_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_hatch_video_upload_enabled:[27470,"bool",!1,!1]
     */
    public static final ABProp AI_HATCH_VIDEO_UPLOAD_ENABLED = new ABProp(27470, "false", "false");

    /**
     * The {@code call_info_optimizations_version} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: call_info_optimizations_version:[27483,"int",0,0]
     */
    public static final ABProp CALL_INFO_OPTIMIZATIONS_VERSION = new ABProp(27483, "0", "0");

    /**
     * The {@code smb_business_broadcast_send_web_smba} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_business_broadcast_send_web_smba:[27486,"bool",!1,!0]
     */
    public static final ABProp SMB_BUSINESS_BROADCAST_SEND_WEB_SMBA = new ABProp(27486, "false", "true");

    /**
     * The {@code wa_web_hq_image_thumbnail_in_chat_scans} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_hq_image_thumbnail_in_chat_scans:[27512,"int",0,0]
     */
    public static final ABProp WA_WEB_HQ_IMAGE_THUMBNAIL_IN_CHAT_SCANS = new ABProp(27512, "0", "0");

    /**
     * The {@code ctwa_ae_model_meta_data_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ctwa_ae_model_meta_data_enabled:[27515,"bool",!1,!1]
     */
    public static final ABProp CTWA_AE_MODEL_META_DATA_ENABLED = new ABProp(27515, "false", "false");

    /**
     * The {@code ctwa_ae_model_meta_data_signal_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ctwa_ae_model_meta_data_signal_enabled:[27516,"bool",!1,!1]
     */
    public static final ABProp CTWA_AE_MODEL_META_DATA_SIGNAL_ENABLED = new ABProp(27516, "false", "false");

    /**
     * The {@code ai_chat_threads_implicit_routing_strategy} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_chat_threads_implicit_routing_strategy:[27519,"int",0,0]
     */
    public static final ABProp AI_CHAT_THREADS_IMPLICIT_ROUTING_STRATEGY = new ABProp(27519, "0", "0");

    /**
     * The {@code wa_webtp_preload_thumbnail_renderer_no_exposure} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_webtp_preload_thumbnail_renderer_no_exposure:[27534,"bool",!1,!1]
     */
    public static final ABProp WA_WEBTP_PRELOAD_THUMBNAIL_RENDERER_NO_EXPOSURE = new ABProp(27534, "false", "false");

    /**
     * The {@code wa_webtp_thumbnail_renderer_mode} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_webtp_thumbnail_renderer_mode:[27535,"int",0,0]
     */
    public static final ABProp WA_WEBTP_THUMBNAIL_RENDERER_MODE = new ABProp(27535, "0", "0");

    /**
     * The {@code smb_core_rec_card} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_core_rec_card:[27568,"bool",!1,!1]
     */
    public static final ABProp SMB_CORE_REC_CARD = new ABProp(27568, "false", "false");

    /**
     * The {@code smb_auth_agents_feature_control_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_auth_agents_feature_control_enabled:[27585,"bool",!1,!0]
     */
    public static final ABProp SMB_AUTH_AGENTS_FEATURE_CONTROL_ENABLED = new ABProp(27585, "false", "true");

    /**
     * The {@code enable_webrtc_video_jb} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_webrtc_video_jb:[27591,"bool",!1,!1]
     */
    public static final ABProp ENABLE_WEBRTC_VIDEO_JB = new ABProp(27591, "false", "false");

    /**
     * The {@code wa_web_important_msg_notification} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_important_msg_notification:[27614,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_IMPORTANT_MSG_NOTIFICATION = new ABProp(27614, "false", "false");

    /**
     * The {@code wa_web_edit_before_forwarding_to_status} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_edit_before_forwarding_to_status:[27616,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_EDIT_BEFORE_FORWARDING_TO_STATUS = new ABProp(27616, "false", "false");

    /**
     * The {@code status_e2ee_send_over_status_stanza} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: status_e2ee_send_over_status_stanza:[27620,"bool",!1,!1]
     */
    public static final ABProp STATUS_E2EE_SEND_OVER_STATUS_STANZA = new ABProp(27620, "false", "false");

    /**
     * The {@code status_e2ee_recv_over_status_stanza} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: status_e2ee_recv_over_status_stanza:[27622,"bool",!1,!1]
     */
    public static final ABProp STATUS_E2EE_RECV_OVER_STATUS_STANZA = new ABProp(27622, "false", "false");

    /**
     * The {@code web_pathfinder_logging} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_pathfinder_logging:[27628,"int",0,3]
     */
    public static final ABProp WEB_PATHFINDER_LOGGING = new ABProp(27628, "0", "3");

    /**
     * The {@code ai_rich_response_unknown_sender_verification_masking_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_rich_response_unknown_sender_verification_masking_enabled:[27635,"bool",!1,!1]
     */
    public static final ABProp AI_RICH_RESPONSE_UNKNOWN_SENDER_VERIFICATION_MASKING_ENABLED = new ABProp(27635, "false", "false");

    /**
     * The {@code ctwa_3pd_aggregated_conversion_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ctwa_3pd_aggregated_conversion_enabled:[27640,"bool",!1,!1]
     */
    public static final ABProp CTWA_3PD_AGGREGATED_CONVERSION_ENABLED = new ABProp(27640, "false", "false");

    /**
     * The {@code enable_copy_paste_p2p} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_copy_paste_p2p:[27642,"bool",!1,!0]
     */
    public static final ABProp ENABLE_COPY_PASTE_P2P = new ABProp(27642, "false", "true");

    /**
     * The {@code enable_order_details_for_payment_key} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_order_details_for_payment_key:[27643,"bool",!1,!0]
     */
    public static final ABProp ENABLE_ORDER_DETAILS_FOR_PAYMENT_KEY = new ABProp(27643, "false", "true");

    /**
     * The {@code ai_hatch_commands_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_hatch_commands_enabled:[27660,"bool",!1,!1]
     */
    public static final ABProp AI_HATCH_COMMANDS_ENABLED = new ABProp(27660, "false", "false");

    /**
     * The {@code expand_fmx_mex_should_use_fmx_use_case} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: expand_fmx_mex_should_use_fmx_use_case:[27662,"bool",!1,!1]
     */
    public static final ABProp EXPAND_FMX_MEX_SHOULD_USE_FMX_USE_CASE = new ABProp(27662, "false", "false");

    /**
     * The {@code integrity_checkpoints_default_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: integrity_checkpoints_default_enabled:[27663,"bool",!0,!0]
     */
    public static final ABProp INTEGRITY_CHECKPOINTS_DEFAULT_ENABLED = new ABProp(27663, "true", "true");

    /**
     * The {@code web_drawer_descriptor_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_drawer_descriptor_enabled:[27677,"bool",!1,!0]
     */
    public static final ABProp WEB_DRAWER_DESCRIPTOR_ENABLED = new ABProp(27677, "false", "true");

    /**
     * The {@code web_voip_sctp_worker_safari_exp} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_voip_sctp_worker_safari_exp:[27695,"int",1,1]
     */
    public static final ABProp WEB_VOIP_SCTP_WORKER_SAFARI_EXP = new ABProp(27695, "1", "1");

    /**
     * The {@code wa_web_scrollable_reaction_tray_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_scrollable_reaction_tray_enabled:[27709,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_SCROLLABLE_REACTION_TRAY_ENABLED = new ABProp(27709, "false", "false");

    /**
     * The {@code wa_web_frequent_reactions_store_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_frequent_reactions_store_enabled:[27710,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_FREQUENT_REACTIONS_STORE_ENABLED = new ABProp(27710, "false", "false");

    /**
     * The {@code wa_web_frequent_reactions_weight_reducer} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_frequent_reactions_weight_reducer:[27711,"int",90,90]
     */
    public static final ABProp WA_WEB_FREQUENT_REACTIONS_WEIGHT_REDUCER = new ABProp(27711, "90", "90");

    /**
     * The {@code wa_web_frequent_reactions_reacts_ago_threshold} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_frequent_reactions_reacts_ago_threshold:[27712,"int",10,10]
     */
    public static final ABProp WA_WEB_FREQUENT_REACTIONS_REACTS_AGO_THRESHOLD = new ABProp(27712, "10", "10");

    /**
     * The {@code wa_web_enable_mention_message} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_enable_mention_message:[27714,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_ENABLE_MENTION_MESSAGE = new ABProp(27714, "false", "false");

    /**
     * The {@code wa_web_focus_management_for_status_audience} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_focus_management_for_status_audience:[27719,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_FOCUS_MANAGEMENT_FOR_STATUS_AUDIENCE = new ABProp(27719, "false", "false");

    /**
     * The {@code animated_soccer_ball_test_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: animated_soccer_ball_test_enabled:[27750,"bool",!1,!1]
     */
    public static final ABProp ANIMATED_SOCCER_BALL_TEST_ENABLED = new ABProp(27750, "false", "false");

    /**
     * The {@code animated_soccer_ball_prod_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: animated_soccer_ball_prod_enabled:[27751,"bool",!1,!1]
     */
    public static final ABProp ANIMATED_SOCCER_BALL_PROD_ENABLED = new ABProp(27751, "false", "false");

    /**
     * The {@code web_media_worker_split_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_media_worker_split_enabled:[27753,"bool",!1,!1]
     */
    public static final ABProp WEB_MEDIA_WORKER_SPLIT_ENABLED = new ABProp(27753, "false", "false");

    /**
     * The {@code wa_web_loader_button_uix_improvement} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_loader_button_uix_improvement:[27768,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_LOADER_BUTTON_UIX_IMPROVEMENT = new ABProp(27768, "false", "false");

    /**
     * The {@code web_anr_async_contacts_restore_from_db_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_anr_async_contacts_restore_from_db_enabled:[27775,"bool",!1,!1]
     */
    public static final ABProp WEB_ANR_ASYNC_CONTACTS_RESTORE_FROM_DB_ENABLED = new ABProp(27775, "false", "false");

    /**
     * The {@code channel_status_fill_gap_page_size} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channel_status_fill_gap_page_size:[27777,"int",100,100]
     */
    public static final ABProp CHANNEL_STATUS_FILL_GAP_PAGE_SIZE = new ABProp(27777, "100", "100");

    /**
     * The {@code wa_web_media_upload_retry_retries_count} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_media_upload_retry_retries_count:[27782,"int",0,0]
     */
    public static final ABProp WA_WEB_MEDIA_UPLOAD_RETRY_RETRIES_COUNT = new ABProp(27782, "0", "0");

    /**
     * The {@code remove_device_pn_dependencies} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: remove_device_pn_dependencies:[27791,"bool",!1,!1]
     */
    public static final ABProp REMOVE_DEVICE_PN_DEPENDENCIES = new ABProp(27791, "false", "false");

    /**
     * The {@code opus_t} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: opus_t:[27803,"int",2147483647,2147483647]
     */
    public static final ABProp OPUS_T = new ABProp(27803, "2147483647", "2147483647");

    /**
     * The {@code use_custom_soccer_ball_for_reaction_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: use_custom_soccer_ball_for_reaction_enabled:[27807,"bool",!1,!1]
     */
    public static final ABProp USE_CUSTOM_SOCCER_BALL_FOR_REACTION_ENABLED = new ABProp(27807, "false", "false");

    /**
     * The {@code ai_chat_meta_ai_home_web_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_chat_meta_ai_home_web_enabled:[27817,"bool",!1,!1]
     */
    public static final ABProp AI_CHAT_META_AI_HOME_WEB_ENABLED = new ABProp(27817, "false", "false");

    /**
     * The {@code lightweight_group_creation} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: lightweight_group_creation:[27819,"bool",!1,!1]
     */
    public static final ABProp LIGHTWEIGHT_GROUP_CREATION = new ABProp(27819, "false", "false");

    /**
     * The {@code soccer_reaction_in_tray_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: soccer_reaction_in_tray_enabled:[27833,"bool",!1,!1]
     */
    public static final ABProp SOCCER_REACTION_IN_TRAY_ENABLED = new ABProp(27833, "false", "false");

    /**
     * The {@code soccer_ball_reaction_full_animation_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: soccer_ball_reaction_full_animation_enabled:[27834,"bool",!1,!1]
     */
    public static final ABProp SOCCER_BALL_REACTION_FULL_ANIMATION_ENABLED = new ABProp(27834, "false", "false");

    /**
     * The {@code coexv2_send_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: coexv2_send_enabled:[27839,"bool",!1,!1]
     */
    public static final ABProp COEXV2_SEND_ENABLED = new ABProp(27839, "false", "false");

    /**
     * The {@code web_biz_quality_telemetry_message_clicks_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_biz_quality_telemetry_message_clicks_enabled:[27854,"bool",!1,!0]
     */
    public static final ABProp WEB_BIZ_QUALITY_TELEMETRY_MESSAGE_CLICKS_ENABLED = new ABProp(27854, "false", "true");

    /**
     * The {@code web_biz_quality_telemetry_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_biz_quality_telemetry_enabled:[27855,"bool",!1,!0]
     */
    public static final ABProp WEB_BIZ_QUALITY_TELEMETRY_ENABLED = new ABProp(27855, "false", "true");

    /**
     * The {@code wds_web_badge} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wds_web_badge:[27856,"bool",!1,!0]
     */
    public static final ABProp WDS_WEB_BADGE = new ABProp(27856, "false", "true");

    /**
     * The {@code wa_web_search_emoji_picker} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_search_emoji_picker:[27857,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_SEARCH_EMOJI_PICKER = new ABProp(27857, "false", "false");

    /**
     * The {@code inapp_signup_agm_cta_experiment} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: inapp_signup_agm_cta_experiment:[27860,"int",1,1]
     */
    public static final ABProp INAPP_SIGNUP_AGM_CTA_EXPERIMENT = new ABProp(27860, "1", "1");

    /**
     * The {@code timeout_mex_call_expand_fmx_trust_signals} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: timeout_mex_call_expand_fmx_trust_signals:[27862,"int",600,600]
     */
    public static final ABProp TIMEOUT_MEX_CALL_EXPAND_FMX_TRUST_SIGNALS = new ABProp(27862, "600", "600");

    /**
     * The {@code ai_hatch_document_upload_size_limit_mb} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_hatch_document_upload_size_limit_mb:[27873,"int",20,20]
     */
    public static final ABProp AI_HATCH_DOCUMENT_UPLOAD_SIZE_LIMIT_MB = new ABProp(27873, "20", "20");

    /**
     * The {@code ai_hatch_forwarding_html_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_hatch_forwarding_html_enabled:[27876,"bool",!1,!1]
     */
    public static final ABProp AI_HATCH_FORWARDING_HTML_ENABLED = new ABProp(27876, "false", "false");

    /**
     * The {@code sender_secret_encrypted_message_remove_message_secret} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: sender_secret_encrypted_message_remove_message_secret:[27913,"bool",!1,!1]
     */
    public static final ABProp SENDER_SECRET_ENCRYPTED_MESSAGE_REMOVE_MESSAGE_SECRET = new ABProp(27913, "false", "false");

    /**
     * The {@code wa_web_jump_to_cart} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_jump_to_cart:[27939,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_JUMP_TO_CART = new ABProp(27939, "false", "false");

    /**
     * The {@code wa_webtp_pdf_renderer_mode_no_exposure} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_webtp_pdf_renderer_mode_no_exposure:[27941,"int",0,0]
     */
    public static final ABProp WA_WEBTP_PDF_RENDERER_MODE_NO_EXPOSURE = new ABProp(27941, "0", "0");

    /**
     * The {@code giphy_pma_shutoff_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: giphy_pma_shutoff_enabled:[27942,"bool",!1,!1]
     */
    public static final ABProp GIPHY_PMA_SHUTOFF_ENABLED = new ABProp(27942, "false", "false");

    /**
     * The {@code aura_premium_stickers_killswitch} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: aura_premium_stickers_killswitch:[27946,"bool",!1,!1]
     */
    public static final ABProp AURA_PREMIUM_STICKERS_KILLSWITCH = new ABProp(27946, "false", "false");

    /**
     * The {@code wa_web_chatlist_render_chat_open} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_chatlist_render_chat_open:[27947,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_CHATLIST_RENDER_CHAT_OPEN = new ABProp(27947, "false", "false");

    /**
     * The {@code wds_web_profile_photo} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wds_web_profile_photo:[27954,"bool",!1,!0]
     */
    public static final ABProp WDS_WEB_PROFILE_PHOTO = new ABProp(27954, "false", "true");

    /**
     * The {@code web_show_to_hide_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_show_to_hide_enabled:[27958,"bool",!1,!1]
     */
    public static final ABProp WEB_SHOW_TO_HIDE_ENABLED = new ABProp(27958, "false", "false");

    /**
     * The {@code p2p_pills_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: p2p_pills_enabled:[27959,"bool",!1,!1]
     */
    public static final ABProp P2P_PILLS_ENABLED = new ABProp(27959, "false", "false");

    /**
     * The {@code web_voip_capture_video_rotation_type} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_voip_capture_video_rotation_type:[27973,"int",0,0]
     */
    public static final ABProp WEB_VOIP_CAPTURE_VIDEO_ROTATION_TYPE = new ABProp(27973, "0", "0");

    /**
     * The {@code backfill_supports_coex_companion} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: backfill_supports_coex_companion:[27975,"bool",!1,!1]
     */
    public static final ABProp BACKFILL_SUPPORTS_COEX_COMPANION = new ABProp(27975, "false", "false");

    /**
     * The {@code hosted_message_flag_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: hosted_message_flag_enabled:[27979,"bool",!1,!1]
     */
    public static final ABProp HOSTED_MESSAGE_FLAG_ENABLED = new ABProp(27979, "false", "false");

    /**
     * The {@code wavoip_ml_bwe_hd_target_model_download_versions_v2} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wavoip_ml_bwe_hd_target_model_download_versions_v2:[27990,"string","",""]
     */
    public static final ABProp WAVOIP_ML_BWE_HD_TARGET_MODEL_DOWNLOAD_VERSIONS_V2 = new ABProp(27990, "", "");

    /**
     * The {@code wavoip_ml_bwe_cong_model_download_versions_v2} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wavoip_ml_bwe_cong_model_download_versions_v2:[27991,"string","",""]
     */
    public static final ABProp WAVOIP_ML_BWE_CONG_MODEL_DOWNLOAD_VERSIONS_V2 = new ABProp(27991, "", "");

    /**
     * The {@code wavoip_ml_bwe_tr_model_download_versions_v2} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wavoip_ml_bwe_tr_model_download_versions_v2:[27996,"string","",""]
     */
    public static final ABProp WAVOIP_ML_BWE_TR_MODEL_DOWNLOAD_VERSIONS_V2 = new ABProp(27996, "", "");

    /**
     * The {@code wavoip_ml_bwe_plc_model_download_versions_v2} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wavoip_ml_bwe_plc_model_download_versions_v2:[27998,"string","",""]
     */
    public static final ABProp WAVOIP_ML_BWE_PLC_MODEL_DOWNLOAD_VERSIONS_V2 = new ABProp(27998, "", "");

    /**
     * The {@code smbw_business_broadcast_smart_column_detection_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smbw_business_broadcast_smart_column_detection_enabled:[27999,"bool",!1,!1]
     */
    public static final ABProp SMBW_BUSINESS_BROADCAST_SMART_COLUMN_DETECTION_ENABLED = new ABProp(27999, "false", "false");

    /**
     * The {@code wavoip_ml_nadl_model_download_versions_v2} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wavoip_ml_nadl_model_download_versions_v2:[28015,"string","",""]
     */
    public static final ABProp WAVOIP_ML_NADL_MODEL_DOWNLOAD_VERSIONS_V2 = new ABProp(28015, "", "");

    /**
     * The {@code wavoip_ml_bwe_gc_undershoot_model_download_versions_v2} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wavoip_ml_bwe_gc_undershoot_model_download_versions_v2:[28019,"string","",""]
     */
    public static final ABProp WAVOIP_ML_BWE_GC_UNDERSHOOT_MODEL_DOWNLOAD_VERSIONS_V2 = new ABProp(28019, "", "");

    /**
     * The {@code wavoip_ml_bwe_gc_hd_target_model_download_versions_v2} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wavoip_ml_bwe_gc_hd_target_model_download_versions_v2:[28021,"string","",""]
     */
    public static final ABProp WAVOIP_ML_BWE_GC_HD_TARGET_MODEL_DOWNLOAD_VERSIONS_V2 = new ABProp(28021, "", "");

    /**
     * The {@code ai_chat_meta_ai_home_default_landing_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_chat_meta_ai_home_default_landing_enabled:[28033,"bool",!1,!1]
     */
    public static final ABProp AI_CHAT_META_AI_HOME_DEFAULT_LANDING_ENABLED = new ABProp(28033, "false", "false");

    /**
     * The {@code web_voip_video_low_cap_width} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_voip_video_low_cap_width:[28041,"int",480,480]
     */
    public static final ABProp WEB_VOIP_VIDEO_LOW_CAP_WIDTH = new ABProp(28041, "480", "480");

    /**
     * The {@code web_voip_video_low_cap_height} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_voip_video_low_cap_height:[28042,"int",270,270]
     */
    public static final ABProp WEB_VOIP_VIDEO_LOW_CAP_HEIGHT = new ABProp(28042, "270", "270");

    /**
     * The {@code web_voip_video_mid_cap_width} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_voip_video_mid_cap_width:[28043,"int",640,640]
     */
    public static final ABProp WEB_VOIP_VIDEO_MID_CAP_WIDTH = new ABProp(28043, "640", "640");

    /**
     * The {@code web_voip_video_mid_cap_height} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_voip_video_mid_cap_height:[28044,"int",360,360]
     */
    public static final ABProp WEB_VOIP_VIDEO_MID_CAP_HEIGHT = new ABProp(28044, "360", "360");

    /**
     * The {@code web_calling_auto_popout_video} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_calling_auto_popout_video:[28046,"bool",!1,!1]
     */
    public static final ABProp WEB_CALLING_AUTO_POPOUT_VIDEO = new ABProp(28046, "false", "false");

    /**
     * The {@code wa_web_falco_console_logger} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_falco_console_logger:[28054,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_FALCO_CONSOLE_LOGGER = new ABProp(28054, "false", "false");

    /**
     * The {@code blocklist_system_msg_on_full_refetch} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: blocklist_system_msg_on_full_refetch:[28070,"bool",!1,!1]
     */
    public static final ABProp BLOCKLIST_SYSTEM_MSG_ON_FULL_REFETCH = new ABProp(28070, "false", "false");

    /**
     * The {@code group_member_updates_username_description_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: group_member_updates_username_description_enabled:[28087,"bool",!1,!0]
     */
    public static final ABProp GROUP_MEMBER_UPDATES_USERNAME_DESCRIPTION_ENABLED = new ABProp(28087, "false", "true");

    /**
     * The {@code enhanced_mention_suggestions_min_mention_char_count} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enhanced_mention_suggestions_min_mention_char_count:[28089,"int",-1,-1]
     */
    public static final ABProp ENHANCED_MENTION_SUGGESTIONS_MIN_MENTION_CHAR_COUNT = new ABProp(28089, "-1", "-1");

    /**
     * The {@code m2_audience_dynamic_rules} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: m2_audience_dynamic_rules:[28099,"bool",!1,!1]
     */
    public static final ABProp M2_AUDIENCE_DYNAMIC_RULES = new ABProp(28099, "false", "false");

    /**
     * The {@code coexv2_recv_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: coexv2_recv_enabled:[28110,"bool",!1,!1]
     */
    public static final ABProp COEXV2_RECV_ENABLED = new ABProp(28110, "false", "false");

    /**
     * The {@code always_backfill_to_coex_companion} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: always_backfill_to_coex_companion:[28124,"bool",!1,!1]
     */
    public static final ABProp ALWAYS_BACKFILL_TO_COEX_COMPANION = new ABProp(28124, "false", "false");

    /**
     * The {@code consumer_graphql_enable_double_log_for_survey} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: consumer_graphql_enable_double_log_for_survey:[28129,"bool",!1,!1]
     */
    public static final ABProp CONSUMER_GRAPHQL_ENABLE_DOUBLE_LOG_FOR_SURVEY = new ABProp(28129, "false", "false");

    /**
     * The {@code smb_business_broadcast_send_web_no_exp} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_business_broadcast_send_web_no_exp:[28138,"bool",!1,!1]
     */
    public static final ABProp SMB_BUSINESS_BROADCAST_SEND_WEB_NO_EXP = new ABProp(28138, "false", "false");

    /**
     * The {@code smb_business_broadcast_send_web_smba_no_exp} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_business_broadcast_send_web_smba_no_exp:[28139,"bool",!1,!0]
     */
    public static final ABProp SMB_BUSINESS_BROADCAST_SEND_WEB_SMBA_NO_EXP = new ABProp(28139, "false", "true");

    /**
     * The {@code inapp_signup_m1_logging_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: inapp_signup_m1_logging_enabled:[28142,"bool",!1,!0]
     */
    public static final ABProp INAPP_SIGNUP_M1_LOGGING_ENABLED = new ABProp(28142, "false", "true");

    /**
     * The {@code syncd_use_index_for_lthash_lookup} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: syncd_use_index_for_lthash_lookup:[28144,"bool",!1,!1]
     */
    public static final ABProp SYNCD_USE_INDEX_FOR_LTHASH_LOOKUP = new ABProp(28144, "false", "false");

    /**
     * The {@code appointment_booking_bloks_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: appointment_booking_bloks_enabled:[28146,"bool",!1,!1]
     */
    public static final ABProp APPOINTMENT_BOOKING_BLOKS_ENABLED = new ABProp(28146, "false", "false");

    /**
     * The {@code channels_visibility_logging_fullscreen_media_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_visibility_logging_fullscreen_media_enabled:[28148,"bool",!1,!1]
     */
    public static final ABProp CHANNELS_VISIBILITY_LOGGING_FULLSCREEN_MEDIA_ENABLED = new ABProp(28148, "false", "false");

    /**
     * The {@code web_chat_theme_drawer_title} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_chat_theme_drawer_title:[28157,"bool",!1,!1]
     */
    public static final ABProp WEB_CHAT_THEME_DRAWER_TITLE = new ABProp(28157, "false", "false");

    /**
     * The {@code fetch_qp_via_graphql_web_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: fetch_qp_via_graphql_web_enabled:[28158,"bool",!1,!1]
     */
    public static final ABProp FETCH_QP_VIA_GRAPHQL_WEB_ENABLED = new ABProp(28158, "false", "false");

    /**
     * The {@code consumer_graphql_web_to_fetch_qp_surface_ids} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: consumer_graphql_web_to_fetch_qp_surface_ids:[28159,"string","{}","{}"]
     */
    public static final ABProp CONSUMER_GRAPHQL_WEB_TO_FETCH_QP_SURFACE_IDS = new ABProp(28159, "{}", "{}");

    /**
     * The {@code out_contact_invites_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: out_contact_invites_enabled:[28170,"int",0,0]
     */
    public static final ABProp OUT_CONTACT_INVITES_ENABLED = new ABProp(28170, "0", "0");

    /**
     * The {@code web_voip_low_resource_device} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_voip_low_resource_device:[28203,"bool",!1,!1]
     */
    public static final ABProp WEB_VOIP_LOW_RESOURCE_DEVICE = new ABProp(28203, "false", "false");

    /**
     * The {@code channels_pulse_on_unread_badge_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_pulse_on_unread_badge_enabled:[28224,"bool",!1,!1]
     */
    public static final ABProp CHANNELS_PULSE_ON_UNREAD_BADGE_ENABLED = new ABProp(28224, "false", "false");

    /**
     * The {@code enable_web_log_download} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_web_log_download:[28226,"bool",!1,!1]
     */
    public static final ABProp ENABLE_WEB_LOG_DOWNLOAD = new ABProp(28226, "false", "false");

    /**
     * The {@code ai_asset_replacement_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_asset_replacement_enabled:[28265,"bool",!1,!1]
     */
    public static final ABProp AI_ASSET_REPLACEMENT_ENABLED = new ABProp(28265, "false", "false");

    /**
     * The {@code ai_genai_straw_hat} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_genai_straw_hat:[28268,"bool",!1,!1]
     */
    public static final ABProp AI_GENAI_STRAW_HAT = new ABProp(28268, "false", "false");

    /**
     * The {@code business_broadcasts_syncd_wam_logging} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: business_broadcasts_syncd_wam_logging:[28277,"bool",!1,!0]
     */
    public static final ABProp BUSINESS_BROADCASTS_SYNCD_WAM_LOGGING = new ABProp(28277, "false", "true");

    /**
     * The {@code ai_group_tee_history_share_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_group_tee_history_share_enabled:[28278,"bool",!1,!1]
     */
    public static final ABProp AI_GROUP_TEE_HISTORY_SHARE_ENABLED = new ABProp(28278, "false", "false");

    /**
     * The {@code web_enable_camera_capture_refresh} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_enable_camera_capture_refresh:[28316,"bool",!1,!1]
     */
    public static final ABProp WEB_ENABLE_CAMERA_CAPTURE_REFRESH = new ABProp(28316, "false", "false");

    /**
     * The {@code cross_device_message_editing} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: cross_device_message_editing:[28340,"bool",!1,!0]
     */
    public static final ABProp CROSS_DEVICE_MESSAGE_EDITING = new ABProp(28340, "false", "true");

    /**
     * The {@code aura_kill_switch} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: aura_kill_switch:[28345,"bool",!1,!1]
     */
    public static final ABProp AURA_KILL_SWITCH = new ABProp(28345, "false", "false");

    /**
     * The {@code coex_iicon_backfill} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: coex_iicon_backfill:[28349,"bool",!1,!1]
     */
    public static final ABProp COEX_IICON_BACKFILL = new ABProp(28349, "false", "false");

    /**
     * The {@code group_suspension_appeals_redesign_variant_enable} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: group_suspension_appeals_redesign_variant_enable:[28376,"bool",!1,!1]
     */
    public static final ABProp GROUP_SUSPENSION_APPEALS_REDESIGN_VARIANT_ENABLE = new ABProp(28376, "false", "false");

    /**
     * The {@code canonical_ent_companion_server_cached_nonce_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: canonical_ent_companion_server_cached_nonce_enabled:[28399,"bool",!1,!1]
     */
    public static final ABProp CANONICAL_ENT_COMPANION_SERVER_CACHED_NONCE_ENABLED = new ABProp(28399, "false", "false");

    /**
     * The {@code wa_web_mention_search} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_mention_search:[28455,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_MENTION_SEARCH = new ABProp(28455, "false", "false");

    /**
     * The {@code smb_biz_ai_lists_pills} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_biz_ai_lists_pills:[28470,"string","None","None"]
     */
    public static final ABProp SMB_BIZ_AI_LISTS_PILLS = new ABProp(28470, "None", "None");

    /**
     * The {@code wa_web_biz_broadcasts_catalog_attachment} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_biz_broadcasts_catalog_attachment:[28471,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_BIZ_BROADCASTS_CATALOG_ATTACHMENT = new ABProp(28471, "false", "false");

    /**
     * The {@code mm_tap_target_bloks_client_hydration_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: mm_tap_target_bloks_client_hydration_enabled:[28473,"bool",!1,!1]
     */
    public static final ABProp MM_TAP_TARGET_BLOKS_CLIENT_HYDRATION_ENABLED = new ABProp(28473, "false", "false");

    /**
     * The {@code channel_status_forwarding_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channel_status_forwarding_enabled:[28479,"bool",!1,!1]
     */
    public static final ABProp CHANNEL_STATUS_FORWARDING_ENABLED = new ABProp(28479, "false", "false");

    /**
     * The {@code channel_status_deeplink_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channel_status_deeplink_enabled:[28500,"bool",!0,!0]
     */
    public static final ABProp CHANNEL_STATUS_DEEPLINK_ENABLED = new ABProp(28500, "true", "true");

    /**
     * The {@code wa_web_push_name_in_global_search_non_contacts_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_push_name_in_global_search_non_contacts_enabled:[28506,"bool",!1,!0]
     */
    public static final ABProp WA_WEB_PUSH_NAME_IN_GLOBAL_SEARCH_NON_CONTACTS_ENABLED = new ABProp(28506, "false", "true");

    /**
     * The {@code relax_integrity_constraints_for_bb_wa_tenured_accounts} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: relax_integrity_constraints_for_bb_wa_tenured_accounts:[28516,"bool",!1,!0]
     */
    public static final ABProp RELAX_INTEGRITY_CONSTRAINTS_FOR_BB_WA_TENURED_ACCOUNTS = new ABProp(28516, "false", "true");

    /**
     * The {@code smb_web_category_search_via_graph_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_web_category_search_via_graph_enabled:[28519,"bool",!1,!1]
     */
    public static final ABProp SMB_WEB_CATEGORY_SEARCH_VIA_GRAPH_ENABLED = new ABProp(28519, "false", "false");

    /**
     * The {@code consumer_web_qp_graphql_to_fetch_qp_frequency_mins} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: consumer_web_qp_graphql_to_fetch_qp_frequency_mins:[28529,"int",1320,1320]
     */
    public static final ABProp CONSUMER_WEB_QP_GRAPHQL_TO_FETCH_QP_FREQUENCY_MINS = new ABProp(28529, "1320", "1320");

    /**
     * The {@code biz_ai_tools_settings} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: biz_ai_tools_settings:[28552,"bool",!1,!1]
     */
    public static final ABProp BIZ_AI_TOOLS_SETTINGS = new ABProp(28552, "false", "false");

    /**
     * The {@code wds_web_dialog} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wds_web_dialog:[28557,"bool",!1,!0]
     */
    public static final ABProp WDS_WEB_DIALOG = new ABProp(28557, "false", "true");

    /**
     * The {@code mm_optimized_delivery_archive_signal_sharing_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: mm_optimized_delivery_archive_signal_sharing_enabled:[28558,"bool",!1,!1]
     */
    public static final ABProp MM_OPTIMIZED_DELIVERY_ARCHIVE_SIGNAL_SHARING_ENABLED = new ABProp(28558, "false", "false");

    /**
     * The {@code wds_web_action_tile_refresh} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wds_web_action_tile_refresh:[28564,"bool",!1,!1]
     */
    public static final ABProp WDS_WEB_ACTION_TILE_REFRESH = new ABProp(28564, "false", "false");

    /**
     * The {@code mm_disclosure_handle_tos_failures_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: mm_disclosure_handle_tos_failures_enabled:[28572,"bool",!1,!1]
     */
    public static final ABProp MM_DISCLOSURE_HANDLE_TOS_FAILURES_ENABLED = new ABProp(28572, "false", "false");

    /**
     * The {@code web_biz_simple_signal_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_biz_simple_signal_enabled:[28573,"int",0,0]
     */
    public static final ABProp WEB_BIZ_SIMPLE_SIGNAL_ENABLED = new ABProp(28573, "0", "0");

    /**
     * The {@code web_biz_quality_telemetry_message_reads_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_biz_quality_telemetry_message_reads_enabled:[28574,"bool",!1,!0]
     */
    public static final ABProp WEB_BIZ_QUALITY_TELEMETRY_MESSAGE_READS_ENABLED = new ABProp(28574, "false", "true");

    /**
     * The {@code ai_gizmo_integration_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_gizmo_integration_enabled:[28584,"bool",!1,!0]
     */
    public static final ABProp AI_GIZMO_INTEGRATION_ENABLED = new ABProp(28584, "false", "true");

    /**
     * The {@code ai_subscription_imagine_intent_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_subscription_imagine_intent_enabled:[28585,"bool",!1,!1]
     */
    public static final ABProp AI_SUBSCRIPTION_IMAGINE_INTENT_ENABLED = new ABProp(28585, "false", "false");

    /**
     * The {@code web_biz_quality_telemetry_message_level_actions_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_biz_quality_telemetry_message_level_actions_enabled:[28590,"bool",!1,!0]
     */
    public static final ABProp WEB_BIZ_QUALITY_TELEMETRY_MESSAGE_LEVEL_ACTIONS_ENABLED = new ABProp(28590, "false", "true");

    /**
     * The {@code wa_meta_one_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_meta_one_enabled:[28611,"bool",!1,!1]
     */
    public static final ABProp WA_META_ONE_ENABLED = new ABProp(28611, "false", "false");

    /**
     * The {@code wa_meta_one_rollout_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_meta_one_rollout_enabled:[28612,"bool",!1,!1]
     */
    public static final ABProp WA_META_ONE_ROLLOUT_ENABLED = new ABProp(28612, "false", "false");

    /**
     * The {@code wa_meta_one_eligibility_subscription_status_check_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_meta_one_eligibility_subscription_status_check_enabled:[28613,"bool",!1,!1]
     */
    public static final ABProp WA_META_ONE_ELIGIBILITY_SUBSCRIPTION_STATUS_CHECK_ENABLED = new ABProp(28613, "false", "false");

    /**
     * The {@code wa_web_quick_reactions} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_quick_reactions:[28621,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_QUICK_REACTIONS = new ABProp(28621, "false", "false");

    /**
     * The {@code pnh_history_sync_force_general} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: pnh_history_sync_force_general:[28664,"bool",!0,!0]
     */
    public static final ABProp PNH_HISTORY_SYNC_FORCE_GENERAL = new ABProp(28664, "true", "true");

    /**
     * The {@code username_api_rate_limit_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: username_api_rate_limit_enabled:[28678,"bool",!1,!1]
     */
    public static final ABProp USERNAME_API_RATE_LIMIT_ENABLED = new ABProp(28678, "false", "false");

    /**
     * The {@code web_biz_simple_signal_group_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_biz_simple_signal_group_enabled:[28679,"bool",!1,!1]
     */
    public static final ABProp WEB_BIZ_SIMPLE_SIGNAL_GROUP_ENABLED = new ABProp(28679, "false", "false");

    /**
     * The {@code enable_setup_error_result_check} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_setup_error_result_check:[28689,"bool",!1,!1]
     */
    public static final ABProp ENABLE_SETUP_ERROR_RESULT_CHECK = new ABProp(28689, "false", "false");

    /**
     * The {@code wa_web_reshare_poster_side_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_reshare_poster_side_enabled:[28732,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_RESHARE_POSTER_SIDE_ENABLED = new ABProp(28732, "false", "false");

    /**
     * The {@code group_history_after_join_prerequisites} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: group_history_after_join_prerequisites:[28787,"bool",!1,!1]
     */
    public static final ABProp GROUP_HISTORY_AFTER_JOIN_PREREQUISITES = new ABProp(28787, "false", "false");

    /**
     * The {@code auth_agent_soft_offboarding_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: auth_agent_soft_offboarding_enabled:[28802,"bool",!1,!1]
     */
    public static final ABProp AUTH_AGENT_SOFT_OFFBOARDING_ENABLED = new ABProp(28802, "false", "false");

    /**
     * The {@code inapp_signup_qpl_logging_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: inapp_signup_qpl_logging_enabled:[28806,"bool",!1,!0]
     */
    public static final ABProp INAPP_SIGNUP_QPL_LOGGING_ENABLED = new ABProp(28806, "false", "true");

    /**
     * The {@code wa_web_status_resharer_flow_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_status_resharer_flow_enabled:[28812,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_STATUS_RESHARER_FLOW_ENABLED = new ABProp(28812, "false", "false");

    /**
     * The {@code wa_web_status_reshare_attribution_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_status_reshare_attribution_enabled:[28813,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_STATUS_RESHARE_ATTRIBUTION_ENABLED = new ABProp(28813, "false", "false");

    /**
     * The {@code web_calling_full_screen_toggle_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_calling_full_screen_toggle_enabled:[28830,"bool",!1,!1]
     */
    public static final ABProp WEB_CALLING_FULL_SCREEN_TOGGLE_ENABLED = new ABProp(28830, "false", "false");

    /**
     * The {@code web_anr_throttle_signal_snapshot_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_anr_throttle_signal_snapshot_enabled:[28890,"bool",!1,!1]
     */
    public static final ABProp WEB_ANR_THROTTLE_SIGNAL_SNAPSHOT_ENABLED = new ABProp(28890, "false", "false");

    /**
     * The {@code im_nfm_multi_step_form_killswitch} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: im_nfm_multi_step_form_killswitch:[28891,"bool",!1,!1]
     */
    public static final ABProp IM_NFM_MULTI_STEP_FORM_KILLSWITCH = new ABProp(28891, "false", "false");

    /**
     * The {@code wa_web_bot_tos_check_refiniement} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_bot_tos_check_refiniement:[28897,"bool",!1,!0]
     */
    public static final ABProp WA_WEB_BOT_TOS_CHECK_REFINIEMENT = new ABProp(28897, "false", "true");

    /**
     * The {@code wa_web_voip_adaptive_grid_page_size} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_voip_adaptive_grid_page_size:[28909,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_VOIP_ADAPTIVE_GRID_PAGE_SIZE = new ABProp(28909, "false", "false");

    /**
     * The {@code web_bot_profile_gql_migration_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_bot_profile_gql_migration_enabled:[28941,"bool",!1,!1]
     */
    public static final ABProp WEB_BOT_PROFILE_GQL_MIGRATION_ENABLED = new ABProp(28941, "false", "false");

    /**
     * The {@code web_contact_sort_letters_first} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_contact_sort_letters_first:[28962,"int",-1,-1]
     */
    public static final ABProp WEB_CONTACT_SORT_LETTERS_FIRST = new ABProp(28962, "-1", "-1");

    /**
     * The {@code ctwa_native_web_draft_ad_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ctwa_native_web_draft_ad_enabled:[28989,"bool",!1,!1]
     */
    public static final ABProp CTWA_NATIVE_WEB_DRAFT_AD_ENABLED = new ABProp(28989, "false", "false");

    /**
     * The {@code mm_optimized_delivery_token_fallback_disabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: mm_optimized_delivery_token_fallback_disabled:[29002,"bool",!0,!0]
     */
    public static final ABProp MM_OPTIMIZED_DELIVERY_TOKEN_FALLBACK_DISABLED = new ABProp(29002, "true", "true");

    /**
     * The {@code smbw_business_broadcast_duplicate_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smbw_business_broadcast_duplicate_enabled:[29021,"bool",!1,!1]
     */
    public static final ABProp SMBW_BUSINESS_BROADCAST_DUPLICATE_ENABLED = new ABProp(29021, "false", "false");

    /**
     * The {@code username_key_redesign_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: username_key_redesign_enabled:[29026,"bool",!1,!1]
     */
    public static final ABProp USERNAME_KEY_REDESIGN_ENABLED = new ABProp(29026, "false", "false");

    /**
     * The {@code smb_business_broadcast_pro_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_business_broadcast_pro_enabled:[29033,"bool",!1,!1]
     */
    public static final ABProp SMB_BUSINESS_BROADCAST_PRO_ENABLED = new ABProp(29033, "false", "false");

    /**
     * The {@code mm_optimized_delivery_unique_token_per_message_id_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: mm_optimized_delivery_unique_token_per_message_id_enabled:[29037,"bool",!0,!0]
     */
    public static final ABProp MM_OPTIMIZED_DELIVERY_UNIQUE_TOKEN_PER_MESSAGE_ID_ENABLED = new ABProp(29037, "true", "true");

    /**
     * The {@code wa_web_blocked_participant_chat_warning} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_blocked_participant_chat_warning:[29038,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_BLOCKED_PARTICIPANT_CHAT_WARNING = new ABProp(29038, "false", "false");

    /**
     * The {@code wa_web_blocked_participant_call_warning} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_blocked_participant_call_warning:[29039,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_BLOCKED_PARTICIPANT_CALL_WARNING = new ABProp(29039, "false", "false");

    /**
     * The {@code web_anr_prune_cmc} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_anr_prune_cmc:[29060,"bool",!1,!1]
     */
    public static final ABProp WEB_ANR_PRUNE_CMC = new ABProp(29060, "false", "false");

    /**
     * The {@code web_frequently_contacted_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_frequently_contacted_enabled:[29063,"int",-1,-1]
     */
    public static final ABProp WEB_FREQUENTLY_CONTACTED_ENABLED = new ABProp(29063, "-1", "-1");

    /**
     * The {@code web_anr_batch_profile_picture_bridge_operations} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_anr_batch_profile_picture_bridge_operations:[29122,"bool",!1,!1]
     */
    public static final ABProp WEB_ANR_BATCH_PROFILE_PICTURE_BRIDGE_OPERATIONS = new ABProp(29122, "false", "false");

    /**
     * The {@code channels_in_app_policy_detail_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_in_app_policy_detail_enabled:[29132,"bool",!1,!0]
     */
    public static final ABProp CHANNELS_IN_APP_POLICY_DETAIL_ENABLED = new ABProp(29132, "false", "true");

    /**
     * The {@code animated_emoji_use_lazy_parsing} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: animated_emoji_use_lazy_parsing:[29140,"bool",!1,!1]
     */
    public static final ABProp ANIMATED_EMOJI_USE_LAZY_PARSING = new ABProp(29140, "false", "false");

    /**
     * The {@code group_calling_wave_receiving_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: group_calling_wave_receiving_enabled:[29161,"bool",!1,!1]
     */
    public static final ABProp GROUP_CALLING_WAVE_RECEIVING_ENABLED = new ABProp(29161, "false", "false");

    /**
     * The {@code no_large_emoji_regex} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: no_large_emoji_regex:[29172,"bool",!1,!1]
     */
    public static final ABProp NO_LARGE_EMOJI_REGEX = new ABProp(29172, "false", "false");

    /**
     * The {@code wmi_async_await_prep} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wmi_async_await_prep:[29197,"bool",!1,!1]
     */
    public static final ABProp WMI_ASYNC_AWAIT_PREP = new ABProp(29197, "false", "false");

    /**
     * The {@code shortcake_companion_prologue__passkeys__handoff_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: shortcake_companion_prologue__passkeys__handoff_enabled:[29204,"bool",!1,!0]
     */
    public static final ABProp SHORTCAKE_COMPANION_PROLOGUE__PASSKEYS__HANDOFF_ENABLED = new ABProp(29204, "false", "true");

    /**
     * The {@code shortcake_companion_prologue__passkeys__enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: shortcake_companion_prologue__passkeys__enabled:[29206,"bool",!1,!0]
     */
    public static final ABProp SHORTCAKE_COMPANION_PROLOGUE__PASSKEYS__ENABLED = new ABProp(29206, "false", "true");

    /**
     * The {@code channels_questions_responses_drawer_loading_shimmer_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channels_questions_responses_drawer_loading_shimmer_enabled:[29209,"bool",!1,!0]
     */
    public static final ABProp CHANNELS_QUESTIONS_RESPONSES_DRAWER_LOADING_SHIMMER_ENABLED = new ABProp(29209, "false", "true");

    /**
     * The {@code info_drawer_refresh} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: info_drawer_refresh:[29210,"bool",!1,!1]
     */
    public static final ABProp INFO_DRAWER_REFRESH = new ABProp(29210, "false", "false");

    /**
     * The {@code vid_port_enable_capture_fps_median_filter} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: vid_port_enable_capture_fps_median_filter:[29214,"bool",!1,!1]
     */
    public static final ABProp VID_PORT_ENABLE_CAPTURE_FPS_MEDIAN_FILTER = new ABProp(29214, "false", "false");

    /**
     * The {@code acs_use_graphql_for_migration_test} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: acs_use_graphql_for_migration_test:[29217,"bool",!1,!1]
     */
    public static final ABProp ACS_USE_GRAPHQL_FOR_MIGRATION_TEST = new ABProp(29217, "false", "false");

    /**
     * The {@code acs_use_graphql_for_forward_counter} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: acs_use_graphql_for_forward_counter:[29218,"bool",!1,!1]
     */
    public static final ABProp ACS_USE_GRAPHQL_FOR_FORWARD_COUNTER = new ABProp(29218, "false", "false");

    /**
     * The {@code enable_call_transfer_notification} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_call_transfer_notification:[29242,"bool",!1,!1]
     */
    public static final ABProp ENABLE_CALL_TRANSFER_NOTIFICATION = new ABProp(29242, "false", "false");

    /**
     * The {@code group_calling_wave_sending_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: group_calling_wave_sending_enabled:[29247,"bool",!1,!1]
     */
    public static final ABProp GROUP_CALLING_WAVE_SENDING_ENABLED = new ABProp(29247, "false", "false");

    /**
     * The {@code wa_web_large_group_presence_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_large_group_presence_enabled:[29279,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_LARGE_GROUP_PRESENCE_ENABLED = new ABProp(29279, "false", "false");

    /**
     * The {@code wa_web_small_group_presence_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_small_group_presence_enabled:[29280,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_SMALL_GROUP_PRESENCE_ENABLED = new ABProp(29280, "false", "false");

    /**
     * The {@code wa_meta_one_launch_free_trial_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_meta_one_launch_free_trial_enabled:[29290,"bool",!1,!1]
     */
    public static final ABProp WA_META_ONE_LAUNCH_FREE_TRIAL_ENABLED = new ABProp(29290, "false", "false");

    /**
     * The {@code wa_web_match_primary_icons} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_match_primary_icons:[29293,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_MATCH_PRIMARY_ICONS = new ABProp(29293, "false", "false");

    /**
     * The {@code web_anr_group_metadata_yield} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_anr_group_metadata_yield:[29294,"bool",!1,!1]
     */
    public static final ABProp WEB_ANR_GROUP_METADATA_YIELD = new ABProp(29294, "false", "false");

    /**
     * The {@code biz_ai_web_onboarding_handoff} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: biz_ai_web_onboarding_handoff:[29298,"bool",!1,!0]
     */
    public static final ABProp BIZ_AI_WEB_ONBOARDING_HANDOFF = new ABProp(29298, "false", "true");

    /**
     * The {@code aura_media_offload_benefit_active} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: aura_media_offload_benefit_active:[29308,"bool",!1,!1]
     */
    public static final ABProp AURA_MEDIA_OFFLOAD_BENEFIT_ACTIVE = new ABProp(29308, "false", "false");

    /**
     * The {@code enable_sync_for_draft_messages} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_sync_for_draft_messages:[29314,"bool",!1,!1]
     */
    public static final ABProp ENABLE_SYNC_FOR_DRAFT_MESSAGES = new ABProp(29314, "false", "false");

    /**
     * The {@code ctwa_3pd_data_sharing_title_change} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ctwa_3pd_data_sharing_title_change:[29332,"bool",!1,!0]
     */
    public static final ABProp CTWA_3PD_DATA_SHARING_TITLE_CHANGE = new ABProp(29332, "false", "true");

    /**
     * The {@code ctwa_3pd_data_sharing_additional_logging} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ctwa_3pd_data_sharing_additional_logging:[29333,"bool",!1,!0]
     */
    public static final ABProp CTWA_3PD_DATA_SHARING_ADDITIONAL_LOGGING = new ABProp(29333, "false", "true");

    /**
     * The {@code payments_upr_bubble_countries} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: payments_upr_bubble_countries:[29342,"string","",""]
     */
    public static final ABProp PAYMENTS_UPR_BUBBLE_COUNTRIES = new ABProp(29342, "", "");

    /**
     * The {@code mark_as_verified_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: mark_as_verified_enabled:[29343,"bool",!1,!1]
     */
    public static final ABProp MARK_AS_VERIFIED_ENABLED = new ABProp(29343, "false", "false");

    /**
     * The {@code wa_individual_new_chat_thread_capping_limit} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_individual_new_chat_thread_capping_limit:[29369,"int",0,0]
     */
    public static final ABProp WA_INDIVIDUAL_NEW_CHAT_THREAD_CAPPING_LIMIT = new ABProp(29369, "0", "0");

    /**
     * The {@code biz_ai_tools_sync} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: biz_ai_tools_sync:[29383,"bool",!1,!0]
     */
    public static final ABProp BIZ_AI_TOOLS_SYNC = new ABProp(29383, "false", "true");

    /**
     * The {@code aura_media_offload_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: aura_media_offload_enabled:[29391,"bool",!1,!1]
     */
    public static final ABProp AURA_MEDIA_OFFLOAD_ENABLED = new ABProp(29391, "false", "false");

    /**
     * The {@code web_read_self_watermark_receive_store_ts} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_read_self_watermark_receive_store_ts:[29396,"bool",!1,!1]
     */
    public static final ABProp WEB_READ_SELF_WATERMARK_RECEIVE_STORE_TS = new ABProp(29396, "false", "false");

    /**
     * The {@code web_anr_spinner_gpu_animation} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_anr_spinner_gpu_animation:[29405,"bool",!1,!1]
     */
    public static final ABProp WEB_ANR_SPINNER_GPU_ANIMATION = new ABProp(29405, "false", "false");

    /**
     * The {@code wa_web_chat_themes_logging} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_chat_themes_logging:[29457,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_CHAT_THEMES_LOGGING = new ABProp(29457, "false", "false");

    /**
     * The {@code bug_reporting_not_shipped_yet_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: bug_reporting_not_shipped_yet_enabled:[29458,"bool",!1,!1]
     */
    public static final ABProp BUG_REPORTING_NOT_SHIPPED_YET_ENABLED = new ABProp(29458, "false", "false");

    /**
     * The {@code web_anr_async_sqlite_bridge_operations} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_anr_async_sqlite_bridge_operations:[29460,"bool",!1,!1]
     */
    public static final ABProp WEB_ANR_ASYNC_SQLITE_BRIDGE_OPERATIONS = new ABProp(29460, "false", "false");

    /**
     * The {@code wa_web_canonical_reg_reload_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_canonical_reg_reload_enabled:[29472,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_CANONICAL_REG_RELOAD_ENABLED = new ABProp(29472, "false", "false");

    /**
     * The {@code web_remove_message_secret_from_quoted_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_remove_message_secret_from_quoted_enabled:[29491,"bool",!1,!0]
     */
    public static final ABProp WEB_REMOVE_MESSAGE_SECRET_FROM_QUOTED_ENABLED = new ABProp(29491, "false", "true");

    /**
     * The {@code web_move_message_secret_top_level_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_move_message_secret_top_level_enabled:[29492,"bool",!1,!0]
     */
    public static final ABProp WEB_MOVE_MESSAGE_SECRET_TOP_LEVEL_ENABLED = new ABProp(29492, "false", "true");

    /**
     * The {@code enable_webcodec_require_keyframe} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_webcodec_require_keyframe:[29510,"bool",!0,!0]
     */
    public static final ABProp ENABLE_WEBCODEC_REQUIRE_KEYFRAME = new ABProp(29510, "true", "true");

    /**
     * The {@code ctwa_favorites_list_sends_signals} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ctwa_favorites_list_sends_signals:[29529,"bool",!1,!1]
     */
    public static final ABProp CTWA_FAVORITES_LIST_SENDS_SIGNALS = new ABProp(29529, "false", "false");

    /**
     * The {@code wa_web_expansion_countries_bonsai_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_expansion_countries_bonsai_enabled:[29543,"bool",!1,!0]
     */
    public static final ABProp WA_WEB_EXPANSION_COUNTRIES_BONSAI_ENABLED = new ABProp(29543, "false", "true");

    /**
     * The {@code web_read_self_watermark_send_store_ts} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_read_self_watermark_send_store_ts:[29546,"bool",!1,!1]
     */
    public static final ABProp WEB_READ_SELF_WATERMARK_SEND_STORE_TS = new ABProp(29546, "false", "false");

    /**
     * The {@code wa_win_pdf_rendering_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_win_pdf_rendering_enabled:[29548,"bool",!1,!1]
     */
    public static final ABProp WA_WIN_PDF_RENDERING_ENABLED = new ABProp(29548, "false", "false");

    /**
     * The {@code web_anr_async_native_app_state_bridge_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_anr_async_native_app_state_bridge_enabled:[29551,"bool",!1,!1]
     */
    public static final ABProp WEB_ANR_ASYNC_NATIVE_APP_STATE_BRIDGE_ENABLED = new ABProp(29551, "false", "false");

    /**
     * The {@code web_calling_offline_resume_ordering} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_calling_offline_resume_ordering:[29564,"bool",!1,!1]
     */
    public static final ABProp WEB_CALLING_OFFLINE_RESUME_ORDERING = new ABProp(29564, "false", "false");

    /**
     * The {@code web_whats_new_carousel} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_whats_new_carousel:[29618,"bool",!1,!1]
     */
    public static final ABProp WEB_WHATS_NEW_CAROUSEL = new ABProp(29618, "false", "false");

    /**
     * The {@code web_whats_new_banner} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_whats_new_banner:[29619,"bool",!1,!1]
     */
    public static final ABProp WEB_WHATS_NEW_BANNER = new ABProp(29619, "false", "false");

    /**
     * The {@code web_whats_new_banner_short_cooldown} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_whats_new_banner_short_cooldown:[29620,"bool",!1,!1]
     */
    public static final ABProp WEB_WHATS_NEW_BANNER_SHORT_COOLDOWN = new ABProp(29620, "false", "false");

    /**
     * The {@code web_whats_new_auto_modal} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_whats_new_auto_modal:[29621,"bool",!1,!1]
     */
    public static final ABProp WEB_WHATS_NEW_AUTO_MODAL = new ABProp(29621, "false", "false");

    /**
     * The {@code web_whats_new_auto_modal_short_cooldown} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_whats_new_auto_modal_short_cooldown:[29622,"bool",!1,!1]
     */
    public static final ABProp WEB_WHATS_NEW_AUTO_MODAL_SHORT_COOLDOWN = new ABProp(29622, "false", "false");

    /**
     * The {@code premium_msg_bb_campaign_sync_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: premium_msg_bb_campaign_sync_enabled:[29650,"bool",!1,!1]
     */
    public static final ABProp PREMIUM_MSG_BB_CAMPAIGN_SYNC_ENABLED = new ABProp(29650, "false", "false");

    /**
     * The {@code p2p_pills_allowlist_entries} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: p2p_pills_allowlist_entries:[29708,"string","{ \"entries\": [{ \"business_id\": \"34666845417\", \"pills\": [\"CHAT\", \"PROFILE\", \"ABOUT_US\"] }]}","{ \"entries\": [{ \"business_id\": \"34666845417\", \"pills\": [\"CHAT\", \"PROFILE\", \"ABOUT_US\"] }]}"]
     */
    public static final ABProp P2P_PILLS_ALLOWLIST_ENTRIES = new ABProp(29708, "{ \"entries\": [{ \"business_id\": \"34666845417\", \"pills\": [\"CHAT\", \"PROFILE\", \"ABOUT_US\"] }]}", "{ \"entries\": [{ \"business_id\": \"34666845417\", \"pills\": [\"CHAT\", \"PROFILE\", \"ABOUT_US\"] }]}");

    /**
     * The {@code web_whats_new_banner_short_cooldown_v2} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_whats_new_banner_short_cooldown_v2:[29709,"bool",!1,!1]
     */
    public static final ABProp WEB_WHATS_NEW_BANNER_SHORT_COOLDOWN_V2 = new ABProp(29709, "false", "false");

    /**
     * The {@code p2p_pills_enabled_for_ineligible_contacts} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: p2p_pills_enabled_for_ineligible_contacts:[29715,"bool",!1,!1]
     */
    public static final ABProp P2P_PILLS_ENABLED_FOR_INELIGIBLE_CONTACTS = new ABProp(29715, "false", "false");

    /**
     * The {@code wa_web_bot_orphan_logic_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_bot_orphan_logic_enabled:[29753,"bool",!1,!0]
     */
    public static final ABProp WA_WEB_BOT_ORPHAN_LOGIC_ENABLED = new ABProp(29753, "false", "true");

    /**
     * The {@code enable_web_voip_webtransport} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_web_voip_webtransport:[29764,"bool",!1,!1]
     */
    public static final ABProp ENABLE_WEB_VOIP_WEBTRANSPORT = new ABProp(29764, "false", "false");

    /**
     * The {@code ai_tab_unread_badge_recency_window_hours} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_tab_unread_badge_recency_window_hours:[29800,"int",-1,-1]
     */
    public static final ABProp AI_TAB_UNREAD_BADGE_RECENCY_WINDOW_HOURS = new ABProp(29800, "-1", "-1");

    /**
     * The {@code payments_br_p2m_buyer_logging_phase_2} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: payments_br_p2m_buyer_logging_phase_2:[29803,"bool",!1,!1]
     */
    public static final ABProp PAYMENTS_BR_P2M_BUYER_LOGGING_PHASE_2 = new ABProp(29803, "false", "false");

    /**
     * The {@code enable_web_voip_eager_mic_acquire} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_web_voip_eager_mic_acquire:[29836,"bool",!1,!1]
     */
    public static final ABProp ENABLE_WEB_VOIP_EAGER_MIC_ACQUIRE = new ABProp(29836, "false", "false");

    /**
     * The {@code wa_meta_one_subscription_notifications_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_meta_one_subscription_notifications_enabled:[29866,"bool",!1,!0]
     */
    public static final ABProp WA_META_ONE_SUBSCRIPTION_NOTIFICATIONS_ENABLED = new ABProp(29866, "false", "true");

    /**
     * The {@code web_configurable_quick_actions_m1} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_configurable_quick_actions_m1:[29874,"bool",!1,!1]
     */
    public static final ABProp WEB_CONFIGURABLE_QUICK_ACTIONS_M1 = new ABProp(29874, "false", "false");

    /**
     * The {@code web_get_msg_exist_optmise} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_get_msg_exist_optmise:[29880,"bool",!1,!1]
     */
    public static final ABProp WEB_GET_MSG_EXIST_OPTMISE = new ABProp(29880, "false", "false");

    /**
     * The {@code wa_auth_agent_offboarding_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_auth_agent_offboarding_enabled:[29923,"bool",!1,!1]
     */
    public static final ABProp WA_AUTH_AGENT_OFFBOARDING_ENABLED = new ABProp(29923, "false", "false");

    /**
     * The {@code wa_web_biz_profile_graphql_migration_bypass_lid_check_dogfooding} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_biz_profile_graphql_migration_bypass_lid_check_dogfooding:[29965,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_BIZ_PROFILE_GRAPHQL_MIGRATION_BYPASS_LID_CHECK_DOGFOODING = new ABProp(29965, "false", "false");

    /**
     * The {@code group_history_setting_decouple_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: group_history_setting_decouple_enabled:[29973,"bool",!1,!1]
     */
    public static final ABProp GROUP_HISTORY_SETTING_DECOUPLE_ENABLED = new ABProp(29973, "false", "false");

    /**
     * The {@code unified_response_ai_content_search_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: unified_response_ai_content_search_enabled:[30000,"bool",!1,!1]
     */
    public static final ABProp UNIFIED_RESPONSE_AI_CONTENT_SEARCH_ENABLED = new ABProp(30000, "false", "false");

    /**
     * The {@code wa_web_enable_chat_thread_and_info_status_ring} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_enable_chat_thread_and_info_status_ring:[30026,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_ENABLE_CHAT_THREAD_AND_INFO_STATUS_RING = new ABProp(30026, "false", "false");

    /**
     * The {@code wa_smb_forward_bb_web_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_smb_forward_bb_web_enabled:[30028,"bool",!1,!0]
     */
    public static final ABProp WA_SMB_FORWARD_BB_WEB_ENABLED = new ABProp(30028, "false", "true");

    /**
     * The {@code wa_web_select_all_chats_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_select_all_chats_enabled:[30040,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_SELECT_ALL_CHATS_ENABLED = new ABProp(30040, "false", "false");

    /**
     * The {@code win_hybrid_bt_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: win_hybrid_bt_enabled:[30041,"bool",!1,!0]
     */
    public static final ABProp WIN_HYBRID_BT_ENABLED = new ABProp(30041, "false", "true");

    /**
     * The {@code web_anr_skip_unused_contacts_db_updates_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_anr_skip_unused_contacts_db_updates_enabled:[30043,"bool",!1,!1]
     */
    public static final ABProp WEB_ANR_SKIP_UNUSED_CONTACTS_DB_UPDATES_ENABLED = new ABProp(30043, "false", "false");

    /**
     * The {@code receipt_mode_bitmask_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: receipt_mode_bitmask_enabled:[30084,"bool",!1,!0]
     */
    public static final ABProp RECEIPT_MODE_BITMASK_ENABLED = new ABProp(30084, "false", "true");

    /**
     * The {@code smb_web_enable_fb_linking} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_web_enable_fb_linking:[30112,"bool",!1,!1]
     */
    public static final ABProp SMB_WEB_ENABLE_FB_LINKING = new ABProp(30112, "false", "false");

    /**
     * The {@code wa_web_adaptive_layout_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_adaptive_layout_enabled:[30140,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_ADAPTIVE_LAYOUT_ENABLED = new ABProp(30140, "false", "false");

    /**
     * The {@code channel_status_resharing_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: channel_status_resharing_enabled:[30155,"bool",!1,!1]
     */
    public static final ABProp CHANNEL_STATUS_RESHARING_ENABLED = new ABProp(30155, "false", "false");

    /**
     * The {@code calling_voicemail_quoted_replies_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: calling_voicemail_quoted_replies_enabled:[30165,"bool",!1,!1]
     */
    public static final ABProp CALLING_VOICEMAIL_QUOTED_REPLIES_ENABLED = new ABProp(30165, "false", "false");

    /**
     * The {@code dm_after_read_timer_sender_options_seconds} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: dm_after_read_timer_sender_options_seconds:[30176,"string","{\"timers\": [0, 300, 3600, 43200]}","{\"timers\": [0, 300, 3600, 43200]}"]
     */
    public static final ABProp DM_AFTER_READ_TIMER_SENDER_OPTIONS_SECONDS = new ABProp(30176, "{\"timers\": [0, 300, 3600, 43200]}", "{\"timers\": [0, 300, 3600, 43200]}");

    /**
     * The {@code p2p_pills_auto_send_messages} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: p2p_pills_auto_send_messages:[30208,"bool",!1,!1]
     */
    public static final ABProp P2P_PILLS_AUTO_SEND_MESSAGES = new ABProp(30208, "false", "false");

    /**
     * The {@code wa_web_canonical_wam_falco_buffer_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_canonical_wam_falco_buffer_enabled:[30212,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_CANONICAL_WAM_FALCO_BUFFER_ENABLED = new ABProp(30212, "false", "false");

    /**
     * The {@code wa_webtp_use_async_pdf_send} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_webtp_use_async_pdf_send:[30214,"bool",!1,!1]
     */
    public static final ABProp WA_WEBTP_USE_ASYNC_PDF_SEND = new ABProp(30214, "false", "false");

    /**
     * The {@code wa_web_canonical_wam_falco_buffer_size} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_canonical_wam_falco_buffer_size:[30219,"int",2000,2000]
     */
    public static final ABProp WA_WEB_CANONICAL_WAM_FALCO_BUFFER_SIZE = new ABProp(30219, "2000", "2000");

    /**
     * The {@code web_anr_optimized_initial_contacts_sync_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_anr_optimized_initial_contacts_sync_enabled:[30227,"bool",!1,!1]
     */
    public static final ABProp WEB_ANR_OPTIMIZED_INITIAL_CONTACTS_SYNC_ENABLED = new ABProp(30227, "false", "false");

    /**
     * The {@code media_force_transcode_on_elst} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: media_force_transcode_on_elst:[30235,"bool",!1,!0]
     */
    public static final ABProp MEDIA_FORCE_TRANSCODE_ON_ELST = new ABProp(30235, "false", "true");

    /**
     * The {@code web_group_hover_card_variant} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_group_hover_card_variant:[30260,"int",0,0]
     */
    public static final ABProp WEB_GROUP_HOVER_CARD_VARIANT = new ABProp(30260, "0", "0");

    /**
     * The {@code wa_web_voip_stack_log_level} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_voip_stack_log_level:[30261,"int",3,3]
     */
    public static final ABProp WA_WEB_VOIP_STACK_LOG_LEVEL = new ABProp(30261, "3", "3");

    /**
     * The {@code biz_vpv_dimensions_logging_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: biz_vpv_dimensions_logging_enabled:[30266,"bool",!1,!1]
     */
    public static final ABProp BIZ_VPV_DIMENSIONS_LOGGING_ENABLED = new ABProp(30266, "false", "false");

    /**
     * The {@code wa_web_biz_broadcasts_contextual_entrypoints} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_biz_broadcasts_contextual_entrypoints:[30270,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_BIZ_BROADCASTS_CONTEXTUAL_ENTRYPOINTS = new ABProp(30270, "false", "false");

    /**
     * The {@code wmi_task_scheduler_second_step} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wmi_task_scheduler_second_step:[30276,"bool",!1,!1]
     */
    public static final ABProp WMI_TASK_SCHEDULER_SECOND_STEP = new ABProp(30276, "false", "false");

    /**
     * The {@code enable_join_group_context_non_auto_expose} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: enable_join_group_context_non_auto_expose:[30282,"bool",!1,!0]
     */
    public static final ABProp ENABLE_JOIN_GROUP_CONTEXT_NON_AUTO_EXPOSE = new ABProp(30282, "false", "true");

    /**
     * The {@code unified_response_markdown_links_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: unified_response_markdown_links_enabled:[30330,"bool",!1,!1]
     */
    public static final ABProp UNIFIED_RESPONSE_MARKDOWN_LINKS_ENABLED = new ABProp(30330, "false", "false");

    /**
     * The {@code group_history_new_user_threshold_secs} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: group_history_new_user_threshold_secs:[30333,"int",2592000,2592000]
     */
    public static final ABProp GROUP_HISTORY_NEW_USER_THRESHOLD_SECS = new ABProp(30333, "2592000", "2592000");

    /**
     * The {@code smb_do_label_localize_on_create_enabled_code} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_do_label_localize_on_create_enabled_code:[30344,"bool",!1,!1]
     */
    public static final ABProp SMB_DO_LABEL_LOCALIZE_ON_CREATE_ENABLED_CODE = new ABProp(30344, "false", "false");

    /**
     * The {@code group_history_new_user_threshold_receiver_enforcement_secs} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: group_history_new_user_threshold_receiver_enforcement_secs:[30345,"int",2592000,2592000]
     */
    public static final ABProp GROUP_HISTORY_NEW_USER_THRESHOLD_RECEIVER_ENFORCEMENT_SECS = new ABProp(30345, "2592000", "2592000");

    /**
     * The {@code calling_screen_share_milestone_version} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: calling_screen_share_milestone_version:[30350,"int",2,2]
     */
    public static final ABProp CALLING_SCREEN_SHARE_MILESTONE_VERSION = new ABProp(30350, "2", "2");

    /**
     * The {@code smb_do_label_localize_backfill_enabled_code} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_do_label_localize_backfill_enabled_code:[30352,"bool",!1,!1]
     */
    public static final ABProp SMB_DO_LABEL_LOCALIZE_BACKFILL_ENABLED_CODE = new ABProp(30352, "false", "false");

    /**
     * The {@code auth_agents_consumer_offboarding_exp_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: auth_agents_consumer_offboarding_exp_enabled:[30360,"bool",!1,!0]
     */
    public static final ABProp AUTH_AGENTS_CONSUMER_OFFBOARDING_EXP_ENABLED = new ABProp(30360, "false", "true");

    /**
     * The {@code calling_voicemail_attached_icce_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: calling_voicemail_attached_icce_enabled:[30383,"int",0,0]
     */
    public static final ABProp CALLING_VOICEMAIL_ATTACHED_ICCE_ENABLED = new ABProp(30383, "0", "0");

    /**
     * The {@code ptv_quoted_replies_cutout_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ptv_quoted_replies_cutout_enabled:[30384,"bool",!1,!1]
     */
    public static final ABProp PTV_QUOTED_REPLIES_CUTOUT_ENABLED = new ABProp(30384, "false", "false");

    /**
     * The {@code web_memory_reduction} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_memory_reduction:[30394,"bool",!1,!1]
     */
    public static final ABProp WEB_MEMORY_REDUCTION = new ABProp(30394, "false", "false");

    /**
     * The {@code ctwa_smb_lists_dropdown_application_fix_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ctwa_smb_lists_dropdown_application_fix_enabled:[30401,"bool",!1,!1]
     */
    public static final ABProp CTWA_SMB_LISTS_DROPDOWN_APPLICATION_FIX_ENABLED = new ABProp(30401, "false", "false");

    /**
     * The {@code opus_admin} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: opus_admin:[30454,"bool",!1,!1]
     */
    public static final ABProp OPUS_ADMIN = new ABProp(30454, "false", "false");

    /**
     * The {@code group_history_receiver_dedup} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: group_history_receiver_dedup:[30462,"bool",!1,!1]
     */
    public static final ABProp GROUP_HISTORY_RECEIVER_DEDUP = new ABProp(30462, "false", "false");

    /**
     * The {@code web_bug_reporting_request_peer_log_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_bug_reporting_request_peer_log_enabled:[30485,"bool",!1,!1]
     */
    public static final ABProp WEB_BUG_REPORTING_REQUEST_PEER_LOG_ENABLED = new ABProp(30485, "false", "false");

    /**
     * The {@code server_driven_copy_m2} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: server_driven_copy_m2:[30492,"bool",!1,!1]
     */
    public static final ABProp SERVER_DRIVEN_COPY_M2 = new ABProp(30492, "false", "false");

    /**
     * The {@code wa_web_rich_response_replying_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_rich_response_replying_enabled:[30493,"bool",!1,!0]
     */
    public static final ABProp WA_WEB_RICH_RESPONSE_REPLYING_ENABLED = new ABProp(30493, "false", "true");

    /**
     * The {@code wa_web_status_reaction_sticker_reply_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_status_reaction_sticker_reply_enabled:[30494,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_STATUS_REACTION_STICKER_REPLY_ENABLED = new ABProp(30494, "false", "false");

    /**
     * The {@code wa_web_status_question_sticker_reply_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_web_status_question_sticker_reply_enabled:[30495,"bool",!1,!1]
     */
    public static final ABProp WA_WEB_STATUS_QUESTION_STICKER_REPLY_ENABLED = new ABProp(30495, "false", "false");

    /**
     * The {@code inapp_signup_web_cta_logging_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: inapp_signup_web_cta_logging_enabled:[30498,"bool",!1,!0]
     */
    public static final ABProp INAPP_SIGNUP_WEB_CTA_LOGGING_ENABLED = new ABProp(30498, "false", "true");

    /**
     * The {@code wavoip_ml_qpl_exp_tag} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wavoip_ml_qpl_exp_tag:[30539,"string","none","none"]
     */
    public static final ABProp WAVOIP_ML_QPL_EXP_TAG = new ABProp(30539, "none", "none");

    /**
     * The {@code wavoip_legacy_ml_qpl_exp_tag} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wavoip_legacy_ml_qpl_exp_tag:[30561,"string","none","none"]
     */
    public static final ABProp WAVOIP_LEGACY_ML_QPL_EXP_TAG = new ABProp(30561, "none", "none");

    /**
     * The {@code web_qp_smb_bb_recent_message_send} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_qp_smb_bb_recent_message_send:[30568,"bool",!1,!1]
     */
    public static final ABProp WEB_QP_SMB_BB_RECENT_MESSAGE_SEND = new ABProp(30568, "false", "false");

    /**
     * The {@code web_qp_smb_bb_pmf_test_high_engagement_user} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_qp_smb_bb_pmf_test_high_engagement_user:[30569,"bool",!1,!1]
     */
    public static final ABProp WEB_QP_SMB_BB_PMF_TEST_HIGH_ENGAGEMENT_USER = new ABProp(30569, "false", "false");

    /**
     * The {@code web_qp_bb_re_engagement_past_29_days} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_qp_bb_re_engagement_past_29_days:[30570,"bool",!1,!1]
     */
    public static final ABProp WEB_QP_BB_RE_ENGAGEMENT_PAST_29_DAYS = new ABProp(30570, "false", "false");

    /**
     * The {@code web_coex_simple_signal_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_coex_simple_signal_enabled:[30577,"int",0,0]
     */
    public static final ABProp WEB_COEX_SIMPLE_SIGNAL_ENABLED = new ABProp(30577, "0", "0");

    /**
     * The {@code p2p_pills_new_business_metadata_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: p2p_pills_new_business_metadata_enabled:[30578,"bool",!1,!1]
     */
    public static final ABProp P2P_PILLS_NEW_BUSINESS_METADATA_ENABLED = new ABProp(30578, "false", "false");

    /**
     * The {@code web_bot_profile_pic_gql_migration_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_bot_profile_pic_gql_migration_enabled:[30597,"bool",!1,!0]
     */
    public static final ABProp WEB_BOT_PROFILE_PIC_GQL_MIGRATION_ENABLED = new ABProp(30597, "false", "true");

    /**
     * The {@code ai_search_ask_button_web_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_search_ask_button_web_enabled:[30604,"bool",!1,!1]
     */
    public static final ABProp AI_SEARCH_ASK_BUTTON_WEB_ENABLED = new ABProp(30604, "false", "false");

    /**
     * The {@code p2p_pills_graphql_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: p2p_pills_graphql_enabled:[30629,"bool",!1,!1]
     */
    public static final ABProp P2P_PILLS_GRAPHQL_ENABLED = new ABProp(30629, "false", "false");

    /**
     * The {@code payments_upr_custom_payment_methods_sync_countries} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: payments_upr_custom_payment_methods_sync_countries:[30647,"string","",""]
     */
    public static final ABProp PAYMENTS_UPR_CUSTOM_PAYMENT_METHODS_SYNC_COUNTRIES = new ABProp(30647, "", "");

    /**
     * The {@code shortcake_companion_prologue__passkeys__assertion_timeout_seconds} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: shortcake_companion_prologue__passkeys__assertion_timeout_seconds:[30661,"int",600,600]
     */
    public static final ABProp SHORTCAKE_COMPANION_PROLOGUE__PASSKEYS__ASSERTION_TIMEOUT_SECONDS = new ABProp(30661, "600", "600");

    /**
     * The {@code shortcake_companion_prologue__passkeys__request_options_ttl_seconds} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: shortcake_companion_prologue__passkeys__request_options_ttl_seconds:[30662,"int",600,600]
     */
    public static final ABProp SHORTCAKE_COMPANION_PROLOGUE__PASSKEYS__REQUEST_OPTIONS_TTL_SECONDS = new ABProp(30662, "600", "600");

    /**
     * The {@code wds_web_menu_reaction_detail_panel_v2} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wds_web_menu_reaction_detail_panel_v2:[30694,"bool",!1,!1]
     */
    public static final ABProp WDS_WEB_MENU_REACTION_DETAIL_PANEL_V2 = new ABProp(30694, "false", "false");

    /**
     * The {@code web_read_self_watermark_processing} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_read_self_watermark_processing:[30736,"bool",!1,!1]
     */
    public static final ABProp WEB_READ_SELF_WATERMARK_PROCESSING = new ABProp(30736, "false", "false");

    /**
     * The {@code web_disable_compose_box_for_deprecated_chats} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_disable_compose_box_for_deprecated_chats:[30753,"bool",!1,!0]
     */
    public static final ABProp WEB_DISABLE_COMPOSE_BOX_FOR_DEPRECATED_CHATS = new ABProp(30753, "false", "true");

    /**
     * The {@code wa_webtp_pdf_sharer_consent_copy_v2} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: wa_webtp_pdf_sharer_consent_copy_v2:[30771,"bool",!1,!1]
     */
    public static final ABProp WA_WEBTP_PDF_SHARER_CONSENT_COPY_V2 = new ABProp(30771, "false", "false");

    /**
     * The {@code web_getters_lru_cache_size_limit} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_getters_lru_cache_size_limit:[30796,"int",0,0]
     */
    public static final ABProp WEB_GETTERS_LRU_CACHE_SIZE_LIMIT = new ABProp(30796, "0", "0");

    /**
     * The {@code device_sync_on_stale_chat_open} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: device_sync_on_stale_chat_open:[30820,"bool",!1,!1]
     */
    public static final ABProp DEVICE_SYNC_ON_STALE_CHAT_OPEN = new ABProp(30820, "false", "false");

    /**
     * The {@code device_sync_on_stale_chat_open_threshold_seconds} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: device_sync_on_stale_chat_open_threshold_seconds:[30821,"int",43200,43200]
     */
    public static final ABProp DEVICE_SYNC_ON_STALE_CHAT_OPEN_THRESHOLD_SECONDS = new ABProp(30821, "43200", "43200");

    /**
     * The {@code web_conversation_cleanup_temp_collection} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_conversation_cleanup_temp_collection:[30829,"bool",!1,!1]
     */
    public static final ABProp WEB_CONVERSATION_CLEANUP_TEMP_COLLECTION = new ABProp(30829, "false", "false");

    /**
     * The {@code status_player_avatar_status_creation_entrypoint} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: status_player_avatar_status_creation_entrypoint:[30912,"bool",!1,!0]
     */
    public static final ABProp STATUS_PLAYER_AVATAR_STATUS_CREATION_ENTRYPOINT = new ABProp(30912, "false", "true");

    /**
     * The {@code web_calling_speaker_strip_resize_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: web_calling_speaker_strip_resize_enabled:[30928,"bool",!1,!1]
     */
    public static final ABProp WEB_CALLING_SPEAKER_STRIP_RESIZE_ENABLED = new ABProp(30928, "false", "false");

    /**
     * The {@code p2p_pills_max_wait_on_contact_card_send} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: p2p_pills_max_wait_on_contact_card_send:[30943,"int",5,5]
     */
    public static final ABProp P2P_PILLS_MAX_WAIT_ON_CONTACT_CARD_SEND = new ABProp(30943, "5", "5");

    /**
     * The {@code ai_subscription_metering_enabled} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: ai_subscription_metering_enabled:[30960,"bool",!1,!1]
     */
    public static final ABProp AI_SUBSCRIPTION_METERING_ENABLED = new ABProp(30960, "false", "false");

    /**
     * The {@code send_sms_web_infra} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: send_sms_web_infra:[31004,"bool",!1,!1]
     */
    public static final ABProp SEND_SMS_WEB_INFRA = new ABProp(31004, "false", "false");

    /**
     * The {@code smb_qp_web_debug_recunit} AB prop.
     *
     * <p>Source definition in {@code WAWebABPropsConfigs}: smb_qp_web_debug_recunit:[31009,"bool",!1,!1]
     */
    public static final ABProp SMB_QP_WEB_DEBUG_RECUNIT = new ABProp(31009, "false", "false");

    /**
     * Constructs a new {@code ABProp} definition.
     *
     * @throws NullPointerException if {@code defaultValue} or {@code debugDefaultValue}
     *         is {@code null}
     */
    public ABProp {
        Objects.requireNonNull(defaultValue, "defaultValue cannot be null");
        Objects.requireNonNull(debugDefaultValue, "debugDefaultValue cannot be null");
    }

    /**
     * Converts a string value to a boolean.
     *
     * <p>The following values are considered {@code true}: {@code "1"}, {@code "True"},
     * and {@code "true"}. All other values are considered {@code false}.
     *
     * @param value the string value to convert
     * @return the boolean representation of the given value
     */
    public static boolean toBoolean(String value) {
        return "1".equals(value)
                || "True".equals(value)
                || "true".equals(value);
    }

    /**
     * Attempts to convert a string value to an integer.
     *
     * @param value the string value to parse
     * @return an {@link OptionalInt} containing the integer value if parsing succeeds,
     *         or empty if it fails
     */
    public static OptionalInt toInt(String value) {
        try {
            return OptionalInt.of(Integer.parseInt(value));
        } catch (NumberFormatException exception) {
            return OptionalInt.empty();
        }
    }

    /**
     * Attempts to convert a string value to a long.
     *
     * @param value the string value to parse
     * @return an {@link OptionalLong} containing the long value if parsing succeeds,
     *         or empty if it fails
     */
    public static OptionalLong toLong(String value) {
        try {
            return OptionalLong.of(Long.parseLong(value));
        } catch (NumberFormatException exception) {
            return OptionalLong.empty();
        }
    }

    /**
     * Attempts to convert a string value to a double (floating-point).
     *
     * @param value the string value to parse
     * @return an {@link OptionalDouble} containing the double value if parsing succeeds,
     *         or empty if it fails
     */
    public static OptionalDouble toDouble(String value) {
        try {
            return OptionalDouble.of(Double.parseDouble(value));
        } catch (NumberFormatException exception) {
            return OptionalDouble.empty();
        }
    }

    @Override
    public String toString() {
        return "ABProp[code=%d, defaultValue='%s', debugDefaultValue='%s']"
                .formatted(code, defaultValue, debugDefaultValue);
    }
}
