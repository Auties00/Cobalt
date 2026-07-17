package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ChannelUserType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebChannelMessageVisibilityTrackingWamEvent")
@WamEvent(id = 5998)
public interface ChannelMessageVisibilityTrackingEvent extends WamEventSpec {
    @WamProperty(index = 10, type = WamType.ENUM)
    Optional<ChannelUserType> channelUserType();

    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> cid();

    @WamProperty(index = 9, type = WamType.BOOLEAN)
    Optional<Boolean> containsMusic();

    @WamProperty(index = 12, type = WamType.BOOLEAN)
    Optional<Boolean> isOriginalAuthor();

    @WamProperty(index = 8, type = WamType.BOOLEAN)
    Optional<Boolean> isStarredPost();

    @WamProperty(index = 7, type = WamType.BOOLEAN)
    Optional<Boolean> isVpvImpression();

    @WamProperty(index = 4, type = WamType.STRING)
    Optional<String> postId();

    @WamProperty(index = 11, type = WamType.INTEGER)
    OptionalLong traceIdInt();

    @WamProperty(index = 6, type = WamType.STRING)
    Optional<String> unifiedSessionId();
}
