package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.GroupTypeClient;
import com.github.auties00.cobalt.wire.wam.type.MentionType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebMentionPickerActionWamEvent")
@WamEvent(id = 7082)
public interface MentionPickerActionEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong groupSize();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<GroupTypeClient> groupTypeClient();

    @WamProperty(index = 3, type = WamType.BOOLEAN)
    Optional<Boolean> isAGroup();

    @WamProperty(index = 6, type = WamType.STRING)
    Optional<String> mentionGroupId();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<MentionType> mentionType();

    @WamProperty(index = 5, type = WamType.STRING)
    Optional<String> threadId();
}
