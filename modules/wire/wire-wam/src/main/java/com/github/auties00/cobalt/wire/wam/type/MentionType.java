package com.github.auties00.cobalt.wire.wam.type;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEnum;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

@WhatsAppWebModule(moduleName = "WAWebWamEnumMentionType")
@WamEnum
public enum MentionType {
    @WamEnumConstant(0) REGULAR_USER,
    @WamEnumConstant(1) GROUP,
    @WamEnumConstant(2) META_AI_BOT,
    @WamEnumConstant(3) EVERYONE,
    @WamEnumConstant(4) NON_GROUP_USER
}
