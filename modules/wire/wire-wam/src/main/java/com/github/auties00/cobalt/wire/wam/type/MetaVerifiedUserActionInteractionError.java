package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMetaVerifiedUserActionInteractionError")
@WamEnum
public enum MetaVerifiedUserActionInteractionError {
    @WamEnumConstant(1) EDIT_PROFILE_PICTURE_LIMIT_REACHED,
    @WamEnumConstant(2) NEW_SUBSCRIPTION,
    @WamEnumConstant(3) LINKED_DEVICE_LIMIT_REACHED_ERROR,
    @WamEnumConstant(4) PROFILE_PIC_NOT_AUTHORISED_ERROR
}
