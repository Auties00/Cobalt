package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ImagineAction;
import com.github.auties00.cobalt.wire.wam.type.ImagineActionSource;
import com.github.auties00.cobalt.wire.wam.type.ImagineActionSourceSubtype;
import com.github.auties00.cobalt.wire.wam.type.ImagineActionTarget;
import com.github.auties00.cobalt.wire.wam.type.ImagineActionThreadType;
import com.github.auties00.cobalt.wire.wam.type.ImagineMediaType;
import com.github.auties00.cobalt.wire.wam.type.ImplementationType;
import com.github.auties00.cobalt.wire.wam.type.TextModalityType;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebImagineActionsWamEvent")
@WamEvent(id = 5620)
public interface ImagineActionsEvent extends WamEventSpec {
    @WamProperty(index = 14, type = WamType.STRING)
    Optional<String> aiSessionId();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<ImagineAction> imagineAction();

    @WamProperty(index = 8, type = WamType.TIMER)
    Optional<Instant> imagineActionDuration();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<ImagineActionSource> imagineActionSource();

    @WamProperty(index = 13, type = WamType.ENUM)
    Optional<ImagineActionSourceSubtype> imagineActionSourceSubtype();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<ImagineActionTarget> imagineActionTarget();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<ImagineActionThreadType> imagineActionThreadType();

    @WamProperty(index = 12, type = WamType.ENUM)
    Optional<ImagineMediaType> imagineMediaType();

    @WamProperty(index = 11, type = WamType.ENUM)
    Optional<ImplementationType> implementationType();

    @WamProperty(index = 6, type = WamType.BOOLEAN)
    Optional<Boolean> isCancelled();

    @WamProperty(index = 7, type = WamType.BOOLEAN)
    Optional<Boolean> isSent();

    @WamProperty(index = 9, type = WamType.INTEGER)
    OptionalLong maxIndex();

    @WamProperty(index = 16, type = WamType.STRING)
    Optional<String> metaAiConversationThreadId();

    @WamProperty(index = 10, type = WamType.INTEGER)
    OptionalLong selectedImageIndex();

    @WamProperty(index = 17, type = WamType.ENUM)
    Optional<TextModalityType> textModality();

    @WamProperty(index = 15, type = WamType.STRING)
    Optional<String> threadSessionId();

    @WamProperty(index = 18, type = WamType.STRING)
    Optional<String> unifiedSessionId();
}
