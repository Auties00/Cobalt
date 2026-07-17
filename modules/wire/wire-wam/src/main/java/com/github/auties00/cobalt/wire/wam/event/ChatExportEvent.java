package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ExportModeType;
import com.github.auties00.cobalt.wire.wam.type.ExportResultType;
import com.github.auties00.cobalt.wire.wam.type.MessageChatType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebChatExportWamEvent")
@WamEvent(id = 7734)
public interface ChatExportEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<MessageChatType> chatType();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong exportDateRangeUsed();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong exportDurationMs();

    @WamProperty(index = 4, type = WamType.STRING)
    Optional<String> exportErrorReason();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong exportFileSizeBytes();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong exportMessageCount();

    @WamProperty(index = 7, type = WamType.ENUM)
    Optional<ExportModeType> exportMode();

    @WamProperty(index = 8, type = WamType.ENUM)
    Optional<ExportResultType> exportResult();

    @WamProperty(index = 9, type = WamType.INTEGER)
    OptionalLong mediaCount();
}
