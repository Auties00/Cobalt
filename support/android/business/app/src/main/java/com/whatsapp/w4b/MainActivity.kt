package com.whatsapp.w4b

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.whatsapp.w4b.crypto.sha256
import com.whatsapp.w4b.server.SERVER_PORT
import com.whatsapp.w4b.server.startServer
import com.whatsapp.w4b.ui.theme.WhatsAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startServer(baseContext)
        setContent {
            WhatsAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ServerInfo()
                    AppSignatureInfo(baseContext)
                }
            }
        }
    }
}

@Composable
fun ServerInfo() {
    Text(
        text = "Server running on port $SERVER_PORT"
    )
}

@Suppress("DEPRECATION")
@SuppressLint("PackageManagerGetSignatures")
@Composable
fun AppSignatureInfo(context: Context) {
    Text(
        text = "App signature: ${sha256(context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES).signatures[0].toByteArray())}"
    )
}