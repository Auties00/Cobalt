package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.CustomerAdsSharingSettingEnabled;
import com.github.auties00.cobalt.wire.wam.type.OrderSignalType;
import com.github.auties00.cobalt.wire.wam.type.OrderStatus;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebCtwaOrderSignalWamEvent")
@WamEvent(id = 4264)
public interface CtwaOrderSignalEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong ctwaOrderSignalVersion();

    @WamProperty(index = 10, type = WamType.STRING)
    Optional<String> ctwaSignalMetadata();

    @WamProperty(index = 11, type = WamType.ENUM)
    Optional<CustomerAdsSharingSettingEnabled> customerAdsSharingSettingEnabled();

    @WamProperty(index = 2, type = WamType.STRING)
    Optional<String> deepLinkConversionData();

    @WamProperty(index = 3, type = WamType.STRING)
    Optional<String> deepLinkConversionSource();

    @WamProperty(index = 4, type = WamType.BOOLEAN)
    Optional<Boolean> eventSharingSettingEnabled();

    @WamProperty(index = 5, type = WamType.BOOLEAN)
    Optional<Boolean> globalSharingSettingEnabled();

    @WamProperty(index = 8, type = WamType.BOOLEAN)
    Optional<Boolean> orderPaid();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<OrderSignalType> orderSignalType();

    @WamProperty(index = 7, type = WamType.ENUM)
    Optional<OrderStatus> orderStatus();

    @WamProperty(index = 9, type = WamType.STRING)
    Optional<String> threadIdHmac();
}
