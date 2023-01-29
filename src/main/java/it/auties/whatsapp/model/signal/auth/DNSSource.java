package it.auties.whatsapp.model.signal.auth;

import static it.auties.protobuf.base.ProtobufType.BOOL;
import static it.auties.protobuf.base.ProtobufType.MESSAGE;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class DNSSource implements ProtobufMessage {
  @ProtobufProperty(index = 15, type = MESSAGE, implementation = DNSSource.DNSSourceDNSResolutionMethod.class)
  private DNSSourceDNSResolutionMethod dnsMethod;

  @ProtobufProperty(index = 16, type = BOOL)
  private boolean appCached;

  @AllArgsConstructor
  @Accessors(fluent = true)
  @ProtobufName("DNSResolutionMethod")
  public enum DNSSourceDNSResolutionMethod {

    SYSTEM(0),
    GOOGLE(1),
    HARDCODED(2),
    OVERRIDE(3),
    FALLBACK(4);
    @Getter
    private final int index;

    @JsonCreator
    public static DNSSourceDNSResolutionMethod of(int index) {
      return Arrays.stream(values()).filter(entry -> entry.index() == index).findFirst()
          .orElse(null);
    }
  }
}