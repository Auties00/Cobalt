package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumStatusPrivacySettingsView")
@WamEnum
public enum StatusPrivacySettingsView {
    @WamEnumConstant(1) SETTINGS_FULL,
    @WamEnumConstant(2) SETTINGS_BOTTOM_SHEET,
    @WamEnumConstant(3) SELECTION_PILLS
}
