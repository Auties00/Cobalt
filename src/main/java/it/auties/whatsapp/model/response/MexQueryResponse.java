package it.auties.whatsapp.model.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.whatsapp.model.contact.ContactJid;

import java.util.List;
import java.util.Map;

public record MexQueryResponse(ContactJid contactJid, Object data) implements ResponseWrapper {
    @JsonCreator
    @SuppressWarnings("unchecked")
    public static MexQueryResponse ofJson(Map<String, Object> json){
        try {
            var data = (Map<String, ?>) json.get("data");
            if(data == null){
                return new MexQueryResponse(null, null);
            }


            var updates = (List<?>) data.get("xwa2_users_updates_since");
            if(updates == null || updates.isEmpty()){
                return new MexQueryResponse(null, null);
            }

            var latestUpdate = (Map<String, ?>) updates.get(0);
            var jidName = (String) latestUpdate.get("jid");
            if(jidName == null){
                return new MexQueryResponse(null, null);
            }

            var jid = ContactJid.of(jidName);
            var updatesData = (List<?>) latestUpdate.get("updates");
            if(updatesData == null || updatesData.isEmpty()){
                return new MexQueryResponse(null, null);
            }

            var latestUpdateData = (Map<String, ?>) updatesData.get(0);
            return new MexQueryResponse(jid, latestUpdateData.get("text"));
        }catch (Throwable throwable){
            return new MexQueryResponse(null, null);
        }
    }
}
