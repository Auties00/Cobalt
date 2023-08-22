package it.auties.whatsapp.model.exchange;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public record MexQueryRequest(@JsonProperty("queryId") String queryId, @JsonProperty("variables") Entry body) {
    public MexQueryRequest(List<User> users, List<String> updates){
        this(
                String.valueOf(ThreadLocalRandom.current().nextLong(100_000, 1_000_000)),
                new Entry(users, updates)
        );
    }

    public record Entry(List<User> users, List<String> updates){

    }

    public record User(@JsonProperty("user_id") String userId){

    }
}
