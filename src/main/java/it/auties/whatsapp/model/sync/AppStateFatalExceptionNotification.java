package it.auties.whatsapp.model.sync;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.*;

import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import java.util.*;
import lombok.*;
import lombok.experimental.*;
import lombok.extern.jackson.*;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class AppStateFatalExceptionNotification implements ProtobufMessage {

  @ProtobufProperty(index = 1, type = STRING, repeated = true)
  private List<String> collectionNames;

  @ProtobufProperty(index = 2, type = INT64)
  private long timestamp;

  public static class AppStateFatalExceptionNotificationBuilder {
    public AppStateFatalExceptionNotificationBuilder collectionNames(List<String> collectionNames) {
      if (this.collectionNames == null) this.collectionNames = new ArrayList<>();
      this.collectionNames.addAll(collectionNames);
      return this;
    }
  }
}
