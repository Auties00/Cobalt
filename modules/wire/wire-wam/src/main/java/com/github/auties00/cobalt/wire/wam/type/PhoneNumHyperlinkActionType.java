package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPhoneNumHyperlinkActionType")
@WamEnum
public enum PhoneNumHyperlinkActionType {
    @WamEnumConstant(1) PHONE_NUM_HYPERLINK_MSG_RECEIVED,
    @WamEnumConstant(2) LONG_PRESS_PHONE_NUM_HYPERLINK,
    @WamEnumConstant(3) CLICK_PHONE_NUM_HYPERLINK,
    @WamEnumConstant(4) CLICK_CALL,
    @WamEnumConstant(5) CLICK_ADD_TO_CONTACTS,
    @WamEnumConstant(6) CLICK_MESSAGE_ON_WHATSAPP,
    @WamEnumConstant(7) CLICK_COPY_PHONE_NUMBER,
    @WamEnumConstant(8) CLOSE_DIALOG_BOX,
    @WamEnumConstant(9) MESSAGE_SENT,
    @WamEnumConstant(10) CLICK_CALL_ON_WHATSAPP,
    @WamEnumConstant(11) CLICK_INVITE_TO_WHATSAPP
}
