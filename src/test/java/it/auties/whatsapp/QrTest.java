package it.auties.whatsapp;

import it.auties.whatsapp.util.BytesHelper;
import org.junit.jupiter.api.Test;

public class QrTest {
    @Test
    public void print(){
        System.out.println(BytesHelper.bytesToInt(new byte[]{0, 0, 0, 0, 77, 94}, 6));
    }
}
