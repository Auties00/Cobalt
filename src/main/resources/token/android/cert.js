console.log("[*] Loading cert script...")

const KeyGenParameterSpec = Java.use('android.security.keystore.KeyGenParameterSpec');
const KeyProperties = Java.use('android.security.keystore.KeyProperties');
const KeyPairGenerator = Java.use('java.security.KeyPairGenerator');
const KeyStore = Java.use('java.security.KeyStore');
const Signature = Java.use('java.security.Signature');
const Date = Java.use('java.util.Date');
const Base64 = Java.use('java.util.Base64');
const ByteArrayOutputStream = Java.use('java.io.ByteArrayOutputStream');
const ByteBuffer = Java.use('java.nio.ByteBuffer');
const ByteOrder = Java.use('java.nio.ByteOrder');
const StandardCharsets = Java.use('java.nio.charset.StandardCharsets');
const Objects = Java.use('java.util.Objects');
const AtomicInteger = Java.use('java.util.concurrent.atomic.AtomicInteger');
const AppSignature = "3987d043d10aefaf5a8710b3671418fe57e0e19b653c9df82558feb5ffce5d44";
const Counter = AtomicInteger.$new(0);

function bytesToHex(data) {
    return ByteBuffer.$wrap(data).asLongBuffer().get().toHexString();
}

function hexStringToByteArray(s) {
    var byteArray = [];
    for (var i = 0; i < s.length; i += 2) {
        byteArray.push(parseInt(s.substring(i, i + 2), 16));
    }
    return Java.array('byte', byteArray);
}

console.log("[*] Loaded cert script")
recv(function (authKey, enc) {
    Java.perform(function() {
    var alias = "ws_cert_" + Counter.incrementAndGet();
        var ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        ks.deleteEntry(alias);
        var expireTime = Date.$new();
        expireTime.setTime(System.currentTimeMillis() + 80 * 365 * 24 * 60 * 60 * 1000);
        var attestationChallenge = ByteBuffer.allocate(authKey.length + 9);
        attestationChallenge.order(ByteOrder.BIG_ENDIAN);
        attestationChallenge.putLong(System.currentTimeMillis() / 1000 - 15);
        attestationChallenge.put(0x1F);
        attestationChallenge.put(authKey);
        var keyPairGenerator = KeyPairGenerator.getInstance("EC", "AndroidKeyStore");
        keyPairGenerator.initialize(
            KeyGenParameterSpec.Builder.$new(
                alias, KeyProperties.PURPOSE_SIGN
            )
            .setDigests([KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512])
            .setUserAuthenticationRequired(false)
            .setCertificateNotAfter(expireTime)
            .setAttestationChallenge(attestationChallenge.array())
            .build()
        );
        keyPairGenerator.generateKeyPair();
        var certs = Objects.requireNonNull(ks.getCertificateChain(alias), "Missing certificates");
        var ba = ByteArrayOutputStream.$new();
        for (var i = certs.length - 1; i >= 1; i--) {
            ba.write(certs[i].getEncoded());
        }
        var c0Hex = bytesToHex(certs[0].getEncoded());
        var pubHex = bytesToHex(authKey);
        var timeBytes = ByteBuffer.allocate(8).putLong(System.currentTimeMillis()).array();
        var time = bytesToHex(timeBytes).substring(4);
        var pubIndex = c0Hex.indexOf(pubHex);
        var timeIndex = pubIndex + 64 + 20;
        var signIndex = timeIndex + time.length + 80;
        var tailIndex = signIndex + AppSignature.length;
        var newC0Hex = c0Hex.substring(0, timeIndex) +
            time +
            c0Hex.substring(timeIndex + time.length, signIndex) +
            AppSignature +
            c0Hex.substring(tailIndex);
        ba.write(hexStringToByteArray(newC0Hex));
        var s = Signature.getInstance("SHA256withECDSA");
        var entry = ks.getEntry(alias, null);
        s.initSign(entry.getPrivateKey());
        s.update(enc.getBytes(StandardCharsets.UTF_8));
        ks.deleteEntry(alias);
        var encSign = Base64.getEncoder().encodeToString(s.sign());
        var encCert = Base64.getEncoder().encodeToString(ba.toByteArray());
        send([encCert, encSign]);
    });
});