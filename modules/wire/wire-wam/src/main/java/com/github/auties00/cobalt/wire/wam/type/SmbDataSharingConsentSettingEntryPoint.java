package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumSmbDataSharingConsentSettingEntryPoint")
@WamEnum
public enum SmbDataSharingConsentSettingEntryPoint {
    @WamEnumConstant(0) ENTRY_POINT_ORDER_SCREEN,
    @WamEnumConstant(1) ENTRY_POINT_SETTINGS_SCREEN,
    @WamEnumConstant(2) ENTRY_POINT_LABELS_SCREEN,
    @WamEnumConstant(3) ENTRY_POINT_CHAT_SCREEN,
    @WamEnumConstant(4) ENTRY_POINT_UNKNOWN,
    @WamEnumConstant(5) ENTRY_POINT_DEEP_LINK_GENERIC,
    @WamEnumConstant(6) ENTRY_POINT_DEEP_LINK_ADS_MANAGER_3PD_GUIDANCE_CARD
}
