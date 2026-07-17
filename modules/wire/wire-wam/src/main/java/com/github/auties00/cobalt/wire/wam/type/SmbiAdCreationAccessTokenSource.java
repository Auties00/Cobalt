package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumSmbiAdCreationAccessTokenSource")
@WamEnum
public enum SmbiAdCreationAccessTokenSource {
    @WamEnumConstant(1) WA_ACCOUNT_ACCESS_TOKEN,
    @WamEnumConstant(2) FB_ACCESS_TOKEN
}
