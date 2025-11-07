package com.github.auties00.cobalt.socket.state;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.model.jid.JidServer;
import com.github.auties00.cobalt.model.privacy.PrivacySettingEntryBuilder;
import com.github.auties00.cobalt.model.privacy.PrivacySettingType;
import com.github.auties00.cobalt.model.privacy.PrivacySettingValue;
import com.github.auties00.cobalt.node.Node;
import com.github.auties00.cobalt.node.NodeBuilder;
import com.github.auties00.cobalt.socket.SocketStream;

import java.util.List;

public final class WebQueryPrivacySettingsStreamNodeHandler extends SocketStream.Handler {
    public WebQueryPrivacySettingsStreamNodeHandler(WhatsAppClient whatsapp) {
        super(whatsapp, "success");
    }

    @Override
    public void handle(Node node) {
        var queryRequestBody = new NodeBuilder()
                .description("privacy")
                .build();
        var queryRequest = new NodeBuilder()
                .description("iq")
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .attribute("xmlns", "privacy")
                .content(queryRequestBody);
        var result = whatsapp.sendNode(queryRequest);
        result.streamChildren("privacy")
                .flatMap(Node::streamChildren)
                .forEach(this::addPrivacySetting);
    }

    private void addPrivacySetting(Node entry) {
        var privacyType = entry.getAttributeAsString("name")
                .flatMap(PrivacySettingType::of);
        if(privacyType.isEmpty()) {
            return;
        }

        var privacyValue = entry.getAttributeAsString("value")
                .flatMap(PrivacySettingValue::of);
        if(privacyValue.isEmpty()) {
            return;
        }

        var privacySetting = whatsapp.store()
                .findPrivacySetting(privacyType.get());
        var excluded = queryPrivacyExcludedContacts(privacyType.get(), privacyValue.get());
        var newEntry = new PrivacySettingEntryBuilder()
                .type(privacyType.get())
                .value(privacyValue.get())
                .excluded(excluded)
                .build();
        whatsapp.store().addPrivacySetting(newEntry);
    }

    private List<Jid> queryPrivacyExcludedContacts(PrivacySettingType type, PrivacySettingValue value) {
        if (value != PrivacySettingValue.CONTACTS_EXCEPT) {
            return List.of();
        }

        var queryRequestBodyContent = new NodeBuilder()
                .description("list")
                .attribute("name", type.data())
                .attribute("value", value.data())
                .build();
        var queryRequestBody = new NodeBuilder()
                .description("privacy")
                .content(queryRequestBodyContent)
                .build();
        var queryRequest = new NodeBuilder()
                .description("iq")
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .attribute("xmlns", "privacy")
                .content(queryRequestBody);
        return whatsapp.sendNode(queryRequest)
                .streamChild("privacy")
                .flatMap(node -> node.streamChild("list"))
                .flatMap(node -> node.streamChildren("user"))
                .flatMap(user -> user.streamAttributeAsJid("jid"))
                .toList();
    }
}
