package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.KmpSyncdFlowEnum;
import com.github.auties00.cobalt.wire.wam.type.MutationBundleType;
import com.github.auties00.cobalt.wire.wam.type.MutationDirectionType;
import com.github.auties00.cobalt.wire.wam.type.SyncdCollectionType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebMdSyncdBundleWamEvent")
@WamEvent(id = 5966)
public interface MdSyncdBundleEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> appSessionId();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong bundleVersion();

    @WamProperty(index = 3, type = WamType.STRING)
    Optional<String> companionSessionIds();

    @WamProperty(index = 4, type = WamType.STRING)
    Optional<String> computedLthash();

    @WamProperty(index = 5, type = WamType.STRING)
    Optional<String> expectedMac();

    @WamProperty(index = 17, type = WamType.ENUM)
    Optional<KmpSyncdFlowEnum> kmpSyncdFlow();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<MutationBundleType> mutationBundle();

    @WamProperty(index = 7, type = WamType.ENUM)
    Optional<MutationDirectionType> mutationDirection();

    @WamProperty(index = 8, type = WamType.STRING)
    Optional<String> patchMac();

    @WamProperty(index = 9, type = WamType.INTEGER)
    OptionalLong patchSize();

    @WamProperty(index = 10, type = WamType.STRING)
    Optional<String> processingErrorMessage();

    @WamProperty(index = 11, type = WamType.INTEGER)
    OptionalLong seqNumber();

    @WamProperty(index = 12, type = WamType.STRING)
    Optional<String> snapshotMac();

    @WamProperty(index = 13, type = WamType.INTEGER)
    OptionalLong snapshotSize();

    @WamProperty(index = 14, type = WamType.ENUM)
    Optional<SyncdCollectionType> syncdCollection();

    @WamProperty(index = 15, type = WamType.STRING)
    Optional<String> syncdKeyhash();

    @WamProperty(index = 16, type = WamType.STRING)
    Optional<String> syncdKeyid();
}
