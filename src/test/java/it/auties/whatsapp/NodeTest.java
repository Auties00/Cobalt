package it.auties.whatsapp;

import it.auties.whatsapp.binary.BinaryDecoder;
import it.auties.whatsapp.binary.BinaryEncoder;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.request.Node;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class NodeTest {
    @Test
    public void test(){
        var complexJid = ContactJid.ofCompanion("3495089819", 1, 2);
        var withJid = Node.withAttributes("iq",
                Map.of("jid", complexJid));
        var withString = Node.withAttributes("iq",
                Map.of("jid", complexJid.toString()));
        var encoder = new BinaryEncoder();
        var decoder = new BinaryDecoder();
        var decodedWithJid = decoder.decode(encoder.encode(withJid));
        var decodedWithString = decoder.decode(encoder.encode(withString));
        System.out.printf("%s - %s%n", decodedWithJid.attributes().getString("jid"), decodedWithString.attributes().getJid("jid").orElseThrow());
    }
}
