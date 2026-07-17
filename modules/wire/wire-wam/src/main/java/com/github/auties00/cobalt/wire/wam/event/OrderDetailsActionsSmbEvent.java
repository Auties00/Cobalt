package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.LastMessageDirection;
import com.github.auties00.cobalt.wire.wam.type.OrderDetailsCreationAction;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebOrderDetailsActionsSmbWamEvent")
@WamEvent(id = 3456)
public interface OrderDetailsActionsSmbEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> acceptedPayMethods();

    @WamProperty(index = 2, type = WamType.STRING)
    Optional<String> actionCategory();

    @WamProperty(index = 13, type = WamType.STRING)
    Optional<String> entryPointConversionSource();

    @WamProperty(index = 3, type = WamType.STRING)
    Optional<String> extraAttributes();

    @WamProperty(index = 4, type = WamType.BOOLEAN)
    Optional<Boolean> hasAddedPrice();

    @WamProperty(index = 5, type = WamType.BOOLEAN)
    Optional<Boolean> hasCatalog();

    @WamProperty(index = 6, type = WamType.BOOLEAN)
    Optional<Boolean> hasNote();

    @WamProperty(index = 14, type = WamType.ENUM)
    Optional<LastMessageDirection> lastMessageDirection();

    @WamProperty(index = 15, type = WamType.INTEGER)
    OptionalLong messageDepth();

    @WamProperty(index = 7, type = WamType.STRING)
    Optional<String> orderDetailEntryPoint();

    @WamProperty(index = 8, type = WamType.ENUM)
    Optional<OrderDetailsCreationAction> orderDetailsCreationAction();

    @WamProperty(index = 11, type = WamType.BOOLEAN)
    Optional<Boolean> orderEligibleToSend();

    @WamProperty(index = 9, type = WamType.BOOLEAN)
    Optional<Boolean> paymentStatus();

    @WamProperty(index = 10, type = WamType.STRING)
    Optional<String> paymentType();

    @WamProperty(index = 12, type = WamType.BOOLEAN)
    Optional<Boolean> sharingOrderStatusEvents();

    @WamProperty(index = 16, type = WamType.STRING)
    Optional<String> threadIdHmac();
}
