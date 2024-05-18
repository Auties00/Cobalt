import http from 'http';
import url from "url";
import {nextTick} from 'node:process';

let CountdownLatch = function (limit, onSuccess) {
    this.limit = limit
    this.count = 0
    this.waitBlock = onSuccess
}
CountdownLatch.prototype.countDown = function () {
    this.count = this.count + 1
    if (this.limit <= this.count) {
        this.waitBlock()
    }
}
CountdownLatch.prototype.onSuccess = function (success) {
    this.waitBlock = success
}

function semaphore(capacity) {
    var semaphore = {
        capacity: capacity || 1,
        current: 0,
        queue: [],
        firstHere: false,

        take: function() {
            if (semaphore.firstHere === false) {
                semaphore.current++;
                semaphore.firstHere = true;
                var isFirst = 1;
            } else {
                var isFirst = 0;
            }
            var item = { n: 1 };

            if (typeof arguments[0] == 'function') {
                item.task = arguments[0];
            } else {
                item.n = arguments[0];
            }

            if (arguments.length >= 2)  {
                if (typeof arguments[1] == 'function') item.task = arguments[1];
                else item.n = arguments[1];
            }

            var task = item.task;
            item.task = function() { task(semaphore.leave); };

            if (semaphore.current + item.n - isFirst > semaphore.capacity) {
                if (isFirst === 1) {
                    semaphore.current--;
                    semaphore.firstHere = false;
                }
                return semaphore.queue.push(item);
            }

            semaphore.current += item.n - isFirst;
            item.task(semaphore.leave);
            if (isFirst === 1) semaphore.firstHere = false;
        },

        leave: function(n) {
            n = n || 1;

            semaphore.current -= n;

            if (!semaphore.queue.length) {
                if (semaphore.current < 0) {
                    throw new Error('leave called too many times.');
                }

                return;
            }

            var item = semaphore.queue[0];

            if (item.n + semaphore.current > semaphore.capacity) {
                return;
            }

            semaphore.queue.shift();
            semaphore.current += item.n;

            nextTick(item.task);
        },

        available: function(n) {
            n = n || 1;
            return(semaphore.current + n <= semaphore.capacity);
        }
    };

    return semaphore;
}


let setupLatch = new CountdownLatch(2)
let integrityTokenProvider, integrityRequestType, integrityRequestBuilder

