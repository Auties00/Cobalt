package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamChannel;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ChannelForwardContentType;
import com.github.auties00.cobalt.wire.wam.type.MediaType;
import com.github.auties00.cobalt.wire.wam.type.MessageChatType;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebPsChannelPostForwardWamEvent")
@WamEvent(id = 4746, channel = WamChannel.PRIVATE, privateStatsId = 0)
public interface PsChannelPostForwardEvent extends WamEventSpec {
    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<ChannelForwardContentType> channelForwardContentType();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<MessageChatType> channelForwardGroupType();

    @WamProperty(index = 2, type = WamType.STRING)
    Optional<String> cid();

    @WamProperty(index = 7, type = WamType.STRING)
    Optional<String> destinationChannelId();

    @WamProperty(index = 8, type = WamType.STRING)
    Optional<String> destinationPostId();

    @WamProperty(index = 9, type = WamType.BOOLEAN)
    Optional<Boolean> isSecondOrder();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<MediaType> mediaType();

    @WamProperty(index = 4, type = WamType.STRING)
    Optional<String> postId();
}
