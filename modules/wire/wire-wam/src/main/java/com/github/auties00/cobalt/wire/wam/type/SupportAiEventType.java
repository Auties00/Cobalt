package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumSupportAiEventType")
@WamEnum
public enum SupportAiEventType {
    @WamEnumConstant(1) CONTACT_US_CLICKED,
    @WamEnumConstant(2) START_CHAT_CLICKED,
    @WamEnumConstant(6) NO_INTERNET_DIALOG_SHOWN,
    @WamEnumConstant(7) TICKET_CREATION_DIALOG_SHOWN,
    @WamEnumConstant(8) REVIEW_INFORMATION_LEARN_MORE_CLICKED,
    @WamEnumConstant(9) SUPPORT_AI_SCREEN_SHOWN,
    @WamEnumConstant(10) THUMB_UP_CLICKED,
    @WamEnumConstant(11) THUMB_DOWN_CLICKED,
    @WamEnumConstant(12) SUBMIT_MESSAGE_FEEDBACK,
    @WamEnumConstant(13) SUBMIT_MESSAGE_FEEDBACK_FAILED,
    @WamEnumConstant(14) SUBMIT_MESSAGE_FEEDBACK_SUCCEEDED,
    @WamEnumConstant(15) NEGATIVE_FEEDBACK_OPTIONS_SCREEN_CANCELLED,
    @WamEnumConstant(16) NEGATIVE_FEEDBACK_OPTIONS_SCREEN_SHOWN,
    @WamEnumConstant(17) CONTACT_US_ERROR_DIALOG_SHOWN,
    @WamEnumConstant(18) CONTACT_US_ERROR_DIALOG_EMAIL_BUTTON_CLICKED,
    @WamEnumConstant(19) CREATE_SUPPORT_TICKET_SUCCESS,
    @WamEnumConstant(20) CREATE_SUPPORT_TICKET_ERROR,
    @WamEnumConstant(21) SUPPORT_AI_SCREEN_OK_CLICKED,
    @WamEnumConstant(22) SUPPORT_AI_SCREEN_SHOWN_ON_THE_CHAT,
    @WamEnumConstant(23) SUPPORT_AI_CITATION_TAPPED
}
