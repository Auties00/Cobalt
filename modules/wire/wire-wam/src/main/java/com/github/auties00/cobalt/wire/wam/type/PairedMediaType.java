package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPairedMediaType")
@WamEnum
public enum PairedMediaType {
    @WamEnumConstant(0) SD_PHOTO,
    @WamEnumConstant(1) HD_PHOTO,
    @WamEnumConstant(2) SD_VIDEO,
    @WamEnumConstant(3) HD_VIDEO,
    @WamEnumConstant(4) MOTION_PHOTO_PARENT,
    @WamEnumConstant(5) MOTION_PHOTO_CHILD,
    @WamEnumConstant(6) HEVC_VIDEO_PARENT,
    @WamEnumConstant(7) HEVC_VIDEO_CHILD
}
