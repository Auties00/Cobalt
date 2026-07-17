package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.MutationBundleType;
import com.github.auties00.cobalt.wire.wam.type.MutationDirectionType;
import com.github.auties00.cobalt.wire.wam.type.SyncdCollectionType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebMdSyncdMutationsSummaryWamEvent")
@WamEvent(id = 6302)
public interface MdSyncdMutationsSummaryEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> appSessionId();

    @WamProperty(index = 2, type = WamType.STRING)
    Optional<String> companionSessionIds();

    @WamProperty(index = 3, type = WamType.BOOLEAN)
    Optional<Boolean> isInBootstrap();

    @WamProperty(index = 4, type = WamType.STRING)
    Optional<String> lidMutations();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<MutationBundleType> mutationBundle();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<MutationDirectionType> mutationDirection();

    @WamProperty(index = 7, type = WamType.STRING)
    Optional<String> patchMac();

    @WamProperty(index = 8, type = WamType.STRING)
    Optional<String> removeMutations();

    @WamProperty(index = 9, type = WamType.INTEGER)
    OptionalLong seqNumber();

    @WamProperty(index = 10, type = WamType.STRING)
    Optional<String> setMutations();

    @WamProperty(index = 11, type = WamType.STRING)
    Optional<String> snapshotMac();

    @WamProperty(index = 12, type = WamType.ENUM)
    Optional<SyncdCollectionType> syncdCollection();

    @WamProperty(index = 13, type = WamType.STRING)
    Optional<String> syncdKeyidKeyhash();
}
