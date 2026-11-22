package com.myvpn.app

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.wireguard.android.backend.BackendException
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream

private val Bg = Color(0xFF0B0F14)
private val Panel = Color(0xFF151C24)
private val Green = Color(0xFF42D6A4)
private val Muted = Color(0xFF91A0AE)

class MainActivity : ComponentActivity() {
    private val backend by lazy { GoBackend(this) }
    private val tunnel = MyTunnel()
    private var configText by mutableStateOf<String?>(null)
    private var connected by mutableStateOf(false)
    private var status by mutableStateOf("Not connected")
    private var busy by mutableStateOf(false)

    private val configPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                        ?: error("Could not read selected file")
                }
            }.onSuccess { text ->
                configText = text
                status = "Configuration loaded"
            }.onFailure { error ->
                status = error.message ?: "Could not read configuration"
            }
        }
    }

    private val vpnPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) startTunnel()
        else status = "VPN permission was not granted"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyVpnScreen(
                connected = connected,
                busy = busy,
                status = status,
                hasConfig = configText != null,
                onImport = { configPicker.launch(arrayOf("text/*", "application/octet-stream")) },
                onToggle = { toggleTunnel() }
            )
        }
    }

    private fun toggleTunnel() {
        if (busy) return
        if (connected) {
            busy = true
            lifecycleScope.launch(Dispatchers.IO) {
                runCatching { backend.setState(tunnel, Tunnel.State.DOWN, null) }
                withContext(Dispatchers.Main) {
                    connected = false
                    busy = false
                    status = "Disconnected"
                }
            }
            return
        }

        val raw = configText
        if (raw.isNullOrBlank()) {
            status = "Import your WireGuard .conf file first"
            return
        }
        val permissionIntent: Intent? = VpnService.prepare(this)
        if (permissionIntent != null) vpnPermission.launch(permissionIntent) else startTunnel()
    }

    private fun startTunnel() {
        val raw = configText ?: return
        busy = true
        status = "Connecting…"
        lifecycleScope.launch(Dispatchers.IO) {
            val result = runCatching {
                val config = Config.parse(ByteArrayInputStream(raw.toByteArray(Charsets.UTF_8)))
                backend.setState(tunnel, Tunnel.State.UP, config)
            }
            withContext(Dispatchers.Main) {
                busy = false
                result.onSuccess {
                    connected = true
                    status = "WireGuard tunnel is up"
                }.onFailure { error ->
                    connected = false
                    status = error.message ?: "Connection failed"
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Do not tear down the tunnel when the UI Activity is recreated or backgrounded.
    }
}

private class MyTunnel : Tunnel {
    override fun getName(): String = "MyVPN"
    override fun onStateChange(newState: Tunnel.State) = Unit
}

@Composable
private fun MyVpnScreen(
    connected: Boolean,
    busy: Boolean,
    status: String,
    hasConfig: Boolean,
    onImport: () -> Unit,
    onToggle: () -> Unit
) {
    MaterialTheme(colorScheme = darkColorScheme(
        background = Bg, surface = Panel, primary = Green,
        onBackground = Color.White, onSurface = Color.White
    )) {
        Surface(Modifier.fillMaxSize(), color = Bg) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(16.dp), color = Panel) {
                        Icon(Icons.Default.Security, null, tint = Green,
                            modifier = Modifier.padding(13.dp).size(27.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("My VPN", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("PRIVATE WIREGUARD CONNECTION", color = Muted, fontSize = 10.sp, letterSpacing = 1.sp)
                    }
                    Surface(color = Color(0xFF19352E), shape = CircleShape) {
                        Text("BETA", color = Green, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                    }
                }

                Spacer(Modifier.height(48.dp))
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(210.dp)) {
                    Surface(shape = CircleShape, color = Color(0xFF12251F), modifier = Modifier.fillMaxSize()) {}
                    Surface(shape = CircleShape, color = Color(0xFF19372E), modifier = Modifier.size(166.dp)) {}
                    Surface(shape = CircleShape, color = Green, modifier = Modifier.size(120.dp)) {
                        Icon(Icons.Default.Security, null, tint = Color(0xFF092018),
                            modifier = Modifier.padding(34.dp).fillMaxSize())
                    }
                }
                Spacer(Modifier.height(22.dp))
                Text(if (connected) "Protected" else "Your connection", fontSize = 23.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(7.dp))
                Text(status, color = Muted, fontSize = 13.sp)
                Spacer(Modifier.height(28.dp))

                Button(
                    onClick = onToggle,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (connected) Color(0xFF26333C) else Green,
                        contentColor = if (connected) Color.White else Color(0xFF071710))
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text(if (connected) "DISCONNECT" else "CONNECT", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }

                Spacer(Modifier.height(24.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp)) {
                        Text("VPN SERVER", color = Muted, fontSize = 11.sp, letterSpacing = 1.sp)
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("My private server", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text("WireGuard", color = Green, fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(if (hasConfig) "Configuration imported" else "No configuration added", color = Muted, fontSize = 12.sp)
                        Spacer(Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = onImport,
                            enabled = !busy && !connected,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.UploadFile, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Add WireGuard config")
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                Text("MY VPN  •  PRIVATE BY DESIGN", color = Color(0xFF53616E), fontSize = 10.sp, letterSpacing = 1.2.sp)
            }
        }
    }
}
