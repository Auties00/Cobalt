package it.auties.whatsapp.model.payment;

import static it.auties.protobuf.base.ProtobufType.INT64;
import static it.auties.protobuf.base.ProtobufType.STRING;
import static it.auties.protobuf.base.ProtobufType.UINT32;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
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
public class PaymentMoney
    implements ProtobufMessage {

  @ProtobufProperty(index = 1, type = INT64)
  private long money;

  @ProtobufProperty(index = 2, type = UINT32)
  private int offset;

  @ProtobufProperty(index = 3, type = STRING)
  private String currencyCode;
}
