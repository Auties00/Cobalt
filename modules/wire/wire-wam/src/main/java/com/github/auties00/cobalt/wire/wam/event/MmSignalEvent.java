package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamChannel;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ConsentSource;
import com.github.auties00.cobalt.wire.wam.type.MmDirectionFrom;
import com.github.auties00.cobalt.wire.wam.type.MmSignalType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebMmSignalWamEvent")
@WamEvent(id = 5572, channel = WamChannel.PRIVATE, privateStatsId = 0)
public interface MmSignalEvent extends WamEventSpec {
    @WamProperty(index = 10, type = WamType.ENUM)
    Optional<ConsentSource> consentSource();

    @WamProperty(index = 4, type = WamType.BOOLEAN)
    Optional<Boolean> disclosed();

    @WamProperty(index = 5, type = WamType.BOOLEAN)
    Optional<Boolean> isLatestConversionToken();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong mmConversationDepth();

    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong mmConversationRepeat();

    @WamProperty(index = 8, type = WamType.INTEGER)
    OptionalLong mmConversionSchemaVersion();

    @WamProperty(index = 9, type = WamType.ENUM)
    Optional<MmDirectionFrom> mmDirectionFrom();

    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> mmSignalData();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<MmSignalType> mmSignalType();
}
