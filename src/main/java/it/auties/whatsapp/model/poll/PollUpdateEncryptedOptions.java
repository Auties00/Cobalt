package it.auties.whatsapp.model.poll;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.protobuf.base.ProtobufType;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

/**
 * A model class that represents the cypher data to decode the votes of a user inside
 * {@link it.auties.whatsapp.model.message.standard.PollUpdateMessage}
 */
@AllArgsConstructor(staticName = "of")
@Data
@Accessors(fluent = true)
@Jacksonized
@Builder
@ProtobufName("PollVoteMessage")
public class PollUpdateEncryptedOptions
    implements ProtobufMessage {

  @ProtobufProperty(implementation = byte[].class, index = 1, name = "selectedOptions", repeated = true, type = ProtobufType.BYTES)
  @Default
  private List<byte[]> selectedOptions = new ArrayList<>();

  public static class PollUpdateEncryptedOptionsBuilder {

    public PollUpdateEncryptedOptionsBuilder selectedOptions(List<byte[]> selectedOptions) {
      if (!selectedOptions$set) {
        this.selectedOptions$value = selectedOptions;
        this.selectedOptions$set = true;
        return this;
      }
      this.selectedOptions$value.addAll(selectedOptions);
      return this;
    }
  }
}
