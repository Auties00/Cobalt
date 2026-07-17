package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMediaUploadModeType")
@WamEnum
public enum MediaUploadModeType {
    @WamEnumConstant(1) REGULAR,
    @WamEnumConstant(2) FAST_FORWARD_EXIST_CHECK,
    @WamEnumConstant(3) VIDEO_EXIST_CHECK,
    @WamEnumConstant(4) PRODUCT,
    @WamEnumConstant(5) MEDIA_RETRY,
    @WamEnumConstant(6) WEB_REUPLOAD,
    @WamEnumConstant(7) THUMBNAIL,
    @WamEnumConstant(8) EXPRESS_PATH_UPLOAD
}
