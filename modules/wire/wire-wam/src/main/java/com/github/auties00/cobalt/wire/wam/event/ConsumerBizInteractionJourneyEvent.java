package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamChannel;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ConsumerBizActionTargetEnum;
import com.github.auties00.cobalt.wire.wam.type.ConsumerBizActionTypeEnum;
import com.github.auties00.cobalt.wire.wam.type.ConsumerBizEntryPointEnum;
import com.github.auties00.cobalt.wire.wam.type.ConsumerBizFeatureEnum;
import com.github.auties00.cobalt.wire.wam.type.ConsumerBizSurfaceEnum;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebConsumerBizInteractionJourneyWamEvent")
@WamEvent(id = 7760, channel = WamChannel.PRIVATE, privateStatsId = 113760892)
public interface ConsumerBizInteractionJourneyEvent extends WamEventSpec {
    @WamProperty(index = 9, type = WamType.STRING)
    Optional<String> businessJid();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<ConsumerBizActionTargetEnum> consumerBizActionTarget();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<ConsumerBizActionTypeEnum> consumerBizActionType();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<ConsumerBizEntryPointEnum> consumerBizEntryPoint();

    @WamProperty(index = 4, type = WamType.STRING)
    Optional<String> consumerBizExtraAttributes();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<ConsumerBizFeatureEnum> consumerBizFeature();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong consumerBizSeqId();

    @WamProperty(index = 7, type = WamType.STRING)
    Optional<String> consumerBizSessionId();

    @WamProperty(index = 8, type = WamType.ENUM)
    Optional<ConsumerBizSurfaceEnum> consumerBizSurface();

    @WamProperty(index = 10, type = WamType.STRING)
    Optional<String> sensitiveExtraAttributes();
}
