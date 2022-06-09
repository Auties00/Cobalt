package it.auties.whatsapp;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.*;

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
public class UserReceipt {

  @ProtobufProperty(index = 1, type = STRING)
  @NonNull
  private String userJid;

  @ProtobufProperty(index = 2, type = INT64)
  private long receiptTimestamp;

  @ProtobufProperty(index = 3, type = INT64)
  private long readTimestamp;

  @ProtobufProperty(index = 4, type = INT64)
  private long playedTimestamp;

  @ProtobufProperty(index = 5, type = STRING, repeated = true)
  private List<String> pendingDeviceJid;

  @ProtobufProperty(index = 6, type = STRING, repeated = true)
  private List<String> deliveredDeviceJid;

  public static class UserReceiptBuilder {

    public UserReceiptBuilder pendingDeviceJid(List<String> pendingDeviceJid) {
      if (this.pendingDeviceJid == null) this.pendingDeviceJid = new ArrayList<>();
      this.pendingDeviceJid.addAll(pendingDeviceJid);
      return this;
    }

    public UserReceiptBuilder deliveredDeviceJid(List<String> deliveredDeviceJid) {
      if (this.deliveredDeviceJid == null) this.deliveredDeviceJid = new ArrayList<>();
      this.deliveredDeviceJid.addAll(deliveredDeviceJid);
      return this;
    }
  }
}
