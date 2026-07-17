package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumRadioType")
@WamEnum
public enum RadioType {
    @WamEnumConstant(0) CELLULAR_UNKNOWN,
    @WamEnumConstant(1) WIFI_UNKNOWN,
    @WamEnumConstant(100) CELLULAR_EDGE,
    @WamEnumConstant(101) CELLULAR_IDEN,
    @WamEnumConstant(102) CELLULAR_UMTS,
    @WamEnumConstant(103) CELLULAR_EVDO,
    @WamEnumConstant(104) CELLULAR_GPRS,
    @WamEnumConstant(105) CELLULAR_HSDPA,
    @WamEnumConstant(106) CELLULAR_HSUPA,
    @WamEnumConstant(107) CELLULAR_HSPA,
    @WamEnumConstant(108) CELLULAR_CDMA,
    @WamEnumConstant(109) CELLULAR_1XRTT,
    @WamEnumConstant(110) CELLULAR_EHRPD,
    @WamEnumConstant(111) CELLULAR_LTE,
    @WamEnumConstant(112) CELLULAR_HSPAP,
    @WamEnumConstant(113) CELLULAR_NR,
    @WamEnumConstant(114) CELLULAR_NRNSA,
    @WamEnumConstant(115) CELLULAR_IWLAN,
    @WamEnumConstant(116) CELLULAR_TD_SCDMA
}
