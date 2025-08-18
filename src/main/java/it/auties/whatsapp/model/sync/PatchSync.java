package it.auties.whatsapp.model.sync;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;

@ProtobufMessage(name = "SyncdPatch")
public final class PatchSync {
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    private VersionSync version;
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    private final List<MutationSync> mutations;
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    private final ExternalBlobReference externalMutations;
    @ProtobufProperty(index = 4, type = ProtobufType.BYTES)
    private final byte[] snapshotMac;
    @ProtobufProperty(index = 5, type = ProtobufType.BYTES)
    private final byte[] patchMac;
    @ProtobufProperty(index = 6, type = ProtobufType.MESSAGE)
    private final KeyId keyId;
    @ProtobufProperty(index = 7, type = ProtobufType.MESSAGE)
    private final ExitCode exitCode;
    @ProtobufProperty(index = 8, type = ProtobufType.UINT32)
    private final Integer deviceIndex;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public PatchSync(
            VersionSync version,
            List<MutationSync> mutations,
            ExternalBlobReference externalMutations,
            byte[] snapshotMac,
            byte[] patchMac,
            KeyId keyId,
            ExitCode exitCode,
            Integer deviceIndex
    ) {
        this.version = version;
        this.mutations = mutations;
        this.externalMutations = externalMutations;
        this.snapshotMac = snapshotMac;
        this.patchMac = patchMac;
        this.keyId = keyId;
        this.exitCode = exitCode;
        this.deviceIndex = deviceIndex;
    }

    public long encodedVersion() {
        return hasVersion() ? version.version() : 0L;
    }

    public boolean hasVersion() {
        return version != null && version.version() != null;
    }

    public boolean hasExternalMutations() {
        return externalMutations != null;
    }

    public VersionSync version() {
        return version;
    }

    public List<MutationSync> mutations() {
        return mutations;
    }

    public ExternalBlobReference externalMutations() {
        return externalMutations;
    }

    public byte[] snapshotMac() {
        return snapshotMac;
    }

    public byte[] patchMac() {
        return patchMac;
    }

    public KeyId keyId() {
        return keyId;
    }

    public ExitCode exitCode() {
        return exitCode;
    }

    public Integer deviceIndex() {
        return deviceIndex;
    }

    public void setVersion(VersionSync version) {
        this.version = version;
    }
}