Interceptor.attach(Module.getExportByName("SharedModules", "mbedtls_gcm_update"), {
    onEnter(args) {
        console.log("[*] Called mbedtls_gcm_update", Memory.readUtf8String(args[2], args[1].toInt32()))
    }
})