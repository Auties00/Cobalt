package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumWebcNativeUpsellCtaSourceType")
@WamEnum
public enum WebcNativeUpsellCtaSourceType {
    @WamEnumConstant(1) INTRO_PANEL,
    @WamEnumConstant(2) CHATLIST_DROPDOWN,
    @WamEnumConstant(3) BUTTERBAR,
    @WamEnumConstant(4) QR_BANNER,
    @WamEnumConstant(5) SEARCH_RESULTS,
    @WamEnumConstant(6) CALL_BTN_MODAL,
    @WamEnumConstant(7) CALL_BTN_MODAL_2,
    @WamEnumConstant(8) MISSED_CALL_MODAL,
    @WamEnumConstant(9) MISSED_CALL_MODAL_2,
    @WamEnumConstant(10) QR_BANNER_2,
    @WamEnumConstant(11) QR_DOWNLOAD_BUTTON,
    @WamEnumConstant(12) QR_SLIM_BANNER,
    @WamEnumConstant(13) QR_LARGE_BANNER,
    @WamEnumConstant(14) SETTINGS,
    @WamEnumConstant(15) SETTINGS_HELP,
    @WamEnumConstant(16) LINK_DEVICE_APPLE_TOUCHSCREEN_OVERLAY
}
