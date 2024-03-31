const Modifier = Java.use("java.lang.reflect.Modifier")
const projectId = 293955441834

function findIntegrityManagerProvider() {
    const loadedClasses = Java.enumerateLoadedClassesSync()
    for(const className of loadedClasses) {
        if(className.startsWith("com.google.android.play.core.integrity")) {
             var targetClass = Java.use(className)
             var methods = targetClass.class.getDeclaredMethods()
             for(var method of methods) {
                if (Modifier.isStatic(method.getModifiers())) {
                    var parameterTypes = method.getParameterTypes()
                    if (parameterTypes.length === 1 && parameterTypes[0].getName() === "android.content.Context") {
                        return targetClass[method.getName()];
                    }
                }
             }
        }
    }

    throw new Error('Cannot find IntegrityManagerFactoryCreate method');
}

function getIntegrityManager(provider) {
    const ActivityThread = Java.use('android.app.ActivityThread');
    const context = ActivityThread.currentApplication().getApplicationContext();
    console.log("[*] Loaded activity context")
    const integrityFactory = provider.overload("android.content.Context").call(context);
    const methods = integrityFactory.class.getDeclaredMethods()
    if(methods.length != 1) {
        throw new Error('Too many methods in integrity factory: ' + methods.length);
    }

    const method = methods[0]
    console.log("[*] Found integrity factory method")
    return integrityFactory[method.getName()].call(integrityFactory);
}

function findPrepareIntegrityRequestMeta(integrityManager) {
    const integrityManagerMethods = integrityManager.class.getDeclaredMethods()
    if(integrityManagerMethods.length != 1) {
        throw new Error('Too many methods in integrity manager: ' + integrityManagerMethods.length);
    }

    const integrityManagerMethod = integrityManagerMethods[0]
    console.log("[*] Found prepare integrity request method")
    const integrityManagerMethodParamTypes = integrityManagerMethod.getParameterTypes()
    if (integrityManagerMethodParamTypes.length !== 1) {
        throw new Error('Unexpected number of parameters: ' + integrityManagerMethodParamTypes.length);
    }

    const requestType = integrityManagerMethodParamTypes[0];
    const requestTypeClass = Java.use(requestType.getName())
    console.log("[*] Found prepare integrity request type")
    var requestTypeMethods = requestType.getDeclaredMethods()
    for(var requestTypeMethod of requestTypeMethods) {
        if (Modifier.isStatic(requestTypeMethod.getModifiers()) && requestTypeMethod.getParameterTypes().length == 0) {
            return [requestTypeClass, requestTypeClass[requestTypeMethod.getName()]]
        }
    }

   throw new Error('Cannot find prepare request builder method');
}

function createIntegrityTokenProvider(integrityManager, prepareIntegrityRequestType, prepareIntegrityRequestBuilder, callback) {
    const integrityRequestBuilder = prepareIntegrityRequestBuilder.overload().call(prepareIntegrityRequestType)
    integrityRequestBuilder.setCloudProjectNumber(projectId)
    const integrityRequest = integrityRequestBuilder.build()
    const result = integrityManager.prepareIntegrityToken(integrityRequest);
    const onSuccessListenerType = Java.use("com.google.android.gms.tasks.OnSuccessListener")
    const onSuccessImplementation = Java.registerClass({
        name: 'IntegrityTokenProviderHandler',
        implements: [onSuccessListenerType],
        methods: {
            onSuccess: function (integrityTokenProvider) {
                callback(integrityTokenProvider)
            }
        }
    });
    var listenerInstance = onSuccessImplementation.$new();
    result["addOnSuccessListener"].overload('com.google.android.gms.tasks.OnSuccessListener').call(result, listenerInstance)
}

function findIntegrityRequestMeta(integrityTokenProvider) {
    const integrityManagerMethods = integrityTokenProvider.class.getDeclaredMethods()
    if(integrityManagerMethods.length != 1) {
        throw new Error('Too many methods in integrity manager: ' + integrityManagerMethods.length);
    }

    const integrityManagerMethod = integrityManagerMethods[0]
    console.log("[*] Found integrity request method")
    const integrityManagerMethodParamTypes = integrityManagerMethod.getParameterTypes()
    if (integrityManagerMethodParamTypes.length !== 1) {
        throw new Error('Unexpected number of parameters: ' + integrityManagerMethodParamTypes.length);
    }

    const requestType = integrityManagerMethodParamTypes[0];
    const requestTypeClass = Java.use(requestType.getName())
    console.log("[*] Found integrity request type")
    var requestTypeMethods = requestType.getDeclaredMethods()
    for(var requestTypeMethod of requestTypeMethods) {
        if (Modifier.isStatic(requestTypeMethod.getModifiers()) && requestTypeMethod.getParameterTypes().length == 0) {
            return [requestTypeClass, requestTypeClass[requestTypeMethod.getName()]]
        }
    }

   throw new Error('Cannot find request builder method');
}

// authKey is the curve25519 public key encoded as base64 with flags DEFAULT | NO_PADDING | NO_WRAP
function calculateIntegrityToken(integrityTokenProvider, integrityRequestType, integrityRequestBuilder, authKey, callBack) {
    const integrityRequestBuilder = integrityRequestBuilder.overload().call(integrityRequestType)
    integrityRequestBuilder.setRequestHash(authKey)
    const integrityRequest = integrityRequestBuilder.build()
    const result = integrityTokenProvider.request(integrityRequest);
    const onSuccessListenerType = Java.use("com.google.android.gms.tasks.OnSuccessListener")
    const onSuccessImplementation = Java.registerClass({
        name: 'TokenHandler',
        implements: [onSuccessListenerType],
        methods: {
            onSuccess: function (result) {
                callback(result.token())
            }
        }
    });
    var listenerInstance = onSuccessImplementation.$new();
    result["addOnSuccessListener"].overload('com.google.android.gms.tasks.OnSuccessListener').call(result, listenerInstance)
}

Java.perform(function() {
    console.log("[*] Loading GPIA script...")
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
            recv(function (authKey) {
                console.log("[*] Computing token for ", authKey)
                calculateIntegrityToken(
                    integrityTokenProvider,
                    integrityRequestType,
                    integrityRequestBuilder,
                    authKey,
                    (token) {
                        console.log("[*] Computed token ", integrityToken)
                        send(token);
                    }
                );
            });
            console.log("[*] Loaded GPIA script: listening for requests")
        }
    )
})