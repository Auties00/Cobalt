package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumSystemMessageTypeType")
@WamEnum
public enum SystemMessageTypeType {
    @WamEnumConstant(1) E2E_ENCRYPTED_MESSAGES_CALLS,
    @WamEnumConstant(2) E2E_ENCRYPTED_MESSAGES,
    @WamEnumConstant(3) E2E_ENCRYPTED_BROADCAST_LIST,
    @WamEnumConstant(4) E2E_ENCRYPTED_MESSAGE_YOURSELF,
    @WamEnumConstant(8) OFFICIAL_ACCOUNT_INFO,
    @WamEnumConstant(9) GROUP_ADD,
    @WamEnumConstant(10) GROUP_RESET_INVITE,
    @WamEnumConstant(11) COMMUNITY_DESCRIPTION_CHANGED,
    @WamEnumConstant(12) GROUP_DESCRIPTION_CHANGED,
    @WamEnumConstant(13) GROUP_PARTICIPANTS_CHANGED,
    @WamEnumConstant(14) COMMUNITY_MEMBERS_CHANGED,
    @WamEnumConstant(15) GROUP_INVITE_LINK_UNAVAILABLE,
    @WamEnumConstant(16) GROUP_INVITE_LINK_AVAILABLE,
    @WamEnumConstant(17) GROUP_JOIN_REQUEST,
    @WamEnumConstant(18) GROUP_SUGGEST
}
