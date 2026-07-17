package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumStatusPrivacySurface")
@WamEnum
public enum StatusPrivacySurface {
    @WamEnumConstant(1) STATUS_PRIVACY_SETTINGS,
    @WamEnumConstant(2) TEXT_COMPOSER,
    @WamEnumConstant(3) MEDIA_COMPOSER,
    @WamEnumConstant(4) CONTACT_PICKER,
    @WamEnumConstant(5) VOICE_COMPOSER,
    @WamEnumConstant(6) STATUS_VIEWER_CLOSE_SHARING_MIMICRY
}
