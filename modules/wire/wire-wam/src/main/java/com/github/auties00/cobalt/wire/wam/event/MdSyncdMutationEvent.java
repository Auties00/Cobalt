package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.MutationBundleType;
import com.github.auties00.cobalt.wire.wam.type.MutationDirectionType;
import com.github.auties00.cobalt.wire.wam.type.MutationOperationType;
import com.github.auties00.cobalt.wire.wam.type.SyncdCollectionType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebMdSyncdMutationWamEvent")
@WamEvent(id = 5970)
public interface MdSyncdMutationEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> appSessionId();

    @WamProperty(index = 2, type = WamType.STRING)
    Optional<String> companionSessionIds();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong contentLength();

    @WamProperty(index = 4, type = WamType.BOOLEAN)
    Optional<Boolean> isInBootstrap();

    @WamProperty(index = 5, type = WamType.BOOLEAN)
    Optional<Boolean> isUsingLid();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<MutationBundleType> mutationBundle();

    @WamProperty(index = 7, type = WamType.ENUM)
    Optional<MutationDirectionType> mutationDirection();

    @WamProperty(index = 8, type = WamType.STRING)
    Optional<String> mutationMac();

    @WamProperty(index = 9, type = WamType.STRING)
    Optional<String> mutationName();

    @WamProperty(index = 10, type = WamType.ENUM)
    Optional<MutationOperationType> mutationOperation();

    @WamProperty(index = 15, type = WamType.STRING)
    Optional<String> patchMac();

    @WamProperty(index = 11, type = WamType.INTEGER)
    OptionalLong seqNumber();

    @WamProperty(index = 12, type = WamType.ENUM)
    Optional<SyncdCollectionType> syncdCollection();

    @WamProperty(index = 13, type = WamType.STRING)
    Optional<String> syncdKeyhash();

    @WamProperty(index = 14, type = WamType.STRING)
    Optional<String> syncdKeyid();
}
