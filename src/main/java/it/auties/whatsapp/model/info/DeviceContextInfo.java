package it.auties.whatsapp.model.info;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.sync.DeviceListMetadata;

import java.util.Optional;

@ProtobufMessage(name = "MessageContextInfo")
public final class DeviceContextInfo implements Info {
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    private final DeviceListMetadata deviceListMetadata;

    @ProtobufProperty(index = 2, type = ProtobufType.INT32)
    private final int deviceListMetadataVersion;
    @ProtobufProperty(index = 4, type = ProtobufType.BYTES)
    private final byte[] paddingBytes;
    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    private byte[] messageSecret;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public DeviceContextInfo(DeviceListMetadata deviceListMetadata, int deviceListMetadataVersion, byte[] messageSecret, byte[] paddingBytes) {
        this.deviceListMetadata = deviceListMetadata;
        this.deviceListMetadataVersion = deviceListMetadataVersion;
        this.messageSecret = messageSecret;
        this.paddingBytes = paddingBytes;
    }

    public Optional<DeviceListMetadata> deviceListMetadata() {
        return Optional.ofNullable(deviceListMetadata);
    }

    public int deviceListMetadataVersion() {
        return deviceListMetadataVersion;
    }

    public Optional<byte[]> messageSecret() {
        return Optional.ofNullable(messageSecret);
    }

    public void setMessageSecret(byte[] messageSecret) {
        this.messageSecret = messageSecret;
    }

    public Optional<byte[]> paddingBytes() {
        return Optional.ofNullable(paddingBytes);
    }
}