package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumSmbDataSharingConsentScreenEntryPoint")
@WamEnum
public enum SmbDataSharingConsentScreenEntryPoint {
    @WamEnumConstant(0) NEW_ORDER,
    @WamEnumConstant(1) CART,
    @WamEnumConstant(2) LABEL_CHAT,
    @WamEnumConstant(3) LABEL_MESSAGE,
    @WamEnumConstant(4) BLOCK,
    @WamEnumConstant(5) REPORT,
    @WamEnumConstant(6) CTWA_CHAT,
    @WamEnumConstant(7) DATA_SHARING_TOOLS,
    @WamEnumConstant(8) CONTACT_INFO_CARD,
    @WamEnumConstant(9) DATA_SHARING_SYSTEM_MESSAGE,
    @WamEnumConstant(10) LISTS_MANAGEMENT
}
