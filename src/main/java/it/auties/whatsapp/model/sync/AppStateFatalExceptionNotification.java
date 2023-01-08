package it.auties.whatsapp.model.sync;

import static it.auties.protobuf.base.ProtobufType.INT64;
import static it.auties.protobuf.base.ProtobufType.STRING;

import it.auties.protobuf.base.ProtobufMessage;
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
public class AppStateFatalExceptionNotification
    implements ProtobufMessage {

  @ProtobufProperty(index = 1, type = STRING, repeated = true)
  private List<String> collectionNames;

  @ProtobufProperty(index = 2, type = INT64)
  private Long timestamp;

  public static class AppStateFatalExceptionNotificationBuilder {

    public AppStateFatalExceptionNotificationBuilder collectionNames(List<String> collectionNames) {
      if (this.collectionNames == null) {
        this.collectionNames = new ArrayList<>();
      }
      this.collectionNames.addAll(collectionNames);
      return this;
    }
  }
}
