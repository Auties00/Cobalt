package it.auties.whatsapp.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public record MexQueryRequest(@JsonProperty("queryId") String queryId, @JsonProperty("variables") Body body) {
    public MexQueryRequest(List<User> users, List<String> updates){
        this(
                String.valueOf(ThreadLocalRandom.current().nextLong(100_000, 1_000_000)),
                new Body(users, updates)
        );
    }

    public record Body(List<User> users, List<String> updates){

    }

    public record User(@JsonProperty("user_id") String userId){

    }
}
