package it.auties.whatsapp.protobuf.signal.session;

import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public record MessageKeys(SecretKeySpec cipherKey, SecretKeySpec macKey, IvParameterSpec iv, int counter) {

}
