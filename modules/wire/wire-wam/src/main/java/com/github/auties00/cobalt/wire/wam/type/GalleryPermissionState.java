package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumGalleryPermissionState")
@WamEnum
public enum GalleryPermissionState {
    @WamEnumConstant(1) FULL,
    @WamEnumConstant(2) PARTIAL,
    @WamEnumConstant(3) NONE
}
