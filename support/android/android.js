const Modifier = Java.use("java.lang.reflect.Modifier")
const projectId = 293955441834
const appSignature = "3987d043d10aefaf5a8710b3671418fe57e0e19b653c9df82558feb5ffce5d44"

function findIntegrityManagerProvider() {
    const loadedClasses = Java.enumerateLoadedClassesSync()
    for (const className of loadedClasses) {
        if (className.startsWith("com.google.android.play.core.integrity")) {
            const targetClass = Java.use(className)
            const methods = targetClass.class.getDeclaredMethods()
            for (const method of methods) {
                if (Modifier.isStatic(method.getModifiers())) {
                    const parameterTypes = method.getParameterTypes()
                    if (parameterTypes.length === 1 && parameterTypes[0].getName() === "android.content.Context") {
                        return targetClass[method.getName()]
                    }
                }
            }
        }
    }

    throw new Error('Cannot find IntegrityManagerFactoryCreate method')
}

function getIntegrityManager(provider) {
    const ActivityThread = Java.use('android.app.ActivityThread')
    const context = ActivityThread.currentApplication().getApplicationContext()
    console.log("[*] Loaded activity context")
    const integrityFactory = provider.overload("android.content.Context").call(context)
    const methods = integrityFactory.class.getDeclaredMethods()
    if (methods.length !== 1) {
        throw new Error('Too many methods in integrity factory: ' + methods.length)
    }

    const method = methods[0]
    console.log("[*] Found integrity factory method")
    return integrityFactory[method.getName()].call(integrityFactory)
}

function findPrepareIntegrityRequestMeta(integrityManager) {
    const integrityManagerMethods = integrityManager.class.getDeclaredMethods()
    if (integrityManagerMethods.length !== 1) {
        throw new Error('Too many methods in integrity manager: ' + integrityManagerMethods.length)
    }

    const integrityManagerMethod = integrityManagerMethods[0]
    console.log("[*] Found prepare integrity request method")
    const integrityManagerMethodParamTypes = integrityManagerMethod.getParameterTypes()
    if (integrityManagerMethodParamTypes.length !== 1) {
        throw new Error('Unexpected number of parameters: ' + integrityManagerMethodParamTypes.length)
    }

    const requestType = integrityManagerMethodParamTypes[0]
    const requestTypeClass = Java.use(requestType.getName())
    console.log("[*] Found prepare integrity request type")
    const requestTypeMethods = requestType.getDeclaredMethods()
    for (const requestTypeMethod of requestTypeMethods) {
        if (Modifier.isStatic(requestTypeMethod.getModifiers()) && requestTypeMethod.getParameterTypes().length === 0) {
            return [requestTypeClass, requestTypeClass[requestTypeMethod.getName()]]
        }
    }

    throw new Error('Cannot find prepare request builder method')
}

function createIntegrityTokenProvider(integrityManager, prepareIntegrityRequestType, prepareIntegrityRequestBuilder, callback) {
    const integrityTokenPrepareRequestBuilder = prepareIntegrityRequestBuilder.overload().call(prepareIntegrityRequestType)
    integrityTokenPrepareRequestBuilder.setCloudProjectNumber(projectId)
    const integrityTokenPrepareRequest = integrityTokenPrepareRequestBuilder.build()
    const integrityTokenPrepareResponse = integrityManager.prepareIntegrityToken(integrityTokenPrepareRequest)
    const onTokenProviderCreatedListenerType = Java.registerClass({
        name: 'IntegrityTokenProviderHandler',
        implements: [Java.use("com.google.android.gms.tasks.OnSuccessListener")],
        methods: {
            onSuccess: function (integrityTokenProvider) {
                callback(Java.cast(integrityTokenProvider, Java.use(integrityTokenProvider.$className)))
            }
        }
    })
    const onTokenProviderCreatedListener = onTokenProviderCreatedListenerType.$new()
    integrityTokenPrepareResponse["addOnSuccessListener"].overload('com.google.android.gms.tasks.OnSuccessListener').call(integrityTokenPrepareResponse, onTokenProviderCreatedListener)
}

