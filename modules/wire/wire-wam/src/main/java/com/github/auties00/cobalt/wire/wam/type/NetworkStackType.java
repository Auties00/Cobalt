package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumNetworkStackType")
@WamEnum
public enum NetworkStackType {
    @WamEnumConstant(0) NATIVE,
    @WamEnumConstant(1) OK_HTTP,
    @WamEnumConstant(2) LIGER,
    @WamEnumConstant(3) APACHE,
    @WamEnumConstant(4) WATLS,
    @WamEnumConstant(5) CRONET,
    @WamEnumConstant(6) TIGON_HUC,
    @WamEnumConstant(7) TIGON_MNS
}
