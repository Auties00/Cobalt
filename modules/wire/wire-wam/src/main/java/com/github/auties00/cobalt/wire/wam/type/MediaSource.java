package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMediaSource")
@WamEnum
public enum MediaSource {
    @WamEnumConstant(1) LOCAL_MEDIA_GALLERY,
    @WamEnumConstant(2) NEW_MEDIA_CAMERA,
    @WamEnumConstant(3) CATALOG_MEDIA,
    @WamEnumConstant(4) STATUS_MEDIA,
    @WamEnumConstant(5) BIZ_PROFILE_IMAGE,
    @WamEnumConstant(6) FB_MEDIA,
    @WamEnumConstant(7) IG_MEDIA
}
