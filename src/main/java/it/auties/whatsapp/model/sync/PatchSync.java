package it.auties.whatsapp.model.sync;

import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.With;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.List;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.*;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@With
@Accessors(fluent = true)
public class PatchSync implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = MESSAGE, concreteType = VersionSync.class)
    private VersionSync version;

    @ProtobufProperty(index = 2, type = MESSAGE, concreteType = MutationSync.class, repeated = true)
    @Default
    private List<MutationSync> mutations = new ArrayList<>();

    @ProtobufProperty(index = 3, type = MESSAGE, concreteType = ExternalBlobReference.class)
    private ExternalBlobReference externalMutations;

    @ProtobufProperty(index = 4, type = BYTES)
    private byte[] snapshotMac;

    @ProtobufProperty(index = 5, type = BYTES)
    private byte[] patchMac;

    @ProtobufProperty(index = 6, type = MESSAGE, concreteType = KeyId.class)
    private KeyId keyId;

    @ProtobufProperty(index = 7, type = MESSAGE, concreteType = ExitCode.class)
    private ExitCode exitCode;

    @ProtobufProperty(index = 8, type = UINT32)
    private Integer deviceIndex;

    public long version() {
        return hasVersion() ?
                version.version() :
                0L;
    }

    public boolean hasVersion() {
        return version != null && version.version() != null;
    }

    public boolean hasExternalMutations() {
        return externalMutations != null;
    }

    public static class PatchSyncBuilder {
        public PatchSyncBuilder mutations(List<MutationSync> mutations) {
            if (!mutations$set) {
                this.mutations$value = mutations;
                this.mutations$set = true;
                return this;
            }

            this.mutations$value.addAll(mutations);
            return this;
        }
    }
}
