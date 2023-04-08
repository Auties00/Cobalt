package it.auties.whatsapp;

import it.auties.protobuf.serialization.performance.Protobuf;
import it.auties.whatsapp.model.signal.auth.KeyIndexList;

public class MiscTest {
    public static void main(String[] args) {
        var msg = "%0a%15%08%cb%8e%95%f1%07%10%f3%dd%bb%a1%06%18%04\"%05%00%01%02%03%04%12@%f2g%00%02%84%bc%94%06%0c%a6%e2%12O%a1={~7>%aa|%b8%e9q3m{%a7%88!%ba%d6WKc%b2%8a%d4Kc%11%c6%c0%c4#%00I%09%e2%83%8fT%86%c3g%8dv%bd%1cA%d5K`%0f<".getBytes();
        System.out.println(Protobuf.readMessage(msg, KeyIndexList.class));
    }
}
