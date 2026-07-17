package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.MediaType;
import com.github.auties00.cobalt.wire.wam.type.MessageType;
import com.github.auties00.cobalt.wire.wam.type.ReceiptAggregationType;
import com.github.auties00.cobalt.wire.wam.type.ReceiptStanzaStage;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebReceiptStanzaReceiveWamEvent")
@WamEvent(id = 2496, betaWeight = 1000, releaseWeight = 2000)
public interface ReceiptStanzaReceiveEvent extends WamEventSpec {
    @WamProperty(index = 12, type = WamType.INTEGER)
    OptionalLong dbReadsCount();

    @WamProperty(index = 13, type = WamType.INTEGER)
    OptionalLong dbWritesCount();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<MediaType> mediaType();

    @WamProperty(index = 10, type = WamType.ENUM)
    Optional<MessageType> messageType();

    @WamProperty(index = 14, type = WamType.BOOLEAN)
    Optional<Boolean> processingDeferred();

    @WamProperty(index = 11, type = WamType.ENUM)
    Optional<ReceiptAggregationType> receiptAggregation();

    @WamProperty(index = 1, type = WamType.TIMER)
    Optional<Instant> receiptStanzaDuration();

    @WamProperty(index = 6, type = WamType.BOOLEAN)
    Optional<Boolean> receiptStanzaHasOrphaned();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong receiptStanzaOfflineCount();

    @WamProperty(index = 8, type = WamType.INTEGER)
    OptionalLong receiptStanzaProcessedCount();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong receiptStanzaRetryVer();

    @WamProperty(index = 9, type = WamType.ENUM)
    Optional<ReceiptStanzaStage> receiptStanzaStage();

    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong receiptStanzaTotalCount();

    @WamProperty(index = 4, type = WamType.STRING)
    Optional<String> receiptStanzaType();

    @WamProperty(index = 15, type = WamType.INTEGER)
    OptionalLong stanzaBatchSize();

    @WamProperty(index = 16, type = WamType.INTEGER)
    OptionalLong stanzaProcessCount();
}
