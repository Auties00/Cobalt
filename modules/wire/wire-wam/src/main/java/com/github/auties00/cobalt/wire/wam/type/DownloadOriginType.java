package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumDownloadOriginType")
@WamEnum
public enum DownloadOriginType {
    @WamEnumConstant(1) CHAT_PERSONAL,
    @WamEnumConstant(2) CHAT_GROUP,
    @WamEnumConstant(3) STATUS_USER,
    @WamEnumConstant(4) STATUS_ADS,
    @WamEnumConstant(5) PRODUCT_CATALOG,
    @WamEnumConstant(6) GDPR,
    @WamEnumConstant(7) STICKER_PICKER,
    @WamEnumConstant(8) PROFILE_PICTURE,
    @WamEnumConstant(9) BLOKS,
    @WamEnumConstant(10) P2B,
    @WamEnumConstant(11) MESSAGE_HISTORY_SYNC,
    @WamEnumConstant(12) COMMUNITY,
    @WamEnumConstant(13) CHANNEL,
    @WamEnumConstant(14) BROADCAST,
    @WamEnumConstant(15) INTEROP,
    @WamEnumConstant(16) WAMO_STATUS,
    @WamEnumConstant(17) WAMO_BIZ_PROFILE,
    @WamEnumConstant(18) WAMO_PREFERENCES,
    @WamEnumConstant(19) RICH_ORDER_STATUS,
    @WamEnumConstant(20) WA_BACKUP
}
