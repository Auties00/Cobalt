Java.perform(function () {
    console.log("[*] Loading script...")

    const Modifier = Java.use("java.lang.reflect.Modifier")
    const KeyPairGenerator = Java.use('java.security.KeyPairGenerator')
    const KeyStore = Java.use('java.security.KeyStore')
    const KeyStorePrivateKeyEntry = Java.use("java.security.KeyStore$PrivateKeyEntry")
    const Signature = Java.use('java.security.Signature')
    const Base64 = Java.use('java.util.Base64')
    const Date = Java.use('java.util.Date')
    const ByteBuffer = Java.use('java.nio.ByteBuffer')
    const ByteOrder = Java.use("java.nio.ByteOrder")
    const ByteArrayOutputStream = Java.use('java.io.ByteArrayOutputStream')
    const System = Java.use('java.lang.System')
    const Arrays = Java.use('java.util.Arrays')
    const File = Java.use('java.io.File')
    const Files = Java.use('java.nio.file.Files')
    const MessageDigest = Java.use('java.security.MessageDigest')
    const ZipInputStream = Java.use("java.util.zip.ZipInputStream")
    const ActivityThread = Java.use('android.app.ActivityThread')
    const OnSuccessListenerType = Java.use("com.google.android.gms.tasks.OnSuccessListener")
    const OnFailureListenerType = Java.use("com.google.android.gms.tasks.OnFailureListener")
    const KeyGenParameterSpecBuilder = Java.use('android.security.keystore.KeyGenParameterSpec$Builder')
    const KeyProperties = Java.use('android.security.keystore.KeyProperties')
    const PackageManager = Java.use("android.content.pm.PackageManager")
    const Math = Java.use("java.lang.Math")
    const JavaString = Java.use("java.lang.String")
    const StandardCharsets = Java.use("java.nio.charset.StandardCharsets")
    const SecretKeyFactory = Java.use("javax.crypto.SecretKeyFactory")
    const PBEKeySpec = Java.use("javax.crypto.spec.PBEKeySpec")
    const Key = Java.use("java.security.Key")
    console.log("[*] Loaded types")

    const projectId = 293955441834
    const appSignature = "3987d043d10aefaf5a8710b3671418fe57e0e19b653c9df82558feb5ffce5d44"
    const secretKeySalt = Base64.getDecoder().decode("PkTwKSZqUfAUyR0rPQ8hYJ0wNsQQ3dW1+3SCnyTXIfEAxxS75FwkDf47wNv/c8pP3p0GXKR6OOQmhyERwx74fw1RYSU10I4r1gyBVDbRJ40pidjM41G1I1oN")
    console.log("[*] Loaded constants")

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
        const prepareIntegrityManagerMethods = integrityManager.class.getDeclaredMethods()
        if (prepareIntegrityManagerMethods.length !== 1) {
            throw new Error('Too many methods in integrity manager: ' + prepareIntegrityManagerMethods.length)
        }

        const prepareIntegrityManagerMethod = prepareIntegrityManagerMethods[0]
        console.log("[*] Found prepare integrity request method")
        const prepareIntegrityManagerMethodParamTypes = prepareIntegrityManagerMethod.getParameterTypes()
        if (prepareIntegrityManagerMethodParamTypes.length !== 1) {
            throw new Error('Unexpected number of parameters: ' + prepareIntegrityManagerMethodParamTypes.length)
        }

        const prepareRequestType = prepareIntegrityManagerMethodParamTypes[0]
        const prepareRequestTypeClass = Java.use(prepareRequestType.getName())
        console.log("[*] Found prepare integrity request type")
        const prepareRequestTypeMethods = prepareRequestType.getDeclaredMethods()
        for (const prepareRequestTypeMethod of prepareRequestTypeMethods) {
            if (Modifier.isStatic(prepareRequestTypeMethod.getModifiers()) && prepareRequestTypeMethod.getParameterTypes().length === 0) {
                return [prepareRequestTypeClass, prepareRequestTypeClass[prepareRequestTypeMethod.getName()]]
            }
        }

        throw new Error('Cannot find prepare request builder method')
    }

    function createIntegrityTokenProvider(integrityManager, prepareIntegrityRequestType, prepareIntegrityRequestBuilder, onSuccess, onError) {
        const integrityTokenPrepareRequestBuilder = prepareIntegrityRequestBuilder.overload().call(prepareIntegrityRequestType)
        integrityTokenPrepareRequestBuilder.setCloudProjectNumber(projectId)
        const integrityTokenPrepareRequest = integrityTokenPrepareRequestBuilder.build()
        const integrityTokenPrepareResponse = integrityManager.prepareIntegrityToken(integrityTokenPrepareRequest)
        const onTokenProviderCreatedListenerType = Java.registerClass({
            name: 'IntegrityTokenProviderHandler',
            implements: [OnSuccessListenerType],
            methods: {
                onSuccess: function (integrityTokenProvider) {
                    onSuccess(Java.cast(integrityTokenProvider, Java.use(integrityTokenProvider.$className)))
                }
            }
        })
        let onTokenProviderFailedListenerType = Java.registerClass({
            name: 'IntegrityTokenProviderErrorHandler',
            implements: [OnFailureListenerType],
            methods: {
                onFailure: function (result) {
                    let javaResult = Java.cast(result, Java.use(result.$className))
                    onError(javaResult.getMessage())
                }
            }
        })
        const onTokenProviderCreatedListener = onTokenProviderCreatedListenerType.$new()
        const onTokenProviderFailureListener = onTokenProviderFailedListenerType.$new()
        integrityTokenPrepareResponse["addOnSuccessListener"].overload('com.google.android.gms.tasks.OnSuccessListener').call(integrityTokenPrepareResponse, onTokenProviderCreatedListener)
        integrityTokenPrepareResponse["addOnFailureListener"].overload('com.google.android.gms.tasks.OnFailureListener').call(integrityTokenPrepareResponse, onTokenProviderFailureListener)
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
    let integrityCounter = 0

    function calculateIntegrityToken(integrityTokenProvider, integrityRequestType, integrityRequestBuilderMethod, authKey, onSuccess, onError) {
        integrityCounter++
        let integrityRequestBuilder = integrityRequestBuilderMethod.overload().call(integrityRequestType)
        integrityRequestBuilder.setRequestHash(authKey)
        let integrityRequest = integrityRequestBuilder.build()
        let javaIntegrityTokenProvider = Java.cast(integrityTokenProvider, Java.use(integrityTokenProvider.$className))
        let integrityTokenResponse = javaIntegrityTokenProvider.request(integrityRequest)
        let onIntegrityTokenSuccessListenerType = Java.registerClass({
            name: 'TokenSuccessHandler' + integrityCounter,
            implements: [OnSuccessListenerType],
            methods: {
                onSuccess: function (result) {
                    console.log("[*] Token wrapper type ", result.$className)
                    let javaResult = Java.cast(result, Java.use(result.$className))
                    onSuccess(javaResult.token())
                }
            }
        })
        let onIntegrityTokenErrorListenerType = Java.registerClass({
            name: 'TokenFailureHandler' + integrityCounter,
            implements: [OnFailureListenerType],
            methods: {
                onFailure: function (result) {
                    let javaResult = Java.cast(result, Java.use(result.$className))
                    onError(javaResult.getMessage())
                }
            }
        })
        let onIntegrityTokenSuccessListener = onIntegrityTokenSuccessListenerType.$new()
        let onIntegrityTokenErrorListener = onIntegrityTokenErrorListenerType.$new()
        integrityTokenResponse["addOnSuccessListener"].overload('com.google.android.gms.tasks.OnSuccessListener').call(integrityTokenResponse, onIntegrityTokenSuccessListener)
        integrityTokenResponse["addOnFailureListener"].overload('com.google.android.gms.tasks.OnFailureListener').call(integrityTokenResponse, onIntegrityTokenErrorListener)
    }

    function createGpiaListener(integrityTokenProvider, integrityRequestType, integrityRequestBuilder) {
        console.log("[*] Ready for next gpia request")
        recv("gpia", function (message) {
            createGpiaListener(integrityTokenProvider, integrityRequestType, integrityRequestBuilder)
            console.log("[*] Computing gpia token...")
            let authKey = message["authKey"]
            let nonce = Base64.getEncoder().withoutPadding().encodeToString(Base64.getDecoder().decode(authKey))
            calculateIntegrityToken(
                integrityTokenProvider,
                integrityRequestType,
                integrityRequestBuilder,
                nonce,
                (token) => {
                    console.log("[*] Finished computing gpia token")
                    send({
                        "caller": "gpia",
                        "authKey": authKey,
                        "token": token
                    })
                },
                (error) => {
                    console.log("[*] Error while computing gpia token")
                    send({
                        "caller": "gpia",
                        "authKey": authKey,
                        "type": "error",
                        "description": error
                    })
                }
            )
        })
    }

    function setupGpia() {
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
                createGpiaListener(
                    integrityTokenProvider,
                    integrityRequestType,
                    integrityRequestBuilder
                )
            },
            (error) => {
                console.log("[*] Cannot prepare integrity manager: ", error)
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

    let certificateCounter = 0

    function createCertificateListener() {
        console.log("[*] Ready for next cert request")
        recv("cert", function (message) {
            createCertificateListener()
            console.log("[*] Computing certificate...")
            let data = message["data"]
            let decodedData = Base64.getDecoder().decode(data)
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
            let newC0HexBytes = hexStringToByteArray(newC0Hex)
            ba.write(newC0HexBytes, 0, newC0HexBytes.length)

            let s = Signature.getInstance('SHA256withECDSA')
            let entry = Java.cast(ks.getEntry(alias, null), KeyStorePrivateKeyEntry)
            let privateKey = entry.getPrivateKey()
            s.initSign(privateKey)
            s.update(enc)
            ks.deleteEntry(alias)

            let encAuthKey = Base64.getEncoder().encodeToString(authKey)
            let encSign = Base64.getUrlEncoder().withoutPadding().encodeToString(s.sign())
            let encCert = Base64.getEncoder().encodeToString(ba.toByteArray())
            ba.close()

            console.log("[*] Finished computing certificate")
            send({
                "caller": "cert",
                "authKey": encAuthKey,
                "signature": encSign,
                "certificate": encCert
            })
        })
    }

    function sha256(file, length) {
        let inputStream = Files.newInputStream(file, Java.array("java.nio.file.OpenOption", new Array(0)))
        let data = Java.array("byte", new Array(4096).fill(0))
        let digest = MessageDigest.getInstance("SHA-256")
        let total = 0
        let read
        while ((read = inputStream.read(data)) !== -1 && (length === undefined || total < length)) {
            digest.update(data, 0, length === undefined ? read : Math.min(read, length - total));
            total += read
        }
        inputStream.close();
        return digest.digest();
    }

    function sha1(data) {
        let digest = MessageDigest.getInstance("SHA-1")
        digest.update(data, 0, data.length)
        return digest.digest();
    }

    function md5(inputStream) {
        let data = Java.array("byte", new Array(4096).fill(0))
        let digest = MessageDigest.getInstance("MD5")
        let read
        while ((read = inputStream.read(data, 0, data.length)) !== -1) {
            digest.update(data, 0, read);
        }
        return digest.digest();
    }


    function getApk(context) {
        let packageName = context.getPackageName()
        let rawPath = context.getPackageManager().getApplicationInfo(packageName, 0).sourceDir.value
        let file = File.$new(rawPath)
        return file.toPath()
    }

    function readZipEntry(zipInputStream) {
        let output = ByteArrayOutputStream.$new()
        let data = Java.array("byte", new Array(4096).fill(0))
        let read
        while ((read = zipInputStream.read(data, 0, data.length)) !== -1) {
            output.write(data, 0, read)
        }
        output.close()
        return output.toByteArray()
    }

    function getDataInApk(apkPath) {
        let zipInputStream = ZipInputStream.$new(Files.newInputStream(apkPath, Java.array("java.nio.file.OpenOption", new Array(0))))
        let zipEntry = undefined
        let classesMd5 = undefined
        let aboutLogo = undefined
        do {
            zipEntry = zipInputStream.getNextEntry()
            if (zipEntry != null) {
                if (zipEntry.getName().includes("classes.dex")) {
                    console.log("[*] Found classes.dex: ", zipEntry.getName())
                    classesMd5 = md5(zipInputStream)
                } else if (zipEntry.getName().includes("about_logo.png")) {
                    console.log("[*] Found about_logo.png: ", zipEntry.getName())
                    aboutLogo = readZipEntry(zipInputStream)
                }
            }
        } while (zipEntry !== undefined && (classesMd5 === undefined || aboutLogo === undefined))
        zipInputStream.close()
        return [classesMd5, aboutLogo]
    }

    let infoData = undefined

    function computeInfo() {
        console.log("[*] Computing info...")
        let context = ActivityThread.currentApplication().getApplicationContext()
        let packageName = context.getPackageName()

        let packageInfo = context.getPackageManager().getPackageInfo(packageName, 0)
        let packageVersion = packageInfo.versionName.value

        let apkPath = getApk(context)
        let apkSha256 = sha256(apkPath)
        let apkShatr = sha256(apkPath, 10 * 1024 * 1024)
        let [classesMd5, aboutLogo] = getDataInApk(apkPath)
        if (classesMd5 === undefined || aboutLogo === undefined) {
            throw new Error("Incomplete apk data")
        }

        let packageNameBytes = JavaString.$new(packageName).getBytes(StandardCharsets.UTF_8.value)
        let password = Java.array("byte", new Array(packageNameBytes.length + aboutLogo.length).fill(0))
        System.arraycopy(packageNameBytes, 0, password, 0, packageNameBytes.length)
        System.arraycopy(aboutLogo, 0, password, packageNameBytes.length, aboutLogo.length)
        let passwordChars = Java.array("char", new Array(password.length).fill(''))
        for (let i = 0; i < passwordChars.length; i++) {
            passwordChars[i] = String.fromCharCode(password[i] & 0xFF);
        }

        let factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1And8BIT")
        let key = PBEKeySpec.$new(passwordChars, secretKeySalt, 128, 512)
        let secretKey = Java.cast(factory.generateSecret(key), Key).getEncoded()

        let signatures = context.getPackageManager().getPackageInfo(packageName, PackageManager.GET_SIGNATURES.value).signatures.value
        if (signatures.length !== 1) {
            throw new Error("Unexpected number of signatures: ", signatures.length)
        }
        let signature = signatures[0].toByteArray()

        console.log("[*] Finished computing info")
        infoData = {
            "type": "success",
            "caller": "info",
            "packageName": packageName,
            "version": packageVersion,
            "apkSha256": Base64.getEncoder().encodeToString(apkSha256),
            "apkShatr": Base64.getEncoder().encodeToString(apkShatr),
            "apkSize": Files.size(apkPath),
            "classesMd5": Base64.getEncoder().encodeToString(classesMd5),
            "secretKey": Base64.getEncoder().encodeToString(secretKey),
            "signature": Base64.getEncoder().encodeToString(signature),
            "signatureSha1": Base64.getEncoder().encodeToString(sha1(signature))
        }
    }

    function createInfoListener() {
        console.log("[*] Ready for next info request")

        recv("info", function (message) {
            createInfoListener()
            try {
                let messageId = message["id"]
                if (infoData === undefined) {
                    computeInfo();
                }

                send({
                    "id": messageId,
                    ...infoData
                })
            } catch (error) {
                console.log("[*] An error occurred while computing info")
                console.log(error.stack)
                send({
                    "id": messageId,
                    "caller": "info",
                    "type": "error",
                    "description": error.toString()
                })
            }
        })
    }

    console.log("[*] Loaded methods")

    setupGpia()
    createInfoListener()
    createCertificateListener()
    console.log("[*] Loaded script")
})