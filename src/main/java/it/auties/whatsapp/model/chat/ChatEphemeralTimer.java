package it.auties.whatsapp.model.chat;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.base.ProtobufMessage;
import java.time.Duration;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@AllArgsConstructor
@Accessors(fluent = true)
public enum ChatEphemeralTimer
    implements ProtobufMessage {
  OFF(Duration.ofDays(0)),
  ONE_DAY(Duration.ofDays(1)),
  ONE_WEEK(Duration.ofDays(7)),
  THREE_MONTHS(Duration.ofDays(90));

  @Getter
  private final Duration period;

  @JsonCreator
  public static ChatEphemeralTimer of(long value) {
    return Arrays.stream(values())
        .filter(entry -> entry.period()
            .toSeconds() == value || entry.period()
            .toDays() == value)
        .findFirst()
        .orElse(OFF);
  }

  @Override
  public Object toValue() {
    return period.toSeconds();
  }

  @Override
  public boolean isValueBased() {
    return true;
  }
}