let integrityCounter = 0
let integritySemaphore = semaphore()
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
    const Path = Java.use("java.nio.file.Path")

    const projectId = 293955441834
    const appSignature = "3987d043d10aefaf5a8710b3671418fe57e0e19b653c9df82558feb5ffce5d44"
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
            name: 'IntegrityTokenProviderHandler', implements: [OnSuccessListenerType], methods: {
                onSuccess: function (result) {
                    onSuccess(Java.cast(result, Java.use(result.$className)))
                }
            }
        })
        let onTokenProviderFailedListenerType = Java.registerClass({
            name: 'IntegrityTokenProviderErrorHandler', implements: [OnFailureListenerType], methods: {
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
            name: 'TokenSuccessHandler' + integrityCounter, implements: [OnSuccessListenerType], methods: {
                onSuccess: function (result) {
                    let javaResult = Java.cast(result, Java.use(result.$className))
                    onSuccess(javaResult.token())
                }
            }
        })
        let onIntegrityTokenErrorListenerType = Java.registerClass({
            name: 'TokenFailureHandler' + integrityCounter, implements: [OnFailureListenerType], methods: {
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
        createIntegrityTokenProvider(integrityManager, prepareIntegrityRequestType, prepareIntegrityRequestBuilder, (result) => {
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
        }, (error) => {
            throw new Error('Cannot prepare integrity manager: ', error.toString(), "\n", error.stack.toString())
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
        return Java.cast(file.toPath(), Path)
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

        infoData = {
            "packageName": packageName,
            "version": packageVersion,
            "apkPath": apkPath.toString(),
            "apkSha256": Base64.getEncoder().encodeToString(apkSha256),
            "apkShatr": Base64.getEncoder().encodeToString(apkShatr),
            "apkSize": Files.size(apkPath),
            "classesMd5": Base64.getEncoder().encodeToString(classesMd5),
            "secretKey": Base64.getEncoder().encodeToString(secretKey),
            "signature": Base64.getEncoder().encodeToString(signature),
            "signatureSha1": Base64.getEncoder().encodeToString(sha1(signature))
        }
        console.log("[*] Initialized info component")
        setupLatch.countDown()
    }

    function onIntegrity(req, res) {
        integritySemaphore.take(() => {
            let authKey = req.authKey
            try {
                let nonce = Base64.getEncoder().withoutPadding().encodeToString(Base64.getUrlDecoder().decode(authKey))
                calculateIntegrityToken(integrityTokenProvider, integrityRequestType, integrityRequestBuilder, nonce, (token) => {
                    res.end(JSON.stringify({
                        "token": token
                    }))
                    setTimeout(() => integritySemaphore.leave(1), 1000)
                }, (error) => {
                    res.end(JSON.stringify({
                        "error": error.toString() + "\n" + error.stack
                    }))
                    setTimeout(() => integritySemaphore.leave(1), 1000)
                })
            } catch (error) {
                res.end(JSON.stringify({
                    "error": error.toString() + "\n" + error.stack
                }))
                setTimeout(() => integritySemaphore.leave(1), 1000)
            }
        })
    }

    function onCert(req, res) {
        let authKey = Base64.getDecoder().decode(req.authKey)
        let enc = Base64.getDecoder().decode(req.enc)
        try {
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

            let encSign = Base64.getUrlEncoder().withoutPadding().encodeToString(s.sign())
            let encCert = Base64.getEncoder().encodeToString(ba.toByteArray())
            ba.close()

            res.end(JSON.stringify({
                "signature": encSign,
                "certificate": encCert
            }))
        } catch (error) {
            res.end(JSON.stringify({
                "error": error.toString() + "\n" + error.stack
            }))
        }
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


    function onInfo(res) {
        try {
            res.end(JSON.stringify(infoData))
        } catch (error) {
            res.end(JSON.stringify({
                "error": error.toString() + "\n" + error.stack
            }))
        }
    }

    function convertToHex(str) {
        let hex = "";
        for (let i = 0; i < str.length; i++) {
            hex += str.charCodeAt(i).toString(16) + " ";
        }
        return hex.slice(0, -1);
    }

    function convertArrayBufferToHex(buffer) {
        let hex = "";
        const view = new DataView(buffer);
        for (let i = 0; i < buffer.byteLength; i++) {
            hex += view.getUint8(i).toString(16).padStart(2, "0") + " ";
        }
        return hex.slice(0, -1);
    }


    console.log("[*] Initializing server components...")
    setupLatch.onSuccess(() => {
        console.log("[*] All server components are ready")
        const serverPort = infoData["packageName"] === personalPackageId ? personalServerPort : businessServerPort
        const server = http.createServer((req, res) => {
            let parsedRequest = url.parse(req.url, true)
                            switch (parsedRequest.pathname) {
                                case "/integrity":
                                    res.writeHead(200, {"Content-Type": "application/json"});
                                    onIntegrity(parsedRequest.query, res)
                                    break;
                                case "/cert":
                                    res.writeHead(200, {"Content-Type": "application/json"});
                                    onCert(parsedRequest.query, res)
                                    break;
                                case "/info":
                                    res.writeHead(200, {"Content-Type": "application/json"});
                                    onInfo(res)
                                    break;
                                default:
                                    res.writeHead(404, {"Content-Type": "application/json"});
                                    res.end(JSON.stringify({"error": "Unknown method"}))
                                    break;
                            }
        })

        server.listen(serverPort, () => {
            console.log("[*] Server ready on port", serverPort)
        })
    })
    initIntegrityComponent()
    intInfoComponent()
})