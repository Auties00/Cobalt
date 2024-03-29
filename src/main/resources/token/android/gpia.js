console.log("[*] Loading GPIA script...")
const IntegrityManagerFactory = Java.use('com.google.android.play.core.integrity.IntegrityManagerFactory');
const IntegrityTokenRequest = Java.use('com.google.android.play.core.integrity.IntegrityTokenRequest');
const Context = Java.use('android.content.Context');
const ByteString = Java.use('com.google.protobuf.ByteString');
console.log("[*] Loaded GPIA script")
recv(function (authKey) {
    Java.perform(function() {
        const context = Java.use('android.app.ActivityThread').currentApplication().getApplicationContext();
        const integrityManager = IntegrityManagerFactory.create(context.$handle);
        const nonce = ByteString.copyFromUtf8(authKey);
        const requestBuilder = IntegrityTokenRequest.builder();
        requestBuilder.setNonce(nonce);
        const request = requestBuilder.build();
        const integrityTokenResponse = integrityManager.requestIntegrityToken(request);
        send(integrityTokenResponse.result().token());
    });
});