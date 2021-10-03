import it.auties.whatsapp4j.beta.manager.MultiDeviceKeysManager;
import it.auties.whatsapp4j.beta.socket.MultiDeviceSocket;
import it.auties.whatsapp4j.beta.utils.SignedKeyPair;
import it.auties.whatsapp4j.common.api.WhatsappConfiguration;
import it.auties.whatsapp4j.common.utils.CypherUtils;
import lombok.SneakyThrows;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.edec.EdECObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HexFormat;
import java.util.concurrent.CountDownLatch;

public class Main {
    private static final HexFormat HEX = HexFormat.of();
    public static void main(String[] args) throws Exception {
        var latch = new CountDownLatch(1);
        var manager = new MultiDeviceKeysManager();
        new MultiDeviceSocket(WhatsappConfiguration.defaultOptions(), manager)
                .connect();
        latch.await();
    }

    @SneakyThrows
    private static PublicKey publicFromHex(String hex){
        var keyFactory = KeyFactory.getInstance("X25519");
        var subjectPublicKeyInfo = new SubjectPublicKeyInfo(new AlgorithmIdentifier(EdECObjectIdentifiers.id_X25519), HEX.parseHex(hex));
        var x509EncodedKeySpec = new X509EncodedKeySpec(subjectPublicKeyInfo.getEncoded());
        return keyFactory.generatePublic(x509EncodedKeySpec);
    }

    @SneakyThrows
    private static PrivateKey privateFromHex(String hex) {
        var keyFactory = KeyFactory.getInstance("Ed25519");
        var privateKeyInfo = new PrivateKeyInfo(new AlgorithmIdentifier(EdECObjectIdentifiers.id_Ed25519), new DEROctetString(HEX.parseHex(hex)));
        var privateKeySpec = new PKCS8EncodedKeySpec(privateKeyInfo.getEncoded());
        return keyFactory.generatePrivate(privateKeySpec);
    }
}
