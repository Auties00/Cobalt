package it.auties.whatsapp.model.mobile;

import java.time.ZonedDateTime;

public record AccountInfo(ZonedDateTime lastRegistrationTimestamp, ZonedDateTime creationTimestamp) {
}
