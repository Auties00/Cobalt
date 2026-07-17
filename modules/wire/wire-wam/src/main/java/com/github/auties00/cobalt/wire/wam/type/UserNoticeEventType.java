package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumUserNoticeEvent")
@WamEnum
public enum UserNoticeEventType {
    @WamEnumConstant(0) TRIGGERED,
    @WamEnumConstant(1) BANNER_APPEAR,
    @WamEnumConstant(2) BANNER_SELECT,
    @WamEnumConstant(3) MODAL_APPEAR,
    @WamEnumConstant(4) MODAL_DISMISS,
    @WamEnumConstant(5) MODAL_LINK_FOLLOW,
    @WamEnumConstant(6) MODAL_ACCEPT,
    @WamEnumConstant(7) BLOCKING_MODAL_APPEAR,
    @WamEnumConstant(8) BLOCKING_MODAL_LINK_FOLLOW,
    @WamEnumConstant(9) BLOCKING_MODAL_ACCEPT,
    @WamEnumConstant(10) BANNER_DISMISS,
    @WamEnumConstant(11) EDUCATION_1_APPEAR,
    @WamEnumConstant(12) EDUCATION_1_DISMISS,
    @WamEnumConstant(13) EDUCATION_1_CONTINUE,
    @WamEnumConstant(14) MODAL_BACK,
    @WamEnumConstant(15) BLOCKING_MODAL_BACK,
    @WamEnumConstant(100) STARTED,
    @WamEnumConstant(105) PDFN_SHOWN_0,
    @WamEnumConstant(110) BADGE_APPEARED_NEXT_TO_SETTINGS,
    @WamEnumConstant(111) PDFN_SHOWN_1,
    @WamEnumConstant(112) PDFN_SHOWN_2,
    @WamEnumConstant(113) PDFN_SHOWN_3,
    @WamEnumConstant(114) PDFN_SHOWN_4,
    @WamEnumConstant(115) PDFN_SHOWN_5,
    @WamEnumConstant(116) PDFN_SHOWN_6,
    @WamEnumConstant(117) PDFN_SHOWN_7,
    @WamEnumConstant(118) PDFN_SHOWN_8,
    @WamEnumConstant(119) PDFN_SHOWN_9,
    @WamEnumConstant(120) BADGE_APPEARED_NEXT_TO_HELP,
    @WamEnumConstant(130) BADGE_SHOWN_IN_HELP,
    @WamEnumConstant(145) PDFN_DISMISSED,
    @WamEnumConstant(150) BADGE_EXPIRED,
    @WamEnumConstant(155) PDFN_OK,
    @WamEnumConstant(160) PDFN_SOFT_OPT_IN,
    @WamEnumConstant(162) PDFN_SOFT_OPT_OUT,
    @WamEnumConstant(165) PDFN_DENIED,
    @WamEnumConstant(170) BADGE_SELECTED,
    @WamEnumConstant(175) PDFN_ACCEPTED,
    @WamEnumConstant(400) PDFN_FAILED_TO_DOWNLOAD,
    @WamEnumConstant(410) PDFN_PARTIAL_DOWNLOAD,
    @WamEnumConstant(420) PDFN_ERROR_MISMATCHED_TEMPLATE,
    @WamEnumConstant(499) PDFN_ERROR_UNKNOWN,
    @WamEnumConstant(999) FINAL_END,
    @WamEnumConstant(1014) PDFN_0_SECONDARY_BTN_CLICKED,
    @WamEnumConstant(1015) PDFN_1_SECONDARY_BTN_CLICKED,
    @WamEnumConstant(1016) PDFN_2_SECONDARY_BTN_CLICKED,
    @WamEnumConstant(1017) PDFN_3_SECONDARY_BTN_CLICKED,
    @WamEnumConstant(1018) PDFN_4_SECONDARY_BTN_CLICKED,
    @WamEnumConstant(1019) PDFN_5_SECONDARY_BTN_CLICKED,
    @WamEnumConstant(1020) PDFN_6_SECONDARY_BTN_CLICKED,
    @WamEnumConstant(1021) PDFN_7_SECONDARY_BTN_CLICKED,
    @WamEnumConstant(1022) PDFN_8_SECONDARY_BTN_CLICKED,
    @WamEnumConstant(1023) PDFN_9_SECONDARY_BTN_CLICKED
}
