package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumPsaMessageRemoveEntryPoint")
@WamEnum
public enum PsaMessageRemoveEntryPoint {
    @WamEnumConstant(1) BLOCK_FROM_CONTACT_INFO,
    @WamEnumConstant(2) BLOCK_FROM_CHAT,
    @WamEnumConstant(3) UNBLOCK_FROM_CONTACT_INFO,
    @WamEnumConstant(4) UNBLOCK_FROM_CHAT,
    @WamEnumConstant(5) UNBLOCK_FROM_PRIVACY_SETTINGS,
    @WamEnumConstant(6) ARCHIVE_FROM_CHAT_LIST,
    @WamEnumConstant(7) ARCHIVE_FROM_DELETE_OPTION,
    @WamEnumConstant(8) UNARCHIVE_FROM_ARCHIVED_CHAT_LIST,
    @WamEnumConstant(9) CLEAR_FROM_CONTACT_INFO,
    @WamEnumConstant(10) CLEAR_FROM_CHAT_LIST,
    @WamEnumConstant(11) DELETE_ALL_FROM_CHAT_LIST,
    @WamEnumConstant(12) DELETE_ALL_FROM_CONTACT_INFO,
    @WamEnumConstant(13) DELETE_ALL_FROM_CONVERSATION,
    @WamEnumConstant(14) CLEAR_FROM_CONVERSATION,
    @WamEnumConstant(15) BLOCK_FROM_CONSENT_MODAL
}
