package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ApplicationState;
import com.github.auties00.cobalt.wire.wam.type.Collection;
import com.github.auties00.cobalt.wire.wam.type.MdBootstrapHistoryPayloadType;
import com.github.auties00.cobalt.wire.wam.type.MdBootstrapPayloadType;
import com.github.auties00.cobalt.wire.wam.type.MdBootstrapSource;
import com.github.auties00.cobalt.wire.wam.type.MdBootstrapStepResult;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebMdBootstrapDataAppliedWamEvent")
@WamEvent(id = 2298)
public interface MdBootstrapDataAppliedEvent extends WamEventSpec {
    @WamProperty(index = 22, type = WamType.STRING)
    Optional<String> appContext();

    @WamProperty(index = 23, type = WamType.INTEGER)
    OptionalLong appContextBitfield();

    @WamProperty(index = 21, type = WamType.ENUM)
    Optional<ApplicationState> applicationState();

    @WamProperty(index = 15, type = WamType.INTEGER)
    OptionalLong chunkChatsApplied();

    @WamProperty(index = 16, type = WamType.INTEGER)
    OptionalLong chunkMsgsApplied();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<Collection> collection();

    @WamProperty(index = 26, type = WamType.STRING)
    Optional<String> gkContext();

    @WamProperty(index = 14, type = WamType.INTEGER)
    OptionalLong historySyncChunkOrder();

    @WamProperty(index = 24, type = WamType.STRING)
    Optional<String> historySyncRetryRequestId();

    @WamProperty(index = 11, type = WamType.INTEGER)
    OptionalLong historySyncStageProgress();

    @WamProperty(index = 17, type = WamType.INTEGER)
    OptionalLong mdBootstrapChatsCount();

    @WamProperty(index = 8, type = WamType.INTEGER)
    OptionalLong mdBootstrapContactsCount();

    @WamProperty(index = 10, type = WamType.ENUM)
    Optional<MdBootstrapHistoryPayloadType> mdBootstrapHistoryPayloadType();

    @WamProperty(index = 25, type = WamType.INTEGER)
    OptionalLong mdBootstrapInlineContactsCount();

    @WamProperty(index = 18, type = WamType.INTEGER)
    OptionalLong mdBootstrapMessagesCount();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<MdBootstrapPayloadType> mdBootstrapPayloadType();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<MdBootstrapSource> mdBootstrapSource();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong mdBootstrapStepDuration();

    @WamProperty(index = 12, type = WamType.ENUM)
    Optional<MdBootstrapStepResult> mdBootstrapStepResult();

    @WamProperty(index = 19, type = WamType.STRING)
    Optional<String> mdDroppedMsgType();

    @WamProperty(index = 9, type = WamType.STRING)
    Optional<String> mdRegAttemptId();

    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> mdSessionId();

    @WamProperty(index = 20, type = WamType.STRING)
    Optional<String> mdSyncFailureReason();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong mdTimestamp();

    @WamProperty(index = 13, type = WamType.BOOLEAN)
    Optional<Boolean> sentViaMms();

    @WamProperty(index = 7, type = WamType.BOOLEAN)
    Optional<Boolean> usedSnapshot();
}
