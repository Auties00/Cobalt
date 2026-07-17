package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumLwiAdsContentType")
@WamEnum
public enum LwiAdsContentType {
    @WamEnumConstant(1) LWI_ADS_CONTENT_TYPE_PRODUCT,
    @WamEnumConstant(2) LWI_ADS_CONTENT_TYPE_STATUS,
    @WamEnumConstant(3) LWI_ADS_CONTENT_TYPE_DEVICE_MEDIA,
    @WamEnumConstant(4) LWI_ADS_CONTENT_TYPE_BUSINESS_PROFILE_PIC,
    @WamEnumConstant(9) LWI_ADS_CONTENT_TYPE_CAMERA,
    @WamEnumConstant(10) LWI_ADS_CONTENT_TYPE_RECENTLY_USED_MEDIA,
    @WamEnumConstant(11) LWI_ADS_CONTENT_TYPE_CATALOGS_ALL,
    @WamEnumConstant(12) LWI_ADS_CONTENT_TYPE_STATUSES_ALL,
    @WamEnumConstant(13) LWI_ADS_CONTENT_TYPE_RECREATION_MEDIA,
    @WamEnumConstant(14) LWI_ADS_CONTENT_DRAFT_AD_NUX,
    @WamEnumConstant(15) LWI_ADS_CONTENT_TYPE_PREVIOUS_ADS,
    @WamEnumConstant(16) LWI_ADS_CONTENT_TYPE_FB_MEDIA,
    @WamEnumConstant(17) LWI_ADS_CONTENT_TYPE_IG_MEDIA
}
