package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.BizPlatform;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebMerchantCommerceEventWamEvent")
@WamEvent(id = 4688)
public interface MerchantCommerceEventEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> acceptedPaymentMethods();

    @WamProperty(index = 2, type = WamType.STRING)
    Optional<String> adId();

    @WamProperty(index = 3, type = WamType.STRING)
    Optional<String> appSessionId();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<BizPlatform> bizPlatform();

    @WamProperty(index = 6, type = WamType.STRING)
    Optional<String> commerceExperience();

    @WamProperty(index = 20, type = WamType.STRING)
    Optional<String> commerceFlowId();

    @WamProperty(index = 7, type = WamType.STRING)
    Optional<String> commerceInteractionAction();

    @WamProperty(index = 21, type = WamType.STRING)
    Optional<String> commerceInteractionActionType();

    @WamProperty(index = 22, type = WamType.STRING)
    Optional<String> commerceOrderStatus();

    @WamProperty(index = 8, type = WamType.STRING)
    Optional<String> commercePaymentStatus();

    @WamProperty(index = 9, type = WamType.STRING)
    Optional<String> commerceSessionId();

    @WamProperty(index = 10, type = WamType.STRING)
    Optional<String> commerceSurface();

    @WamProperty(index = 11, type = WamType.STRING)
    Optional<String> extraAttributes();

    @WamProperty(index = 12, type = WamType.BOOLEAN)
    Optional<Boolean> isCtwaOriginated();

    @WamProperty(index = 13, type = WamType.BOOLEAN)
    Optional<Boolean> isEligibleForAdSignal();

    @WamProperty(index = 14, type = WamType.BOOLEAN)
    Optional<Boolean> merchantHasCatalog();

    @WamProperty(index = 15, type = WamType.BOOLEAN)
    Optional<Boolean> merchantIsDiscoverable();

    @WamProperty(index = 17, type = WamType.STRING)
    Optional<String> p2mFlow();

    @WamProperty(index = 18, type = WamType.STRING)
    Optional<String> referral();

    @WamProperty(index = 19, type = WamType.INTEGER)
    OptionalLong sequenceId();
}
