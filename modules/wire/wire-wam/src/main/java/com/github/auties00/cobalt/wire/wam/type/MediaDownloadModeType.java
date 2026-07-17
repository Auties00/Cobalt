package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMediaDownloadModeType")
@WamEnum
public enum MediaDownloadModeType {
    @WamEnumConstant(1) MANUAL,
    @WamEnumConstant(2) FULL,
    @WamEnumConstant(3) PREFETCH,
    @WamEnumConstant(4) HEADER,
    @WamEnumConstant(5) THUMBNAIL,
    @WamEnumConstant(6) EXPRESS_PATH_DOWNLOAD,
    @WamEnumConstant(7) PREFETCH_FOR_THUMB_PREVIEW,
    @WamEnumConstant(8) FULL_FOR_THUMB_PREVIEW
}
