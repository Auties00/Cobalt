import Fastify from 'fastify'

const fastify = Fastify({
    logger: true
})

let CountdownLatch = function (limit) {
    this.limit = limit
    this.count = 0
    this.waitBlock = function () {
    }
}
CountdownLatch.prototype.countDown = function () {
    this.count = this.count + 1
    if (this.limit <= this.count) {
        return this.waitBlock()
    }
}
CountdownLatch.prototype.await = function (success) {
    this.waitBlock = success
}

let setupLatch = new CountdownLatch(2)
let integrityTokenProvider, integrityRequestType, integrityRequestBuilder

let integrityCounter = 0
let certificateCounter = 0
let infoData

Java.perform(function () {
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

    const projectId = 293955441834
    const secretKeySalt = Base64.getDecoder().decode("PkTwKSZqUfAUyR0rPQ8hYJ0wNsQQ3dW1+3SCnyTXIfEAxxS75FwkDf47wNv/c8pP3p0GXKR6OOQmhyERwx74fw1RYSU10I4r1gyBVDbRJ40pidjM41G1I1oN")
    const personalPackageId = "com.whatsapp"
    const personalServerPort = 1119
    const businessServerPort = 1120

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
        const integrityFactory = provider.overload("android.content.Context").call(context)
        const methods = integrityFactory.class.getDeclaredMethods()
        if (methods.length !== 1) {
            throw new Error('Too many methods in integrity factory: ' + methods.length)
        }

        const method = methods[0]
        return integrityFactory[method.getName()].call(integrityFactory)
    }

    function findPrepareIntegrityRequestMeta(integrityManager) {
        const prepareIntegrityManagerMethods = integrityManager.class.getDeclaredMethods()
        if (prepareIntegrityManagerMethods.length !== 1) {
            throw new Error('Too many methods in integrity manager: ' + prepareIntegrityManagerMethods.length)
        }

        const prepareIntegrityManagerMethod = prepareIntegrityManagerMethods[0]
        const prepareIntegrityManagerMethodParamTypes = prepareIntegrityManagerMethod.getParameterTypes()
        if (prepareIntegrityManagerMethodParamTypes.length !== 1) {
            throw new Error('Unexpected number of parameters: ' + prepareIntegrityManagerMethodParamTypes.length)
        }

        const prepareRequestType = prepareIntegrityManagerMethodParamTypes[0]
        const prepareRequestTypeClass = Java.use(prepareRequestType.getName())
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
                onSuccess: function (result) {
                    onSuccess(Java.cast(result, Java.use(result.$className)))
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

    function initIntegrityComponent() {
        const integrityManagerProvider = findIntegrityManagerProvider()
        const integrityManager = getIntegrityManager(integrityManagerProvider)
        const [prepareIntegrityRequestType, prepareIntegrityRequestBuilder] = findPrepareIntegrityRequestMeta(integrityManager)
        createIntegrityTokenProvider(
            integrityManager,
            prepareIntegrityRequestType,
            prepareIntegrityRequestBuilder,
            (result) => {
                const integrityManagerMethods = result.class.getDeclaredMethods()
                if (integrityManagerMethods.length !== 1) {
                    throw new Error('Too many methods in integrity manager: ' + integrityManagerMethods.length)
                }

                const integrityManagerMethod = integrityManagerMethods[0]
                const integrityManagerMethodParamTypes = integrityManagerMethod.getParameterTypes()
                if (integrityManagerMethodParamTypes.length !== 1) {
                    throw new Error('Unexpected number of parameters: ' + integrityManagerMethodParamTypes.length)
                }

                const requestType = integrityManagerMethodParamTypes[0]
                const requestTypeClass = Java.use(requestType.getName())
                const requestTypeMethods = requestType.getDeclaredMethods()
                for (const requestTypeMethod of requestTypeMethods) {
                    if (Modifier.isStatic(requestTypeMethod.getModifiers()) && requestTypeMethod.getParameterTypes().length === 0) {
                        integrityTokenProvider = result
                        integrityRequestType = requestTypeClass
                        integrityRequestBuilder = requestTypeClass[requestTypeMethod.getName()]
                        console.log("[*] Initialized integrity component")
                        setupLatch.countDown()
                        return
                    }
                }

                throw new Error('Cannot find request builder method')
            },
            (error) => {
                throw new Error('Cannot prepare integrity manager: ', error.toString(), "\n", error.stack.toString())
            }
        )
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
                    classesMd5 = md5(zipInputStream)
                } else if (zipEntry.getName().includes("about_logo.png")) {
                    aboutLogo = readZipEntry(zipInputStream)
                }
            }
        } while (zipEntry !== undefined && (classesMd5 === undefined || aboutLogo === undefined))
        zipInputStream.close()
        return [classesMd5, aboutLogo]
    }

    function intInfoComponent() {
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

        console.log("[*] Initialized info component")
        infoData = {
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
        setupLatch.countDown()
    }

    function onIntegrity(req, res) {
        let authKey = req.request.query.authKey
        try {
            let nonce = Base64.getEncoder().withoutPadding().encodeToString(Base64.getDecoder().decode(authKey))
            calculateIntegrityToken(
                integrityTokenProvider,
                integrityRequestType,
                integrityRequestBuilder,
                nonce,
                (token) => {
                    res.send({
                        "token": token
                    })
                },
                (error) => {
                    res.send({
                        "error": error.toString() + "\n" + error.stack
                    })
                }
            )
        } catch (error) {
            res.send({
                "error": error.toString() + "\n" + error.stack
            })
        }
    }

    function onCert(req, res) {
        let data = req.request.query.data
        let decodedData = Base64.getDecoder().decode(data)
        let authKey = Arrays.copyOf(decodedData, 32)
        let enc = Arrays.copyOfRange(decodedData, 32, decodedData.length)
        try {
            certificateCounter++
            let alias = "ws_cert_" + certificateCounter

            let keyPairGenerator = KeyPairGenerator.getInstance("EC", "AndroidKeyStore")

            let attestationChallenge = ByteBuffer.allocate(authKey.length + 8 + 1)
            attestationChallenge.order(ByteOrder.BIG_ENDIAN.value)
            attestationChallenge.putLong((System.currentTimeMillis()) / 1000)
            attestationChallenge.put(0x1F)
            attestationChallenge.put(authKey)
            let attestationChallengeBytes = Java.array("byte", new Array(attestationChallenge.remaining()).fill(0));
            attestationChallenge.get(attestationChallengeBytes);

            let keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            keyStore.deleteEntry(alias)

            let date = Date.$new()
            date.setTime((System.currentTimeMillis()) + (1000 * 60 * 60 * 24))
            let spec = KeyGenParameterSpecBuilder.$new(alias, KeyProperties.PURPOSE_SIGN.value)
                .setDigests(Java.array('java.lang.String', [KeyProperties.DIGEST_SHA256.value, KeyProperties.DIGEST_SHA512.value]))
                .setUserAuthenticationRequired(false)
                .setCertificateNotAfter(date)
                .setAttestationChallenge(attestationChallengeBytes)
                .build()
            keyPairGenerator.initialize(spec)
            keyPairGenerator.generateKeyPair()
            keyStore.load(null)

            let entry = keyStore.getEntry(alias, null)
            let keyStoreEntry = Java.cast(entry, KeyStorePrivateKeyEntry)
            let keyStoreEntryPrivateKey = keyStoreEntry.getPrivateKey()
            let signature = Signature.getInstance("SHA256withECDSA")
            signature.initSign(keyStoreEntryPrivateKey)
            signature.update(enc)
            let sign = signature.sign()

            let certs = keyStore.getCertificateChain(alias)
            let chain = ByteArrayOutputStream.$new()
            let firstEncodedChain = certs[2].getEncoded();
            chain.write(firstEncodedChain, 0, firstEncodedChain.length)
            let secondEncodedChain = certs[1].getEncoded();
            chain.write(secondEncodedChain, 0, secondEncodedChain.length)
            let thirdEncodedChain = certs[0].getEncoded();
            chain.write(thirdEncodedChain, 0, thirdEncodedChain.length)
            chain.close()

            let encSign = Base64.getUrlEncoder().withoutPadding().encodeToString(sign)
            let encCert = Base64.getEncoder().encodeToString(chain.toByteArray())

            res.send({
                "signature": encSign,
                "certificate": encCert
            })
        } catch (error) {
            res.send({
                "error": error.toString() + "\n" + error.stack
            })
        }
    }

    function onInfo(req, res) {
        try {
            res.send(infoData)
        } catch (error) {
            res.send({
                "error": error.toString() + "\n" + error.stack
            })
        }
    }

    console.log("[*] Initializing server components...")
    initIntegrityComponent()
    intInfoComponent()
    setupLatch.await(() => {
            console.log("[*] All server components are ready")

            const serverPort = infoData["packageName"] === personalPackageId ? personalServerPort : businessServerPort

            fastify
                .get('/gpia', (req, res) => onIntegrity(req, res))
                .get('/cert', (req, res) => onCert(req, res))
                .get('/info', (req, res) => onInfo(req, res))

            fastify.listen({port: serverPort}, (err, address) => {
                if (err) throw err
                console.log("[*] Listening at ", address)
            })
        }
    );
})