package it.auties.whatsapp.socket;

import it.auties.whatsapp.util.Clock;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Optional;

@AllArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(fluent = true)
public class StatusResponse extends Response {
    private String status;
    private ZonedDateTime timestamp;

    public StatusResponse(Node source) {
        super(source);
        this.status = new String((byte[]) source.content(), StandardCharsets.UTF_8);
        var timestamp = source.attributes().getLong("t");
        this.timestamp = Clock.parse(timestamp)
                .orElse(ZonedDateTime.now());
    }

    public Optional<String> status() {
        return Optional.ofNullable(status);
    }
}