function findIntegrityRequestMeta(integrityTokenProvider) {
    const integrityManagerMethods = integrityTokenProvider.class.getDeclaredMethods()
    if (integrityManagerMethods.length !== 1) {
        throw new Error('Too many methods in integrity manager: ' + integrityManagerMethods.length)
    }

    const integrityManagerMethod = integrityManagerMethods[0]
    console.log("[*] Found integrity request method")
    const integrityManagerMethodParamTypes = integrityManagerMethod.getParameterTypes()
    if (integrityManagerMethodParamTypes.length !== 1) {
        throw new Error('Unexpected number of parameters: ' + integrityManagerMethodParamTypes.length)
    }

    const requestType = integrityManagerMethodParamTypes[0]
    const requestTypeClass = Java.use(requestType.getName())
    console.log("[*] Found integrity request type")
    const requestTypeMethods = requestType.getDeclaredMethods()
    for (const requestTypeMethod of requestTypeMethods) {
        if (Modifier.isStatic(requestTypeMethod.getModifiers()) && requestTypeMethod.getParameterTypes().length === 0) {
            return [requestTypeClass, requestTypeClass[requestTypeMethod.getName()]]
        }
    }

    throw new Error('Cannot find request builder method')
}

// authKey is the curve25519 public key encoded as base64 with flags DEFAULT | NO_PADDING | NO_WRAP
var integrityCounter = 0
function calculateIntegrityToken(integrityTokenProvider, integrityRequestType, integrityRequestBuilderMethod, authKey, callback) {
    integrityCounter++
    let integrityRequestBuilder = integrityRequestBuilderMethod.overload().call(integrityRequestType)
    integrityRequestBuilder.setRequestHash(authKey)
    let integrityRequest = integrityRequestBuilder.build()
    let javaIntegrityTokenProvider = Java.cast(integrityTokenProvider, Java.use(integrityTokenProvider.$className))
    let integrityTokenResponse = javaIntegrityTokenProvider.request(integrityRequest)
    let onIntegrityTokenListenerType = Java.registerClass({
        name: 'TokenHandler' + counter,
        implements: [Java.use("com.google.android.gms.tasks.OnSuccessListener")],
        methods: {
            onSuccess: function (result) {
                var javaResult = Java.cast(result, Java.use(result.$className))
                callback(javaResult.token())
            }
        }
    })
    let onIntegrityTokenListener = onIntegrityTokenListenerType.$new()
    integrityTokenResponse["addOnSuccessListener"].overload('com.google.android.gms.tasks.OnSuccessListener').call(integrityTokenResponse, onIntegrityTokenListener)
}

function createGpiaListener() {
     const integrityManagerProvider = findIntegrityManagerProvider()
     console.log("[*] Found integrity provider")
     const integrityManager = getIntegrityManager(integrityManagerProvider)
     console.log("[*] Found integrity manager")
     const [prepareIntegrityRequestType, prepareIntegrityRequestBuilder] = findPrepareIntegrityRequestMeta(integrityManager)
     console.log("[*] Found prepare integrity metadata")
     createIntegrityTokenProvider(
        integrityManager,
        prepareIntegrityRequestType,
        prepareIntegrityRequestBuilder,
        (integrityTokenProvider) => {
            const [integrityRequestType, integrityRequestBuilder] = findIntegrityRequestMeta(integrityTokenProvider)
            console.log("[*] Found integrity metadata")
            recv("gpia", function (authKey) {
                calculateIntegrityToken(
                    integrityTokenProvider,
                    integrityRequestType,
                    integrityRequestBuilder,
                    authKey,
                    (token) => {
                        send({
                            "authKey": authKey,
                            "token": token
                        })
                    }
                )
            })
            console.log("[*] Listening for gpia requests")
        }
     )
}

function hexStringToByteArray(s) {
    const result = []
    for (let i = 0; i < s.length; i += 2) {
        result.push(parseInt(s.substring(i, i + 2), 16))
    }
    return Java.array('byte', result)
}

function toHexString(byteArray) {
    let result = ''
    for (let i = 0; i < byteArray.length; i++) {
        result += ('0' + (byteArray[i] & 0xFF).toString(16)).slice(-2)
    }
    return result
}

