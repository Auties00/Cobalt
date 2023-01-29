package it.auties.whatsapp.model.response;

import it.auties.whatsapp.model.request.Node;
import it.auties.whatsapp.util.Clock;
import java.time.ZonedDateTime;
import java.util.NoSuchElementException;
import lombok.NonNull;

public record ContactStatusResponse(@NonNull String status, @NonNull ZonedDateTime timestamp)
    implements ResponseWrapper {

  public ContactStatusResponse(@NonNull Node source) {
    this(source.contentAsString()
            .orElseThrow(() -> new NoSuchElementException("Missing status")),
        Clock.parseSeconds(source.attributes()
                .getLong(
                    "t"))
            .orElse(ZonedDateTime.now()));
  }
}
