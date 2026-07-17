package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebWamClientErrorsWamEvent")
@WamEvent(id = 1144)
public interface WamClientErrorsEvent extends WamEventSpec {
    @WamProperty(index = 27, type = WamType.BOOLEAN)
    Optional<Boolean> isFromWamsys();

    @WamProperty(index = 28, type = WamType.INTEGER)
    OptionalLong wamClientBufferDropErrorCount();

    @WamProperty(index = 29, type = WamType.INTEGER)
    OptionalLong wamClientBufferFetchErrorCount();

    @WamProperty(index = 43, type = WamType.INTEGER)
    OptionalLong wamClientBufferRotateErrorCount();

    @WamProperty(index = 30, type = WamType.INTEGER)
    OptionalLong wamClientBufferStoreErrorCount();

    @WamProperty(index = 42, type = WamType.INTEGER)
    OptionalLong wamClientCorruptedBuffersCount();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong wamClientDroppedEventCount();

    @WamProperty(index = 41, type = WamType.INTEGER)
    OptionalLong wamClientDroppedEventCountNoEnoughStorage();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong wamClientDroppedEventSize();

    @WamProperty(index = 1, type = WamType.BOOLEAN)
    Optional<Boolean> wamClientErrorFlags();

    @WamProperty(index = 37, type = WamType.INTEGER)
    OptionalLong wamClientMetadataReadErrorCount();

    @WamProperty(index = 38, type = WamType.INTEGER)
    OptionalLong wamClientMetadataWriteErrorCount();

    @WamProperty(index = 24, type = WamType.INTEGER)
    OptionalLong wamClientPrivateDroppedEventCount();

    @WamProperty(index = 25, type = WamType.INTEGER)
    OptionalLong wamClientPrivateDroppedEventSize();

    @WamProperty(index = 34, type = WamType.INTEGER)
    OptionalLong wamClientPrivateRealtimeDroppedEventCount();

    @WamProperty(index = 35, type = WamType.INTEGER)
    OptionalLong wamClientPrivateRealtimeDroppedEventSize();

    @WamProperty(index = 36, type = WamType.INTEGER)
    OptionalLong wamClientPrivateRealtimeRejectedEventCount();

    @WamProperty(index = 31, type = WamType.INTEGER)
    OptionalLong wamClientPrivateRejectedEventCount();

    @WamProperty(index = 22, type = WamType.INTEGER)
    OptionalLong wamClientRealtimeDroppedEventCount();

    @WamProperty(index = 23, type = WamType.INTEGER)
    OptionalLong wamClientRealtimeDroppedEventSize();

    @WamProperty(index = 32, type = WamType.INTEGER)
    OptionalLong wamClientRealtimeRejectedEventCount();

    @WamProperty(index = 33, type = WamType.INTEGER)
    OptionalLong wamClientRejectedEventCount();

    @WamProperty(index = 18, type = WamType.BOOLEAN)
    Optional<Boolean> wamErrorBadCurrentEventBufferChecksum();

    @WamProperty(index = 16, type = WamType.BOOLEAN)
    Optional<Boolean> wamErrorBadEventBuffer();

    @WamProperty(index = 15, type = WamType.BOOLEAN)
    Optional<Boolean> wamErrorBadFileHeader();

    @WamProperty(index = 8, type = WamType.BOOLEAN)
    Optional<Boolean> wamErrorBadFileSize();

    @WamProperty(index = 17, type = WamType.BOOLEAN)
    Optional<Boolean> wamErrorBadHeaderChecksum();

    @WamProperty(index = 19, type = WamType.BOOLEAN)
    Optional<Boolean> wamErrorBadRotatedEventBufferChecksum();

    @WamProperty(index = 11, type = WamType.BOOLEAN)
    Optional<Boolean> wamErrorCloseFile();

    @WamProperty(index = 14, type = WamType.BOOLEAN)
    Optional<Boolean> wamErrorCreateWamFile();

    @WamProperty(index = 9, type = WamType.BOOLEAN)
    Optional<Boolean> wamErrorFseekFile();

    @WamProperty(index = 10, type = WamType.BOOLEAN)
    Optional<Boolean> wamErrorOpenFile();

    @WamProperty(index = 26, type = WamType.BOOLEAN)
    Optional<Boolean> wamErrorOpenPsUploadQueueFile();

    @WamProperty(index = 13, type = WamType.BOOLEAN)
    Optional<Boolean> wamErrorOpenWamFile();

    @WamProperty(index = 20, type = WamType.BOOLEAN)
    Optional<Boolean> wamErrorPersistence();

    @WamProperty(index = 7, type = WamType.BOOLEAN)
    Optional<Boolean> wamErrorReadFile();

    @WamProperty(index = 12, type = WamType.BOOLEAN)
    Optional<Boolean> wamErrorRemoveFile();

    @WamProperty(index = 6, type = WamType.BOOLEAN)
    Optional<Boolean> wamErrorWriteEventBuffer();

    @WamProperty(index = 4, type = WamType.BOOLEAN)
    Optional<Boolean> wamErrorWriteFile();

    @WamProperty(index = 5, type = WamType.BOOLEAN)
    Optional<Boolean> wamErrorWriteHeader();

    @WamProperty(index = 39, type = WamType.BOOLEAN)
    Optional<Boolean> wamFirstErrorReadMetadata();

    @WamProperty(index = 40, type = WamType.BOOLEAN)
    Optional<Boolean> wamFirstErrorWriteMetadata();
}