var certificateCounter = 0
function createCertificateListener() {
    const KeyGenParameterSpecBuilder = Java.use('android.security.keystore.KeyGenParameterSpec$Builder')
    const KeyProperties = Java.use('android.security.keystore.KeyProperties')
    const KeyPairGenerator = Java.use('java.security.KeyPairGenerator')
    const KeyStore = Java.use('java.security.KeyStore')
    const KeyStorePrivateKeyEntry = Java.use("java.security.KeyStore$PrivateKeyEntry")
    const Signature = Java.use('java.security.Signature')
    const Base64 = Java.use('java.util.Base64')
    const Date = Java.use('java.util.Date')
    const AtomicInteger = Java.use('java.util.concurrent.atomic.AtomicInteger')
    const ByteBuffer = Java.use('java.nio.ByteBuffer')
    const ByteOrder = Java.use("java.nio.ByteOrder")
    const StandardCharsets = Java.use('java.nio.charset.StandardCharsets')
    const ByteArrayOutputStream = Java.use('java.io.ByteArrayOutputStream')
    const System = Java.use('java.lang.System')

    recv("cert", function (data) {
        let decodedData = Base64.getUrlDecoder().decode(data)
        let authKey = Arrays.copyOf(decodedData, 32)
        let enc = Arrays.copyOfRange(decodedData, 32, decodedData.length)

        certificateCounter++
        let alias = "ws_cert_" + certificateCounter
        let ks = KeyStore.getInstance('AndroidKeyStore')
        ks.load(null)
        ks.deleteEntry(alias)

        let expireTime = Date.$new()
        expireTime.setTime(System.currentTimeMillis().valueOf() + 80 * 365 * 24 * 60 * 60 * 1000)

        let attestationChallenge = ByteBuffer.allocate(authKey.length + 9)
        attestationChallenge.order(ByteOrder.BIG_ENDIAN.value)
        attestationChallenge.putLong(System.currentTimeMillis().valueOf() / 1000 - 15)
        attestationChallenge.put(0x1F)
        attestationChallenge.put(authKey)
        let attestationChallengeBytes = Java.array("byte", new Array(attestationChallenge.remaining()).fill(0));
        attestationChallenge.get(attestationChallengeBytes);

        let keyPairGenerator = KeyPairGenerator.getInstance('EC', 'AndroidKeyStore')
        let keySpec = KeyGenParameterSpecBuilder.$new(alias, KeyProperties.PURPOSE_SIGN.value)
                           .setDigests(Java.array('java.lang.String', [KeyProperties.DIGEST_SHA256.value, KeyProperties.DIGEST_SHA512.value]))
                           .setUserAuthenticationRequired(false)
                           .setCertificateNotAfter(expireTime)
                           .setAttestationChallenge(attestationChallengeBytes)
                           .build()
        keyPairGenerator.initialize(keySpec)
        keyPairGenerator.generateKeyPair()

        let certs = ks.getCertificateChain(alias)
        let ba = ByteArrayOutputStream.$new()
        for (let i = certs.length - 1; i >= 1; i--) {
            let encoded = certs[i].getEncoded()
            ba.write(encoded, 0, encoded.length)
        }

        let c0Hex = toHexString(certs[0].getEncoded())
        let pubHex = toHexString(authKey)
        let timeBytes = ByteBuffer.allocate(8)
                    .putLong(System.currentTimeMillis())
                    .array()
        let time = toHexString(timeBytes).substring(4)
        let pubIndex = c0Hex.indexOf(pubHex)
        let timeIndex = pubIndex + 64 + 20
        let signIndex = timeIndex + time.length + 80
        let tailIndex = signIndex + appSignature.length
        let newC0Hex = c0Hex.substring(0, timeIndex)
                        + time
                        + c0Hex.substring(timeIndex + time.length, signIndex)
                         + appSignature
                         + c0Hex.substring(tailIndex)
        let data = hexStringToByteArray(newC0Hex)
        ba.write(data, 0, data.length)

        let s = Signature.getInstance('SHA256withECDSA')
        let entry = Java.cast(ks.getEntry(alias, null), KeyStorePrivateKeyEntry)
        let privateKey = entry.getPrivateKey()
        s.initSign(privateKey)
        s.update(enc)
        ks.deleteEntry(alias)

        let encAuthKey = Base64.getUrlEncoder().encodeToString(authKey)
        let encSign = Base64.getUrlEncoder().encodeToString(s.sign())
        let encCert = Base64.getUrlEncoder().encodeToString(ba.toByteArray())
        ba.close()

        send({
            "authKey": encAuthKey,
            "signature": encSign,
            "certificate", encCert
        })
    })
    console.log("[*] Listening for cert requests")
}

Java.perform(function () {
    console.log("[*] Loading script...")
    createGpiaListener()
    createCertificateListener()
})