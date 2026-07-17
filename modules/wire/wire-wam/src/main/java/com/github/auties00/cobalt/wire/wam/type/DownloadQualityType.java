package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumDownloadQualityType")
@WamEnum
public enum DownloadQualityType {
    @WamEnumConstant(1) NORMAL_QUALITY,
    @WamEnumConstant(2) MID_QUALITY,
    @WamEnumConstant(3) INELIGIBLE_IMAGE_TOO_SMALL,
    @WamEnumConstant(4) INELIGIBLE_PARTIAL_HASHES_NOT_FOUND,
    @WamEnumConstant(5) ERROR_DETERMINING_ELIGIBILITY,
    @WamEnumConstant(6) NOT_DOWNLOADED_ENOUGH_BYTES_TO_DETERMINE_ELIGIBILITY
}
