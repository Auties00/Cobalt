package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumManageAdsEntryPoint")
@WamEnum
public enum ManageAdsEntryPoint {
    @WamEnumConstant(1) WEB_OVERFLOW_MENU,
    @WamEnumConstant(2) SMB_CHAT_LIST_CTWA_BANNER,
    @WamEnumConstant(3) SMB_NATIVE_ADS_MANAGEMENT,
    @WamEnumConstant(4) SMB_BUSINESS_TOOLS_MANAGE_ADS_LIST_ITEM
}
