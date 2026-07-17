package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamChannel;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.BizPlatform;
import com.github.auties00.cobalt.wire.wam.type.EntryPointConversationInitiated;
import com.github.auties00.cobalt.wire.wam.type.FlowEntryPoint;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebExtensionScreenProgressWamEvent")
@WamEvent(id = 4112, channel = WamChannel.PRIVATE, privateStatsId = 0)
public interface ExtensionScreenProgressEvent extends WamEventSpec {
    @WamProperty(index = 22, type = WamType.STRING)
    Optional<String> adContext();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<BizPlatform> bizPlatform();

    @WamProperty(index = 2, type = WamType.STRING)
    Optional<String> businessOwnerJid();

    @WamProperty(index = 24, type = WamType.INTEGER)
    OptionalLong clickSequenceNumber();

    @WamProperty(index = 3, type = WamType.STRING)
    Optional<String> embeddedError();

    @WamProperty(index = 4, type = WamType.STRING)
    Optional<String> embeddedFlow();

    @WamProperty(index = 5, type = WamType.STRING)
    Optional<String> embeddedFlowType();

    @WamProperty(index = 19, type = WamType.ENUM)
    Optional<EntryPointConversationInitiated> entryPointConversationInitiated();

    @WamProperty(index = 20, type = WamType.STRING)
    Optional<String> entryPointConversionApp();

    @WamProperty(index = 21, type = WamType.STRING)
    Optional<String> entryPointConversionSource();

    @WamProperty(index = 6, type = WamType.STRING)
    Optional<String> extensionCategory();

    @WamProperty(index = 17, type = WamType.BOOLEAN)
    Optional<Boolean> extensionRestoredFromCache();

    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong extensionScreenLength();

    @WamProperty(index = 18, type = WamType.STRING)
    Optional<String> extensionStatus();

    @WamProperty(index = 8, type = WamType.STRING)
    Optional<String> extensionsFlowId();

    @WamProperty(index = 9, type = WamType.STRING)
    Optional<String> extensionsMessageId();

    @WamProperty(index = 10, type = WamType.STRING)
    Optional<String> extensionsSessionId();

    @WamProperty(index = 23, type = WamType.ENUM)
    Optional<FlowEntryPoint> flowEntryPoint();

    @WamProperty(index = 11, type = WamType.STRING)
    Optional<String> flowStatusExit();

    @WamProperty(index = 12, type = WamType.STRING)
    Optional<String> hsmCategory();

    @WamProperty(index = 16, type = WamType.STRING)
    Optional<String> hsmTag();

    @WamProperty(index = 25, type = WamType.BOOLEAN)
    Optional<Boolean> isSuccessScreen();

    @WamProperty(index = 13, type = WamType.BOOLEAN)
    Optional<Boolean> isTemplate();

    @WamProperty(index = 26, type = WamType.STRING)
    Optional<String> layoutType();

    @WamProperty(index = 14, type = WamType.STRING)
    Optional<String> screenProgress();

    @WamProperty(index = 15, type = WamType.INTEGER)
    OptionalLong sequenceNumber();

    @WamProperty(index = 27, type = WamType.INTEGER)
    OptionalLong shoppingCartItemsCount();
}
