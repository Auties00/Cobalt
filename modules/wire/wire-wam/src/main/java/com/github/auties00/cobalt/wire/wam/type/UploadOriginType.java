package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumUploadOriginType")
@WamEnum
public enum UploadOriginType {
    @WamEnumConstant(1) UNKNOWN,
    @WamEnumConstant(2) CHAT_PERSONAL,
    @WamEnumConstant(3) CHAT_GROUP,
    @WamEnumConstant(4) STATUS_USER,
    @WamEnumConstant(5) PRODUCT_CATALOG,
    @WamEnumConstant(6) STICKER_WEB,
    @WamEnumConstant(7) PAYMENTS_KYC,
    @WamEnumConstant(8) MESSAGE_HISTORY_SYNC,
    @WamEnumConstant(9) COMMUNITY,
    @WamEnumConstant(10) CHANNEL,
    @WamEnumConstant(11) BROADCAST,
    @WamEnumConstant(12) MULTI_CHAT,
    @WamEnumConstant(13) INTEROP,
    @WamEnumConstant(14) WA_BACKUP
}
