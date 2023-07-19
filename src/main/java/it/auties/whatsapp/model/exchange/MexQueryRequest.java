package it.auties.whatsapp.model.exchange;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record MexQueryRequest(@JsonProperty("queryId") String queryId, @JsonProperty("variables") Entry body) {
    public MexQueryRequest(List<User> users, List<String> updates){
        this("6420453474633624", new Entry(users, updates));
    }

    public record Entry(List<User> users, List<String> updates){

    }

    public record User(@JsonProperty("user_id") String userId){

    }
}
