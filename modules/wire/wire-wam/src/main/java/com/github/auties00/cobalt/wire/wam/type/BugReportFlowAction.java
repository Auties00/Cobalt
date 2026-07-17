package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumBugReportFlowAction")
@WamEnum
public enum BugReportFlowAction {
    @WamEnumConstant(1) NO_INTERNET_CONNECTION_IMPRESSION,
    @WamEnumConstant(2) BUG_REPORT_FORM_IMPRESSION,
    @WamEnumConstant(3) CANCEL_CLICK,
    @WamEnumConstant(4) ADD_SCREENSHOT_CLICK,
    @WamEnumConstant(5) REMOVE_SCREENSHOT_CLICK,
    @WamEnumConstant(6) SUBMIT_CLICK,
    @WamEnumConstant(7) SUCCESS_VIEW_IMPRESSION,
    @WamEnumConstant(8) ERROR_VIEW_IMPRESSION,
    @WamEnumConstant(9) SUBMISSION_FAILED,
    @WamEnumConstant(10) SUBMISSION_SUCCESSFUL,
    @WamEnumConstant(11) DEVICE_LOG_UPLOAD_FAILED,
    @WamEnumConstant(12) DEVICE_LOG_UPLOAD_SUCCESSFUL,
    @WamEnumConstant(13) MEDIA_UPLOAD_FAILED,
    @WamEnumConstant(14) MEDIA_UPLOAD_SUCCESSFUL,
    @WamEnumConstant(15) MEDIA_UPLOAD_RETRY_CLICK,
    @WamEnumConstant(16) SUBMIT_BUG_WITHOUT_FAILED_MEDIA_CLICK,
    @WamEnumConstant(17) SUBMIT_BUG_WITHOUT_FAILED_MEDIA_CANCEL_CLICK,
    @WamEnumConstant(18) SUBMIT_BUG_RETRY_CLICK,
    @WamEnumConstant(19) SUBMIT_BUG_RETRY_VIA_EMAIL_CLICK,
    @WamEnumConstant(20) SUBMIT_BUG_RETRY_CANCEL_CLICK,
    @WamEnumConstant(21) SUBMIT_BUG_CATEGORY_CLICK,
    @WamEnumConstant(22) MEDIA_ATTACHMENT_FILE_SETUP_ERROR,
    @WamEnumConstant(23) EDUCATION_NUX_FROM_SETTINGS_IMPRESSION,
    @WamEnumConstant(24) EDUCATION_NUX_FROM_RAGESHAKE_IMPRESSION,
    @WamEnumConstant(25) RAGESHAKE_TURN_OFF_FROM_BOTTOM_SHEET_CLICKED,
    @WamEnumConstant(26) RAGESHAKE_TURN_OFF_TOGGLE_CLICKED,
    @WamEnumConstant(27) RAGESHAKE_TURN_ON_TOGGLE_CLICKED,
    @WamEnumConstant(28) RAGESHAKE_BOTTOM_SHEET_IMPRESSION,
    @WamEnumConstant(29) RAGESHAKE_SCREENSHOT_BLOCKED_ALERT_IMPRESSION,
    @WamEnumConstant(30) BUG_REPORT_ATTACHMENT_UPLOAD_FAILED,
    @WamEnumConstant(31) BUG_REPORT_ATTACHMENT_UPLOAD_SUCCESSFUL,
    @WamEnumConstant(32) DOGFOODING_BUG_REPORT_TASK_CREATION_SUCCESSFUL,
    @WamEnumConstant(33) DOGFOODING_BUG_REPORT_TASK_CREATION_FAILED
}
