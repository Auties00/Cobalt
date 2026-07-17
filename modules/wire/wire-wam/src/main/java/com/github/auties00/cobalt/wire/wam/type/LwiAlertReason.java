package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumLwiAlertReason")
@WamEnum
public enum LwiAlertReason {
    @WamEnumConstant(1) LWI_ERROR_CATALOG_LIST_FB_PAGE_NOT_LINKED,
    @WamEnumConstant(2) LWI_ERROR_CATALOG_LIST_EMPTY,
    @WamEnumConstant(3) LWI_ERROR_CATALOG_LIST_NO_ELIGIBLE_PRODUCT,
    @WamEnumConstant(4) LWI_ERROR_UNSUPPORTED_OLD_FB_VERSION,
    @WamEnumConstant(5) LWI_ERROR_STATUS_EMPTY,
    @WamEnumConstant(6) LWI_NONCE_ERROR,
    @WamEnumConstant(7) LWI_ERROR_AD_ACCOUNT_CURRENCY_MISMATCH,
    @WamEnumConstant(8) LWI_AD_CREATION_ERROR,
    @WamEnumConstant(9) LWI_MISSING_PAYMENT_METHOD,
    @WamEnumConstant(10) LWI_NO_INTERNET_CONNECTION,
    @WamEnumConstant(11) LWI_SPECIAL_ADS_CATEGORY,
    @WamEnumConstant(12) LWI_LOGIN_ON_FACEBOOK,
    @WamEnumConstant(13) LWI_REMOVE_FB_ACCOUNT,
    @WamEnumConstant(14) LWI_WEBSITE_NOT_SAFE,
    @WamEnumConstant(15) LWI_WEBSITE_NOT_AVAILABLE,
    @WamEnumConstant(16) LWI_NON_HTTPS,
    @WamEnumConstant(17) LWI_FB_WEB_LOGIN_TOKEN_FETCH_ERROR,
    @WamEnumConstant(18) LWI_RETRY_WEB_LOGIN_DIALOG_ERROR,
    @WamEnumConstant(19) LWI_UNABLE_TO_REDIRECT_BROWSER_NOT_FOUND,
    @WamEnumConstant(20) LWI_AD_ACCOUNT_SWITCH_ERROR,
    @WamEnumConstant(21) LWI_POLICY_REVIEW_REQUIRED,
    @WamEnumConstant(22) LWI_SOMETHING_WENT_WRONG,
    @WamEnumConstant(23) LWI_MAXIMUM_RETRY_CODE_REACH,
    @WamEnumConstant(24) LWI_ERROR_WRONG_CODE,
    @WamEnumConstant(25) LWI_ERROR_INCORRECT_EMAIL_FORMAT,
    @WamEnumConstant(26) LWI_NO_RESULTS_FOUND,
    @WamEnumConstant(27) LWI_ACTION_ESTIMATED_REACH_FAILED_TO_LOAD,
    @WamEnumConstant(28) LWI_ACTION_INVALID_USER,
    @WamEnumConstant(29) LWI_LOCATION_ACCESS_PERMISSION,
    @WamEnumConstant(30) LWI_ACTION_INVALID_MEDIA_SELECTION,
    @WamEnumConstant(31) LWI_ACTION_SCREEN_DESTROYED,
    @WamEnumConstant(32) LWI_ACTION_INFO_BUTTON_TAPPED,
    @WamEnumConstant(33) LWI_INTEGRITY_ERROR,
    @WamEnumConstant(34) LWI_INVALID_STATE,
    @WamEnumConstant(35) LWI_FAILED_TO_ENROLL_COUPON,
    @WamEnumConstant(36) LWI_CHANGES_NOT_SAVED,
    @WamEnumConstant(37) LWI_LOCATION_SYSTEM_SETTING_RESOLUTION_REQUIRED,
    @WamEnumConstant(38) LWI_ACTION_CURRENT_LOCATION_FAILED,
    @WamEnumConstant(39) LWI_BILLING_RESOURCE_HTTP_ERROR,
    @WamEnumConstant(40) LWI_BILLING_WEBVIEW_NETWORK_ERROR_SCREEN_SHOWN,
    @WamEnumConstant(41) LWI_BILLING_EXCEPTION_SERIALIZING_RESPONSE,
    @WamEnumConstant(42) LWI_BILLING_EXCEPTION_FETCHING_RESOURCE_FROM_CACHE_OR_NETWORK,
    @WamEnumConstant(43) LWI_GRAPHQL_ERROR,
    @WamEnumConstant(45) LWI_EDIT_GOAL_ALERT,
    @WamEnumConstant(46) LWI_ACTION_ESTIMATED_CONVERSATIONS_FAILED_TO_LOAD,
    @WamEnumConstant(47) LWI_UPLOAD_MEDIA_FAILED
}
