package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumDeepLinkOpenFrom")
@WamEnum
public enum DeepLinkOpenFrom {
    @WamEnumConstant(1) DEEP_LINK_EXTERNAL,
    @WamEnumConstant(2) DEEP_LINK_WA_LINK_CLICK,
    @WamEnumConstant(3) QR_CODE_SHEET,
    @WamEnumConstant(4) DEEP_LINK_BANNER,
    @WamEnumConstant(5) DEEP_LINK_SMB_NOTIFICATION,
    @WamEnumConstant(6) DEEP_LINK_MESSENGER_APP
}
