package com.whatsapp.w4b.server

import android.content.Context
import com.whatsapp.w4b.generator.generateCertificate
import com.whatsapp.w4b.generator.generateIntegrityToken
import com.whatsapp.w4b.result.ServerResult
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respond
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import java.util.Base64

const val SERVER_PORT = 8080

fun startServer(context: Context) {
    val server = embeddedServer(Netty, SERVER_PORT) {
        install(ContentNegotiation) {
            json()
        }
        routing {
            get("/") {
                val authKey = Base64.getUrlDecoder().decode(call.request.queryParameters["authKey"])
                val enc = call.request.queryParameters["enc"]!!
                val certificate = generateCertificate(authKey, enc)
                val integrityToken = generateIntegrityToken(authKey, context)
                val result = ServerResult(certificate.authHeader, certificate.signatureHeader, integrityToken.token)
                call.respond(result)
            }
        }
    }
    server.start()
}
