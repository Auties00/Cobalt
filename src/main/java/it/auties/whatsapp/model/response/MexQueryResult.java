package it.auties.whatsapp.model.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.whatsapp.model.contact.ContactJid;

import java.util.List;
import java.util.Map;

public record MexQueryResult(ContactJid contactJid, Object data) {
    @JsonCreator
    @SuppressWarnings("unchecked")
    public static MexQueryResult ofJson(Map<String, Object> json){
        try {
            var data = (Map<String, ?>) json.get("data");
            if(data == null){
                return new MexQueryResult(null, null);
            }


            var updates = (List<?>) data.get("xwa2_users_updates_since");
            if(updates == null || updates.isEmpty()){
                return new MexQueryResult(null, null);
            }

            var latestUpdate = (Map<String, ?>) updates.get(0);
            var jidName = (String) latestUpdate.get("jid");
            if(jidName == null){
                return new MexQueryResult(null, null);
            }

            var jid = ContactJid.of(jidName);
            var updatesData = (List<?>) latestUpdate.get("updates");
            if(updatesData == null || updatesData.isEmpty()){
                return new MexQueryResult(null, null);
            }

            var latestUpdateData = (Map<String, ?>) updatesData.get(0);
            return new MexQueryResult(jid, latestUpdateData.get("text"));
        }catch (Throwable throwable){
            return new MexQueryResult(null, null);
        }
    }
}
