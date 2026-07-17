package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumKicErrorCodeType")
@WamEnum
public enum KicErrorCodeType {
    @WamEnumConstant(1) NONE,
    @WamEnumConstant(2) MESSAGE_MISSING,
    @WamEnumConstant(3) MESSAGE_REVOKED,
    @WamEnumConstant(4) SENDER_UNKEPT,
    @WamEnumConstant(5) OLDER_REQUEST,
    @WamEnumConstant(6) ORPHAN_EXPIRED,
    @WamEnumConstant(7) TIE_BREAK_IGNORED,
    @WamEnumConstant(8) MESSAGE_EXPIRED,
    @WamEnumConstant(9) NO_PERMISSION_TO_EDIT,
    @WamEnumConstant(10) MESSAGE_FROM_EX_MEMBER,
    @WamEnumConstant(11) OFFLINE,
    @WamEnumConstant(12) SENDER_DISABLED,
    @WamEnumConstant(13) KEPT_BEYOND_EXPIRY,
    @WamEnumConstant(14) NOT_PART_OF_THE_GROUP,
    @WamEnumConstant(15) CONTACT_BLOCKED,
    @WamEnumConstant(999) UNKNOWN
}
