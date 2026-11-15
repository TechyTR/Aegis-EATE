package com.nevruz.aegis.core

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aegisguard.core.privilege.PrivilegeManager
import com.aegisguard.core.privilege.PrivilegeTier

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ControlPanelScreen()
                }
            }
        }
    }
}

@Composable
fun ControlPanelScreen() {
    val context = LocalContext.current
    var isSpoofingActive by remember { mutableStateOf(true) }
    var useRoot by remember { mutableStateOf(false) }
    var useShizuku by remember { mutableStateOf(false) }

    val activeTier = PrivilegeManager.getActiveTier(useRoot, useShizuku)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("AegisGuard (VPN'siz)", style = MaterialTheme.typography.headlineMedium)

        AssistChip(
            onClick = { },
            label = {
                Text(
                    when (activeTier) {
                        PrivilegeTier.STANDARD -> "Mod: Standart Kalkan (VPN & Root Yok)"
                        PrivilegeTier.SHIZUKU -> "Mod: Shizuku Sistem Koruması"
                        PrivilegeTier.ROOT -> "Mod: Root (SU) Transparan Koruma"
                    }
                )
            }
        )

        // 1. Ana Koruma (Erişilebilirlik Servisi - VPN Kullanmaz)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Gerçek Zamanlı Web Kalkanı", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Mevcut VPN bağlantınızı bozmadan ziyaret ettiğiniz siteleri arka planda analiz eder.",
                    style = MaterialTheme.typography.bodySmall
                )
                Button(
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Arka Plan Kalkanını Etkinleştir")
                }
            }
        }

        // 2. Sahte Cihaz Kimliği Anahtarı
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Sahte Cihaz Kimliği (Spoofing)", style = MaterialTheme.typography.titleMedium)
                    Text("Gerçek cihaz bilgilerini gizleyerek tarar", style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = isSpoofingActive,
                    onCheckedChange = { isSpoofingActive = it }
                )
            }
        }

        HorizontalDivider()
        Text("Opsiyonel Derin Sistem Modülleri", style = MaterialTheme.typography.titleSmall)

        // 3. Shizuku Modülü
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Shizuku Entegrasyonu", style = MaterialTheme.typography.titleMedium)
                    Text("Yerel proxy ve pil kısıtlaması muafiyeti", style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = useShizuku,
                    onCheckedChange = { wantShizuku ->
                        if (wantShizuku) {
                            if (PrivilegeManager.isShizukuAvailable()) {
                                useShizuku = true
                                PrivilegeManager.applyEnhancedProtections(context, PrivilegeTier.SHIZUKU, true)
                            } else {
                                PrivilegeManager.requestShizukuPermission(1001)
                            }
                        } else {
                            useShizuku = false
                            PrivilegeManager.applyEnhancedProtections(context, PrivilegeTier.SHIZUKU, false)
                        }
                    }
                )
            }
        }

        // 4. Root (SU) Modülü
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Root (SU) Çekirdek Koruması", style = MaterialTheme.typography.titleMedium)
                    Text("VPN'e dokunmadan transparan trafik süzme", style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = useRoot,
                    onCheckedChange = { wantRoot ->
                        val granted = if (wantRoot) PrivilegeManager.hasRootAccess() else false
                        useRoot = granted
                        PrivilegeManager.applyEnhancedProtections(context, PrivilegeTier.ROOT, granted)
                    }
                )
            }
        }
    }
}
