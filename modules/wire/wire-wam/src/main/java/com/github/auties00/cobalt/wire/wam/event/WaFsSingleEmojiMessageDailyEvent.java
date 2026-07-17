package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebWaFsSingleEmojiMessageDailyWamEvent")
@WamEvent(id = 5602)
public interface WaFsSingleEmojiMessageDailyEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.BOOLEAN)
    Optional<Boolean> animatedEmojiEnabled();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong animatedEmojiReceiveCnt();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong animatedEmojiSendCnt();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong emojiClickCnt();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong emojiReplyCount();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong pauseAnimationCnt();

    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong replayAnimationCnt();

    @WamProperty(index = 8, type = WamType.INTEGER)
    OptionalLong singleEmojiReceiveCnt();

    @WamProperty(index = 9, type = WamType.INTEGER)
    OptionalLong singleEmojiSendCnt();
}
