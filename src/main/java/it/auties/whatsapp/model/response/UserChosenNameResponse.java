package it.auties.whatsapp.model.response;

import com.alibaba.fastjson2.JSON;

import java.util.Optional;

public final class UserChosenNameResponse {
    private final String name;

    private UserChosenNameResponse(String name) {
        this.name = name;
    }

    public static Optional<UserChosenNameResponse> ofJson(byte[] json) {
        if(json == null) {
            return Optional.empty();
        }

        var jsonObject = JSON.parseObject(json);
        if(jsonObject == null) {
            return Optional.empty();
        }

        var data = jsonObject.getJSONObject("data");
        if(data == null) {
            return Optional.empty();
        }

        var responses = data.getJSONArray("xwa2_users_updates_since");
        if(responses == null || responses.isEmpty()) {
            return Optional.empty();
        }

        var response = responses.getJSONObject(0);
        if(response == null) {
            return Optional.empty();
        }

        var updates = response.getJSONArray("updates");
        if(updates == null || updates.isEmpty()) {
            return Optional.empty();
        }

        var update = updates.getJSONObject(0);
        if(update == null) {
            return Optional.empty();
        }

        var text = update.getString("text");
        var result = new UserChosenNameResponse(text);
        return Optional.of(result);
    }

    public Optional<String> name() {
        return Optional.ofNullable(name);
    }
}