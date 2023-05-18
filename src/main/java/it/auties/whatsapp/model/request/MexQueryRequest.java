package it.auties.whatsapp.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// {"queryId":"6420453474633624","variables":{"users":[{"user_id":\"393396139846"}],"updates":["STATUS"]}}
public record MexQueryRequest(@JsonProperty("queryId") String queryId, @JsonProperty("variables") Entry body) {
    public MexQueryRequest(List<User> users, List<String> updates){
        this("6420453474633624", new Entry(users, updates));
    }

    public record Entry(List<User> users, List<String> updates){

    }

    public record User(@JsonProperty("user_id") String userId){

    }
}
