package it.auties.whatsapp.model.sync;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
@ProtobufName("SyncdMutations")
public class MutationsSync
    implements ProtobufMessage {

  @ProtobufProperty(index = 1, type = MESSAGE, implementation = MutationSync.class, repeated = true)
  private List<MutationSync> mutations;

  public static class MutationsSyncBuilder {

    public MutationsSyncBuilder mutations(List<MutationSync> mutations) {
      if (this.mutations == null) {
        this.mutations = new ArrayList<>();
      }
      this.mutations.addAll(mutations);
      return this;
    }
  }
}