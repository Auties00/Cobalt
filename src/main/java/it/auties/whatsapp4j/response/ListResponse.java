package it.auties.whatsapp4j.response;

import java.util.List;

public record ListResponse(List<Object> data) implements Response {
}
