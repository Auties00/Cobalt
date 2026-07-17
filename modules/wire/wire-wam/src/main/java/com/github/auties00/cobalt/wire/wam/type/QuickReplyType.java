package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumQuickReplyType")
@WamEnum
public enum QuickReplyType {
    @WamEnumConstant(1) QUICK_REPLY_TYPE_PIX_KEY,
    @WamEnumConstant(2) QUICK_REPLY_TYPE_SHARE_UPI_QR,
    @WamEnumConstant(3) QUICK_REPLY_TYPE_SETUP_UPI_QR,
    @WamEnumConstant(4) QUICK_REPLY_TYPE_PAYMENT_KEY,
    @WamEnumConstant(5) QUICK_REPLY_TYPE_REQUEST_CONTACT_INFO
}
