package it.auties.whatsapp.serialization;

import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.model.chat.Chat;

public class DefaultControllerSerializerProvider extends DefaultControllerProviderBase
        implements ControllerSerializerProvider {

    private boolean updateHash(Chat entry) {
        var lastHashCode = hashCodesMap.get(entry.jid());
        var newHashCode = entry.fullHashCode();
        if (lastHashCode == null) {
            hashCodesMap.put(entry.jid(), newHashCode);
            return true;
        }
        if (newHashCode == lastHashCode) {
            return false;
        }
        hashCodesMap.put(entry.jid(), newHashCode);
        return true;
    }

    private void serializeChat(Store store, Chat chat, boolean async) {
        var preferences = SmileFile.of("%s/%s%s.smile", store.id(), CHAT_PREFIX, chat.jid());
        preferences.write(chat, async);
    }

    @Override
    public void serializeKeys(Keys keys, boolean async) {
        var preferences = SmileFile.of("%s/keys.smile", keys.id());
        preferences.write(keys, async);
    }

    @Override
    public void serializeStore(Store store, boolean async) {
        var preferences = SmileFile.of("%s/store.smile", store.id());
        preferences.write(store, async);
        store.chats().stream().filter(this::updateHash).forEach(chat -> serializeChat(store, chat, async));
    }


}
